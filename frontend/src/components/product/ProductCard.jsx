import { format, parseISO } from 'date-fns';
import { MapPin, Clock, Tag } from 'lucide-react';

export default function ProductCard({ product, onClick }) {
  const statusBadge = () => {
    if (product.status === 'EXPIRING_SOON' || product.daysUntilExpiry <= 2)
      return <span className="badge-expiring">Expiring Soon</span>;
    if (product.status === 'EXPIRED')
      return <span className="badge-expired">Expired</span>;
    return <span className="badge-active">Fresh Deal</span>;
  };

  const urgencyColor = product.daysUntilExpiry <= 1 ? 'border-l-red-400'
    : product.daysUntilExpiry <= 3 ? 'border-l-amber-400' : 'border-l-emerald-400';

  return (
    <div onClick={onClick}
      className={`card border-l-4 ${urgencyColor} p-4 cursor-pointer hover:shadow-md transition-shadow`}>
      {product.imageUrl ? (
        <img src={product.imageUrl} alt={product.name}
          className="w-full h-36 object-cover rounded-lg mb-3" />
      ) : (
        <div className="w-full h-36 bg-emerald-50 rounded-lg mb-3 flex items-center justify-center text-3xl">🛒</div>
      )}
      <div className="flex items-center justify-between mb-2">
        {statusBadge()}
        <span className="text-xs text-gray-400 capitalize">
          {product.category.replace('_', ' ').toLowerCase()}
        </span>
      </div>
      <h3 className="font-semibold text-gray-900 text-sm leading-tight mb-2 line-clamp-2">{product.name}</h3>
      <div className="flex items-center gap-2 mb-3">
        <span className="text-lg font-bold text-emerald-700">₹{product.discountedPrice}</span>
        <span className="text-sm text-gray-400 line-through">₹{product.originalPrice}</span>
        <span className="ml-auto bg-red-100 text-red-600 text-xs font-bold px-2 py-0.5 rounded-full">
          {product.discountPercent}% OFF
        </span>
      </div>
      <div className="flex items-center gap-1.5 text-xs text-gray-500 mb-1">
        <Clock size={12} />
        <span>Expires {format(parseISO(product.expiryDate), 'dd MMM yyyy')}
          {' '}({product.daysUntilExpiry <= 0 ? 'today' : `${product.daysUntilExpiry}d left`})
        </span>
      </div>
      <div className="flex items-center gap-1.5 text-xs text-gray-500">
        <MapPin size={12} />
        <span className="truncate">{product.shopName}</span>
      </div>
      <div className="mt-2 flex items-center gap-1 text-xs text-gray-400">
        <Tag size={11} />
        <span>{product.stockQuantity} left in stock</span>
      </div>
    </div>
  );
}
