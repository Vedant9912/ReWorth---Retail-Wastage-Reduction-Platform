import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { shopApi, productApi } from '../api';
import Layout from '../components/layout/Layout';
import { useAuthStore } from '../store/authStore';
import { Store, Package, TrendingUp, Clock, Plus, ArrowRight, Loader2 } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import toast from 'react-hot-toast';

export default function DashboardPage() {
  const { user } = useAuthStore();
  const isOwner = user?.role === 'SHOP_OWNER' || user?.role === 'ADMIN';
  const [shops, setShops]       = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading]   = useState(true);

  useEffect(() => {
    if (!isOwner) { setLoading(false); return; }
    Promise.all([shopApi.myShops(), productApi.myProducts()])
      .then(([s, p]) => { setShops(s.data || []); setProducts(p.data || []); })
      .catch(() => toast.error('Failed to load dashboard'))
      .finally(() => setLoading(false));
  }, []);

  const expiringSoon = products.filter(p => p.daysUntilExpiry <= 2 && p.status !== 'EXPIRED');
  const totalSavings = products.reduce((sum, p) => sum + (p.originalPrice - p.discountedPrice), 0);

  if (loading) return (
    <Layout><div className="flex justify-center py-20"><Loader2 size={32} className="animate-spin text-emerald-600"/></div></Layout>
  );

  if (!isOwner) return (
    <Layout>
      <div className="max-w-xl mx-auto text-center py-20">
        <div className="text-6xl mb-4">🛒</div>
        <h2 className="text-xl font-bold mb-2">Welcome, {user?.fullName}!</h2>
        <p className="text-gray-500 mb-6">Browse nearby deals and save money while reducing waste.</p>
        <Link to="/browse" className="btn-primary inline-flex items-center gap-2">
          <Store size={16}/> Browse Deals
        </Link>
      </div>
    </Layout>
  );

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h1 className="text-xl font-bold">Welcome back, {user?.fullName} 👋</h1>
          <p className="text-gray-500 text-sm">Here's your shop overview</p>
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            { icon: <Store size={20}/>,     color: 'emerald', label: 'Shops',           value: shops.length },
            { icon: <Package size={20}/>,   color: 'blue',    label: 'Products',        value: products.length },
            { icon: <Clock size={20}/>,     color: 'amber',   label: 'Expiring Soon',   value: expiringSoon.length },
            { icon: <TrendingUp size={20}/>,color: 'purple',  label: 'Total Savings ₹', value: totalSavings.toFixed(0) },
          ].map(stat => (
            <div key={stat.label} className="card p-4">
              <div className={`inline-flex p-2 rounded-lg bg-${stat.color}-100 text-${stat.color}-600 mb-3`}>{stat.icon}</div>
              <div className="text-2xl font-bold text-gray-900">{stat.value}</div>
              <div className="text-xs text-gray-500 mt-0.5">{stat.label}</div>
            </div>
          ))}
        </div>

        {expiringSoon.length > 0 && (
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
            <div className="flex items-center gap-2 mb-3">
              <Clock size={18} className="text-amber-600"/>
              <h3 className="font-semibold text-amber-800">⚠️ {expiringSoon.length} products expiring within 48 hours</h3>
            </div>
            <div className="grid sm:grid-cols-2 gap-2">
              {expiringSoon.slice(0,4).map(p => (
                <div key={p.id} className="bg-white rounded-lg px-3 py-2 text-sm flex justify-between items-center">
                  <span className="font-medium truncate">{p.name}</span>
                  <span className="text-amber-600 text-xs ml-2 shrink-0">
                    {p.daysUntilExpiry <= 0 ? 'Today!' : `${p.daysUntilExpiry}d`}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="grid sm:grid-cols-2 gap-4">
          <div className="card p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">My Shops</h3>
              <Link to="/shop/manage" className="text-emerald-600 text-sm flex items-center gap-1 hover:underline">
                Manage <ArrowRight size={14}/>
              </Link>
            </div>
            {shops.length === 0 ? (
              <div className="text-center py-6">
                <p className="text-gray-400 text-sm mb-3">No shops yet</p>
                <Link to="/shop/manage" className="btn-primary text-sm inline-flex items-center gap-1">
                  <Plus size={14}/> Add Shop
                </Link>
              </div>
            ) : (
              <div className="space-y-2">
                {shops.slice(0,3).map(s => (
                  <div key={s.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50">
                    <div className="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center text-sm">🏪</div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">{s.name}</p>
                      <p className="text-xs text-gray-400 truncate">{s.address}</p>
                    </div>
                    <span className="text-xs text-emerald-600 shrink-0">{s.activeProductCount} items</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="card p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">Recent Products</h3>
              <Link to="/shop/products" className="text-emerald-600 text-sm flex items-center gap-1 hover:underline">
                All <ArrowRight size={14}/>
              </Link>
            </div>
            {products.length === 0 ? (
              <div className="text-center py-6">
                <p className="text-gray-400 text-sm mb-3">No products yet</p>
                <Link to="/shop/products" className="btn-primary text-sm inline-flex items-center gap-1">
                  <Plus size={14}/> Add Product
                </Link>
              </div>
            ) : (
              <div className="space-y-2">
                {products.slice(0,4).map(p => (
                  <div key={p.id} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">{p.name}</p>
                      <p className="text-xs text-gray-400">₹{p.discountedPrice} · expires {format(parseISO(p.expiryDate),'dd MMM')}</p>
                    </div>
                    <span className={`text-xs px-2 py-0.5 rounded-full ml-2 shrink-0
                      ${p.daysUntilExpiry <= 2 ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700'}`}>
                      {p.daysUntilExpiry <= 0 ? 'Today' : `${p.daysUntilExpiry}d`}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
