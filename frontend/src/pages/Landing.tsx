import { Link } from 'react-router-dom';
import {
  Upload,
  GitBranch,
  Shield,
  ClipboardCheck,
  Lock,
  FileCheck,
  Server,
} from 'lucide-react';

const navLinks = [
  { href: '#features', label: 'Features' },
  { href: '#security', label: 'Security' },
  { href: '#contact', label: 'Contact' },
];

const features = [
  {
    icon: Upload,
    title: 'High-performance ingestion',
    description: 'Submit and process X12 EDI at scale with low latency. Built for high-volume B2B flows and real-time order sync.',
  },
  {
    icon: GitBranch,
    title: 'Workflow orchestration',
    description: 'Translate, validate, and route documents with configurable pipelines and retailer-specific mappings.',
  },
  {
    icon: Shield,
    title: 'Secure partner management',
    description: 'Manage trading partners and credentials in one place. Role-based access and JWT authentication.',
  },
  {
    icon: ClipboardCheck,
    title: 'Observability & audit logs',
    description: 'Full lifecycle history for every file. Trace, debug, and comply with complete audit trails.',
  },
];

const securityItems = [
  { icon: Lock, title: 'Authentication', description: 'JWT-based API security and session management' },
  { icon: FileCheck, title: 'Audit & compliance', description: 'End-to-end audit logging for every transaction' },
  { icon: Server, title: 'Cloud-native', description: 'Container-ready deployment, runs anywhere' },
];

export default function Landing(): JSX.Element {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-indigo-950 to-slate-900 text-white cursor-default">
      {/* ── Sticky navigation ── */}
      <header className="sticky top-0 z-50 bg-slate-950/80 backdrop-blur-xl border-b border-white/10">
        <nav className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link
            to="/"
            className="text-lg font-bold text-white tracking-tight hover:text-indigo-300 transition-colors cursor-pointer"
          >
            NexaEDI
          </Link>
          <div className="flex items-center gap-8">
            {navLinks.map(({ href, label }) => (
              <a
                key={href}
                href={href}
                className="text-sm font-medium text-slate-400 hover:text-white transition-colors cursor-pointer"
              >
                {label}
              </a>
            ))}
            <Link
              to="/login"
              className="text-sm font-medium text-slate-400 hover:text-white transition-colors cursor-pointer"
            >
              Login
            </Link>
            <Link
              to="/signup"
              className="inline-flex items-center justify-center px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-500 transition-colors cursor-pointer"
            >
              Get Started
            </Link>
          </div>
        </nav>
      </header>

      <main>
        {/* ── Hero ── */}
        <section className="max-w-6xl mx-auto px-6 pt-24 pb-32">
          <h1 className="text-4xl font-bold text-white tracking-tight sm:text-5xl max-w-3xl">
            Cloud-native EDI orchestration for modern B2B
          </h1>
          <p className="mt-6 text-xl text-slate-400 max-w-2xl">
            Ingest, translate, and route EDI at scale. Built for infrastructure teams who need reliability, visibility, and control.
          </p>
          <div className="mt-10 flex flex-wrap gap-4">
            <Link
              to="/signup"
              className="inline-flex items-center justify-center px-6 py-3 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-500 transition-colors cursor-pointer"
            >
              Get Started
            </Link>
            <Link
              to="/login"
              className="inline-flex items-center justify-center px-6 py-3 text-sm font-medium text-slate-300 bg-white/10 border border-white/20 rounded-lg hover:bg-white/15 transition-colors cursor-pointer"
            >
              Login
            </Link>
          </div>
        </section>

        {/* ── Features ── */}
        <section id="features" className="border-t border-white/10">
          <div className="max-w-6xl mx-auto px-6 py-24">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-widest">
              Features
            </p>
            <h2 className="mt-3 text-2xl font-bold text-white sm:text-3xl">
              Everything you need to run EDI in production
            </h2>
            <p className="mt-3 text-slate-400 max-w-xl">
              Built for technical teams. No lock-in, full control.
            </p>
            <div className="mt-16 grid gap-6 sm:grid-cols-2">
              {features.map(({ icon: Icon, title, description }) => (
                <div
                  key={title}
                  className="group rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur-sm transition-colors hover:border-white/20 hover:bg-white/[0.07]"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600/20 text-indigo-400">
                    <Icon className="h-5 w-5" />
                  </div>
                  <h3 className="mt-4 text-lg font-semibold text-white">{title}</h3>
                  <p className="mt-2 text-sm text-slate-400 leading-relaxed">
                    {description}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Security & Trust ── */}
        <section id="security" className="border-t border-white/10 bg-white/[0.02]">
          <div className="max-w-6xl mx-auto px-6 py-24">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-widest">
              Security & Trust
            </p>
            <h2 className="mt-3 text-2xl font-bold text-white sm:text-3xl">
              Enterprise-grade by design
            </h2>
            <p className="mt-3 text-slate-400 max-w-xl">
              Security and operational transparency for regulated environments.
            </p>
            <div className="mt-14 grid gap-6 sm:grid-cols-3">
              {securityItems.map(({ icon: Icon, title, description }) => (
                <div
                  key={title}
                  className="rounded-2xl border border-white/10 bg-white/5 px-6 py-6 backdrop-blur-sm"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400">
                    <Icon className="h-5 w-5" />
                  </div>
                  <h3 className="mt-4 text-base font-semibold text-white">{title}</h3>
                  <p className="mt-2 text-sm text-slate-400 leading-relaxed">
                    {description}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── CTA ── */}
        <section className="border-t border-white/10">
          <div className="max-w-6xl mx-auto px-6 py-24 text-center">
            <h2 className="text-2xl font-bold text-white sm:text-3xl">
              Ready to simplify your EDI pipeline?
            </h2>
            <p className="mt-4 text-slate-400 max-w-xl mx-auto">
              Get started in minutes. No credit card required.
            </p>
            <div className="mt-10">
              <Link
                to="/signup"
                className="inline-flex items-center justify-center px-6 py-3 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-500 transition-colors cursor-pointer"
              >
                Get Started
              </Link>
            </div>
          </div>
        </section>
      </main>

      {/* ── Footer ── */}
      <footer id="contact" className="border-t border-white/10">
        <div className="max-w-6xl mx-auto px-6 py-12 flex flex-col sm:flex-row items-center justify-between gap-6">
          <span className="text-sm font-semibold text-white">NexaEDI</span>
          <div className="flex items-center gap-6 text-sm text-slate-400">
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors cursor-pointer"
            >
              GitHub
            </a>
            <a
              href="mailto:contact@nexaedi.com"
              className="hover:text-white transition-colors cursor-pointer"
            >
              contact@nexaedi.com
            </a>
          </div>
        </div>
        <div className="max-w-6xl mx-auto px-6 pb-8 text-center sm:text-left">
          <p className="text-xs text-slate-500">
            © {new Date().getFullYear()} NexaEDI. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
