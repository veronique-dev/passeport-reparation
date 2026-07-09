import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { SessionStore } from '../../core/services/session-store.service';
import { ApplianceCategory, CategoryChoice, IssueCode, IssueOption } from '../../core/models';

@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss']
})
export class HomePageComponent {
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  readonly categories: CategoryChoice[] = [
    { value: 'WASHING_MACHINE', label: 'Lave-linge', hint: 'Hublot / top' },
    { value: 'DISHWASHER', label: 'Lave-vaisselle', hint: 'Pose libre / intégré' },
    { value: 'OVEN', label: 'Four', hint: 'Encastrable / solo' },
    { value: 'UNSUPPORTED', label: 'Autre', hint: 'Hors périmètre MVP' }
  ];

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  selectedCategory: ApplianceCategory | null = null;
  selectedIssue: IssueCode | null = null;
  issues: IssueOption[] = [];
  loadingIssues = false;
  loading = false;
  error: string | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly store: SessionStore,
    private readonly router: Router
  ) {}

  get canSubmit(): boolean {
    if (!this.selectedFile || !this.selectedCategory || this.loading) {
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
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }
    this.previewUrl = URL.createObjectURL(file);
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
        const unknown = issues.find(i => i.code.endsWith('_UNKNOWN'));
        this.selectedIssue = unknown?.code ?? issues[0]?.code ?? null;
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
    if (!this.canSubmit || !this.selectedFile || !this.selectedCategory) {
      return;
    }
    this.loading = true;
    this.error = null;

    const category = this.selectedCategory;
    const issueCode = category === 'UNSUPPORTED' ? null : this.selectedIssue;

    this.api.uploadMedia(this.selectedFile).pipe(
      switchMap(media =>
        this.api.createDiagnosis(media.mediaId, category, issueCode).pipe(
          switchMap(diagnosis => {
            const repairers$ = diagnosis.supported
              ? this.api.getRepairers(diagnosis.category, 'Lyon')
              : of([]);
            return forkJoin({ diagnosis: of(diagnosis), repairers: repairers$ });
          })
        )
      )
    ).subscribe({
      next: ({ diagnosis, repairers }) => {
        this.store.setResult(diagnosis, this.previewUrl!, repairers);
        this.loading = false;
        void this.router.navigateByUrl('/resultat');
      },
      error: (err) => {
        this.loading = false;
        const apiMessage = err?.error?.message;
        if (apiMessage) {
          this.error = apiMessage;
          return;
        }
        if (err?.status === 0 || err?.status === 404) {
          this.error = 'API indisponible. Vérifie que la gateway tourne sur http://localhost:8090.';
          return;
        }
        this.error = 'Impossible d’analyser la photo. Réessaie.';
      }
    });
  }
}
