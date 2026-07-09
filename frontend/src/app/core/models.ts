export type ApplianceCategory = 'WASHING_MACHINE' | 'DISHWASHER' | 'OVEN' | 'UNSUPPORTED';
export type RepairVerdict = 'REPAIR' | 'ARBITRATE' | 'REPLACE';

export interface CostEstimate {
  repairLow: number;
  repairHigh: number;
  replacementApprox: number;
  currency: string;
}

export interface MediaUploadResponse {
  mediaId: string;
  contentType: string;
  sizeBytes: number;
  url: string;
}

export interface DiagnosisResponse {
  id: string;
  mediaId: string;
  category: ApplianceCategory;
  applianceLabel: string;
  probableIssue: string;
  confidence: number;
  estimate: CostEstimate | null;
  verdict: RepairVerdict | null;
  disclaimer: string;
  supported: boolean;
}

export interface Repairer {
  id: string;
  name: string;
  city: string;
  phone?: string;
  email?: string;
  whatsapp?: string;
  categories: ApplianceCategory[];
  latitude?: number;
  longitude?: number;
  distanceKm?: number;
}
