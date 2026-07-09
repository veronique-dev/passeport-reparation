import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DiagnosisResponse, MediaUploadResponse, Repairer, ApplianceCategory } from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  uploadMedia(file: File): Observable<MediaUploadResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<MediaUploadResponse>(`${this.base}/api/media`, form);
  }

  createDiagnosis(mediaId: string, latitude?: number, longitude?: number): Observable<DiagnosisResponse> {
    return this.http.post<DiagnosisResponse>(`${this.base}/api/diagnoses`, {
      mediaId,
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
}
