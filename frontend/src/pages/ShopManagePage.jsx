import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { shopApi } from '../api';
import Layout from '../components/layout/Layout';
import { Plus, Edit2, Trash2, MapPin, Loader2, X, Store } from 'lucide-react';
import toast from 'react-hot-toast';

const schema = z.object({
  name:        z.string().min(2),
  description: z.string().optional(),
  address:     z.string().min(5),
  latitude:    z.coerce.number().min(-90).max(90),
  longitude:   z.coerce.number().min(-180).max(180),
  phone:       z.string().regex(/^[6-9]\d{9}$/).optional().or(z.literal('')),
  email:       z.string().email().optional().or(z.literal('')),
});

export default function ShopManagePage() {
  const [shops, setShops]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving]   = useState(false);
  const [modal, setModal]     = useState(false);
  const [editing, setEditing] = useState(null);

  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm({
    resolver: zodResolver(schema),
  });

  const loadShops = async () => {
    try { const res = await shopApi.myShops(); setShops(res.data || []); }
    catch { toast.error('Failed to load shops'); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadShops(); }, []);

  const openCreate = () => { reset(); setEditing(null); setModal(true); };
  const openEdit = (shop) => {
    setEditing(shop);
    setValue('name', shop.name); setValue('description', shop.description || '');
    setValue('address', shop.address); setValue('latitude', shop.latitude);
    setValue('longitude', shop.longitude); setValue('phone', shop.phone || '');
    setValue('email', shop.email || '');
    setModal(true);
  };

  const getLocation = () => {
    navigator.geolocation.getCurrentPosition(
      pos => { setValue('latitude', pos.coords.latitude); setValue('longitude', pos.coords.longitude); toast.success('Location filled!'); },
      () => toast.error('Location denied')
    );
  };

  const onSubmit = async (data) => {
    setSaving(true);
    try {
      if (editing) {
        const res = await shopApi.update(editing.id, data);
        setShops(prev => prev.map(s => s.id === editing.id ? res.data : s));
        toast.success('Shop updated!');
      } else {
        const res = await shopApi.create(data);
        setShops(prev => [res.data, ...prev]);
        toast.success('Shop created!');
      }
      setModal(false);
    } catch (err) { toast.error(err.response?.data?.message || 'Failed'); }
    finally { setSaving(false); }
  };

  const handleDelete = async (shop) => {
    if (!confirm(`Delete "${shop.name}"?`)) return;
    try { await shopApi.delete(shop.id); setShops(prev => prev.filter(s => s.id !== shop.id)); toast.success('Shop deleted'); }
    catch { toast.error('Failed to delete'); }
  };

  return (
    <Layout>
      <div className="space-y-5">
        <div className="flex items-center justify-between">
          <div><h1 className="text-xl font-bold">My Shops</h1><p className="text-sm text-gray-500">{shops.length} shop{shops.length !== 1 ? 's' : ''}</p></div>
          <button onClick={openCreate} className="btn-primary flex items-center gap-2"><Plus size={16}/> Add Shop</button>
        </div>

        {loading ? (
          <div className="flex justify-center py-20"><Loader2 size={32} className="animate-spin text-emerald-600"/></div>
        ) : shops.length === 0 ? (
          <div className="text-center py-20">
            <Store size={48} className="mx-auto text-gray-300 mb-4"/>
            <p className="text-gray-500 font-medium">No shops yet</p>
            <button onClick={openCreate} className="btn-primary mt-4 inline-flex items-center gap-2"><Plus size={16}/> Create Your First Shop</button>
          </div>
        ) : (
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {shops.map(shop => (
              <div key={shop.id} className="card p-5">
                <div className="flex items-start justify-between mb-3">
                  <div className="w-10 h-10 bg-emerald-100 rounded-xl flex items-center justify-center text-xl">🏪</div>
                  <div className="flex gap-2">
                    <button onClick={() => openEdit(shop)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg"><Edit2 size={15}/></button>
                    <button onClick={() => handleDelete(shop)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg"><Trash2 size={15}/></button>
                  </div>
                </div>
                <h3 className="font-semibold text-gray-900">{shop.name}</h3>
                {shop.description && <p className="text-xs text-gray-500 mt-1 line-clamp-2">{shop.description}</p>}
                <div className="flex items-center gap-1 mt-2 text-xs text-gray-400"><MapPin size={12}/><span className="truncate">{shop.address}</span></div>
                <div className="mt-3 flex items-center justify-between text-xs">
                  <span className={`px-2 py-0.5 rounded-full font-medium ${shop.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>{shop.active ? 'Active' : 'Inactive'}</span>
                  <span className="text-gray-500">{shop.activeProductCount} products</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {modal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-5 border-b">
              <h2 className="font-bold text-lg">{editing ? 'Edit Shop' : 'Create Shop'}</h2>
              <button onClick={() => setModal(false)} className="p-1 hover:bg-gray-100 rounded-lg"><X size={20}/></button>
            </div>
            <form onSubmit={handleSubmit(onSubmit)} className="p-5 space-y-4">
              <div><label className="block text-sm font-medium mb-1">Shop Name *</label>
                <input {...register('name')} placeholder="Fresh Mart" className="input"/>
                {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name.message}</p>}</div>
              <div><label className="block text-sm font-medium mb-1">Description</label>
                <textarea {...register('description')} rows={2} className="input resize-none"/></div>
              <div><label className="block text-sm font-medium mb-1">Address *</label>
                <input {...register('address')} placeholder="MG Road, Indore, MP" className="input"/>
                {errors.address && <p className="text-red-500 text-xs mt-1">{errors.address.message}</p>}</div>
              <div className="grid grid-cols-2 gap-3">
                <div><label className="block text-sm font-medium mb-1">Latitude *</label>
                  <input {...register('latitude')} type="number" step="any" placeholder="22.7196" className="input"/>
                  {errors.latitude && <p className="text-red-500 text-xs mt-1">Required</p>}</div>
                <div><label className="block text-sm font-medium mb-1">Longitude *</label>
                  <input {...register('longitude')} type="number" step="any" placeholder="75.8577" className="input"/>
                  {errors.longitude && <p className="text-red-500 text-xs mt-1">Required</p>}</div>
              </div>
              <button type="button" onClick={getLocation} className="btn-outline text-sm flex items-center gap-2 py-1.5">
                <MapPin size={14}/> Auto-fill my location
              </button>
              <div className="grid grid-cols-2 gap-3">
                <div><label className="block text-sm font-medium mb-1">Phone</label>
                  <input {...register('phone')} placeholder="9876543210" className="input"/></div>
                <div><label className="block text-sm font-medium mb-1">Email</label>
                  <input {...register('email')} type="email" placeholder="shop@example.com" className="input"/></div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(false)} className="btn-outline flex-1">Cancel</button>
                <button type="submit" disabled={saving} className="btn-primary flex-1 flex items-center justify-center gap-2">
                  {saving ? <><Loader2 size={15} className="animate-spin"/>Saving...</> : (editing ? 'Update' : 'Create')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
