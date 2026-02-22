import { useState, useEffect, type ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import { fetchHealth } from '../api/client';

const pageTitles: Record<string, { title: string; subtitle: string }> = {
  '/':         { title: 'Dashboard',   subtitle: 'Pipeline health and recent activity' },
  '/ingest':   { title: 'Ingest EDI',  subtitle: 'Submit raw X12 EDI files for processing' },
  '/audit':    { title: 'Audit Trail', subtitle: 'Full lifecycle history for every processed file' },
  '/mappings': { title: 'Mappings',    subtitle: 'Retailer-specific EDI translation profiles' },
  '/dev':      { title: 'Dev Tools',   subtitle: 'Local development utilities and database inspector' },
};

export default function Layout({ children }: { children: ReactNode }) {
  const location = useLocation();
  const [apiOnline, setApiOnline] = useState(false);

  const path = '/' + location.pathname.split('/')[1];
  const page = pageTitles[path] ?? { title: 'NexaEDI', subtitle: '' };

  useEffect(() => {
    const check = () =>
      fetchHealth()
        .then(h => setApiOnline(h.status === 'UP'))
        .catch(() => setApiOnline(false));
    check();
    const id = setInterval(check, 15000);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="flex min-h-screen">
      <Sidebar apiOnline={apiOnline} />

      <div className="flex-1 ml-60 flex flex-col min-h-screen">
        {/* Top Bar */}
        <header className="sticky top-0 z-10 bg-white border-b border-slate-200 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-lg font-bold text-slate-900">{page.title}</h1>
            <p className="text-xs text-slate-500 mt-0.5">{page.subtitle}</p>
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-500 bg-slate-50 border border-slate-200 px-3 py-1.5 rounded-lg">
            <span className={`w-1.5 h-1.5 rounded-full ${apiOnline ? 'bg-emerald-500' : 'bg-red-500'}`} />
            {apiOnline ? 'Spring Boot · port 8080' : 'Backend Offline'}
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 px-8 py-6 animate-fade-in">
          {children}
        </main>
      </div>
    </div>
  );
}
