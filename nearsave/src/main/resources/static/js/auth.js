/**
 * auth.js — Authentication utilities for NearSave frontend.
 *
 * Responsibilities:
 *   - Store/read/clear JWT token from localStorage
 *   - Decode JWT payload (without a library — Base64 decode)
 *   - Update navbar based on login state + role
 *   - Provide getAuthHeaders() for authenticated API calls
 *
 * JWT is stored in localStorage under the key "ns_token".
 * On every page load, updateNavbar() reads the token and
 * shows/hides elements based on whether the user is logged in.
 */

const AUTH_KEY = 'ns_token';  // localStorage key for JWT
const USER_KEY = 'ns_user';   // localStorage key for { name, role, userId }

// ── TOKEN STORAGE ─────────────────────────────────────────────────

/** Save JWT + user info after successful login/register */
function saveAuth(token, role, name, userId) {
    localStorage.setItem(AUTH_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify({ role, name, userId }));
}

/** Retrieve the stored JWT string */
function getToken() {
    return localStorage.getItem(AUTH_KEY);
}

/** Retrieve stored user info object { role, name, userId } */
function getUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
}

/** Returns true if a JWT is stored (does NOT verify expiry — server does that) */
function isLoggedIn() {
    return !!getToken();
}

/** Clear all auth state — used by logout() */
function clearAuth() {
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(USER_KEY);
}

// ── HTTP HELPERS ──────────────────────────────────────────────────

/**
 * Returns the Authorization header object for authenticated API calls.
 *
 * Usage: fetch('/api/v1/...', { headers: getAuthHeaders() })
 *
 * The backend's JwtAuthFilter reads the "Authorization" header,
 * strips "Bearer ", and validates the JWT.
 */
function getAuthHeaders() {
    const token = getToken();
    return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
}

/**
 * Universal fetch wrapper with auth headers and JSON parsing.
 *
 * Automatically adds the JWT to every request.
 * Returns { ok: true, data: ... } or { ok: false, message: '...' }
 *
 * @param url     API endpoint
 * @param options Fetch options (method, body, etc.)
 */
async function apiCall(url, options = {}) {
    try {
        const res = await fetch(url, {
            ...options,
            headers: {
                ...getAuthHeaders(),
                ...(options.headers || {})
            }
        });

        const data = await res.json();

        if (!res.ok) {
            // Server returned an error — extract message from our ApiResponse wrapper
            return { ok: false, message: data.message || 'Something went wrong' };
        }

        return { ok: true, data };
    } catch (err) {
        // Network error or JSON parse failure
        return { ok: false, message: 'Network error. Please check your connection.' };
    }
}

// ── NAVBAR STATE ──────────────────────────────────────────────────

/**
 * Updates the navbar to reflect login/logout state.
 *
 * Shows/hides:
 *   .nav-guest-only  → links visible to NOT-logged-in users (Login, Register)
 *   .nav-auth-only   → elements visible to logged-in users (username, Logout)
 *   .customer-only   → "My Tokens" link (CUSTOMER role only)
 *   .retailer-only   → "Dashboard" link (RETAILER role only)
 *
 * Called on every page load via DOMContentLoaded.
 */
function updateNavbar() {
    const user = getUser();
    const loggedIn = isLoggedIn() && user;

    // Toggle guest vs auth elements
    document.querySelectorAll('.nav-guest-only')
        .forEach(el => el.classList.toggle('hidden', loggedIn));
    document.querySelectorAll('.nav-auth-only')
        .forEach(el => el.classList.toggle('hidden', !loggedIn));

    if (loggedIn) {
        // Show username in navbar
        document.querySelectorAll('.nav-username')
            .forEach(el => el.textContent = user.name);

        // Show role-specific nav links
        const isRetailer = user.role === 'RETAILER';
        const isAdmin = user.role === 'ADMIN';
        const isCustomer = user.role === 'CUSTOMER';
        document.querySelectorAll('.retailer-only')
            .forEach(el => el.classList.toggle('hidden', !isRetailer));
        document.querySelectorAll('.admin-only')
            .forEach(el => el.classList.toggle('hidden', !isAdmin));
        document.querySelectorAll('.customer-only')
            .forEach(el => el.classList.toggle('hidden', !isCustomer));
    }
}

/** Clears auth and reloads to homepage */
function logout() {
    clearAuth();
    window.location.href = '/index.html';
}

// ── TOAST NOTIFICATIONS ───────────────────────────────────────────

/**
 * Shows a temporary toast notification.
 *
 * @param message  Text to show
 * @param type     'success' (green) | 'error' (red) | '' (dark)
 * @param duration Milliseconds to show (default 3000)
 */
function showToast(message, type = '', duration = 3000) {
    const toast = document.getElementById('toast');
    if (!toast) return;

    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.remove('hidden');

    // Auto-hide after duration
    setTimeout(() => {
        toast.classList.add('hidden');
    }, duration);
}

// ── INIT ──────────────────────────────────────────────────────────

// Update navbar on every page load
document.addEventListener('DOMContentLoaded', updateNavbar);
