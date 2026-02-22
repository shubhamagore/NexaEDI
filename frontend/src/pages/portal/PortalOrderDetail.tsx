import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Package, MapPin, Calendar, AlertCircle, CheckCircle2, RefreshCw, ExternalLink } from 'lucide-react';
import { getOrderDetail } from '../../api/portal';
import type { OrderDetail } from '../../api/portal';

const statusConfig: Record<string, { label: string; color: string; bg: string; icon: typeof CheckCircle2 }> = {
  RECEIVED:     { label: 'Received',        color: 'text-blue-400',    bg: 'bg-blue-500/10 border-blue-500/20',    icon: Package },
  PROCESSING:   { label: 'Processing…',     color: 'text-amber-400',   bg: 'bg-amber-500/10 border-amber-500/20',  icon: RefreshCw },
  SYNCED:       { label: 'Synced to Store', color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/20', icon: CheckCircle2 },
  ACKNOWLEDGED: { label: 'Complete',        color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/20', icon: CheckCircle2 },
  FAILED:       { label: 'Failed',          color: 'text-red-400',     bg: 'bg-red-500/10 border-red-500/20',      icon: AlertCircle },
};

const platformIcons: Record<string, string> = {
  SHOPIFY: '🛍️', WOOCOMMERCE: '🟣', AMAZON_SELLER: '📦', BIGCOMMERCE: '🔵',
};

const platformLabels: Record<string, string> = {
  SHOPIFY: 'Shopify', WOOCOMMERCE: 'WooCommerce', AMAZON_SELLER: 'Amazon Seller', BIGCOMMERCE: 'BigCommerce',
};

function fmt(val: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
}

interface LineItem {
  description: string; sku: string; quantity: number; unitPrice: number; lineTotal: number;
}

export default function PortalOrderDetail() {
  const { sellerId, orderId } = useParams<{ sellerId: string; orderId: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getOrderDetail(Number(sellerId), Number(orderId))
      .then(setOrder).catch(() => {}).finally(() => setLoading(false));
  }, [sellerId, orderId]);

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-8 h-8 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
    </div>
  );
  if (!order) return (
    <div className="text-center py-20 text-slate-500">Order not found.</div>
  );

  const st = statusConfig[order.status] ?? statusConfig.RECEIVED;
  const StatusIcon = st.icon;
  const lineItems: LineItem[] = (() => {
    try { return JSON.parse(order.lineItems); } catch { return []; }
  })();

  const timeline = [
    { label: 'Order Received from Retailer', time: order.receivedAt, done: true },
    { label: 'Processed by NexaEDI',          time: order.receivedAt, done: order.status !== 'RECEIVED' },
    { label: `Synced to ${platformLabels[order.platform] ?? 'Store'}`, time: order.syncedAt, done: !!order.syncedAt },
    { label: 'Retailer Acknowledged',         time: order.syncedAt,   done: order.status === 'ACKNOWLEDGED' },
  ];

  return (
    <div className="max-w-4xl space-y-6">
      <button onClick={() => navigate(`/portal/${sellerId}/orders`)}
        className="flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors">
        <ArrowLeft className="w-4 h-4" /> Back to Orders
      </button>

      {/* Header */}
      <div className="bg-slate-900 border border-white/5 rounded-2xl p-6">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div className="flex items-start gap-4">
            <div className="text-3xl">{platformIcons[order.platform] ?? '🏪'}</div>
            <div>
              <h1 className="text-xl font-black text-white">{order.poNumber}</h1>
              <p className="text-slate-400 text-sm">{order.retailerName} · Purchase Order</p>
              <div className={`inline-flex items-center gap-2 mt-2 px-3 py-1.5 rounded-xl border text-sm font-semibold ${st.bg} ${st.color}`}>
                <StatusIcon className="w-4 h-4" />
                {st.label}
              </div>
            </div>
          </div>
          <div className="text-right">
            <p className="text-3xl font-black text-white">{fmt(order.orderValue)}</p>
            <p className="text-slate-400 text-sm">{order.totalUnits} units · {order.lineItemCount} SKUs</p>
            {order.platformOrderId && (
              <p className="text-indigo-400 text-xs mt-1 flex items-center gap-1 justify-end">
                <ExternalLink className="w-3 h-3" />
                Store Order: {order.platformOrderId}
              </p>
            )}
          </div>
        </div>

        {order.status === 'FAILED' && order.errorMessage && (
          <div className="mt-4 p-4 bg-red-500/10 border border-red-500/20 rounded-xl flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-red-300 text-sm font-semibold">Why did this fail?</p>
              <p className="text-red-400/80 text-sm mt-1">{order.errorMessage}</p>
              <p className="text-red-400/60 text-xs mt-2">
                Fix: Check that all SKUs listed below exist in your store catalog, then contact support to retry.
              </p>
            </div>
          </div>
        )}
      </div>

      <div className="grid grid-cols-5 gap-6">
        {/* Line Items */}
        <div className="bg-slate-900 border border-white/5 rounded-2xl overflow-hidden col-span-3">
          <div className="px-5 py-4 border-b border-white/5">
            <h2 className="text-sm font-bold text-white">Order Items</h2>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/5 bg-white/2">
                <th className="text-left px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Product</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Qty</th>
                <th className="text-right px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Unit Price</th>
                <th className="text-right px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {lineItems.map((item, i) => (
                <tr key={i} className="hover:bg-white/2 transition-colors">
                  <td className="px-5 py-4">
                    <p className="text-white font-medium text-sm">{item.description}</p>
                    <p className="text-slate-500 text-xs font-mono mt-0.5">SKU: {item.sku}</p>
                  </td>
                  <td className="px-4 py-4 text-center text-white font-bold">{item.quantity}</td>
                  <td className="px-4 py-4 text-right text-slate-300">{fmt(item.unitPrice)}</td>
                  <td className="px-5 py-4 text-right text-white font-bold">{fmt(item.lineTotal)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="border-t border-white/10 bg-white/3">
                <td colSpan={3} className="px-5 py-3 text-right text-sm font-semibold text-slate-400">Order Total</td>
                <td className="px-5 py-3 text-right text-lg font-black text-white">{fmt(order.orderValue)}</td>
              </tr>
            </tfoot>
          </table>
        </div>

        {/* Details + Timeline */}
        <div className="col-span-2 space-y-4">
          {/* Ship To */}
          <div className="bg-slate-900 border border-white/5 rounded-2xl p-5">
            <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3 flex items-center gap-2">
              <MapPin className="w-3.5 h-3.5" /> Ship To
            </h2>
            <p className="text-white font-semibold text-sm">{order.shipToName}</p>
            <p className="text-slate-400 text-sm">{order.shipToCity}, {order.shipToState}</p>
          </div>

          {/* Dates */}
          <div className="bg-slate-900 border border-white/5 rounded-2xl p-5">
            <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3 flex items-center gap-2">
              <Calendar className="w-3.5 h-3.5" /> Key Dates
            </h2>
            <div className="space-y-2.5 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-500">Received</span>
                <span className="text-white">{new Date(order.receivedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}</span>
              </div>
              {order.syncedAt && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Synced to Store</span>
                  <span className="text-emerald-400">{new Date(order.syncedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</span>
                </div>
              )}
              {order.requestedDeliveryDate && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Delivery Requested</span>
                  <span className="text-amber-400">{order.requestedDeliveryDate}</span>
                </div>
              )}
            </div>
          </div>

          {/* Timeline */}
          <div className="bg-slate-900 border border-white/5 rounded-2xl p-5">
            <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-4">Timeline</h2>
            <div className="space-y-0">
              {timeline.map((step, i) => (
                <div key={i} className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 ${step.done ? 'border-emerald-500 bg-emerald-500/20' : 'border-slate-700 bg-slate-800'}`}>
                      {step.done && <div className="w-2 h-2 rounded-full bg-emerald-400" />}
                    </div>
                    {i < timeline.length - 1 && <div className={`w-0.5 h-6 my-1 ${step.done ? 'bg-emerald-500/40' : 'bg-slate-700'}`} />}
                  </div>
                  <div className="pb-4">
                    <p className={`text-xs font-medium ${step.done ? 'text-white' : 'text-slate-600'}`}>{step.label}</p>
                    {step.time && step.done && (
                      <p className="text-xs text-slate-600 mt-0.5">{new Date(step.time).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
