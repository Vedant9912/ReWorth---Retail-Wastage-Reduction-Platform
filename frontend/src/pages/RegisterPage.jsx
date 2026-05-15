import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api';
import { useAuthStore } from '../store/authStore';
import toast from 'react-hot-toast';
import { ShoppingBag, Loader2 } from 'lucide-react';

const schema = z.object({
  fullName: z.string().min(2, 'Min 2 chars'),
  email:    z.string().email('Invalid email'),
  password: z.string().min(6, 'Min 6 characters'),
  phone:    z.string().regex(/^[6-9]\d{9}$/, 'Enter valid 10-digit mobile').optional().or(z.literal('')),
  role:     z.enum(['USER', 'SHOP_OWNER']),
});

export default function RegisterPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const { register, handleSubmit, formState: { errors, isSubmitting }, watch } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { role: 'USER' },
  });
  const role = watch('role');

  const onSubmit = async (data) => {
    try {
      const res = await authApi.register(data);
      if (res.success) {
        setAuth(res.data.user, res.data.accessToken, res.data.refreshToken);
        toast.success('Account created!');
        navigate('/browse');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-emerald-50 to-white px-4 py-10">
      <div className="card w-full max-w-md p-8">
        <div className="flex flex-col items-center mb-8">
          <div className="bg-emerald-100 p-3 rounded-full mb-3">
            <ShoppingBag size={28} className="text-emerald-600" />
          </div>
          <h1 className="text-2xl font-bold">Create Account</h1>
          <p className="text-gray-500 text-sm mt-1">Join ReWorth today</p>
        </div>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-2 p-1 bg-gray-100 rounded-lg">
            {['USER','SHOP_OWNER'].map(r => (
              <label key={r} className={`cursor-pointer text-center py-2 rounded-md text-sm font-medium transition-all
                ${role === r ? 'bg-white text-emerald-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
                <input {...register('role')} type="radio" value={r} className="hidden" />
                {r === 'USER' ? '🛒 Shopper' : '🏪 Shop Owner'}
              </label>
            ))}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
            <input {...register('fullName')} placeholder="Rahul Sharma" className="input" />
            {errors.fullName && <p className="text-red-500 text-xs mt-1">{errors.fullName.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input {...register('email')} type="email" placeholder="you@example.com" className="input" />
            {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input {...register('password')} type="password" placeholder="Min 6 characters" className="input" />
            {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Phone (optional)</label>
            <input {...register('phone')} placeholder="9876543210" className="input" />
            {errors.phone && <p className="text-red-500 text-xs mt-1">{errors.phone.message}</p>}
          </div>
          <button type="submit" disabled={isSubmitting} className="btn-primary w-full flex items-center justify-center gap-2">
            {isSubmitting ? <><Loader2 size={16} className="animate-spin"/>Creating...</> : 'Create Account'}
          </button>
        </form>
        <p className="text-center text-sm text-gray-500 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-emerald-600 font-medium hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
