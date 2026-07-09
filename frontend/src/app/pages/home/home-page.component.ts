import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { SessionStore } from '../../core/services/session-store.service';

@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss']
})
export class HomePageComponent {
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly store: SessionStore,
    private readonly router: Router
  ) {}

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

  analyze(): void {
    if (!this.selectedFile || !this.previewUrl) {
      return;
    }
    this.loading = true;
    this.error = null;

    const coords$ = of(null as GeolocationPosition | null);

    coords$.pipe(
      switchMap(() => this.api.uploadMedia(this.selectedFile!)),
      switchMap(media =>
        this.api.createDiagnosis(media.mediaId).pipe(
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
        this.error = err?.error?.message || 'Impossible d’analyser la photo. Réessaie.';
      }
    });
  }
}
