import { Injectable } from '@angular/core';
import { DiagnosisResponse, Repairer } from '../models';

const PENDING_ANONYMOUS_DIAGNOSIS_KEY = 'pr_pending_anonymous_diagnosis_id';

@Injectable({ providedIn: 'root' })
export class SessionStore {
  private diagnosis: DiagnosisResponse | null = null;
  private previewUrl: string | null = null;
  private repairers: Repairer[] = [];

  setResult(diagnosis: DiagnosisResponse, previewUrl: string, repairers: Repairer[]): void {
    this.diagnosis = diagnosis;
    this.previewUrl = previewUrl;
    this.repairers = repairers;

    if (diagnosis?.id && !diagnosis.userId) {
      this.rememberAnonymousDiagnosis(diagnosis.id);
    } else {
      this.clearPendingAnonymousDiagnosis();
    }
  }

  getDiagnosis(): DiagnosisResponse | null {
    return this.diagnosis;
  }

  getPreviewUrl(): string | null {
    return this.previewUrl;
  }

  getRepairers(): Repairer[] {
    return this.repairers;
  }

  getPendingAnonymousDiagnosisId(): string | null {
    return localStorage.getItem(PENDING_ANONYMOUS_DIAGNOSIS_KEY);
  }

  rememberAnonymousDiagnosis(id: string): void {
    localStorage.setItem(PENDING_ANONYMOUS_DIAGNOSIS_KEY, id);
  }

  clearPendingAnonymousDiagnosis(): void {
    localStorage.removeItem(PENDING_ANONYMOUS_DIAGNOSIS_KEY);
  }

  clear(): void {
    this.diagnosis = null;
    this.previewUrl = null;
    this.repairers = [];
  }
}
