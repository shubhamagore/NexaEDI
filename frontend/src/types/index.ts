export type EdiStatus =
  | 'RECEIVED'
  | 'PARSED'
  | 'VALIDATED'
  | 'TRANSMITTED'
  | 'ACKNOWLEDGED'
  | 'FAILED';

export interface AuditLog {
  id: number;
  correlationId: string;
  retailerId: string;
  transactionSetCode: string | null;
  poNumber: string | null;
  status: EdiStatus;
  sourceFilePath: string | null;
  message: string | null;
  errorDetail: string | null;
  createdAt: string;
  durationMs: number | null;
}

export interface ProcessingResponse {
  correlationId: string;
  message: string;
  acceptedAt: string;
  auditTrailUrl: string;
}

export interface MappingRule {
  segmentId: string;
  elementPosition: number;
  targetField: string;
  required: boolean;
  defaultValue: string | null;
  qualifier: string | null;
  lineLevel: boolean;
}

export interface MappingProfile {
  retailerId: string;
  transactionSetCode: string;
  description: string;
  version: string;
  elementDelimiter: string;
  headerMappings: MappingRule[];
  lineMappings: MappingRule[];
}

export interface StatusSummary {
  received: number;
  parsed: number;
  validated: number;
  transmitted: number;
  acknowledged: number;
  failed: number;
}

export interface DevStats {
  totalRecords: number;
  byStatus: Record<string, number>;
}

export interface HealthStatus {
  status: 'UP' | 'DOWN';
}
