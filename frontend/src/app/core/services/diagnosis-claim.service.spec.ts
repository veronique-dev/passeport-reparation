import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { DiagnosisClaimService } from './diagnosis-claim.service';
import { ApiService } from './api.service';
import { SessionStore } from './session-store.service';
import { DiagnosisResponse } from '../models';

describe('DiagnosisClaimService', () => {
  let service: DiagnosisClaimService;
  let api: jasmine.SpyObj<ApiService>;
  let store: jasmine.SpyObj<SessionStore>;

  const claimed: DiagnosisResponse = {
    id: 'd1',
    mediaId: 'm1',
    category: 'OVEN',
    applianceLabel: 'Four',
    probableIssue: 'Joint',
    confidence: 1,
    estimate: null,
    verdict: 'REPAIR',
    disclaimer: 'indicative',
    supported: true,
    userId: 'u1'
  };

  beforeEach(() => {
    api = jasmine.createSpyObj('ApiService', ['claimDiagnosis']);
    store = jasmine.createSpyObj('SessionStore', [
      'getPendingAnonymousDiagnosisId',
      'clearPendingAnonymousDiagnosis',
      'getDiagnosis',
      'getPreviewUrl',
      'getRepairers',
      'setResult'
    ]);

    TestBed.configureTestingModule({
      providers: [
        DiagnosisClaimService,
        { provide: ApiService, useValue: api },
        { provide: SessionStore, useValue: store }
      ]
    });
    service = TestBed.inject(DiagnosisClaimService);
  });

  it('returns null when nothing pending', (done) => {
    store.getPendingAnonymousDiagnosisId.and.returnValue(null);
    service.claimPendingAfterLogin().subscribe(result => {
      expect(result).toBeNull();
      expect(api.claimDiagnosis).not.toHaveBeenCalled();
      done();
    });
  });

  it('claims pending diagnosis and clears pending id', (done) => {
    store.getPendingAnonymousDiagnosisId.and.returnValue('d1');
    store.getDiagnosis.and.returnValue({ ...claimed, userId: undefined });
    store.getPreviewUrl.and.returnValue('blob:x');
    store.getRepairers.and.returnValue([]);
    api.claimDiagnosis.and.returnValue(of(claimed));

    service.claimPendingAfterLogin().subscribe(result => {
      expect(result).toEqual(claimed);
      expect(api.claimDiagnosis).toHaveBeenCalledWith('d1');
      expect(store.clearPendingAnonymousDiagnosis).toHaveBeenCalled();
      expect(store.setResult).toHaveBeenCalled();
      done();
    });
  });

  it('clears pending id when claim fails', (done) => {
    store.getPendingAnonymousDiagnosisId.and.returnValue('d1');
    api.claimDiagnosis.and.returnValue(throwError(() => ({ status: 409 })));

    service.claimPendingAfterLogin().subscribe(result => {
      expect(result).toBeNull();
      expect(store.clearPendingAnonymousDiagnosis).toHaveBeenCalled();
      done();
    });
  });
});
