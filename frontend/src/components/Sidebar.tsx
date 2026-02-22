import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Upload, ClipboardList, Map,
  Wrench, Zap, ChevronRight,
} from 'lucide-react';

const navItems = [
  { to: '/',         label: 'Dashboard',    icon: LayoutDashboard },
  { to: '/ingest',   label: 'Ingest EDI',   icon: Upload },
  { to: '/audit',    label: 'Audit Trail',  icon: ClipboardList },
  { to: '/mappings', label: 'Mappings',     icon: Map },
];

const devItems = [
  { to: '/dev', label: 'Dev Tools', icon: Wrench },
];

interface Props {
  apiOnline: boolean;
}

export default function Sidebar({ apiOnline }: Props) {
  const location = useLocation();

  const NavItem = ({ to, label, icon: Icon }: { to: string; label: string; icon: typeof LayoutDashboard }) => {
    const active = to === '/' ? location.pathname === '/' : location.pathname.startsWith(to);
    return (
      <NavLink to={to} className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all group ${active ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800'}`}>
        <Icon className="w-4 h-4 shrink-0" />
        <span className="flex-1">{label}</span>
        {active && <ChevronRight className="w-3.5 h-3.5 opacity-70" />}
      </NavLink>
    );
  };

  return (
    <aside className="fixed inset-y-0 left-0 w-60 bg-slate-900 flex flex-col z-20 border-r border-slate-800">
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-slate-800">
        <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center shrink-0">
          <Zap className="w-4 h-4 text-white" />
        </div>
        <div>
          <p className="text-white font-bold text-sm leading-none">NexaEDI</p>
          <p className="text-slate-500 text-xs mt-0.5">Orchestration Platform</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        <p className="px-3 mb-2 text-xs font-semibold text-slate-600 uppercase tracking-widest">Main</p>
        {navItems.map(item => <NavItem key={item.to} {...item} />)}

        <div className="pt-4">
          <p className="px-3 mb-2 text-xs font-semibold text-slate-600 uppercase tracking-widest">Developer</p>
          {devItems.map(item => <NavItem key={item.to} {...item} />)}
        </div>
      </nav>

      {/* Footer */}
      <div className="px-4 py-4 border-t border-slate-800 space-y-3">
        <div className="flex items-center gap-2">
          <span className={`w-2 h-2 rounded-full shrink-0 ${apiOnline ? 'bg-emerald-400' : 'bg-red-400'}`} />
          <span className="text-xs text-slate-400">{apiOnline ? 'API Connected' : 'API Offline'}</span>
        </div>
        <p className="text-xs text-slate-600">v1.0.0-SNAPSHOT · local</p>
      </div>
    </aside>
  );
}
