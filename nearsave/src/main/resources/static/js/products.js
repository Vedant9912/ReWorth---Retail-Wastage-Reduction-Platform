/**
 * products.js — Customer-side product discovery.
 *
 * Flow:
 *   1. Page loads → request GPS coordinates from browser
 *   2. GPS received → call GET /api/v1/products/nearby?lat=&lng=
 *   3. API responds → render product cards + sidebar shops
 *   4. User types in search → filter cards in-memory (no new API call)
 *   5. User clicks category chip → filter cards in-memory
 *
 * All filtering is done client-side after a single API call —
 * fast, no extra round-trips, works offline after first load.
 */

/** All products fetched from API — kept in memory for client-side filtering */
let allProducts = [];

/** Currently active category filter */
let activeCategoryFilter = 'ALL';

// ── EMOJI MAP ─────────────────────────────────────────────────────
// Maps backend Category enum values to display emojis
const CATEGORY_EMOJI = {
    DAIRY:      '🥛',
    BAKERY:     '🍞',
    COSMETICS:  '💄',
    SNACKS:     '🍿',
    BEVERAGES:  '🧃',
    MEDICINES:  '💊',
    OTHER:      '📦'
};

// ── STEP 1: GEOLOCATION ───────────────────────────────────────────

/**
 * Called on DOMContentLoaded.
 * Asks browser for GPS, then triggers the API call.
 *
 * navigator.geolocation.getCurrentPosition() is asynchronous.
 * It shows the browser's "Allow location" prompt to the user.
 */
function initProductDiscovery() {
    if (!navigator.geolocation) {
        showLocationError('Your browser does not support geolocation.');
        return;
    }

    navigator.geolocation.getCurrentPosition(
        onLocationSuccess,   // Callback when GPS is available
        onLocationError,     // Callback when user denies or GPS fails
        {
            enableHighAccuracy: true,
            timeout: 10000,         // 10 seconds before giving up
            maximumAge: 60000       // Accept cached location up to 1 minute old
        }
    );
}

/**
 * GPS coordinates received — update UI and fetch nearby products.
 *
 * @param position  GeolocationPosition object from browser API
 */
async function onLocationSuccess(position) {
    const lat = position.coords.latitude;
    const lng = position.coords.longitude;

    // Update the location pill in the search bar
    document.getElementById('loc-label').textContent = `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
    document.getElementById('hero-location').textContent = 'Within 1 km of your location';

    // Fetch nearby products from the backend
    await fetchNearbyProducts(lat, lng);
}

/** GPS failed or denied — show fallback UI */
function onLocationError(error) {
    const messages = {
        1: 'Location access denied. Please allow location in browser settings.',
        2: 'Location unavailable. Please check your GPS.',
        3: 'Location request timed out. Please try again.'
    };
    showLocationError(messages[error.code] || 'Could not get your location.');
}

function showLocationError(message) {
    document.getElementById('products-loading').classList.add('hidden');
    document.getElementById('products-empty').classList.remove('hidden');
    document.getElementById('product-count').textContent = 'Location unavailable';
    document.getElementById('loc-label').textContent = 'Location unavailable';
    showToast(message, 'error', 5000);
}

// ── STEP 2: API CALL ──────────────────────────────────────────────

/**
 * Fetches products within 1km of the given coordinates.
 *
 * Endpoint: GET /api/v1/products/nearby?lat=X&lng=Y  (PUBLIC — no JWT needed)
 *
 * @param lat  Customer's latitude
 * @param lng  Customer's longitude
 */
async function fetchNearbyProducts(lat, lng) {
    // Show skeleton loading cards while waiting
    document.getElementById('products-loading').classList.remove('hidden');
    document.getElementById('products-grid').classList.add('hidden');
    document.getElementById('products-empty').classList.add('hidden');

    const result = await apiCall(`/api/v1/products/nearby?lat=${lat}&lng=${lng}`);

    // Hide loading skeletons
    document.getElementById('products-loading').classList.add('hidden');

    if (!result.ok) {
        showLocationError(result.message);
        return;
    }

    allProducts = result.data; // Store for client-side filtering

    // Update hero stats
    document.getElementById('stat-products').textContent = allProducts.length;
    const uniqueShops = new Set(allProducts.map(p => p.shopName)).size;
    document.getElementById('stat-shops').textContent = uniqueShops;

    // Build category filter chips from the returned data
    buildCategoryChips(allProducts);

    // Render all products initially
    renderProducts(allProducts);

    // Render sidebar shops
    renderNearbyShops(allProducts);
}

// ── STEP 3: RENDER PRODUCT CARDS ─────────────────────────────────

/**
 * Renders product cards into the #products-grid container.
 *
 * Uses innerHTML for simplicity. Each card is a template literal.
 * The book button triggers booking.js:bookProduct(id).
 *
 * @param products  Array of ProductResponse DTOs from the API
 */
function renderProducts(products) {
    const grid = document.getElementById('products-grid');
    const countEl = document.getElementById('product-count');

    if (products.length === 0) {
        grid.classList.add('hidden');
        document.getElementById('products-empty').classList.remove('hidden');
        countEl.textContent = '0 products found';
        return;
    }

    document.getElementById('products-empty').classList.add('hidden');
    grid.classList.remove('hidden');
    countEl.textContent = `${products.length} product${products.length !== 1 ? 's' : ''} · sorted by distance`;

    // Build all card HTML at once for a single DOM write (performance)
    grid.innerHTML = products.map(p => `
        <div class="product-card" data-id="${p.id}">
            <div class="card-icon">${CATEGORY_EMOJI[p.category] || '📦'}</div>
            <div class="card-name" title="${p.name}">${p.name}</div>
            <div class="card-shop">${p.shopName}</div>
            <div class="card-prices">
                <span class="price-old">₹${p.mrp}</span>
                <span class="price-new">₹${p.discountedPrice}</span>
                <span class="discount-badge">${p.discountPercent}% off</span>
            </div>
            <div class="card-footer">
                <span class="card-dist">
                    <i class="ti ti-map-pin"></i>
                    ${formatDistance(p.distanceMetres)} · exp ${formatDate(p.expiryDate)}
                </span>
                <button class="book-btn" onclick="bookProduct(${p.id}, event)">Book</button>
            </div>
        </div>
    `).join('');
}

// ── STEP 4: CLIENT-SIDE FILTERING ────────────────────────────────

/**
 * Called on every keystroke in the search input.
 * Filters allProducts in memory — no API call needed.
 */
function filterProducts() {
    const query = document.getElementById('search-input').value.toLowerCase().trim();

    let filtered = allProducts;

    // Apply category filter first
    if (activeCategoryFilter !== 'ALL') {
        filtered = filtered.filter(p => p.category === activeCategoryFilter);
    }

    // Then apply text search across name, shop name, and category
    if (query) {
        filtered = filtered.filter(p =>
            p.name.toLowerCase().includes(query) ||
            p.shopName.toLowerCase().includes(query) ||
            p.category.toLowerCase().includes(query)
        );
    }

    renderProducts(filtered);
}

/**
 * Sets the active category filter chip and re-renders.
 *
 * @param chipEl  The clicked .filter-chip element
 */
function setCategoryFilter(chipEl) {
    // Deactivate all chips
    document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
    // Activate the clicked one
    chipEl.classList.add('active');
    // Store active filter category
    activeCategoryFilter = chipEl.dataset.cat;
    // Re-filter with the new category
    filterProducts();
}

/**
 * Builds the category filter chip row from actual categories in the data.
 * "All" is always first; remaining categories are alphabetically sorted.
 *
 * @param products  Full product list from API
 */
function buildCategoryChips(products) {
    const filterRow = document.getElementById('filter-row');
    const categories = [...new Set(products.map(p => p.category))].sort();

    // Start with "All" chip (always present)
    filterRow.innerHTML = `<div class="filter-chip active" data-cat="ALL" onclick="setCategoryFilter(this)">All</div>`;

    // Add one chip per unique category
    categories.forEach(cat => {
        const emoji = CATEGORY_EMOJI[cat] || '📦';
        const label = cat.charAt(0) + cat.slice(1).toLowerCase(); // "DAIRY" → "Dairy"
        filterRow.innerHTML += `
            <div class="filter-chip" data-cat="${cat}" onclick="setCategoryFilter(this)">
                ${emoji} ${label}
            </div>
        `;
    });
}

// ── SIDEBAR: NEARBY SHOPS ─────────────────────────────────────────

/**
 * Builds the sidebar shop list by grouping products by shop.
 * Shows shop initials, distance, and item count.
 *
 * @param products  Full product list from API
 */
function renderNearbyShops(products) {
    const container = document.getElementById('nearby-shops');
    if (!container) return;

    // Group products by shop name, track distance
    const shops = {};
    products.forEach(p => {
        if (!shops[p.shopName]) {
            shops[p.shopName] = { count: 0, distance: p.distanceMetres };
        }
        shops[p.shopName].count++;
    });

    // Sort shops by distance
    const sorted = Object.entries(shops).sort((a, b) => a[1].distance - b[1].distance);

    container.innerHTML = sorted.map(([name, info]) => {
        // Generate 2-letter initials (e.g. "Krishna Dairy" → "KD")
        const initials = name.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
        return `
            <div class="nearby-row">
                <div class="shop-init">${initials}</div>
                <div class="shop-info">
                    <div class="shop-name">${name}</div>
                    <div class="shop-dist">${formatDistance(info.distance)} · open now</div>
                </div>
                <span class="shop-count">${info.count} item${info.count !== 1 ? 's' : ''}</span>
            </div>
        `;
    }).join('');
}

// ── HELPERS ───────────────────────────────────────────────────────

/** Formats distance: shows "380m" under 1000, "1.2 km" above */
function formatDistance(metres) {
    return metres < 1000
        ? `${Math.round(metres)}m`
        : `${(metres / 1000).toFixed(1)} km`;
}

/**
 * Formats an ISO date string "2025-07-28" → "28 Jul"
 * Used in the product card expiry display.
 */
function formatDate(dateStr) {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

/** Smooth scrolls to the search section (hero CTA button) */
function scrollToProducts() {
    document.getElementById('search-section')?.scrollIntoView({ behavior: 'smooth' });
}

// ── INIT ──────────────────────────────────────────────────────────

// Start geolocation flow once DOM is ready
document.addEventListener('DOMContentLoaded', initProductDiscovery);
