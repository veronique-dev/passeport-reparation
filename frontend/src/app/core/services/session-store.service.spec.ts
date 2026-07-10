import { SessionStore } from './session-store.service';
import { DiagnosisResponse } from '../models';

describe('SessionStore', () => {
  let store: SessionStore;

  const anonymous: DiagnosisResponse = {
    id: 'd-anon',
    mediaId: 'm1',
    category: 'OVEN',
    applianceLabel: 'Four',
    probableIssue: 'Joint',
    confidence: 1,
    estimate: null,
    verdict: 'REPAIR',
    disclaimer: 'indicative',
    supported: true
  };

  const owned: DiagnosisResponse = {
    ...anonymous,
    id: 'd-owned',
    userId: 'u1'
  };

  beforeEach(() => {
    localStorage.clear();
    store = new SessionStore();
  });

  afterEach(() => localStorage.clear());

  it('remembers anonymous diagnosis id on setResult', () => {
    store.setResult(anonymous, 'blob:preview', []);
    expect(store.getPendingAnonymousDiagnosisId()).toBe('d-anon');
    expect(store.getDiagnosis()?.id).toBe('d-anon');
  });

  it('clears pending id when diagnosis is owned', () => {
    store.rememberAnonymousDiagnosis('old');
    store.setResult(owned, 'blob:preview', []);
    expect(store.getPendingAnonymousDiagnosisId()).toBeNull();
  });

  it('clears in-memory result without wiping pending claim id', () => {
    store.setResult(anonymous, 'blob:preview', []);
    store.clear();
    expect(store.getDiagnosis()).toBeNull();
    expect(store.getPendingAnonymousDiagnosisId()).toBe('d-anon');
  });
});
