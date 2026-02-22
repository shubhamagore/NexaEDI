import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TrendingUp, ShoppingBag, CheckCircle2, AlertCircle, Clock, ArrowRight, RefreshCw } from 'lucide-react';
import { getDashboard, getSeller } from '../../api/portal';
import type { Dashboard, SellerDetail, Order } from '../../api/portal';

const statusConfig: Record<string, { label: string; color: string; dot: string }> = {
  RECEIVED:     { label: 'Received',         color: 'text-blue-400 bg-blue-500/10 border-blue-500/20',    dot: 'bg-blue-400' },
  PROCESSING:   { label: 'Processing',       color: 'text-amber-400 bg-amber-500/10 border-amber-500/20', dot: 'bg-amber-400' },
  SYNCED:       { label: 'Synced to Store',  color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20', dot: 'bg-emerald-400' },
  ACKNOWLEDGED: { label: 'Complete',         color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20', dot: 'bg-emerald-400' },
  FAILED:       { label: 'Needs Attention',  color: 'text-red-400 bg-red-500/10 border-red-500/20',       dot: 'bg-red-400' },
};

const platformIcons: Record<string, string> = {
  SHOPIFY: '🛍️', WOOCOMMERCE: '🟣', AMAZON_SELLER: '📦',
  BIGCOMMERCE: '🔵', ETSY: '🧡',
};

const retailerColors: Record<string, string> = {
  TARGET:  'bg-red-500/20 text-red-300 border-red-500/30',
  WALMART: 'bg-blue-500/20 text-blue-300 border-blue-500/30',
  COSTCO:  'bg-blue-500/20 text-blue-300 border-blue-500/30',
  KROGER:  'bg-emerald-500/20 text-emerald-300 border-emerald-500/30',
  DEFAULT: 'bg-slate-500/20 text-slate-300 border-slate-500/30',
};

function fmt(val: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(val);
}

function timeAgo(iso: string) {
  const diff = Date.now() - new Date(iso).getTime();
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(hours / 24);
  if (days > 0) return `${days}d ago`;
  if (hours > 0) return `${hours}h ago`;
  return 'Just now';
}

function MetricCard({ label, value, sub, icon, color }: {
  label: string; value: string | number; sub?: string;
  icon: typeof TrendingUp; color: string;
}) {
  const Icon = icon;
  return (
    <div className="bg-slate-900 border border-white/5 rounded-2xl p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{label}</p>
          <p className={`text-3xl font-black mt-1 ${color}`}>{value}</p>
          {sub && <p className="text-xs text-slate-500 mt-1">{sub}</p>}
        </div>
        <div className={`p-2.5 rounded-xl ${color.replace('text-', 'bg-').replace('-400', '-500/10')}`}>
          <Icon className={`w-5 h-5 ${color}`} />
        </div>
      </div>
    </div>
  );
}

export default function PortalDashboard() {
  const { sellerId } = useParams<{ sellerId: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<Dashboard | null>(null);
  const [seller, setSeller] = useState<SellerDetail | null>(null);
  const [loading, setLoading] = useState(true);

  const load = () => {
    const id = Number(sellerId);
    Promise.all([getDashboard(id), getSeller(id)])
      .then(([d, s]) => { setData(d); setSeller(s); })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [sellerId]);

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
    </div>
  );

  if (!data) return (
    <div className="flex flex-col items-center justify-center h-64 gap-3">
      <div className="text-red-400 text-4xl">⚠️</div>
      <p className="text-white font-semibold">Could not load dashboard</p>
      <p className="text-slate-400 text-sm">Make sure the Spring Boot app is running on port 8080</p>
      <button onClick={load} className="mt-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm rounded-xl transition-colors">
        Try Again
      </button>
    </div>
  );

  const d = data;

  return (
    <div className="space-y-6 max-w-6xl">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-black text-white">
            Good morning, {seller?.name.split(' ')[0]} 👋
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Here's what's happening with your retail orders.
          </p>
        </div>
        <button onClick={load} className="flex items-center gap-2 bg-white/5 hover:bg-white/10 border border-white/10 text-slate-400 hover:text-white px-4 py-2 rounded-xl text-sm transition-all">
          <RefreshCw className="w-3.5 h-3.5" /> Refresh
        </button>
      </div>

      {/* Failed alert */}
      {d.failedOrders > 0 && (
        <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-400 shrink-0" />
          <div className="flex-1">
            <p className="text-red-300 text-sm font-semibold">
              {d.failedOrders} order{d.failedOrders > 1 ? 's' : ''} need your attention
            </p>
            <p className="text-red-400/70 text-xs mt-0.5">
              A product SKU may not be in your store catalog. Click to review.
            </p>
          </div>
          <button onClick={() => navigate(`/portal/${sellerId}/orders?status=FAILED`)}
            className="text-xs font-semibold text-red-300 hover:text-white bg-red-500/20 px-3 py-1.5 rounded-lg transition-colors">
            Review
          </button>
        </div>
      )}

      {/* Metrics */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard label="Revenue This Month" value={fmt(d.revenueThisMonth)}
          sub={`${fmt(d.revenueThisWeek)} this week`} icon={TrendingUp} color="text-emerald-400" />
        <MetricCard label="Orders This Month" value={d.ordersThisMonth}
          sub={`${d.totalOrders} total all time`} icon={ShoppingBag} color="text-indigo-400" />
        <MetricCard label="Synced to Store" value={d.syncedOrders}
          sub={`${d.successRate}% success rate`} icon={CheckCircle2} color="text-emerald-400" />
        <MetricCard label="Pending / In Progress" value={d.pendingOrders}
          sub={d.failedOrders > 0 ? `${d.failedOrders} failed` : 'All clear'} icon={Clock}
          color={d.failedOrders > 0 ? 'text-red-400' : 'text-slate-400'} />
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Revenue by Retailer */}
        <div className="bg-slate-900 border border-white/5 rounded-2xl p-5 col-span-1">
          <h2 className="text-sm font-bold text-white mb-4">Revenue by Retailer</h2>
          <div className="space-y-3">
            {d.revenueByRetailer.map(r => {
              const pct = d.revenueThisMonth > 0 ? (r.revenue / d.revenueThisMonth) * 100 : 0;
              const colClass = retailerColors[r.retailerId] ?? retailerColors.DEFAULT;
              return (
                <div key={r.retailerId}>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className={`text-xs font-bold px-2 py-0.5 rounded-full border ${colClass}`}>
                      {r.retailerName}
                    </span>
                    <span className="text-sm font-bold text-white">{fmt(r.revenue)}</span>
                  </div>
                  <div className="h-1.5 bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-indigo-500 rounded-full transition-all"
                      style={{ width: `${Math.max(pct, 3)}%` }} />
                  </div>
                  {r.lastOrderAt && (
                    <p className="text-xs text-slate-600 mt-1">Last order {timeAgo(r.lastOrderAt)}</p>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Recent Orders */}
        <div className="bg-slate-900 border border-white/5 rounded-2xl col-span-2 overflow-hidden">
          <div className="flex items-center justify-between px-5 py-4 border-b border-white/5">
            <h2 className="text-sm font-bold text-white">Recent Orders</h2>
            <button onClick={() => navigate(`/portal/${sellerId}/orders`)}
              className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1">
              View all <ArrowRight className="w-3 h-3" />
            </button>
          </div>
          <div className="divide-y divide-white/5">
            {d.recentOrders.map((order: Order) => {
              const st = statusConfig[order.status] ?? statusConfig.RECEIVED;
              const rColor = retailerColors[order.retailerId] ?? retailerColors.DEFAULT;
              return (
                <button key={order.id} onClick={() => navigate(`/portal/${sellerId}/orders/${order.id}`)}
                  className="w-full flex items-center gap-4 px-5 py-4 hover:bg-white/3 transition-colors text-left">
                  <div className="text-xl shrink-0">{platformIcons[order.platform] ?? '🏪'}</div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold text-white truncate">{order.poNumber}</p>
                      <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${rColor}`}>
                        {order.retailerId}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">{timeAgo(order.receivedAt)} · {order.lineItemCount} item{order.lineItemCount !== 1 ? 's' : ''}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-sm font-bold text-white">{fmt(order.orderValue)}</p>
                    <span className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full border mt-1 ${st.color}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${st.dot}`} />
                      {st.label}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
