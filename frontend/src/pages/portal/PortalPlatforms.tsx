import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { CheckCircle2, PlusCircle, Wifi, Activity } from 'lucide-react';
import { getPlatforms, getRetailers } from '../../api/portal';
import type { Platform, RetailerConn } from '../../api/portal';

const platformConfig: Record<string, { emoji: string; label: string; color: string }> = {
  SHOPIFY:       { emoji: '🛍️', label: 'Shopify',              color: 'border-emerald-500/30 bg-emerald-500/5' },
  WOOCOMMERCE:   { emoji: '🟣', label: 'WooCommerce',          color: 'border-violet-500/30 bg-violet-500/5' },
  AMAZON_SELLER: { emoji: '📦', label: 'Amazon Seller Central', color: 'border-amber-500/30 bg-amber-500/5' },
  BIGCOMMERCE:   { emoji: '🔵', label: 'BigCommerce',          color: 'border-blue-500/30 bg-blue-500/5' },
  ETSY:          { emoji: '🧡', label: 'Etsy',                 color: 'border-orange-500/30 bg-orange-500/5' },
  TIKTOK_SHOP:   { emoji: '🎵', label: 'TikTok Shop',          color: 'border-pink-500/30 bg-pink-500/5' },
  QUICKBOOKS:    { emoji: '📊', label: 'QuickBooks',           color: 'border-green-500/30 bg-green-500/5' },
};

const retailerColors: Record<string, string> = {
  TARGET:  'border-red-500/30 bg-red-500/5 text-red-300',
  WALMART: 'border-blue-500/30 bg-blue-500/5 text-blue-300',
  COSTCO:  'border-blue-500/30 bg-blue-500/5 text-blue-300',
  KROGER:  'border-emerald-500/30 bg-emerald-500/5 text-emerald-300',
  DEFAULT: 'border-slate-500/30 bg-slate-500/5 text-slate-300',
};

const ingestionIcons: Record<string, string> = { SFTP: '📂', AS2: '🔒', API: '⚡', EMAIL: '📧' };

const COMING_SOON = ['WOOCOMMERCE', 'BIGCOMMERCE', 'ETSY', 'TIKTOK_SHOP', 'QUICKBOOKS', 'NETSUITE'];

export default function PortalPlatforms() {
  const { sellerId } = useParams<{ sellerId: string }>();
  const [platforms, setPlatforms] = useState<Platform[]>([]);
  const [retailers, setRetailers] = useState<RetailerConn[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getPlatforms(Number(sellerId)), getRetailers(Number(sellerId))])
      .then(([p, r]) => { setPlatforms(p); setRetailers(r); })
      .catch(() => {}).finally(() => setLoading(false));
  }, [sellerId]);

  if (loading) return (
    <div className="flex justify-center py-16">
      <div className="w-6 h-6 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
    </div>
  );

  return (
    <div className="space-y-8 max-w-4xl">
      <div>
        <h1 className="text-2xl font-black text-white">Connected Platforms</h1>
        <p className="text-slate-400 text-sm mt-1">Where your retail orders are automatically delivered.</p>
      </div>

      {/* Connected selling platforms */}
      <section>
        <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">Your Selling Platforms</h2>
        <div className="grid grid-cols-2 gap-4">
          {platforms.map(p => {
            const cfg = platformConfig[p.platformType] ?? { emoji: '🏪', label: p.platformType, color: 'border-slate-500/30 bg-slate-500/5' };
            return (
              <div key={p.id} className={`border rounded-2xl p-5 ${cfg.color}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="text-2xl">{cfg.emoji}</span>
                    <div>
                      <p className="text-white font-bold text-sm">{p.platformName}</p>
                      <p className="text-slate-500 text-xs">{cfg.label}</p>
                      {p.platformUrl && <p className="text-slate-600 text-xs font-mono mt-0.5">{p.platformUrl}</p>}
                    </div>
                  </div>
                  <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1 rounded-full shrink-0">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    Connected
                  </span>
                </div>
                <div className="mt-4 pt-4 border-t border-white/5 flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-xs text-slate-400">
                    <Activity className="w-3.5 h-3.5" />
                    {p.ordersSynced} orders synced
                  </div>
                  <div className="text-xs text-slate-600">
                    Since {new Date(p.connectedAt).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })}
                  </div>
                </div>
              </div>
            );
          })}

          {/* Add Platform CTA */}
          <button className="border border-dashed border-white/10 rounded-2xl p-5 flex flex-col items-center justify-center gap-2 hover:border-indigo-500/30 hover:bg-indigo-500/5 transition-all group">
            <PlusCircle className="w-6 h-6 text-slate-600 group-hover:text-indigo-400 transition-colors" />
            <p className="text-slate-500 group-hover:text-slate-300 text-sm font-medium transition-colors">Add Platform</p>
            <p className="text-slate-600 text-xs text-center">Connect WooCommerce, Amazon, or any selling platform</p>
          </button>
        </div>
      </section>

      {/* Retailer Connections */}
      <section>
        <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">Retailer Connections</h2>
        <p className="text-slate-500 text-xs mb-4">Orders from these retailers are automatically picked up and routed to your store.</p>
        <div className="space-y-3">
          {retailers.map(r => {
            const colClass = retailerColors[r.retailerId] ?? retailerColors.DEFAULT;
            return (
              <div key={r.id} className={`border rounded-2xl p-5 ${colClass.split(' ').slice(0, 2).join(' ')}`}>
                <div className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg border ${colClass.split(' ').slice(0, 2).join(' ')}`}>
                      🏬
                    </div>
                    <div>
                      <p className="text-white font-bold">{r.retailerName}</p>
                      <div className="flex items-center gap-3 mt-1">
                        <span className="flex items-center gap-1 text-xs text-slate-400">
                          {ingestionIcons[r.ingestionMethod] ?? '📡'} {r.ingestionMethod}
                        </span>
                        <span className="text-xs text-slate-600">·</span>
                        <span className="text-xs text-slate-400">{r.totalOrdersReceived} orders total</span>
                        {r.lastOrderReceivedAt && (
                          <>
                            <span className="text-xs text-slate-600">·</span>
                            <span className="text-xs text-slate-400">
                              Last: {new Date(r.lastOrderReceivedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1 rounded-full">
                      <Wifi className="w-3 h-3" /> Active
                    </span>
                  </div>
                </div>
              </div>
            );
          })}

          <button className="w-full border border-dashed border-white/10 rounded-2xl p-4 flex items-center justify-center gap-2 hover:border-indigo-500/30 hover:bg-indigo-500/5 transition-all group text-sm">
            <PlusCircle className="w-4 h-4 text-slate-600 group-hover:text-indigo-400 transition-colors" />
            <span className="text-slate-500 group-hover:text-slate-300 transition-colors">Add Retailer Connection</span>
          </button>
        </div>
      </section>

      {/* Coming Soon platforms */}
      <section>
        <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">More Platforms Coming Soon</h2>
        <div className="grid grid-cols-3 gap-3">
          {COMING_SOON.map(type => {
            const cfg = platformConfig[type] ?? { emoji: '🔌', label: type, color: '' };
            return (
              <div key={type} className="border border-white/5 rounded-xl p-4 flex items-center gap-3 opacity-50">
                <span className="text-xl">{cfg.emoji}</span>
                <div>
                  <p className="text-white text-xs font-medium">{cfg.label}</p>
                  <p className="text-slate-600 text-xs">Coming soon</p>
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
