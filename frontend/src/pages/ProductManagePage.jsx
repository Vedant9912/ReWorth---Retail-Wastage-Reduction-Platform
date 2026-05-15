import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { productApi, shopApi } from '../api';
import Layout from '../components/layout/Layout';
import { Plus, Edit2, Trash2, Loader2, X, Package, Search } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import toast from 'react-hot-toast';

const CATEGORIES = ['DAIRY','BAKERY','FRUITS_VEGETABLES','BEVERAGES','SNACKS','FROZEN','MEAT_SEAFOOD','PERSONAL_CARE','OTHER'];

const schema = z.object({
  name:            z.string().min(2),
  description:     z.string().optional(),
  originalPrice:   z.coerce.number().min(0.01),
  discountPercent: z.coerce.number().int().min(1).max(99),
  expiryDate:      z.string().min(1, 'Required'),
  stockQuantity:   z.coerce.number().int().min(1),
  category:        z.string(),
  imageUrl:        z.string().url().optional().or(z.literal('')),
  shopId:          z.coerce.number(),
});

const statusColor = (s) => ({
  ACTIVE:        'bg-emerald-100 text-emerald-700',
  EXPIRING_SOON: 'bg-amber-100 text-amber-700',
  EXPIRED:       'bg-red-100 text-red-700',
  SOLD_OUT:      'bg-gray-100 text-gray-500',
}[s] || '');

export default function ProductManagePage() {
  const [products, setProducts] = useState([]);
  const [shops, setShops]       = useState([]);
  const [loading, setLoading]   = useState(true);
  const [saving, setSaving]     = useState(false);
  const [modal, setModal]       = useState(false);
  const [editing, setEditing]   = useState(null);
  const [search, setSearch]     = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  const { register, handleSubmit, reset, setValue, watch, formState: { errors } } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { stockQuantity: 1, category: 'OTHER' },
  });

  const origPrice = watch('originalPrice');
  const discount  = watch('discountPercent');
  const finalPrice = origPrice && discount ? (origPrice * (100 - discount) / 100).toFixed(2) : '—';

  const load = async () => {
    try {
      const [pRes, sRes] = await Promise.all([productApi.myProducts(), shopApi.myShops()]);
      setProducts(pRes.data || []); setShops(sRes.data || []);
    } catch { toast.error('Failed to load'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const openCreate = () => { reset({ stockQuantity: 1, category: 'OTHER' }); setEditing(null); setModal(true); };
  const openEdit = (p) => {
    setEditing(p);
    setValue('name', p.name); setValue('description', p.description || '');
    setValue('originalPrice', p.originalPrice); setValue('discountPercent', p.discountPercent);
    setValue('expiryDate', p.expiryDate); setValue('stockQuantity', p.stockQuantity);
    setValue('category', p.category); setValue('imageUrl', p.imageUrl || '');
    setValue('shopId', p.shopId);
    setModal(true);
  };

  const onSubmit = async (data) => {
    setSaving(true);
    try {
      if (editing) {
        const res = await productApi.update(editing.id, data);
        setProducts(prev => prev.map(p => p.id === editing.id ? res.data : p));
        toast.success('Product updated!');
      } else {
        const res = await productApi.create(data);
        setProducts(prev => [res.data, ...prev]);
        toast.success('Product added!');
      }
      setModal(false);
    } catch (err) { toast.error(err.response?.data?.message || 'Failed'); }
    finally { setSaving(false); }
  };

  const handleDelete = async (p) => {
    if (!confirm(`Remove "${p.name}"?`)) return;
    try { await productApi.delete(p.id); setProducts(prev => prev.filter(x => x.id !== p.id)); toast.success('Removed'); }
    catch { toast.error('Failed'); }
  };

  const filtered = products.filter(p => {
    const matchSearch = p.name.toLowerCase().includes(search.toLowerCase()) || p.shopName.toLowerCase().includes(search.toLowerCase());
    const matchStatus = !filterStatus || p.status === filterStatus;
    return matchSearch && matchStatus;
  });

  return (
    <Layout>
      <div className="space-y-5">
        <div className="flex items-center justify-between">
          <div><h1 className="text-xl font-bold">Products</h1><p className="text-sm text-gray-500">{products.length} total listings</p></div>
          <button onClick={openCreate} className="btn-primary flex items-center gap-2"><Plus size={16}/> Add Product</button>
        </div>

        <div className="flex flex-wrap gap-3 items-center">
          <div className="relative flex-1 min-w-48">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/>
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search..." className="input pl-9"/>
          </div>
          <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)} className="input w-40">
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="EXPIRING_SOON">Expiring Soon</option>
            <option value="EXPIRED">Expired</option>
          </select>
        </div>

        {loading ? (
          <div className="flex justify-center py-20"><Loader2 size={32} className="animate-spin text-emerald-600"/></div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-20">
            <Package size={48} className="mx-auto text-gray-300 mb-4"/>
            <p className="text-gray-500 font-medium">{search ? 'No products match' : 'No products yet'}</p>
            {!search && <button onClick={openCreate} className="btn-primary mt-4 inline-flex items-center gap-2"><Plus size={16}/> Add First Product</button>}
          </div>
        ) : (
          <div className="card overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>{['Product','Shop','Price','Discount','Expiry','Status','Stock',''].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide">{h}</th>
                ))}</tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(p => (
                  <tr key={p.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3"><div className="font-medium truncate max-w-40">{p.name}</div><div className="text-xs text-gray-400 capitalize">{p.category.replace('_',' ').toLowerCase()}</div></td>
                    <td className="px-4 py-3 text-gray-500 truncate max-w-32">{p.shopName}</td>
                    <td className="px-4 py-3"><div className="font-medium text-emerald-700">₹{p.discountedPrice}</div><div className="text-xs text-gray-400 line-through">₹{p.originalPrice}</div></td>
                    <td className="px-4 py-3"><span className="bg-red-100 text-red-600 text-xs font-bold px-2 py-0.5 rounded-full">{p.discountPercent}%</span></td>
                    <td className="px-4 py-3"><div>{format(parseISO(p.expiryDate),'dd MMM yy')}</div><div className={`text-xs ${p.daysUntilExpiry <= 2 ? 'text-amber-600 font-medium' : 'text-gray-400'}`}>{p.daysUntilExpiry <= 0 ? 'Expired' : `${p.daysUntilExpiry}d left`}</div></td>
                    <td className="px-4 py-3"><span className={`text-xs font-medium px-2 py-0.5 rounded-full ${statusColor(p.status)}`}>{p.status.replace('_',' ')}</span></td>
                    <td className="px-4 py-3 text-gray-500">{p.stockQuantity}</td>
                    <td className="px-4 py-3"><div className="flex gap-1">
                      <button onClick={() => openEdit(p)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg"><Edit2 size={14}/></button>
                      <button onClick={() => handleDelete(p)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg"><Trash2 size={14}/></button>
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-5 border-b sticky top-0 bg-white">
              <h2 className="font-bold text-lg">{editing ? 'Edit Product' : 'Add Product'}</h2>
              <button onClick={() => setModal(false)} className="p-1 hover:bg-gray-100 rounded-lg"><X size={20}/></button>
            </div>
            <form onSubmit={handleSubmit(onSubmit)} className="p-5 space-y-4">
              <div><label className="block text-sm font-medium mb-1">Shop *</label>
                <select {...register('shopId')} className="input">
                  <option value="">Select shop</option>
                  {shops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select></div>
              <div><label className="block text-sm font-medium mb-1">Product Name *</label>
                <input {...register('name')} placeholder="Amul Butter 500g" className="input"/>
                {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name.message}</p>}</div>
              <div><label className="block text-sm font-medium mb-1">Description</label>
                <textarea {...register('description')} rows={2} className="input resize-none"/></div>
              <div className="grid grid-cols-2 gap-3">
                <div><label className="block text-sm font-medium mb-1">Original Price (₹) *</label>
                  <input {...register('originalPrice')} type="number" step="0.01" placeholder="250" className="input"/>
                  {errors.originalPrice && <p className="text-red-500 text-xs mt-1">Required</p>}</div>
                <div><label className="block text-sm font-medium mb-1">Discount % *</label>
                  <input {...register('discountPercent')} type="number" placeholder="30" className="input"/>
                  {errors.discountPercent && <p className="text-red-500 text-xs mt-1">{errors.discountPercent.message}</p>}</div>
              </div>
              {origPrice > 0 && discount > 0 && (
                <div className="bg-emerald-50 rounded-lg px-3 py-2 text-sm text-emerald-700">
                  Final price: <strong>₹{finalPrice}</strong> <span className="text-emerald-500 ml-2">({discount}% OFF)</span>
                </div>
              )}
              <div className="grid grid-cols-2 gap-3">
                <div><label className="block text-sm font-medium mb-1">Expiry Date *</label>
                  <input {...register('expiryDate')} type="date" min={new Date().toISOString().split('T')[0]} className="input"/>
                  {errors.expiryDate && <p className="text-red-500 text-xs mt-1">Required</p>}</div>
                <div><label className="block text-sm font-medium mb-1">Stock Qty *</label>
                  <input {...register('stockQuantity')} type="number" min="1" className="input"/></div>
              </div>
              <div><label className="block text-sm font-medium mb-1">Category</label>
                <select {...register('category')} className="input">
                  {CATEGORIES.map(c => <option key={c} value={c}>{c.replace('_',' ')}</option>)}
                </select></div>
              <div><label className="block text-sm font-medium mb-1">Image URL</label>
                <input {...register('imageUrl')} placeholder="https://..." className="input"/></div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(false)} className="btn-outline flex-1">Cancel</button>
                <button type="submit" disabled={saving} className="btn-primary flex-1 flex items-center justify-center gap-2">
                  {saving ? <><Loader2 size={15} className="animate-spin"/>Saving...</> : (editing ? 'Update' : 'Add Product')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
