import axios from 'axios';
import { apiBaseUrl } from './config';

const http = axios.create({ baseURL: apiBaseUrl || '/' });

export interface AuthResponse {
  token: string;
  sellerId: number;
  email: string;
  name: string;
  company: string;
  plan: string;
}

export const register = (data: { email: string; password: string; fullName: string; companyName: string }) =>
  http.post<AuthResponse>('/auth/register', data).then(r => r.data);

export const login = (data: { email: string; password: string }) =>
  http.post<AuthResponse>('/auth/login', data).then(r => r.data);

export const connectShopify = (sellerId: number, storeDomain: string, accessToken: string, jwt: string) =>
  http.post(`/api/v1/portal/sellers/${sellerId}/shopify/connect`,
    { storeDomain, accessToken },
    { headers: { Authorization: `Bearer ${jwt}` } }
  ).then(r => r.data);

export const testShopifyConnection = (sellerId: number, jwt: string) =>
  http.get(`/api/v1/portal/sellers/${sellerId}/shopify/test`,
    { headers: { Authorization: `Bearer ${jwt}` } }
  ).then(r => r.data);
