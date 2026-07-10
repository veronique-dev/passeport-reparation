import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { HomePageComponent } from './home-page.component';
import { ApiService } from '../../core/services/api.service';
import { SessionStore } from '../../core/services/session-store.service';

describe('HomePageComponent', () => {
  let component: HomePageComponent;
  let fixture: ComponentFixture<HomePageComponent>;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj('ApiService', [
      'uploadMedia',
      'suggestDiagnosis',
      'getIssues',
      'createDiagnosis',
      'getRepairers'
    ]);
    api.getIssues.and.returnValue(of([
      { code: 'OV_DOOR_SEAL', label: 'Joint', category: 'OVEN' },
      { code: 'OV_UNKNOWN', label: 'Autre', category: 'OVEN' }
    ]));

    await TestBed.configureTestingModule({
      declarations: [HomePageComponent],
      providers: [
        { provide: ApiService, useValue: api },
        { provide: SessionStore, useValue: jasmine.createSpyObj('SessionStore', ['setResult', 'clear']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigateByUrl']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomePageComponent);
    component = fixture.componentInstance;
  });

  it('disables submit until category and issue are ready', () => {
    expect(component.canSubmit).toBeFalse();
    component.selectedFile = new File(['x'], 'a.png', { type: 'image/png' });
    component.uploadedMediaId = 'm1';
    component.selectedCategory = 'OVEN';
    expect(component.canSubmit).toBeFalse();
    component.selectedIssue = 'OV_DOOR_SEAL';
    expect(component.canSubmit).toBeTrue();
  });

  it('allows submit for unsupported without issue', () => {
    component.selectedFile = new File(['x'], 'a.png', { type: 'image/png' });
    component.uploadedMediaId = 'm1';
    component.selectedCategory = 'UNSUPPORTED';
    expect(component.canSubmit).toBeTrue();
  });

  it('loads issues when selecting a supported category', fakeAsync(() => {
    component.selectCategory('OVEN');
    tick();
    expect(api.getIssues).toHaveBeenCalledWith('OVEN');
    expect(component.issues.length).toBe(2);
    expect(component.selectedIssue).toBe('OV_UNKNOWN');
    expect(component.loadingIssues).toBeFalse();
  }));
});
