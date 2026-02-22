import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CheckCircle2, XCircle, FileInput, Clock,
  ArrowRight, RefreshCw, TrendingUp,
} from 'lucide-react';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import { fetchStatusSummary, fetchAllAuditLogs } from '../api/client';
import type { StatusSummary, AuditLog } from '../types';

function PipelineStep({ label, count, active }: { label: string; count: number; active: boolean }) {
  return (
    <div className={`flex flex-col items-center gap-1.5 ${active ? 'opacity-100' : 'opacity-40'}`}>
      <div className={`w-10 h-10 rounded-full border-2 flex items-center justify-center text-sm font-bold ${active ? 'border-indigo-500 bg-indigo-50 text-indigo-700' : 'border-slate-200 bg-white text-slate-400'}`}>
        {count}
      </div>
      <p className="text-xs font-medium text-slate-600 text-center">{label}</p>
    </div>
  );
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [summary, setSummary] = useState<StatusSummary | null>(null);
  const [recentLogs, setRecentLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [lastRefresh, setLastRefresh] = useState(new Date());

  const load = async () => {
    try {
      const [sum, logs] = await Promise.all([fetchStatusSummary(), fetchAllAuditLogs()]);
      setSummary(sum);
      setRecentLogs(logs.slice(-20).reverse());
      setLastRefresh(new Date());
    } catch (_) {
      // backend may be offline
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); const id = setInterval(load, 10000); return () => clearInterval(id); }, []);

  const total = summary ? Object.values(summary).reduce((a, b) => a + b, 0) : 0;
  const successRate = summary && total > 0
    ? Math.round((summary.acknowledged / total) * 100) : 0;

  if (loading) return <LoadingSpinner text="Loading dashboard..." />;

  return (
    <div className="space-y-6 animate-slide-in">
      {/* Stats Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Processed" value={total} icon={<FileInput className="w-5 h-5" />} color="indigo" sub="All time" />
        <StatCard label="Acknowledged" value={summary?.acknowledged ?? 0} icon={<CheckCircle2 className="w-5 h-5" />} color="emerald" sub={`${successRate}% success rate`} />
        <StatCard label="Failed" value={summary?.failed ?? 0} icon={<XCircle className="w-5 h-5" />} color="red" sub="In Dead Letter Queue" />
        <StatCard label="In Flight" value={(summary?.received ?? 0) + (summary?.parsed ?? 0) + (summary?.transmitted ?? 0)} icon={<Clock className="w-5 h-5" />} color="amber" sub="Currently processing" />
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Pipeline Flow */}
        <div className="card p-5 col-span-1">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-slate-900">Pipeline Flow</h2>
            <TrendingUp className="w-4 h-4 text-slate-400" />
          </div>
          <div className="flex items-center gap-1.5">
            <PipelineStep label="Received" count={summary?.received ?? 0} active={(summary?.received ?? 0) > 0} />
            <ArrowRight className="w-3.5 h-3.5 text-slate-300 shrink-0 mb-5" />
            <PipelineStep label="Parsed" count={summary?.parsed ?? 0} active={(summary?.parsed ?? 0) > 0} />
            <ArrowRight className="w-3.5 h-3.5 text-slate-300 shrink-0 mb-5" />
            <PipelineStep label="Sent" count={summary?.transmitted ?? 0} active={(summary?.transmitted ?? 0) > 0} />
            <ArrowRight className="w-3.5 h-3.5 text-slate-300 shrink-0 mb-5" />
            <PipelineStep label="Done" count={summary?.acknowledged ?? 0} active={(summary?.acknowledged ?? 0) > 0} />
          </div>
          {summary?.failed > 0 && (
            <div className="mt-4 p-2.5 bg-red-50 border border-red-100 rounded-lg flex items-center gap-2">
              <XCircle className="w-4 h-4 text-red-500 shrink-0" />
              <p className="text-xs text-red-700 font-medium">{summary.failed} file(s) in DLQ</p>
            </div>
          )}
        </div>

        {/* Recent Activity */}
        <div className="card col-span-2 overflow-hidden">
          <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-900">Recent Activity</h2>
            <button onClick={load} className="text-slate-400 hover:text-slate-600 transition-colors p-1 rounded">
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>

          {recentLogs.length === 0 ? (
            <div className="py-12 flex flex-col items-center gap-2">
              <FileInput className="w-8 h-8 text-slate-300" />
              <p className="text-sm text-slate-400">No activity yet</p>
              <button onClick={() => navigate('/ingest')} className="btn-primary text-xs mt-1">
                Ingest your first EDI file
              </button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50">
                    <th className="table-header text-left px-5 py-3">Retailer</th>
                    <th className="table-header text-left px-4 py-3">PO Number</th>
                    <th className="table-header text-left px-4 py-3">Status</th>
                    <th className="table-header text-left px-4 py-3">Time</th>
                    <th className="table-header px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {recentLogs.map(log => (
                    <tr key={log.id} className="table-row" onClick={() => navigate(`/audit/${log.correlationId}`)}>
                      <td className="px-5 py-3">
                        <span className="font-mono text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded font-medium">
                          {log.retailerId}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-700 font-medium">
                        {log.poNumber ?? <span className="text-slate-400 italic">—</span>}
                      </td>
                      <td className="px-4 py-3"><StatusBadge status={log.status} /></td>
                      <td className="px-4 py-3 text-slate-400 text-xs">
                        {new Date(log.createdAt).toLocaleTimeString()}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <ArrowRight className="w-3.5 h-3.5 text-slate-300 ml-auto" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {recentLogs.length > 0 && (
            <div className="px-5 py-3 border-t border-slate-100 flex items-center justify-between">
              <p className="text-xs text-slate-400">Updated {lastRefresh.toLocaleTimeString()}</p>
              <button onClick={() => navigate('/audit')} className="text-xs text-indigo-600 hover:text-indigo-700 font-medium flex items-center gap-1">
                View all <ArrowRight className="w-3 h-3" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
