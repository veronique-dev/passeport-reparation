import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { ApiService } from './api.service';
import { SessionStore } from './session-store.service';
import { DiagnosisResponse } from '../models';

/**
 * US-12 — rattache le dernier passeport anonyme après login.
 */
@Injectable({ providedIn: 'root' })
export class DiagnosisClaimService {
  constructor(
    private readonly api: ApiService,
    private readonly store: SessionStore
  ) {}

  claimPendingAfterLogin(): Observable<DiagnosisResponse | null> {
    const pendingId = this.store.getPendingAnonymousDiagnosisId();
    if (!pendingId) {
      return of(null);
    }

    return this.api.claimDiagnosis(pendingId).pipe(
      tap(diagnosis => {
        this.store.clearPendingAnonymousDiagnosis();
        const current = this.store.getDiagnosis();
        if (current?.id === diagnosis.id) {
          this.store.setResult(diagnosis, this.store.getPreviewUrl() || '', this.store.getRepairers());
        }
      }),
      map(diagnosis => diagnosis),
      catchError(() => {
        // Claim failed (already owned / gone): drop pending to avoid retry loops
        this.store.clearPendingAnonymousDiagnosis();
        return of(null);
      })
    );
  }
}
