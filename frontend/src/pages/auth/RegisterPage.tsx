import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Zap, Eye, EyeOff, AlertCircle, CheckCircle2 } from 'lucide-react';
import { register as apiRegister } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';

const perks = [
  'Retail orders auto-appear in your store',
  'Target, Walmart, Costco & more',
  'Works with Shopify, WooCommerce & Amazon',
  'No EDI knowledge required',
  'Cancel anytime — no contracts',
];

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ fullName: '', email: '', companyName: '', password: '', confirmPassword: '' });
  const [showPw, setShowPw]   = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (form.password !== form.confirmPassword) { setError('Passwords do not match.'); return; }
    if (form.password.length < 8) { setError('Password must be at least 8 characters.'); return; }

    setLoading(true);
    try {
      const data = await apiRegister({
        email: form.email, password: form.password,
        fullName: form.fullName, companyName: form.companyName || form.fullName,
      });
      login(data);
      navigate(`/onboarding/${data.sellerId}`);
    } catch (err: any) {
      setError(err?.response?.data?.error ?? 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-indigo-950 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-4xl grid grid-cols-1 md:grid-cols-2 gap-8 items-center">

        {/* Left — value prop */}
        <div className="hidden md:block">
          <div className="flex items-center gap-3 mb-8">
            <div className="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center">
              <Zap className="w-5 h-5 text-white" />
            </div>
            <p className="text-white font-bold text-xl">NexaEDI</p>
          </div>
          <h1 className="text-3xl font-black text-white mb-3 leading-tight">
            Get compliant with Target<br />in one day.
          </h1>
          <p className="text-slate-400 mb-8">
            Stop paying $2,000/month to SPS Commerce. NexaEDI automates your EDI compliance
            for a fraction of the cost.
          </p>
          <div className="space-y-3">
            {perks.map(perk => (
              <div key={perk} className="flex items-center gap-3">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <p className="text-slate-300 text-sm">{perk}</p>
              </div>
            ))}
          </div>
          <div className="mt-8 p-4 bg-indigo-600/20 border border-indigo-500/30 rounded-2xl">
            <p className="text-indigo-300 text-sm font-semibold">Starting at $99/month</p>
            <p className="text-indigo-400/70 text-xs mt-1">No setup fees · No contracts · Cancel anytime</p>
          </div>
        </div>

        {/* Right — form */}
        <div>
          <div className="flex items-center gap-2 justify-center md:hidden mb-8">
            <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
              <Zap className="w-4 h-4 text-white" />
            </div>
            <p className="text-white font-bold">NexaEDI</p>
          </div>

          <div className="bg-white/5 backdrop-blur border border-white/10 rounded-2xl p-8">
            <h2 className="text-xl font-bold text-white mb-1">Create your account</h2>
            <p className="text-slate-400 text-sm mb-6">Free to start · Connect your store in 2 minutes</p>

            {error && (
              <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 rounded-xl p-3 mb-4">
                <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
                <p className="text-red-300 text-sm">{error}</p>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1.5">Your Name</label>
                  <input required value={form.fullName} onChange={set('fullName')} placeholder="Dave Mitchell"
                    className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm placeholder-slate-600 focus:outline-none focus:border-indigo-500/60 transition" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1.5">Company Name</label>
                  <input value={form.companyName} onChange={set('companyName')} placeholder="Mitchell Gadgets LLC"
                    className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm placeholder-slate-600 focus:outline-none focus:border-indigo-500/60 transition" />
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1.5">Work Email</label>
                <input type="email" required value={form.email} onChange={set('email')} placeholder="dave@yourstore.com"
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm placeholder-slate-600 focus:outline-none focus:border-indigo-500/60 transition" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1.5">Password</label>
                <div className="relative">
                  <input type={showPw ? 'text' : 'password'} required value={form.password} onChange={set('password')}
                    placeholder="Min 8 characters"
                    className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 pr-10 text-white text-sm placeholder-slate-600 focus:outline-none focus:border-indigo-500/60 transition" />
                  <button type="button" onClick={() => setShowPw(!showPw)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500">
                    {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1.5">Confirm Password</label>
                <input type="password" required value={form.confirmPassword} onChange={set('confirmPassword')}
                  placeholder="••••••••"
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm placeholder-slate-600 focus:outline-none focus:border-indigo-500/60 transition" />
              </div>

              <button type="submit" disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 rounded-xl transition-all flex items-center justify-center gap-2 disabled:opacity-60 mt-2">
                {loading ? <><span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Creating account...</> : 'Create Free Account →'}
              </button>
            </form>

            <p className="text-center text-slate-500 text-xs mt-4">
              Already have an account?{' '}
              <Link to="/login" className="text-indigo-400 hover:text-indigo-300 transition-colors">Sign in</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
