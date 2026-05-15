import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import LoginPage        from './pages/LoginPage';
import RegisterPage     from './pages/RegisterPage';
import BrowsePage       from './pages/BrowsePage';
import DashboardPage    from './pages/DashboardPage';
import ShopManagePage   from './pages/ShopManagePage';
import ProductManagePage from './pages/ProductManagePage';
import ProtectedRoute   from './components/auth/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
      <Routes>
        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/"         element={<Navigate to="/browse" replace />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/browse"    element={<BrowsePage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['SHOP_OWNER','ADMIN']} />}>
          <Route path="/shop/manage"   element={<ShopManagePage />} />
          <Route path="/shop/products" element={<ProductManagePage />} />
        </Route>

        <Route path="*" element={<Navigate to="/browse" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
