import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';
import { environment } from '../../../environments/environment';

describe('ApiService', () => {
  let service: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists issues for a category', () => {
    service.getIssues('OVEN').subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/diagnoses/issues?category=OVEN`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('creates a diagnosis', () => {
    service.createDiagnosis('m1', 'OVEN', 'OV_DOOR_SEAL').subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/diagnoses`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      mediaId: 'm1',
      category: 'OVEN',
      issueCode: 'OV_DOOR_SEAL',
      latitude: undefined,
      longitude: undefined
    });
    req.flush({ id: 'd1' });
  });

  it('claims a diagnosis', () => {
    service.claimDiagnosis('d1').subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/diagnoses/d1/claim`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'd1', userId: 'u1' });
  });

  it('lists my diagnoses', () => {
    service.getMyDiagnoses().subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/diagnoses/mine`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('lists repairers with city filter', () => {
    service.getRepairers('OVEN', 'Lyon').subscribe();
    const req = http.expectOne(
      r => r.url === `${environment.apiBaseUrl}/api/repairers` && r.params.get('category') === 'OVEN'
    );
    expect(req.request.params.get('city')).toBe('Lyon');
    req.flush([]);
  });
});
