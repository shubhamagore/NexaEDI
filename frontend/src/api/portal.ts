import axios from 'axios';

const http = axios.create({ baseURL: '/api/v1/portal' });

// Attach JWT from localStorage on every request
http.interceptors.request.use(config => {
  try {
    const stored = localStorage.getItem('nexaedi_auth');
    if (stored) {
      const { token } = JSON.parse(stored);
      if (token) config.headers['Authorization'] = `Bearer ${token}`;
    }
  } catch {}
  return config;
});

export interface SellerSummary {
  id: number; name: string; email: string; company: string; plan: string; planPrice: number;
}
export interface SellerDetail extends SellerSummary {
  platforms: Platform[]; retailers: RetailerConn[];
}
export interface Platform {
  id: number; platformType: string; platformName: string;
  platformUrl: string; status: string; connectedAt: string; ordersSynced: number;
}
export interface RetailerConn {
  id: number; retailerId: string; retailerName: string; ingestionMethod: string;
  status: string; connectedSince: string; lastOrderReceivedAt: string; totalOrdersReceived: number;
}
export interface Dashboard {
  revenueThisMonth: number; revenueThisWeek: number;
  ordersThisMonth: number; totalOrders: number;
  pendingOrders: number; failedOrders: number; syncedOrders: number;
  successRate: number;
  revenueByRetailer: { retailerId: string; retailerName: string; revenue: number; lastOrderAt: string }[];
  recentOrders: Order[];
}
export interface Order {
  id: number; poNumber: string; retailerId: string; retailerName: string;
  platform: string; platformOrderId: string; status: string;
  orderValue: number; currency: string; lineItemCount: number; receivedAt: string;
}
export interface OrderDetail extends Order {
  totalUnits: number; shipToName: string; shipToCity: string; shipToState: string;
  requestedDeliveryDate: string; lineItems: string; correlationId: string;
  syncedAt: string; errorMessage: string;
}

export const listSellers = () => http.get<SellerSummary[]>('/sellers').then(r => r.data);
export const getSeller   = (id: number) => http.get<SellerDetail>(`/sellers/${id}`).then(r => r.data);
export const getDashboard = (id: number) => http.get<Dashboard>(`/sellers/${id}/dashboard`).then(r => r.data);
export const getOrders    = (id: number, params?: { status?: string; retailerId?: string }) =>
  http.get<Order[]>(`/sellers/${id}/orders`, { params }).then(r => r.data);
export const getOrderDetail = (sellerId: number, orderId: number) =>
  http.get<OrderDetail>(`/sellers/${sellerId}/orders/${orderId}`).then(r => r.data);
export const getPlatforms  = (id: number) => http.get<Platform[]>(`/sellers/${id}/platforms`).then(r => r.data);
export const getRetailers  = (id: number) => http.get<RetailerConn[]>(`/sellers/${id}/retailers`).then(r => r.data);
