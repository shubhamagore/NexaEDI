import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, RefreshCw, ArrowRight, ClipboardList, Filter } from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import { fetchAllAuditLogs } from '../api/client';
import type { AuditLog, EdiStatus } from '../types';

const ALL_STATUSES: EdiStatus[] = ['RECEIVED','PARSED','VALIDATED','TRANSMITTED','ACKNOWLEDGED','FAILED'];

const statusColors: Record<EdiStatus, string> = {
  RECEIVED: 'bg-blue-50 text-blue-700 border-blue-200',
  PARSED: 'bg-violet-50 text-violet-700 border-violet-200',
  VALIDATED: 'bg-purple-50 text-purple-700 border-purple-200',
  TRANSMITTED: 'bg-amber-50 text-amber-700 border-amber-200',
  ACKNOWLEDGED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  FAILED: 'bg-red-50 text-red-700 border-red-200',
};

function groupByCorrelation(logs: AuditLog[]) {
  const map = new Map<string, AuditLog[]>();
  logs.forEach(log => {
    if (!map.has(log.correlationId)) map.set(log.correlationId, []);
    map.get(log.correlationId)!.push(log);
  });
  return Array.from(map.entries()).map(([id, entries]) => ({
    correlationId: id,
    latestLog: entries.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0],
    entries,
  }));
}

export default function AuditTrail() {
  const navigate = useNavigate();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState<EdiStatus | 'ALL'>('ALL');
  const [filterRetailer, setFilterRetailer] = useState('ALL');

  const load = async () => {
    try {
      const data = await fetchAllAuditLogs();
      setLogs(data);
    } catch (_) {}
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const groups = groupByCorrelation(logs);
  const retailers = ['ALL', ...Array.from(new Set(logs.map(l => l.retailerId))).sort()];

  const filtered = groups.filter(g => {
    const l = g.latestLog;
    const matchSearch = !search ||
      g.correlationId.toLowerCase().includes(search.toLowerCase()) ||
      (l.poNumber ?? '').toLowerCase().includes(search.toLowerCase()) ||
      l.retailerId.toLowerCase().includes(search.toLowerCase());
    const matchStatus = filterStatus === 'ALL' || l.status === filterStatus;
    const matchRetailer = filterRetailer === 'ALL' || l.retailerId === filterRetailer;
    return matchSearch && matchStatus && matchRetailer;
  }).sort((a, b) => new Date(b.latestLog.createdAt).getTime() - new Date(a.latestLog.createdAt).getTime());

  if (loading) return <LoadingSpinner text="Loading audit trail..." />;

  return (
    <div className="space-y-4 animate-slide-in">
      {/* Controls */}
      <div className="card p-4 flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            className="input pl-9"
            placeholder="Search by correlation ID, PO number, retailer..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select className="input w-auto" value={filterRetailer} onChange={e => setFilterRetailer(e.target.value)}>
            {retailers.map(r => <option key={r}>{r}</option>)}
          </select>
        </div>

        <div className="flex gap-1.5 flex-wrap">
          {(['ALL', ...ALL_STATUSES] as const).map(s => (
            <button
              key={s}
              onClick={() => setFilterStatus(s)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${filterStatus === s ? (s === 'ALL' ? 'bg-slate-800 text-white border-slate-800' : `${statusColors[s as EdiStatus]} border`) : 'bg-white text-slate-500 border-slate-200 hover:border-slate-300'}`}
            >
              {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        <button onClick={load} className="btn-secondary py-2">
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Table */}
      <div className="card overflow-hidden">
        <div className="px-5 py-3 border-b border-slate-100 flex items-center justify-between">
          <p className="text-xs font-semibold text-slate-500">{filtered.length} record{filtered.length !== 1 ? 's' : ''}</p>
        </div>

        {filtered.length === 0 ? (
          <EmptyState
            icon={<ClipboardList className="w-7 h-7" />}
            title="No records found"
            description="Ingest an EDI file to see audit records appear here"
            action={<button onClick={() => navigate('/ingest')} className="btn-primary text-xs">Ingest EDI File</button>}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100">
                  <th className="table-header text-left px-5 py-3">Correlation ID</th>
                  <th className="table-header text-left px-4 py-3">Retailer</th>
                  <th className="table-header text-left px-4 py-3">PO Number</th>
                  <th className="table-header text-left px-4 py-3">Txn Set</th>
                  <th className="table-header text-left px-4 py-3">Latest Status</th>
                  <th className="table-header text-left px-4 py-3">Last Updated</th>
                  <th className="table-header text-left px-4 py-3">Steps</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(({ correlationId, latestLog, entries }) => (
                  <tr key={correlationId} className="table-row" onClick={() => navigate(`/audit/${correlationId}`)}>
                    <td className="px-5 py-3">
                      <code className="text-xs text-slate-500 font-mono">
                        {correlationId.substring(0, 8)}...
                      </code>
                    </td>
                    <td className="px-4 py-3">
                      <span className="bg-slate-100 text-slate-700 text-xs font-semibold px-2 py-1 rounded font-mono">
                        {latestLog.retailerId}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium text-slate-800">
                      {latestLog.poNumber ?? <span className="text-slate-400">—</span>}
                    </td>
                    <td className="px-4 py-3 text-slate-500 text-xs">
                      {latestLog.transactionSetCode ?? '—'}
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={latestLog.status} /></td>
                    <td className="px-4 py-3 text-slate-400 text-xs">
                      {new Date(latestLog.createdAt).toLocaleString()}
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-xs bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-medium">
                        {entries.length} / 5
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <ArrowRight className="w-4 h-4 text-slate-300 ml-auto" />
                    </td>
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
