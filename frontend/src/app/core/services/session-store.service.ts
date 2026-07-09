import { Injectable } from '@angular/core';
import { DiagnosisResponse, Repairer } from '../models';

@Injectable({ providedIn: 'root' })
export class SessionStore {
  private diagnosis: DiagnosisResponse | null = null;
  private previewUrl: string | null = null;
  private repairers: Repairer[] = [];

  setResult(diagnosis: DiagnosisResponse, previewUrl: string, repairers: Repairer[]): void {
    this.diagnosis = diagnosis;
    this.previewUrl = previewUrl;
    this.repairers = repairers;
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

  clear(): void {
    this.diagnosis = null;
    this.previewUrl = null;
    this.repairers = [];
  }
}
