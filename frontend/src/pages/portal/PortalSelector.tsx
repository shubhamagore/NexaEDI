import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store, ArrowRight, Zap, CheckCircle2 } from 'lucide-react';
import { listSellers } from '../../api/portal';
import type { SellerSummary } from '../../api/portal';

const planColors: Record<string, string> = {
  STARTER: 'bg-slate-100 text-slate-700',
  GROWTH:  'bg-indigo-100 text-indigo-700',
  PRO:     'bg-amber-100 text-amber-700',
};

const planBadge: Record<string, string> = {
  STARTER: '⭐ Starter · $99/mo',
  GROWTH:  '🚀 Growth · $249/mo',
  PRO:     '💎 Pro · $499/mo',
};

export default function PortalSelector() {
  const navigate = useNavigate();
  const [sellers, setSellers] = useState<SellerSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listSellers().then(setSellers).catch(() => {}).finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-950 via-slate-900 to-slate-800 flex flex-col items-center justify-center p-6">
      {/* Logo */}
      <div className="flex items-center gap-3 mb-10">
        <div className="w-10 h-10 bg-indigo-500 rounded-xl flex items-center justify-center">
          <Zap className="w-5 h-5 text-white" />
        </div>
        <div>
          <p className="text-white font-bold text-xl leading-none">NexaEDI</p>
          <p className="text-indigo-400 text-sm">Seller Portal · Demo Mode</p>
        </div>
      </div>

      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-white">Choose a Seller Account</h1>
          <p className="text-slate-400 text-sm mt-2">
            This is a demo with pre-seeded data. In production, sellers log in with their own credentials.
          </p>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <div className="w-8 h-8 border-4 border-indigo-400/30 border-t-indigo-400 rounded-full animate-spin" />
          </div>
        ) : (
          <div className="space-y-3">
            {sellers.map(seller => (
              <button
                key={seller.id}
                onClick={() => navigate(`/portal/${seller.id}`)}
                className="w-full group bg-white/5 hover:bg-white/10 border border-white/10 hover:border-indigo-400/50 rounded-2xl p-5 text-left transition-all duration-200"
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 bg-indigo-600/30 rounded-xl flex items-center justify-center shrink-0">
                    <Store className="w-5 h-5 text-indigo-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-white font-semibold">{seller.name}</p>
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${planColors[seller.plan]}`}>
                        {planBadge[seller.plan]}
                      </span>
                    </div>
                    <p className="text-slate-400 text-sm">{seller.company}</p>
                    <p className="text-slate-500 text-xs">{seller.email}</p>
                  </div>
                  <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-indigo-400 transition-colors shrink-0" />
                </div>
              </button>
            ))}
          </div>
        )}

        <div className="mt-8 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-start gap-3">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <p className="text-emerald-300 text-xs">
            All 3 sellers have pre-seeded realistic order data. Dave (Shopify/Target), Maria (WooCommerce/Walmart), and Chen (Amazon/Costco).
          </p>
        </div>
      </div>
    </div>
  );
}
