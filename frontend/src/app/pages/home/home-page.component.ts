import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { SessionStore } from '../../core/services/session-store.service';
import {
  ApplianceCategory,
  CategoryChoice,
  IssueCode,
  IssueOption,
  VisionSuggestResponse
} from '../../core/models';

@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss']
})
export class HomePageComponent {
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  readonly categories: CategoryChoice[] = [
    { value: 'WASHING_MACHINE', label: 'Lave-linge', hint: 'Hublot ou top' },
    { value: 'DISHWASHER', label: 'Lave-vaisselle', hint: 'Pose libre ou intégré' },
    { value: 'OVEN', label: 'Four', hint: 'Encastrable ou solo' },
    { value: 'UNSUPPORTED', label: 'Autre', hint: 'Pas encore couvert' }
  ];

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  uploadedMediaId: string | null = null;
  selectedCategory: ApplianceCategory | null = null;
  selectedIssue: IssueCode | null = null;
  issues: IssueOption[] = [];
  suggestion: VisionSuggestResponse | null = null;
  loadingIssues = false;
  suggesting = false;
  loading = false;
  error: string | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly store: SessionStore,
    private readonly router: Router
  ) {}

  get canSubmit(): boolean {
    if (!this.selectedFile || !this.uploadedMediaId || !this.selectedCategory || this.loading || this.suggesting) {
      return false;
    }
    if (this.selectedCategory === 'UNSUPPORTED') {
      return true;
    }
    return !!this.selectedIssue;
  }

  openPicker(): void {
    this.fileInput?.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.error = null;
    this.selectedFile = file;
    this.uploadedMediaId = null;
    this.suggestion = null;
    this.selectedCategory = null;
    this.selectedIssue = null;
    this.issues = [];
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }
    this.previewUrl = URL.createObjectURL(file);
    this.uploadAndSuggest(file);
  }

  selectCategory(category: ApplianceCategory): void {
    this.selectedCategory = category;
    this.selectedIssue = null;
    this.issues = [];
    this.error = null;

    if (category === 'UNSUPPORTED') {
      return;
    }

    this.loadingIssues = true;
    this.api.getIssues(category).subscribe({
      next: (issues) => {
        this.issues = issues;
        this.loadingIssues = false;
        const preferred = this.suggestion?.suggestedIssueCode;
        const match = preferred ? issues.find(i => i.code === preferred) : undefined;
        const unknown = issues.find(i => i.code.endsWith('_UNKNOWN'));
        this.selectedIssue = match?.code ?? unknown?.code ?? issues[0]?.code ?? null;
      },
      error: () => {
        this.loadingIssues = false;
        this.error = 'Impossible de charger les types de panne.';
      }
    });
  }

  selectIssue(code: IssueCode): void {
    this.selectedIssue = code;
  }

  analyze(): void {
    if (!this.canSubmit || !this.uploadedMediaId || !this.selectedCategory) {
      return;
    }
    this.loading = true;
    this.error = null;

    const category = this.selectedCategory;
    const issueCode = category === 'UNSUPPORTED' ? null : this.selectedIssue;

    this.api.createDiagnosis(this.uploadedMediaId, category, issueCode).pipe(
      switchMap(diagnosis => {
        const repairers$ = diagnosis.supported
          ? this.api.getRepairers(diagnosis.category, 'Lyon')
          : of([]);
        return forkJoin({ diagnosis: of(diagnosis), repairers: repairers$ });
      })
    ).subscribe({
      next: ({ diagnosis, repairers }) => {
        this.store.setResult(diagnosis, this.previewUrl!, repairers);
        this.loading = false;
        void this.router.navigateByUrl('/resultat');
      },
      error: (err) => this.handleError(err)
    });
  }

  private uploadAndSuggest(file: File): void {
    this.suggesting = true;
    this.api.uploadMedia(file).pipe(
      switchMap(media => {
        this.uploadedMediaId = media.mediaId;
        return this.api.suggestDiagnosis(media.mediaId);
      })
    ).subscribe({
      next: (suggestion) => {
        this.suggestion = suggestion;
        this.suggesting = false;
        this.selectCategory(suggestion.category);
      },
      error: (err) => {
        this.suggesting = false;
        // Upload OK but suggestion failed: user can still choose manually
        if (this.uploadedMediaId) {
          this.error = 'Suggestion indisponible — choisis l’appareil toi-même.';
          return;
        }
        this.handleError(err);
      }
    });
  }

  private handleError(err: any): void {
    this.loading = false;
    this.suggesting = false;
    const apiMessage = err?.error?.message;
    if (apiMessage) {
      this.error = apiMessage;
      return;
    }
    if (err?.status === 0 || err?.status === 404) {
      this.error = 'Service indisponible pour le moment. Réessaie dans un instant.';
      return;
    }
    this.error = 'Impossible d’utiliser cette photo. Réessaie avec une autre image.';
  }
}
