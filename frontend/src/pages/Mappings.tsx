import { useState, useEffect } from 'react';
import { Map, ChevronDown, ChevronUp, CheckCircle2, AlertCircle, Tag, Hash } from 'lucide-react';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import { fetchAllMappings } from '../api/client';
import type { MappingProfile, MappingRule } from '../types';

const retailerColors: Record<string, { bg: string; text: string; border: string }> = {
  TARGET:  { bg: 'bg-red-50',    text: 'text-red-700',    border: 'border-red-200' },
  WALMART: { bg: 'bg-blue-50',   text: 'text-blue-700',   border: 'border-blue-200' },
  AMAZON:  { bg: 'bg-amber-50',  text: 'text-amber-700',  border: 'border-amber-200' },
  DEFAULT: { bg: 'bg-indigo-50', text: 'text-indigo-700', border: 'border-indigo-200' },
};

function RuleTable({ rules, title }: { rules: MappingRule[]; title: string }) {
  return (
    <div>
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">{title}</p>
      <div className="overflow-x-auto rounded-lg border border-slate-200">
        <table className="w-full text-xs">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="text-left px-3 py-2 font-semibold text-slate-500">Segment</th>
              <th className="text-left px-3 py-2 font-semibold text-slate-500">Position</th>
              <th className="text-left px-3 py-2 font-semibold text-slate-500">Target Field</th>
              <th className="text-left px-3 py-2 font-semibold text-slate-500">Qualifier</th>
              <th className="text-center px-3 py-2 font-semibold text-slate-500">Required</th>
              <th className="text-left px-3 py-2 font-semibold text-slate-500">Default</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((rule, i) => (
              <tr key={i} className="border-t border-slate-100 hover:bg-slate-50">
                <td className="px-3 py-2">
                  <code className="bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded font-mono font-medium">
                    {rule.segmentId}{String(rule.elementPosition).padStart(2, '0')}
                  </code>
                </td>
                <td className="px-3 py-2 text-slate-600">{rule.elementPosition}</td>
                <td className="px-3 py-2">
                  <code className="text-indigo-600 font-mono">{rule.targetField}</code>
                </td>
                <td className="px-3 py-2 text-slate-500 font-mono">{rule.qualifier ?? '—'}</td>
                <td className="px-3 py-2 text-center">
                  {rule.required
                    ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 inline" />
                    : <span className="text-slate-300">—</span>}
                </td>
                <td className="px-3 py-2 text-slate-400">{rule.defaultValue ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function MappingCard({ profileKey, profile }: { profileKey: string; profile: MappingProfile }) {
  const [expanded, setExpanded] = useState(false);
  const color = retailerColors[profile.retailerId] ?? retailerColors.DEFAULT;
  const totalRules = (profile.headerMappings?.length ?? 0) + (profile.lineMappings?.length ?? 0);

  return (
    <div className={`card overflow-hidden border ${expanded ? color.border : 'border-slate-200'} transition-all`}>
      <button className="w-full text-left" onClick={() => setExpanded(!expanded)}>
        <div className="p-5 flex items-start justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className={`${color.bg} ${color.border} border rounded-xl p-3 shrink-0`}>
              <span className={`text-base font-black font-mono ${color.text}`}>
                {profile.retailerId.substring(0, 2)}
              </span>
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h3 className="font-bold text-slate-900">{profile.retailerId}</h3>
                <span className="bg-slate-100 text-slate-600 text-xs font-semibold px-2 py-0.5 rounded-full border border-slate-200">
                  {profile.transactionSetCode}
                </span>
                <span className="text-xs text-slate-400">v{profile.version}</span>
              </div>
              <p className="text-xs text-slate-500 mt-1">{profile.description}</p>
              <div className="flex items-center gap-4 mt-2">
                <span className="flex items-center gap-1 text-xs text-slate-400">
                  <Hash className="w-3 h-3" /> {profile.headerMappings?.length ?? 0} header rules
                </span>
                <span className="flex items-center gap-1 text-xs text-slate-400">
                  <Tag className="w-3 h-3" /> {profile.lineMappings?.length ?? 0} line rules
                </span>
                <span className="text-xs text-slate-400">Delimiter: <code className="font-mono">'{profile.elementDelimiter}'</code></span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${color.bg} ${color.text}`}>
              {totalRules} rules
            </span>
            {expanded ? <ChevronUp className="w-4 h-4 text-slate-400" /> : <ChevronDown className="w-4 h-4 text-slate-400" />}
          </div>
        </div>
      </button>

      {expanded && (
        <div className="border-t border-slate-100 p-5 space-y-5 animate-fade-in">
          {profile.headerMappings?.length > 0 && (
            <RuleTable rules={profile.headerMappings} title="Header Mappings (Order Level)" />
          )}
          {profile.lineMappings?.length > 0 && (
            <RuleTable rules={profile.lineMappings} title="Line Mappings (PO1 Loop — Item Level)" />
          )}
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <p className="text-xs text-amber-700">
              To add a new retailer, drop a JSON file named <code className="font-mono">{profile.retailerId.toLowerCase()}-{profile.transactionSetCode}.json</code> into the <code className="font-mono">/mappings</code> directory and restart the service.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

export default function Mappings() {
  const [profiles, setProfiles] = useState<Record<string, MappingProfile>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAllMappings()
      .then(setProfiles)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSpinner text="Loading mapping profiles..." />;

  const keys = Object.keys(profiles);

  return (
    <div className="space-y-4 animate-slide-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm text-slate-500 bg-white border border-slate-200 rounded-lg px-4 py-2">
          <Map className="w-4 h-4 text-indigo-500" />
          <span><span className="font-bold text-slate-900">{keys.length}</span> profile{keys.length !== 1 ? 's' : ''} loaded</span>
        </div>
        <div className="text-xs text-slate-400 card px-4 py-2">
          Drop a JSON file in <code className="font-mono text-indigo-600">/mappings</code> → restart to add a retailer
        </div>
      </div>

      {keys.length === 0 ? (
        <EmptyState
          icon={<Map className="w-7 h-7" />}
          title="No mapping profiles loaded"
          description="Add JSON files to the /mappings directory and restart the service"
        />
      ) : (
        <div className="space-y-3">
          {keys.map(key => (
            <MappingCard key={key} profileKey={key} profile={profiles[key]} />
          ))}
        </div>
      )}
    </div>
  );
}
