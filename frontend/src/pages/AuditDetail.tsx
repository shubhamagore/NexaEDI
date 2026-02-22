import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, CheckCircle2, XCircle, Clock, Copy,
  AlertTriangle, Package, Zap, FileText, Database,
} from 'lucide-react';
import toast from 'react-hot-toast';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import { fetchAuditByCorrelationId } from '../api/client';
import type { AuditLog, EdiStatus } from '../types';

const stepConfig: Record<EdiStatus, { icon: typeof CheckCircle2; color: string; bgColor: string; borderColor: string }> = {
  RECEIVED:     { icon: FileText,     color: 'text-blue-600',    bgColor: 'bg-blue-50',    borderColor: 'border-blue-200' },
  PARSED:       { icon: Database,     color: 'text-violet-600',  bgColor: 'bg-violet-50',  borderColor: 'border-violet-200' },
  VALIDATED:    { icon: CheckCircle2, color: 'text-purple-600',  bgColor: 'bg-purple-50',  borderColor: 'border-purple-200' },
  TRANSMITTED:  { icon: Zap,          color: 'text-amber-600',   bgColor: 'bg-amber-50',   borderColor: 'border-amber-200' },
  ACKNOWLEDGED: { icon: CheckCircle2, color: 'text-emerald-600', bgColor: 'bg-emerald-50', borderColor: 'border-emerald-200' },
  FAILED:       { icon: XCircle,      color: 'text-red-600',     bgColor: 'bg-red-50',     borderColor: 'border-red-200' },
};

function TimelineItem({ log, isLast }: { log: AuditLog; isLast: boolean }) {
  const cfg = stepConfig[log.status] ?? stepConfig.RECEIVED;
  const Icon = cfg.icon;
  return (
    <div className="flex gap-4">
      <div className="flex flex-col items-center">
        <div className={`w-9 h-9 rounded-full border-2 ${cfg.bgColor} ${cfg.borderColor} flex items-center justify-center shrink-0 z-10`}>
          <Icon className={`w-4 h-4 ${cfg.color}`} />
        </div>
        {!isLast && <div className="w-0.5 flex-1 bg-slate-200 my-1" />}
      </div>
      <div className={`flex-1 pb-6 ${isLast ? '' : ''}`}>
        <div className={`card p-4 border ${cfg.borderColor}`}>
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <StatusBadge status={log.status} size="md" />
                {log.durationMs != null && (
                  <span className="flex items-center gap-1 text-xs text-slate-400">
                    <Clock className="w-3 h-3" /> {log.durationMs}ms
                  </span>
                )}
              </div>
              <p className="text-sm text-slate-700 mt-2">{log.message}</p>
              {log.sourceFilePath && (
                <p className="text-xs text-slate-400 mt-1 font-mono truncate">{log.sourceFilePath}</p>
              )}
              {log.errorDetail && (
                <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg">
                  <p className="text-xs font-semibold text-red-700 mb-1 flex items-center gap-1">
                    <AlertTriangle className="w-3.5 h-3.5" /> Error Detail
                  </p>
                  <pre className="text-xs text-red-800 whitespace-pre-wrap font-mono overflow-auto max-h-40">{log.errorDetail}</pre>
                </div>
              )}
            </div>
            <p className="text-xs text-slate-400 shrink-0">{new Date(log.createdAt).toLocaleTimeString()}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function AuditDetail() {
  const { correlationId } = useParams<{ correlationId: string }>();
  const navigate = useNavigate();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!correlationId) return;
    fetchAuditByCorrelationId(correlationId)
      .then(setLogs)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [correlationId]);

  if (loading) return <LoadingSpinner />;

  const latest = logs[logs.length - 1];
  const isFailed = latest?.status === 'FAILED';
  const totalDuration = logs.reduce((acc, l) => acc + (l.durationMs ?? 0), 0);

  const copy = (text: string) => { navigator.clipboard.writeText(text); toast.success('Copied'); };

  return (
    <div className="max-w-3xl space-y-6 animate-slide-in">
      {/* Back */}
      <button onClick={() => navigate('/dashboard/audit')} className="flex items-center gap-2 text-sm text-slate-500 hover:text-slate-800 transition-colors">
        <ArrowLeft className="w-4 h-4" /> Back to Audit Trail
      </button>

      {/* Header Card */}
      <div className="card p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-slate-100 rounded-xl flex items-center justify-center">
                <Package className="w-5 h-5 text-slate-600" />
              </div>
              <div>
                <p className="text-lg font-bold text-slate-900">
                  {latest?.poNumber ?? 'Processing...'}
                </p>
                <p className="text-sm text-slate-500">{latest?.retailerId} · {latest?.transactionSetCode ?? '850'}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {latest && <StatusBadge status={latest.status} size="md" />}
              {totalDuration > 0 && (
                <span className="text-xs text-slate-400 flex items-center gap-1">
                  <Clock className="w-3 h-3" /> {totalDuration}ms total
                </span>
              )}
            </div>
          </div>

          {isFailed && (
            <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-xs font-medium">
              <XCircle className="w-4 h-4" /> Moved to DLQ
            </div>
          )}
        </div>

        <div className="mt-4 pt-4 border-t border-slate-100">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Correlation ID</p>
          <div className="flex items-center gap-2">
            <code className="text-sm font-mono text-slate-700">{correlationId}</code>
            <button onClick={() => copy(correlationId!)} className="text-slate-400 hover:text-slate-600">
              <Copy className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-4 mt-4 pt-4 border-t border-slate-100 text-center">
          <div>
            <p className="text-2xl font-bold text-slate-900">{logs.length}</p>
            <p className="text-xs text-slate-400">Pipeline Steps</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-slate-900">{totalDuration}ms</p>
            <p className="text-xs text-slate-400">Total Duration</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-slate-900">
              {logs[0] ? new Date(logs[0].createdAt).toLocaleTimeString() : '—'}
            </p>
            <p className="text-xs text-slate-400">Started At</p>
          </div>
        </div>
      </div>

      {/* Timeline */}
      <div className="card p-6">
        <h2 className="text-sm font-semibold text-slate-900 mb-6">Pipeline Lifecycle</h2>
        <div>
          {logs.map((log, i) => (
            <TimelineItem key={log.id} log={log} isLast={i === logs.length - 1} />
          ))}
        </div>
      </div>
    </div>
  );
}
