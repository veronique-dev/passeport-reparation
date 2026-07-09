import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DiagnosisResponse, Repairer, RepairVerdict } from '../../core/models';
import { SessionStore } from '../../core/services/session-store.service';

@Component({
  selector: 'app-result-page',
  templateUrl: './result-page.component.html',
  styleUrls: ['./result-page.component.scss']
})
export class ResultPageComponent implements OnInit {
  diagnosis: DiagnosisResponse | null = null;
  previewUrl: string | null = null;
  repairers: Repairer[] = [];

  constructor(
    private readonly store: SessionStore,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.diagnosis = this.store.getDiagnosis();
    this.previewUrl = this.store.getPreviewUrl();
    this.repairers = this.store.getRepairers();
    if (!this.diagnosis) {
      void this.router.navigateByUrl('/');
    }
  }

  verdictLabel(verdict: RepairVerdict | null | undefined): string {
    switch (verdict) {
      case 'REPAIR': return 'Réparer';
      case 'ARBITRATE': return 'À arbitrer';
      case 'REPLACE': return 'Remplacer';
      default: return '—';
    }
  }

  telHref(phone?: string): string {
    return phone ? `tel:${phone}` : '#';
  }

  mailHref(email?: string): string {
    return email ? `mailto:${email}` : '#';
  }

  waHref(whatsapp?: string): string {
    if (!whatsapp) {
      return '#';
    }
    const digits = whatsapp.replace(/\D/g, '');
    return `https://wa.me/${digits}`;
  }

  restart(): void {
    this.store.clear();
    void this.router.navigateByUrl('/');
  }
}
