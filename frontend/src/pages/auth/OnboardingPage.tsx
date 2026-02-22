import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, ExternalLink, AlertCircle, Zap, Store, Wifi } from 'lucide-react';
import { connectShopify } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';

const steps = ['Connect Store', 'Add Retailer', 'Done'];

export default function OnboardingPage() {
  const { sellerId } = useParams<{ sellerId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [step, setStep] = useState(0);
  const [storeDomain, setStoreDomain] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const [connecting, setConnecting] = useState(false);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState('');
  const [shopName, setShopName] = useState('');

  const handleConnect = async () => {
    if (!storeDomain || !accessToken) { setError('Both fields are required.'); return; }
    setError('');
    setConnecting(true);
    try {
      const result = await connectShopify(Number(sellerId), storeDomain, accessToken, user!.token);
      setShopName(result.shopName);
      setConnected(true);
      setTimeout(() => setStep(1), 1200);
    } catch (err: any) {
      setError(err?.response?.data?.error ?? 'Connection failed. Check your credentials.');
    } finally {
      setConnecting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-indigo-950 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Zap className="w-6 h-6 text-indigo-400" />
            <span className="text-white font-bold text-lg">NexaEDI Setup</span>
          </div>
          <h1 className="text-2xl font-black text-white">Welcome, {user?.name?.split(' ')[0]}! 👋</h1>
          <p className="text-slate-400 text-sm mt-2">Let's connect your store in 2 minutes.</p>
        </div>

        {/* Progress */}
        <div className="flex items-center justify-center gap-2 mb-8">
          {steps.map((s, i) => (
            <div key={s} className="flex items-center gap-2">
              <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${i === step ? 'bg-indigo-600 text-white' : i < step ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/30' : 'bg-white/5 text-slate-500'}`}>
                {i < step ? <CheckCircle2 className="w-3.5 h-3.5" /> : <span>{i + 1}</span>}
                {s}
              </div>
              {i < steps.length - 1 && <div className={`w-8 h-px ${i < step ? 'bg-emerald-500/50' : 'bg-white/10'}`} />}
            </div>
          ))}
        </div>

        {/* Step 0: Connect Shopify */}
        {step === 0 && (
          <div className="bg-white/5 border border-white/10 rounded-2xl p-8">
            {connected ? (
              <div className="text-center py-4">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
                <p className="text-white font-bold text-lg">Connected!</p>
                <p className="text-slate-400 text-sm">{shopName} is now linked to NexaEDI</p>
              </div>
            ) : (
              <>
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-10 h-10 bg-emerald-600/20 rounded-xl flex items-center justify-center text-xl">🛍️</div>
                  <div>
                    <h2 className="text-white font-bold">Connect Your Shopify Store</h2>
                    <p className="text-slate-400 text-sm">Orders from retailers will appear here automatically</p>
                  </div>
                </div>

                {/* How to get token */}
                <div className="bg-indigo-600/10 border border-indigo-500/20 rounded-xl p-4 mb-6">
                  <p className="text-indigo-300 text-xs font-semibold uppercase tracking-wide mb-2">How to get your Shopify access token</p>
                  <ol className="text-slate-300 text-sm space-y-1.5 list-none">
                    <li className="flex items-start gap-2"><span className="w-5 h-5 bg-indigo-600/30 rounded-full flex items-center justify-center text-xs shrink-0 mt-0.5">1</span>Go to your Shopify Admin → Settings → Apps</li>
                    <li className="flex items-start gap-2"><span className="w-5 h-5 bg-indigo-600/30 rounded-full flex items-center justify-center text-xs shrink-0 mt-0.5">2</span>Click <strong className="text-white">"Develop apps"</strong> → Create an app named "NexaEDI"</li>
                    <li className="flex items-start gap-2"><span className="w-5 h-5 bg-indigo-600/30 rounded-full flex items-center justify-center text-xs shrink-0 mt-0.5">3</span>Under API scopes → enable: <code className="bg-white/10 px-1 rounded text-xs">write_draft_orders, read_products</code></li>
                    <li className="flex items-start gap-2"><span className="w-5 h-5 bg-indigo-600/30 rounded-full flex items-center justify-center text-xs shrink-0 mt-0.5">4</span>Click <strong className="text-white">"Install app"</strong> → copy the Admin API access token</li>
                  </ol>
                  <a href="https://admin.shopify.com" target="_blank" rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 mt-3 text-indigo-400 hover:text-indigo-300 text-xs font-medium transition-colors">
                    <ExternalLink className="w-3.5 h-3.5" /> Open Shopify Admin
                  </a>
                </div>

                {error && (
                  <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/20 rounded-xl p-3 mb-4">
                    <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
                    <p className="text-red-300 text-sm">{error}</p>
                  </div>
                )}

                <div className="space-y-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1.5">Store Domain</label>
                    <input value={storeDomain} onChange={e => setStoreDomain(e.target.value)}
                      placeholder="your-store.myshopify.com"
                      className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white text-sm placeholder-slate-600 focus:outline-none focus:border-indigo-500/60 transition font-mono" />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1.5">Admin API Access Token</label>
                    <input type="password" value={accessToken} onChange={e => setAccessToken(e.target.value)}
                      placeholder="shpat_xxxxxxxxxxxxxxxxxxxxxx"
                      className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white text-sm placeholder-slate-600 focus:outline-none focus:border-indigo-500/60 transition font-mono" />
                  </div>
                  <button onClick={handleConnect} disabled={connecting}
                    className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 rounded-xl flex items-center justify-center gap-2 disabled:opacity-60 transition-all mt-2">
                    {connecting
                      ? <><span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Connecting...</>
                      : <><Store className="w-4 h-4" /> Connect Shopify Store</>}
                  </button>
                  <button onClick={() => setStep(1)} className="w-full text-slate-500 hover:text-slate-300 text-sm py-2 transition-colors">
                    Skip for now →
                  </button>
                </div>
              </>
            )}
          </div>
        )}

        {/* Step 1: Add Retailer */}
        {step === 1 && (
          <div className="bg-white/5 border border-white/10 rounded-2xl p-8">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 bg-blue-600/20 rounded-xl flex items-center justify-center">
                <Wifi className="w-5 h-5 text-blue-400" />
              </div>
              <div>
                <h2 className="text-white font-bold">Which retailer sent you a PO?</h2>
                <p className="text-slate-400 text-sm">We'll set up the EDI connection for you</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 mb-6">
              {[
                { name: 'Target', emoji: '🎯', color: 'border-red-500/30 hover:bg-red-500/10' },
                { name: 'Walmart', emoji: '🔵', color: 'border-blue-500/30 hover:bg-blue-500/10' },
                { name: 'Costco', emoji: '🏬', color: 'border-blue-400/30 hover:bg-blue-400/10' },
                { name: 'Kroger', emoji: '🛒', color: 'border-emerald-500/30 hover:bg-emerald-500/10' },
                { name: 'Home Depot', emoji: '🔨', color: 'border-orange-500/30 hover:bg-orange-500/10' },
                { name: 'Other', emoji: '🏪', color: 'border-slate-500/30 hover:bg-slate-500/10' },
              ].map(r => (
                <button key={r.name} onClick={() => setStep(2)}
                  className={`border rounded-xl p-4 text-left transition-all ${r.color} flex items-center gap-3`}>
                  <span className="text-2xl">{r.emoji}</span>
                  <span className="text-white font-semibold text-sm">{r.name}</span>
                </button>
              ))}
            </div>
            <p className="text-slate-500 text-xs text-center">
              We'll email you setup instructions after you select your retailer. Our team handles the rest.
            </p>
          </div>
        )}

        {/* Step 2: Done */}
        {step === 2 && (
          <div className="bg-white/5 border border-white/10 rounded-2xl p-8 text-center">
            <div className="w-16 h-16 bg-emerald-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="w-8 h-8 text-emerald-400" />
            </div>
            <h2 className="text-white font-black text-xl mb-2">You're all set!</h2>
            <p className="text-slate-400 text-sm mb-6">
              {connected
                ? `${shopName} is connected. Retail orders will now appear in your store automatically.`
                : "Your account is ready. Connect your store anytime from Settings."}
            </p>
            <div className="space-y-3">
              <button onClick={() => navigate(`/portal/${sellerId}`)}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 rounded-xl transition-all">
                Go to My Dashboard →
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
