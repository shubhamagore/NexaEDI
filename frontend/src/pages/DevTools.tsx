import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import {
  Database, Trash2, RefreshCw, CheckCircle2, XCircle,
  BarChart3, Server, HardDrive, Zap,
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import { fetchAllAuditLogs, fetchDevStats, clearAllAuditLogs } from '../api/client';
import type { AuditLog, DevStats } from '../types';

function StatRow({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
      <span className="text-sm text-slate-600">{label}</span>
      <span className={`text-sm font-bold ${color}`}>{value}</span>
    </div>
  );
}

export default function DevTools() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [stats, setStats] = useState<DevStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [clearing, setClearing] = useState(false);
  const [confirmClear, setConfirmClear] = useState(false);

  const load = async () => {
    try {
      const [logData, statData] = await Promise.all([fetchAllAuditLogs(), fetchDevStats()]);
      setLogs(logData.slice().reverse());
      setStats(statData);
    } catch (_) {}
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const handleClear = async () => {
    if (!confirmClear) { setConfirmClear(true); return; }
    setClearing(true);
    try {
      const msg = await clearAllAuditLogs();
      toast.success(msg);
      setLogs([]);
      setStats({ totalRecords: 0, byStatus: {} });
      setConfirmClear(false);
    } catch (_) {
      toast.error('Failed to clear records');
    } finally {
      setClearing(false);
    }
  };

  if (loading) return <LoadingSpinner text="Loading dev tools..." />;

  return (
    <div className="space-y-6 animate-slide-in">
      {/* Warning Banner */}
      <div className="flex items-start gap-3 bg-amber-50 border border-amber-200 rounded-xl p-4">
        <Server className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div>
          <p className="text-sm font-semibold text-amber-900">Local Development Mode</p>
          <p className="text-xs text-amber-700 mt-1">
            These tools are only available when the <code className="font-mono bg-amber-100 px-1 rounded">local</code> Spring profile is active.
            S3 writes to <code className="font-mono bg-amber-100 px-1 rounded">./local-storage/</code> and Shopify returns mock order IDs.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Stats */}
        <div className="card p-5 col-span-1 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-slate-400" /> Database Stats
            </h2>
            <button onClick={load} className="text-slate-400 hover:text-slate-600">
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="text-center py-3">
            <p className="text-4xl font-black text-slate-900">{stats?.totalRecords ?? 0}</p>
            <p className="text-xs text-slate-400 mt-1">Total Audit Records</p>
          </div>

          <div>
            <StatRow label="Received"     value={stats?.byStatus?.RECEIVED ?? 0}     color="text-blue-600" />
            <StatRow label="Parsed"       value={stats?.byStatus?.PARSED ?? 0}       color="text-violet-600" />
            <StatRow label="Validated"    value={stats?.byStatus?.VALIDATED ?? 0}    color="text-purple-600" />
            <StatRow label="Transmitted"  value={stats?.byStatus?.TRANSMITTED ?? 0}  color="text-amber-600" />
            <StatRow label="Acknowledged" value={stats?.byStatus?.ACKNOWLEDGED ?? 0} color="text-emerald-600" />
            <StatRow label="Failed"       value={stats?.byStatus?.FAILED ?? 0}       color="text-red-600" />
          </div>
        </div>

        {/* Stub Status + Actions */}
        <div className="col-span-2 space-y-4">
          {/* Stub Status */}
          <div className="card p-5">
            <h2 className="text-sm font-semibold text-slate-900 mb-4">Active Stubs</h2>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                <div className="flex items-center gap-3">
                  <HardDrive className="w-4 h-4 text-slate-500" />
                  <div>
                    <p className="text-sm font-medium text-slate-800">S3 Storage</p>
                    <p className="text-xs text-slate-400">Writing to <code className="font-mono">./local-storage/</code></p>
                  </div>
                </div>
                <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full">
                  <CheckCircle2 className="w-3.5 h-3.5" /> STUB ACTIVE
                </span>
              </div>
              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                <div className="flex items-center gap-3">
                  <Zap className="w-4 h-4 text-slate-500" />
                  <div>
                    <p className="text-sm font-medium text-slate-800">Shopify Adapter</p>
                    <p className="text-xs text-slate-400">Returns <code className="font-mono">LOCAL-DRAFT-XXXXXXXX</code></p>
                  </div>
                </div>
                <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full">
                  <CheckCircle2 className="w-3.5 h-3.5" /> STUB ACTIVE
                </span>
              </div>
              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                <div className="flex items-center gap-3">
                  <Database className="w-4 h-4 text-slate-500" />
                  <div>
                    <p className="text-sm font-medium text-slate-800">Database</p>
                    <p className="text-xs text-slate-400">H2 in-memory · <code className="font-mono">jdbc:h2:mem:nexaedi_local</code></p>
                  </div>
                </div>
                <span className="flex items-center gap-1.5 text-xs font-semibold text-blue-700 bg-blue-50 border border-blue-200 px-2.5 py-1 rounded-full">
                  <CheckCircle2 className="w-3.5 h-3.5" /> H2 IN-MEMORY
                </span>
              </div>
            </div>
          </div>

          {/* Danger Zone */}
          <div className="card p-5 border-red-100">
            <h2 className="text-sm font-semibold text-red-700 mb-3 flex items-center gap-2">
              <XCircle className="w-4 h-4" /> Danger Zone
            </h2>
            <div className="flex items-center justify-between p-3 bg-red-50 rounded-lg border border-red-200">
              <div>
                <p className="text-sm font-medium text-slate-800">Clear All Audit Records</p>
                <p className="text-xs text-slate-500 mt-0.5">Permanently deletes all {stats?.totalRecords ?? 0} records from the in-memory database.</p>
              </div>
              <button
                onClick={handleClear}
                disabled={clearing}
                className={`btn-danger text-xs shrink-0 ${confirmClear ? 'bg-red-700 animate-pulse' : ''}`}
              >
                {clearing ? (
                  <><span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Clearing...</>
                ) : confirmClear ? (
                  <><Trash2 className="w-3.5 h-3.5" /> Confirm Delete</>
                ) : (
                  <><Trash2 className="w-3.5 h-3.5" /> Clear All</>
                )}
              </button>
            </div>
            {confirmClear && (
              <p className="text-xs text-red-600 mt-2 flex items-center gap-1">
                <AlertIcon /> Click again to confirm. This cannot be undone.
                <button className="ml-2 underline" onClick={() => setConfirmClear(false)}>Cancel</button>
              </p>
            )}
          </div>
        </div>
      </div>

      {/* All Records Table */}
      <div className="card overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h2 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
            <Database className="w-4 h-4 text-slate-400" />
            Raw Audit Log <span className="text-slate-400 font-normal">({logs.length} records)</span>
          </h2>
          <button onClick={load} className="btn-secondary text-xs py-1.5">
            <RefreshCw className="w-3.5 h-3.5" /> Refresh
          </button>
        </div>

        {logs.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-sm text-slate-400">No records in database. Ingest an EDI file to populate.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100">
                  <th className="table-header text-left px-4 py-3">ID</th>
                  <th className="table-header text-left px-4 py-3">Correlation ID</th>
                  <th className="table-header text-left px-4 py-3">Retailer</th>
                  <th className="table-header text-left px-4 py-3">PO#</th>
                  <th className="table-header text-left px-4 py-3">Status</th>
                  <th className="table-header text-left px-4 py-3">Duration</th>
                  <th className="table-header text-left px-4 py-3">Created At</th>
                </tr>
              </thead>
              <tbody>
                {logs.map(log => (
                  <tr key={log.id} className="border-t border-slate-100 hover:bg-slate-50">
                    <td className="px-4 py-2.5 text-slate-400">{log.id}</td>
                    <td className="px-4 py-2.5 font-mono text-slate-500">{log.correlationId.substring(0, 12)}...</td>
                    <td className="px-4 py-2.5 font-semibold text-slate-700">{log.retailerId}</td>
                    <td className="px-4 py-2.5 text-slate-600">{log.poNumber ?? '—'}</td>
                    <td className="px-4 py-2.5"><StatusBadge status={log.status} /></td>
                    <td className="px-4 py-2.5 text-slate-400">{log.durationMs != null ? `${log.durationMs}ms` : '—'}</td>
                    <td className="px-4 py-2.5 text-slate-400">{new Date(log.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function AlertIcon() {
  return (
    <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
      <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
    </svg>
  );
}
