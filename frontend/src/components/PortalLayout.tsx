import { type ReactNode } from 'react';
import { NavLink, useParams, useNavigate, useLocation } from 'react-router-dom';
import { LayoutDashboard, ShoppingBag, Store, Settings, Zap, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const platformIcons: Record<string, string> = {
  SHOPIFY: '🛍️', WOOCOMMERCE: '🟣', AMAZON_SELLER: '📦',
  BIGCOMMERCE: '🔵', ETSY: '🧡', TIKTOK_SHOP: '🎵',
};

export default function PortalLayout({ children }: { children: ReactNode }) {
  const { sellerId } = useParams<{ sellerId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();

  const base = `/portal/${sellerId}`;
  const navItems = [
    { to: base,               label: 'Dashboard', icon: LayoutDashboard, exact: true },
    { to: `${base}/orders`,   label: 'My Orders',  icon: ShoppingBag },
    { to: `${base}/platforms`,label: 'Platforms',  icon: Store },
    { to: `${base}/settings`, label: 'Settings',   icon: Settings },
  ];

  const NavItem = ({ to, label, icon: Icon, exact }: typeof navItems[0]) => {
    const active = exact ? location.pathname === to : location.pathname.startsWith(to);
    return (
      <NavLink to={to} className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${active ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}>
        <Icon className="w-4 h-4 shrink-0" />
        {label}
      </NavLink>
    );
  };

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <div className="flex min-h-screen bg-slate-950">
      {/* Sidebar */}
      <aside className="fixed inset-y-0 left-0 w-64 bg-slate-900 border-r border-white/5 flex flex-col z-20">
        {/* Logo */}
        <div className="flex items-center gap-2.5 px-5 py-5 border-b border-white/5">
          <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
            <Zap className="w-4 h-4 text-white" />
          </div>
          <div>
            <p className="text-white font-bold text-sm leading-none">NexaEDI</p>
            <p className="text-slate-500 text-xs mt-0.5">Seller Portal</p>
          </div>
        </div>

        {/* Seller Identity */}
        {user && (
          <div className="px-4 py-4 border-b border-white/5">
            <div className="flex items-center gap-3 bg-white/5 rounded-xl p-3">
              <div className="w-9 h-9 bg-indigo-600/30 rounded-lg flex items-center justify-center text-lg shrink-0">
                🏪
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-white text-xs font-semibold truncate">{user.name}</p>
                <p className="text-slate-500 text-xs truncate">{user.company || user.email}</p>
              </div>
            </div>
          </div>
        )}

        {/* Nav */}
        <nav className="flex-1 px-3 py-4 space-y-1">
          {navItems.map(item => <NavItem key={item.to} {...item} />)}
        </nav>

        {/* Footer */}
        <div className="px-3 py-4 border-t border-white/5 space-y-1">
          <button onClick={handleLogout} className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-slate-500 hover:text-red-400 hover:bg-red-500/5 transition-all">
            <LogOut className="w-4 h-4" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 ml-64 flex flex-col min-h-screen">
        <main className="flex-1 p-8 animate-fade-in bg-slate-950">
          {children}
        </main>
      </div>
    </div>
  );
}
