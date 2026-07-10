import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { PassportsPageComponent } from './passports-page.component';
import { ApiService } from '../../core/services/api.service';
import { SessionStore } from '../../core/services/session-store.service';
import { DiagnosisResponse } from '../../core/models';

describe('PassportsPageComponent', () => {
  let component: PassportsPageComponent;
  let fixture: ComponentFixture<PassportsPageComponent>;
  let api: jasmine.SpyObj<ApiService>;
  let store: jasmine.SpyObj<SessionStore>;
  let router: jasmine.SpyObj<Router>;

  const items: DiagnosisResponse[] = [
    {
      id: 'old',
      mediaId: 'm1',
      category: 'OVEN',
      applianceLabel: 'Four',
      probableIssue: 'Joint',
      confidence: 1,
      estimate: null,
      verdict: 'REPAIR',
      disclaimer: 'x',
      supported: true,
      createdAt: '2026-07-01T10:00:00Z'
    },
    {
      id: 'new',
      mediaId: 'm2',
      category: 'WASHING_MACHINE',
      applianceLabel: 'Lave-linge',
      probableIssue: 'Pompe',
      confidence: 1,
      estimate: null,
      verdict: 'ARBITRATE',
      disclaimer: 'x',
      supported: true,
      createdAt: '2026-07-10T15:30:00Z'
    }
  ];

  beforeEach(async () => {
    api = jasmine.createSpyObj('ApiService', ['getMyDiagnoses', 'getRepairers']);
    store = jasmine.createSpyObj('SessionStore', ['setResult']);
    router = jasmine.createSpyObj('Router', ['navigateByUrl']);
    api.getMyDiagnoses.and.returnValue(of(items));
    api.getRepairers.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [PassportsPageComponent],
      providers: [
        { provide: ApiService, useValue: api },
        { provide: SessionStore, useValue: store },
        { provide: Router, useValue: router }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(PassportsPageComponent);
    component = fixture.componentInstance;
  });

  it('loads passports newest first with formatted date', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    expect(component.items[0].id).toBe('new');
    expect(component.items[1].id).toBe('old');
    expect(component.formatDate('2026-07-10T15:30:00Z')).toContain('2026');
    expect(component.loading).toBeFalse();
  }));

  it('opens a passport on the result page', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    router.navigateByUrl.and.returnValue(Promise.resolve(true));

    component.open(component.items[0]);
    tick();

    expect(api.getRepairers).toHaveBeenCalledWith('WASHING_MACHINE', 'Lyon');
    expect(store.setResult).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/resultat');
  }));
});
