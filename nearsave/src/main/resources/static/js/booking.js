/**
 * booking.js — Handles the 30-minute token booking flow.
 *
 * Flow:
 *   1. Customer clicks "Book" on a product card
 *   2. If not logged in → redirect to login page
 *   3. POST /api/v1/bookings/reserve with productId
 *   4. Backend returns token code + expiresAt timestamp
 *   5. Show the booking modal with the token code
 *   6. Start the 30-minute countdown ring + timer
 *   7. Save token to sessionStorage (survives page refresh)
 *   8. On refresh: restore timer from sessionStorage using expiresAt
 *
 * The frontend timer is UI-only — the authoritative expiry
 * is enforced by the backend scheduler. If the customer
 * refreshes the page, the timer resumes from the correct time.
 */

/** Interval ID for the running countdown — stored so we can clearInterval() */
let countdownInterval = null;

/** The expiresAt timestamp (ISO string) of the current active booking */
let currentExpiresAt = null;

// SVG ring constants for the countdown circle
const RING_CIRCUMFERENCE = 213.6; // 2 * π * r where r=34
const TOTAL_SECONDS = 30 * 60;    // 1800 seconds = 30 minutes

// ── STEP 1: BOOK BUTTON CLICK ─────────────────────────────────────

/**
 * Called when customer clicks "Book" on a product card.
 *
 * @param productId  The product's ID from the rendered card
 * @param event      Click event — stopped to prevent card click propagation
 */
async function bookProduct(productId, event) {
    event.stopPropagation(); // Don't trigger card click

    // ── Guard: must be logged in as CUSTOMER ─────────────────────
    if (!isLoggedIn()) {
        // Save intended destination so we can redirect back after login
        sessionStorage.setItem('ns_redirect_after_login', `/index.html`);
        window.location.href = '/pages/login.html?reason=book';
        return;
    }

    const user = getUser();
    if (user.role !== 'CUSTOMER') {
        showToast('Only customers can book products. Please log in as a customer.', 'error');
        return;
    }

    // ── UI: Disable the clicked button to prevent double-clicks ──
    const btn = event.target;
    const originalText = btn.textContent;
    btn.disabled = true;
    btn.textContent = '…';

    // ── API Call: POST /api/v1/reservations/reserve ───────────────────
    const result = await apiCall('/api/v1/reservations/reserve', {
        method: 'POST',
        body: JSON.stringify({ productId })
    });

    // Re-enable button regardless of outcome
    btn.disabled = false;
    btn.textContent = originalText;

    if (!result.ok) {
        showToast(result.message, 'error');
        return;
    }

    // ── Success: show the booking modal with token ────────────────
    const booking = result.data;
    showBookingModal(booking);
}

// ── STEP 2: SHOW MODAL ────────────────────────────────────────────

/**
 * Populates and shows the booking success modal.
 *
 * Also persists the booking to sessionStorage so the timer
 * can be restored if the user refreshes the page.
 *
 * @param booking  BookingResponse from the API:
 *                 { bookingId, tokenCode, productName, shopName,
 *                   amountToPay, expiresAt, status }
 */
function showBookingModal(booking) {
    // ── Populate modal fields ─────────────────────────────────────
    document.getElementById('modal-token-code').textContent  = booking.tokenCode;
    document.getElementById('modal-product-name').textContent = booking.productName;
    document.getElementById('modal-shop-name').textContent   = booking.shopName;
    document.getElementById('modal-price').textContent       = `₹${booking.amountToPay}`;
    document.getElementById('modal-expires').textContent     = formatExpiry(booking.expiresAt);

    // ── Persist to sessionStorage for refresh recovery ────────────
    // sessionStorage lives as long as the tab is open
    sessionStorage.setItem('ns_active_booking', JSON.stringify(booking));

    // ── Show the modal overlay ────────────────────────────────────
    document.getElementById('booking-modal-overlay').classList.remove('hidden');
    document.body.style.overflow = 'hidden'; // Prevent background scroll

    // ── Start the countdown timer ─────────────────────────────────
    startCountdown(booking.expiresAt);
}

// ── STEP 3: COUNTDOWN TIMER ───────────────────────────────────────

/**
 * Starts the 30-minute countdown ring and digital timer.
 *
 * Uses the expiresAt timestamp from the API response — NOT a
 * fixed 30-minute countdown from now. This means:
 *   - If the page refreshes, the timer picks up from the correct time
 *   - If network delay caused 10 seconds to pass, those are reflected
 *
 * @param expiresAtISO  ISO datetime string: "2025-07-24T15:30:00"
 */
function startCountdown(expiresAtISO) {
    // Clear any existing timer (e.g. if modal was closed and reopened)
    if (countdownInterval) {
        clearInterval(countdownInterval);
    }

    currentExpiresAt = new Date(expiresAtISO);

    // Tick immediately, then every second
    tick();
    countdownInterval = setInterval(tick, 1000);

    function tick() {
        const now = new Date();
        const secondsLeft = Math.max(0, Math.floor((currentExpiresAt - now) / 1000));

        // ── Update digital timer display ──────────────────────────
        const minutes = Math.floor(secondsLeft / 60);
        const seconds = secondsLeft % 60;
        const timerText = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
        document.getElementById('modal-timer').textContent = timerText;

        // ── Update SVG ring ───────────────────────────────────────
        // strokeDashoffset = circumference * (1 - progress)
        // When secondsLeft = 1800 → offset = 0 (full ring)
        // When secondsLeft = 0    → offset = circumference (empty ring)
        const progress = secondsLeft / TOTAL_SECONDS;
        const offset = RING_CIRCUMFERENCE * (1 - progress);
        const arc = document.getElementById('timer-arc');
        if (arc) {
            arc.style.strokeDashoffset = offset.toFixed(2);
            // Turn red in the last 5 minutes as urgency indicator
            arc.style.stroke = secondsLeft < 300 ? '#DC2626' : '#1D9E75';
        }

        // ── Timer expired ─────────────────────────────────────────
        if (secondsLeft <= 0) {
            clearInterval(countdownInterval);
            countdownInterval = null;
            document.getElementById('modal-timer').textContent = '00:00';
            document.querySelector('.modal-status').textContent = '● EXPIRED';
            document.querySelector('.modal-status').style.background = '#FCEBEB';
            document.querySelector('.modal-status').style.color = '#A32D2D';
            sessionStorage.removeItem('ns_active_booking');
            showToast('Token expired. The stock has been released.', 'error');
        }
    }
}

// ── MODAL CONTROLS ────────────────────────────────────────────────

/** Closes the booking modal and stops the timer */
function closeBookingModal() {
    document.getElementById('booking-modal-overlay').classList.add('hidden');
    document.body.style.overflow = '';
    // Note: we do NOT clear the timer — booking is still active in the background
    // The user can reopen via My Tokens page
}

/** Closes modal if clicking the dark overlay (not the modal box itself) */
function closeModalOnOverlay(event) {
    if (event.target === document.getElementById('booking-modal-overlay')) {
        closeBookingModal();
    }
}

/** Navigate to the My Tokens page */
function goToMyTokens() {
    closeBookingModal();
    window.location.href = '/pages/my-tokens.html';
}

// ── REFRESH RECOVERY ──────────────────────────────────────────────

/**
 * On page load, check if there's an active booking in sessionStorage.
 * If so, restore the timer so the user doesn't lose their slot.
 *
 * This handles the case where the user accidentally refreshes
 * the page while the token countdown is active.
 */
function restoreActiveBooking() {
    const saved = sessionStorage.getItem('ns_active_booking');
    if (!saved) return;

    const booking = JSON.parse(saved);
    const expiresAt = new Date(booking.expiresAt);
    const now = new Date();

    // If the booking hasn't expired yet, restore the timer silently
    if (expiresAt > now) {
        // Don't auto-show the modal on restore — just keep timer running in background
        // The user can check My Tokens page to see their active booking
        startCountdown(booking.expiresAt);
    } else {
        // Booking expired while page was closed — clean up
        sessionStorage.removeItem('ns_active_booking');
    }
}

// ── HELPERS ───────────────────────────────────────────────────────

/**
 * Formats the expiresAt ISO string for display in the modal.
 * "2025-07-24T15:30:00" → "3:30 PM"
 */
function formatExpiry(isoString) {
    return new Date(isoString).toLocaleTimeString('en-IN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    });
}

// ── INIT ──────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', restoreActiveBooking);
