import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { ShoppingBag, MapPin, Package, Store, LayoutDashboard, LogOut, Menu, X } from 'lucide-react';
import { useState } from 'react';

export default function Layout({ children }) {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => { logout(); navigate('/login'); };

  const navLinks = user?.role === 'SHOP_OWNER' || user?.role === 'ADMIN'
    ? [
        { to: '/dashboard',      icon: <LayoutDashboard size={18} />, label: 'Dashboard' },
        { to: '/browse',         icon: <MapPin size={18} />,          label: 'Browse' },
        { to: '/shop/manage',    icon: <Store size={18} />,           label: 'My Shops' },
        { to: '/shop/products',  icon: <Package size={18} />,         label: 'Products' },
      ]
    : [
        { to: '/browse',         icon: <MapPin size={18} />,          label: 'Browse' },
        { to: '/dashboard',      icon: <LayoutDashboard size={18} />, label: 'Dashboard' },
      ];

  const active = (to) => location.pathname.startsWith(to)
    ? 'text-emerald-600 font-medium'
    : 'text-gray-600 hover:text-emerald-600';

  return (
    <div className="min-h-screen flex flex-col">
      <nav className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 flex items-center justify-between h-14">
          <Link to="/browse" className="flex items-center gap-2 font-bold text-emerald-700 text-lg">
            <ShoppingBag size={22} /> ReWorth
          </Link>
          <div className="hidden md:flex items-center gap-6">
            {navLinks.map(l => (
              <Link key={l.to} to={l.to}
                className={`flex items-center gap-1.5 text-sm transition-colors ${active(l.to)}`}>
                {l.icon}{l.label}
              </Link>
            ))}
          </div>
          <div className="hidden md:flex items-center gap-3">
            <span className="text-sm text-gray-500">{user?.fullName}</span>
            <button onClick={handleLogout}
              className="flex items-center gap-1 text-sm text-gray-500 hover:text-red-500 transition-colors">
              <LogOut size={16} /> Logout
            </button>
          </div>
          <button className="md:hidden" onClick={() => setMenuOpen(!menuOpen)}>
            {menuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
        {menuOpen && (
          <div className="md:hidden border-t border-gray-100 bg-white px-4 pb-4">
            {navLinks.map(l => (
              <Link key={l.to} to={l.to} onClick={() => setMenuOpen(false)}
                className={`flex items-center gap-2 py-3 text-sm border-b border-gray-50 ${active(l.to)}`}>
                {l.icon}{l.label}
              </Link>
            ))}
            <button onClick={handleLogout}
              className="flex items-center gap-2 py-3 text-sm text-red-500 w-full">
              <LogOut size={16} /> Logout
            </button>
          </div>
        )}
      </nav>
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-6">
        {children}
      </main>
    </div>
  );
}
