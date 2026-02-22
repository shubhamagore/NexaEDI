import axios from 'axios';
import type {
  AuditLog, ProcessingResponse, MappingProfile,
  StatusSummary, DevStats, HealthStatus,
} from '../types';
import { apiBaseUrl } from './config';

const http = axios.create({
  baseURL: apiBaseUrl || '/',
  headers: { 'Content-Type': 'application/json' },
});

http.interceptors.request.use(config => {
  try {
    const stored = localStorage.getItem('nexaedi_auth');
    if (stored) {
      const { token } = JSON.parse(stored);
      if (token) config.headers['Authorization'] = `Bearer ${token}`;
    }
  } catch {}
  return config;
});

// ── Health ────────────────────────────────────────────────────────────────────
export const fetchHealth = (): Promise<HealthStatus> =>
  http.get('/actuator/health').then(r => r.data);

// ── EDI Ingestion ─────────────────────────────────────────────────────────────
export const ingestEdi = (payload: {
  retailerId: string;
  ediContent: string;
  fileName: string;
}): Promise<ProcessingResponse> =>
  http.post('/api/v1/edi/ingest', payload).then(r => r.data);

// ── Audit ─────────────────────────────────────────────────────────────────────
export const fetchAuditByCorrelationId = (correlationId: string): Promise<AuditLog[]> =>
  http.get(`/api/v1/edi/audit/${correlationId}`).then(r => r.data);

export const fetchStatusSummary = (): Promise<StatusSummary> =>
  http.get('/api/v1/edi/status/summary').then(r => r.data);

// ── Mappings ──────────────────────────────────────────────────────────────────
export const fetchAllMappings = (): Promise<Record<string, MappingProfile>> =>
  http.get('/api/v1/mappings').then(r => r.data);

export const fetchMapping = (retailerId: string, txnCode: string): Promise<MappingProfile> =>
  http.get(`/api/v1/mappings/${retailerId}/${txnCode}`).then(r => r.data);

// ── Dev Tools (local profile only) ───────────────────────────────────────────
export const fetchAllAuditLogs = (): Promise<AuditLog[]> =>
  http.get('/dev/audit-log').then(r => r.data);

export const fetchDevStats = (): Promise<DevStats> =>
  http.get('/dev/stats').then(r => r.data);

export const clearAllAuditLogs = (): Promise<string> =>
  http.delete('/dev/audit-log').then(r => r.data);
