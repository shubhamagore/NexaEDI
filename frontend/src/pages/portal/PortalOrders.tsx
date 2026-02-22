import { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { Search, ArrowRight, RefreshCw, Filter } from 'lucide-react';
import { getOrders, getRetailers } from '../../api/portal';
import type { Order, RetailerConn } from '../../api/portal';

const statusConfig: Record<string, { label: string; color: string; dot: string }> = {
  RECEIVED:     { label: 'Received',        color: 'text-blue-400 bg-blue-500/10 border-blue-500/20',         dot: 'bg-blue-400' },
  PROCESSING:   { label: 'Processing',      color: 'text-amber-400 bg-amber-500/10 border-amber-500/20',       dot: 'bg-amber-400' },
  SYNCED:       { label: 'Synced to Store', color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20', dot: 'bg-emerald-400' },
  ACKNOWLEDGED: { label: 'Complete',        color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20', dot: 'bg-emerald-400' },
  FAILED:       { label: 'Failed',          color: 'text-red-400 bg-red-500/10 border-red-500/20',             dot: 'bg-red-400' },
};

const platformIcons: Record<string, string> = {
  SHOPIFY: '🛍️', WOOCOMMERCE: '🟣', AMAZON_SELLER: '📦', BIGCOMMERCE: '🔵', ETSY: '🧡',
};

function fmt(val: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
}

export default function PortalOrders() {
  const { sellerId } = useParams<{ sellerId: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [orders, setOrders] = useState<Order[]>([]);
  const [retailers, setRetailers] = useState<RetailerConn[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState(searchParams.get('status') ?? 'ALL');
  const [filterRetailer, setFilterRetailer] = useState('ALL');

  const load = () => {
    const id = Number(sellerId);
    setLoading(true);
    Promise.all([
      getOrders(id),
      getRetailers(id),
    ]).then(([o, r]) => { setOrders(o); setRetailers(r); })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [sellerId]);

  const filtered = orders.filter(o => {
    const matchSearch = !search ||
      o.poNumber.toLowerCase().includes(search.toLowerCase()) ||
      o.retailerId.toLowerCase().includes(search.toLowerCase());
    const matchStatus = filterStatus === 'ALL' || o.status === filterStatus;
    const matchRetailer = filterRetailer === 'ALL' || o.retailerId === filterRetailer;
    return matchSearch && matchStatus && matchRetailer;
  });

  const total = orders.length;
  const totalRevenue = orders.reduce((s, o) => s + o.orderValue, 0);

  return (
    <div className="space-y-5 max-w-5xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-white">My Orders</h1>
          <p className="text-slate-400 text-sm mt-1">
            {total} total orders · {fmt(totalRevenue)} in revenue
          </p>
        </div>
        <button onClick={load} className="flex items-center gap-2 bg-white/5 hover:bg-white/10 border border-white/10 text-slate-400 hover:text-white px-3 py-2 rounded-xl text-sm transition-all">
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Filters */}
      <div className="bg-slate-900 border border-white/5 rounded-2xl p-4 flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
          <input
            className="w-full bg-white/5 border border-white/10 rounded-xl pl-9 pr-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/30 transition"
            placeholder="Search PO number or retailer..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-slate-500" />
          <select
            className="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500/50 transition"
            value={filterRetailer}
            onChange={e => setFilterRetailer(e.target.value)}
          >
            <option value="ALL">All Retailers</option>
            {retailers.map(r => <option key={r.retailerId} value={r.retailerId}>{r.retailerName}</option>)}
          </select>
        </div>

        <div className="flex gap-1.5 flex-wrap">
          {['ALL', 'SYNCED', 'ACKNOWLEDGED', 'PROCESSING', 'FAILED'].map(s => (
            <button
              key={s}
              onClick={() => setFilterStatus(s)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border ${
                filterStatus === s
                  ? s === 'ALL' ? 'bg-white text-slate-900 border-white' : `${statusConfig[s]?.color ?? ''} border`
                  : 'bg-white/5 text-slate-500 border-white/10 hover:text-white hover:border-white/20'
              }`}
            >
              {s === 'ALL' ? 'All' : statusConfig[s]?.label ?? s}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-slate-900 border border-white/5 rounded-2xl overflow-hidden">
        <div className="px-5 py-3 border-b border-white/5">
          <p className="text-xs text-slate-500">{filtered.length} order{filtered.length !== 1 ? 's' : ''}</p>
        </div>

        {loading ? (
          <div className="flex justify-center py-16">
            <div className="w-6 h-6 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center">
            <p className="text-slate-500 text-sm">No orders found matching your filters.</p>
          </div>
        ) : (
          <div className="divide-y divide-white/5">
            {filtered.map(order => {
              const st = statusConfig[order.status] ?? statusConfig.RECEIVED;
              return (
                <button
                  key={order.id}
                  onClick={() => navigate(`/portal/${sellerId}/orders/${order.id}`)}
                  className="w-full flex items-center gap-4 px-5 py-4 hover:bg-white/3 transition-colors text-left group"
                >
                  <div className="text-xl shrink-0">{platformIcons[order.platform] ?? '🏪'}</div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-sm font-bold text-white">{order.poNumber}</p>
                      <span className="text-xs font-semibold bg-white/10 text-slate-300 px-2 py-0.5 rounded-full">
                        {order.retailerName}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {order.lineItemCount} item{order.lineItemCount !== 1 ? 's' : ''} ·{' '}
                      {new Date(order.receivedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                      {order.platformOrderId && ` · Store Order ${order.platformOrderId}`}
                    </p>
                  </div>

                  <div className="flex items-center gap-4 shrink-0">
                    <span className={`inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1 rounded-full border ${st.color}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${st.dot}`} />
                      {st.label}
                    </span>
                    <p className="text-base font-black text-white w-28 text-right">{fmt(order.orderValue)}</p>
                    <ArrowRight className="w-4 h-4 text-slate-600 group-hover:text-slate-400 transition-colors" />
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
