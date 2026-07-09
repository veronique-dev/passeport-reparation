export type ApplianceCategory = 'WASHING_MACHINE' | 'DISHWASHER' | 'OVEN' | 'UNSUPPORTED';
export type RepairVerdict = 'REPAIR' | 'ARBITRATE' | 'REPLACE';
export type IssueCode =
  | 'WM_DRAIN_PUMP'
  | 'WM_DOOR_LOCK'
  | 'WM_NO_SPIN'
  | 'WM_ELECTRONIC_BOARD'
  | 'WM_UNKNOWN'
  | 'DW_HEATING'
  | 'DW_DRAIN'
  | 'DW_SPRAY_ARM'
  | 'DW_ELECTRONIC_BOARD'
  | 'DW_UNKNOWN'
  | 'OV_THERMOSTAT'
  | 'OV_HEATING_ELEMENT'
  | 'OV_DOOR_SEAL'
  | 'OV_ELECTRONIC_BOARD'
  | 'OV_UNKNOWN'
  | 'UNSUPPORTED_OTHER';

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

export interface IssueOption {
  code: IssueCode;
  label: string;
  category: ApplianceCategory;
}

export interface DiagnosisResponse {
  id: string;
  mediaId: string;
  category: ApplianceCategory;
  applianceLabel: string;
  issueCode?: IssueCode;
  probableIssue: string;
  confidence: number;
  estimate: CostEstimate | null;
  verdict: RepairVerdict | null;
  disclaimer: string;
  supported: boolean;
  userConfirmed?: boolean;
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

export type VisionSuggestResponse = {
  mediaId: string;
  category: ApplianceCategory;
  applianceLabel: string;
  suggestedIssueCode?: IssueCode;
  probableIssue: string;
  confidence: number;
  supported: boolean;
  provider: string;
  rationale: string;
  suggestionOnly: boolean;
};

export interface CategoryChoice {
  value: ApplianceCategory;
  label: string;
  hint: string;
}
