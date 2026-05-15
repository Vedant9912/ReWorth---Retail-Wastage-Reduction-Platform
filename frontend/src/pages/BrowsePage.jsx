import { useEffect, useState, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { shopApi, productApi } from '../api';
import ProductCard from '../components/product/ProductCard';
import Layout from '../components/layout/Layout';
import { MapPin, Search, SlidersHorizontal, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:       'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:     'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

const shopIcon = L.divIcon({
  html: `<div style="background:#059669;color:white;font-size:11px;font-weight:bold;padding:4px 8px;border-radius:8px;box-shadow:0 2px 6px rgba(0,0,0,0.3)">🏪</div>`,
  className: '', iconAnchor: [20, 20],
});
const userIcon = L.divIcon({
  html: `<div style="background:#2563eb;color:white;padding:6px;border-radius:50%;box-shadow:0 2px 6px rgba(0,0,0,0.3)">📍</div>`,
  className: '', iconAnchor: [12, 12],
});

const CATEGORIES = [
  { value: '', label: 'All' },
  { value: 'DAIRY', label: '🥛 Dairy' },
  { value: 'BAKERY', label: '🍞 Bakery' },
  { value: 'FRUITS_VEGETABLES', label: '🥦 Fruits & Veg' },
  { value: 'BEVERAGES', label: '🧃 Beverages' },
  { value: 'SNACKS', label: '🍿 Snacks' },
  { value: 'FROZEN', label: '🧊 Frozen' },
  { value: 'MEAT_SEAFOOD', label: '🐟 Meat/Seafood' },
];

function RecenterMap({ lat, lng }) {
  const map = useMap();
  useEffect(() => { map.setView([lat, lng], 13); }, [lat, lng]);
  return null;
}

export default function BrowsePage() {
  const [userLat, setUserLat]   = useState(22.7196);
  const [userLng, setUserLng]   = useState(75.8577);
  const [radius, setRadius]     = useState(10);
  const [category, setCategory] = useState('');
  const [shops, setShops]       = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading]   = useState(false);
  const [locating, setLocating] = useState(false);
  const [page, setPage]         = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchData = useCallback(async (lat, lng, p = 0) => {
    setLoading(true);
    try {
      const [shopRes, productRes] = await Promise.all([
        shopApi.nearby(lat, lng, radius),
        productApi.nearby(lat, lng, radius, category || undefined, p),
      ]);
      setShops(shopRes.data || []);
      setProducts(prev => p === 0
        ? (productRes.data?.content || [])
        : [...prev, ...(productRes.data?.content || [])]);
      setTotalPages(productRes.data?.totalPages || 0);
      setPage(p);
    } catch {
      toast.error('Failed to fetch nearby data');
    } finally {
      setLoading(false);
    }
  }, [radius, category]);

  useEffect(() => { fetchData(userLat, userLng, 0); }, [userLat, userLng, radius, category]);

  const getLocation = () => {
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      pos => {
        setUserLat(pos.coords.latitude);
        setUserLng(pos.coords.longitude);
        setLocating(false);
        toast.success('Location updated!');
      },
      () => { toast.error('Location access denied'); setLocating(false); }
    );
  };

  return (
    <Layout>
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-gray-900">Nearby Deals</h1>
            <p className="text-sm text-gray-500">{shops.length} shops · {products.length} products near you</p>
          </div>
          <button onClick={getLocation} disabled={locating}
            className="btn-outline flex items-center gap-2 self-start sm:self-auto">
            {locating ? <Loader2 size={16} className="animate-spin"/> : <MapPin size={16}/>}
            {locating ? 'Locating...' : 'Use My Location'}
          </button>
        </div>

        <div className="card p-3 flex flex-wrap gap-3 items-center">
          <SlidersHorizontal size={16} className="text-gray-400" />
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">Radius:</label>
            <select value={radius} onChange={e => setRadius(Number(e.target.value))}
              className="input w-28 py-1.5 text-sm">
              {[2,5,10,20,50].map(r => <option key={r} value={r}>{r} km</option>)}
            </select>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {CATEGORIES.map(c => (
              <button key={c.value} onClick={() => setCategory(c.value)}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-all
                  ${category === c.value ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
                {c.label}
              </button>
            ))}
          </div>
        </div>

        <div className="card overflow-hidden">
          <MapContainer center={[userLat, userLng]} zoom={13} className="h-64 w-full z-0">
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='© <a href="https://www.openstreetmap.org/copyright">OSM</a>' />
            <RecenterMap lat={userLat} lng={userLng} />
            <Marker position={[userLat, userLng]} icon={userIcon}>
              <Popup>📍 You are here</Popup>
            </Marker>
            <Circle center={[userLat, userLng]} radius={radius * 1000}
              pathOptions={{ color: '#059669', fillColor: '#d1fae5', fillOpacity: 0.15, weight: 1.5 }} />
            {shops.map(shop => (
              <Marker key={shop.id} position={[shop.latitude, shop.longitude]} icon={shopIcon}>
                <Popup>
                  <div className="text-sm">
                    <p className="font-semibold">{shop.name}</p>
                    <p className="text-gray-500 text-xs">{shop.address}</p>
                    {shop.distanceKm && <p className="text-emerald-600 text-xs">{shop.distanceKm} km away</p>}
                    <p className="text-xs mt-1">{shop.activeProductCount} products</p>
                  </div>
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>

        {loading && page === 0 ? (
          <div className="flex justify-center py-12">
            <Loader2 size={32} className="animate-spin text-emerald-600" />
          </div>
        ) : products.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <Search size={40} className="mx-auto mb-3 opacity-40" />
            <p className="font-medium">No products found nearby</p>
            <p className="text-sm mt-1">Try increasing the radius or changing category</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {products.map(p => <ProductCard key={p.id} product={p} />)}
            </div>
            {page + 1 < totalPages && (
              <div className="text-center">
                <button onClick={() => fetchData(userLat, userLng, page + 1)} disabled={loading}
                  className="btn-outline flex items-center gap-2 mx-auto">
                  {loading ? <Loader2 size={16} className="animate-spin"/> : null}
                  Load more
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  );
}
