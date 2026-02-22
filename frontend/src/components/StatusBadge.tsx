import type { EdiStatus } from '../types';

const config: Record<EdiStatus, { label: string; classes: string; dot: string }> = {
  RECEIVED:     { label: 'Received',     classes: 'bg-blue-50 text-blue-700 border-blue-200',     dot: 'bg-blue-500' },
  PARSED:       { label: 'Parsed',       classes: 'bg-violet-50 text-violet-700 border-violet-200', dot: 'bg-violet-500' },
  VALIDATED:    { label: 'Validated',    classes: 'bg-purple-50 text-purple-700 border-purple-200', dot: 'bg-purple-500' },
  TRANSMITTED:  { label: 'Transmitted',  classes: 'bg-amber-50 text-amber-700 border-amber-200',   dot: 'bg-amber-500' },
  ACKNOWLEDGED: { label: 'Acknowledged', classes: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  FAILED:       { label: 'Failed',       classes: 'bg-red-50 text-red-700 border-red-200',         dot: 'bg-red-500' },
};

interface Props {
  status: EdiStatus;
  size?: 'sm' | 'md';
}

export default function StatusBadge({ status, size = 'sm' }: Props) {
  const c = config[status] ?? config.RECEIVED;
  return (
    <span className={`inline-flex items-center gap-1.5 border rounded-full font-medium ${c.classes} ${size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-sm'}`}>
      <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${c.dot}`} />
      {c.label}
    </span>
  );
}
