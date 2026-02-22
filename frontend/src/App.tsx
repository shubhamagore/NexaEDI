import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import PortalLayout from './components/PortalLayout';

// Auth pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import OnboardingPage from './pages/auth/OnboardingPage';

// Dev / Ops pages
import Dashboard from './pages/Dashboard';
import Ingest from './pages/Ingest';
import AuditTrail from './pages/AuditTrail';
import AuditDetail from './pages/AuditDetail';
import Mappings from './pages/Mappings';
import DevTools from './pages/DevTools';

// Customer portal pages
import PortalSelector from './pages/portal/PortalSelector';
import PortalDashboard from './pages/portal/PortalDashboard';
import PortalOrders from './pages/portal/PortalOrders';
import PortalOrderDetail from './pages/portal/PortalOrderDetail';
import PortalPlatforms from './pages/portal/PortalPlatforms';

/** Redirects unauthenticated users to the login page */
function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

/** Redirects authenticated users away from login/register */
function RedirectIfAuth({ children }: { children: React.ReactNode }) {
  const { user, isAuthenticated } = useAuth();
  return isAuthenticated && user
    ? <Navigate to={`/portal/${user.sellerId}`} replace />
    : <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      {/* ── Public auth pages ── */}
      <Route path="/login"  element={<RedirectIfAuth><LoginPage /></RedirectIfAuth>} />
      <Route path="/signup" element={<RedirectIfAuth><RegisterPage /></RedirectIfAuth>} />

      {/* ── Protected onboarding ── */}
      <Route path="/onboarding/:sellerId" element={
        <RequireAuth><OnboardingPage /></RequireAuth>
      } />

      {/* ── Customer Seller Portal (requires auth) ── */}
      <Route path="/portal" element={
        <RequireAuth><PortalSelector /></RequireAuth>
      } />
      <Route path="/portal/:sellerId" element={
        <RequireAuth><PortalLayout><PortalDashboard /></PortalLayout></RequireAuth>
      } />
      <Route path="/portal/:sellerId/orders" element={
        <RequireAuth><PortalLayout><PortalOrders /></PortalLayout></RequireAuth>
      } />
      <Route path="/portal/:sellerId/orders/:orderId" element={
        <RequireAuth><PortalLayout><PortalOrderDetail /></PortalLayout></RequireAuth>
      } />
      <Route path="/portal/:sellerId/platforms" element={
        <RequireAuth><PortalLayout><PortalPlatforms /></PortalLayout></RequireAuth>
      } />
      <Route path="/portal/:sellerId/settings" element={
        <RequireAuth><PortalLayout><div className="text-slate-400 p-8 text-sm">Settings coming soon</div></PortalLayout></RequireAuth>
      } />

      {/* ── Developer / Ops UI (accessible without auth for local dev) ── */}
      <Route path="/*" element={
        <Layout>
          <Routes>
            <Route path="/"                     element={<Dashboard />} />
            <Route path="/ingest"               element={<Ingest />} />
            <Route path="/audit"                element={<AuditTrail />} />
            <Route path="/audit/:correlationId" element={<AuditDetail />} />
            <Route path="/mappings"             element={<Mappings />} />
            <Route path="/dev"                  element={<DevTools />} />
            <Route path="*"                     element={<Navigate to="/" replace />} />
          </Routes>
        </Layout>
      } />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster
          position="top-right"
          toastOptions={{
            className: 'text-sm font-medium',
            style: { borderRadius: '10px', border: '1px solid #e2e8f0', boxShadow: '0 4px 16px rgba(0,0,0,0.08)' },
            success: { iconTheme: { primary: '#10b981', secondary: 'white' } },
            error:   { iconTheme: { primary: '#ef4444', secondary: 'white' } },
          }}
        />
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
