import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApplianceCategory,
  DiagnosisResponse,
  IssueCode,
  IssueOption,
  MediaUploadResponse,
  Repairer,
  VisionSuggestResponse
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  uploadMedia(file: File): Observable<MediaUploadResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<MediaUploadResponse>(`${this.base}/api/media`, form);
  }

  suggestDiagnosis(mediaId: string): Observable<VisionSuggestResponse> {
    return this.http.post<VisionSuggestResponse>(`${this.base}/api/diagnoses/suggest`, { mediaId });
  }

  getIssues(category: ApplianceCategory): Observable<IssueOption[]> {
    return this.http.get<IssueOption[]>(`${this.base}/api/diagnoses/issues`, {
      params: { category }
    });
  }

  createDiagnosis(
    mediaId: string,
    category: ApplianceCategory,
    issueCode?: IssueCode | null,
    latitude?: number,
    longitude?: number
  ): Observable<DiagnosisResponse> {
    return this.http.post<DiagnosisResponse>(`${this.base}/api/diagnoses`, {
      mediaId,
      category,
      issueCode: issueCode || null,
      latitude,
      longitude
    });
  }

  getRepairers(category?: ApplianceCategory, city = 'Lyon', lat?: number, lng?: number): Observable<Repairer[]> {
    const params: Record<string, string> = {};
    if (category && category !== 'UNSUPPORTED') {
      params['category'] = category;
    }
    if (city) {
      params['city'] = city;
    }
    if (lat != null) {
      params['lat'] = String(lat);
    }
    if (lng != null) {
      params['lng'] = String(lng);
    }
    return this.http.get<Repairer[]>(`${this.base}/api/repairers`, { params });
  }

  getMyDiagnoses(): Observable<DiagnosisResponse[]> {
    return this.http.get<DiagnosisResponse[]>(`${this.base}/api/diagnoses/mine`);
  }

  getDiagnosis(id: string): Observable<DiagnosisResponse> {
    return this.http.get<DiagnosisResponse>(`${this.base}/api/diagnoses/${id}`);
  }

  claimDiagnosis(id: string): Observable<DiagnosisResponse> {
    return this.http.post<DiagnosisResponse>(`${this.base}/api/diagnoses/${id}/claim`, {});
  }
}
