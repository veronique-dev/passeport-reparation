import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { SessionStore } from '../../core/services/session-store.service';
import { DiagnosisResponse } from '../../core/models';

@Component({
  selector: 'app-passports-page',
  templateUrl: './passports-page.component.html',
  styleUrls: ['./passports-page.component.scss']
})
export class PassportsPageComponent implements OnInit {
  items: DiagnosisResponse[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly store: SessionStore,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.api.getMyDiagnoses().subscribe({
      next: (items) => {
        this.items = items;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Impossible de charger tes passeports.';
      }
    });
  }

  open(item: DiagnosisResponse): void {
    const repairers$ = item.supported
      ? this.api.getRepairers(item.category, 'Lyon')
      : of([]);
    forkJoin({ diagnosis: of(item), repairers: repairers$ }).subscribe({
      next: ({ diagnosis, repairers }) => {
        this.store.setResult(diagnosis, '', repairers);
        void this.router.navigateByUrl('/resultat');
      }
    });
  }

  verdictLabel(verdict: string | null | undefined): string {
    switch (verdict) {
      case 'REPAIR': return 'Réparer';
      case 'ARBITRATE': return 'À arbitrer';
      case 'REPLACE': return 'Remplacer';
      default: return '—';
    }
  }
}
