import axios from 'axios';

const api = axios.create({ baseURL: 'https://reworth-retail-wastage-reduction-platform.onrender.com/api/v1' });

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('accessToken');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const authApi = {
  register: (data) =>
    api.post('/auth/register', data).then(r => r.data),
  login: (data) =>
    api.post('/auth/login', data).then(r => r.data),
};

export const shopApi = {
  nearby: (lat, lng, radius = 10) =>
    api.get('/shops/nearby', { params: { lat, lng, radius } }).then(r => r.data),
  getById: (id) =>
    api.get(`/shops/${id}`).then(r => r.data),
  myShops: () =>
    api.get('/shop/my-shops').then(r => r.data),
  create: (data) =>
    api.post('/shop', data).then(r => r.data),
  update: (id, data) =>
    api.put(`/shop/${id}`, data).then(r => r.data),
  delete: (id) =>
    api.delete(`/shop/${id}`).then(r => r.data),
};

export const productApi = {
  nearby: (lat, lng, radius = 10, category, page = 0) =>
    api.get('/products/nearby', {
      params: { lat, lng, radius, category, page, size: 20 }
    }).then(r => r.data),
  getById: (id) =>
    api.get(`/products/${id}`).then(r => r.data),
  byShop: (shopId, page = 0) =>
    api.get(`/shops/${shopId}/products`, { params: { page, size: 20 } }).then(r => r.data),
  myProducts: () =>
    api.get('/shop/products').then(r => r.data),
  create: (data) =>
    api.post('/shop/products', data).then(r => r.data),
  update: (id, data) =>
    api.put(`/shop/products/${id}`, data).then(r => r.data),
  delete: (id) =>
    api.delete(`/shop/products/${id}`).then(r => r.data),
};

export default api;
