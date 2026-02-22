import type { ReactNode } from 'react';

interface Props {
  label: string;
  value: number | string;
  icon: ReactNode;
  color: 'indigo' | 'emerald' | 'red' | 'amber' | 'blue' | 'violet';
  sub?: string;
}

const colorMap = {
  indigo: { bg: 'bg-indigo-50',  icon: 'text-indigo-600', value: 'text-indigo-700' },
  emerald:{ bg: 'bg-emerald-50', icon: 'text-emerald-600', value: 'text-emerald-700' },
  red:    { bg: 'bg-red-50',     icon: 'text-red-600',     value: 'text-red-700' },
  amber:  { bg: 'bg-amber-50',   icon: 'text-amber-600',   value: 'text-amber-700' },
  blue:   { bg: 'bg-blue-50',    icon: 'text-blue-600',    value: 'text-blue-700' },
  violet: { bg: 'bg-violet-50',  icon: 'text-violet-600',  value: 'text-violet-700' },
};

export default function StatCard({ label, value, icon, color, sub }: Props) {
  const c = colorMap[color];
  return (
    <div className="card p-5 flex items-start gap-4">
      <div className={`${c.bg} p-2.5 rounded-xl shrink-0`}>
        <span className={`${c.icon} w-5 h-5 block`}>{icon}</span>
      </div>
      <div className="min-w-0">
        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">{label}</p>
        <p className={`text-2xl font-bold mt-0.5 ${c.value}`}>{value}</p>
        {sub && <p className="text-xs text-slate-400 mt-0.5">{sub}</p>}
      </div>
    </div>
  );
}
