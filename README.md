# NearSave — Hyperlocal Near-Expiry Product Discovery

A full-stack web application where local retailers list near-expiry products at 40–50% discount, and customers within 1 km can discover and book them with a 30-minute pickup token.

---

## Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Backend    | Java 17 · Spring Boot 3.2           |
| Database   | MySQL 8.0 (Spatial / ST_ functions) |
| Auth       | Spring Security · JWT (JJWT)        |
| Frontend   | Vanilla HTML · CSS · JavaScript     |
| Scheduling | Spring `@Scheduled` (token expiry)  |
| ORM        | Spring Data JPA · Hibernate         |

---

## Project Structure

```
nearsave/
├── pom.xml                                     ← Maven dependencies
├── sql/
│   └── schema.sql                              ← DB schema + seed data
└── src/main/
    ├── java/com/nearsave/
    │   ├── NearSaveApplication.java            ← Entry point
    │   ├── config/
    │   │   ├── JwtUtil.java                    ← JWT create/validate
    │   │   ├── JwtAuthFilter.java              ← Per-request JWT check
    │   │   ├── SecurityConfig.java             ← URL rules, BCrypt, session
    │   │   ├── UserDetailsServiceImpl.java     ← Spring Security user loader
    │   │   └── JpaAuditingConfig.java          ← @CreatedDate support
    │   ├── entity/
    │   │   ├── User.java                       ← users table
    │   │   ├── Shop.java                       ← shops table (with POINT)
    │   │   ├── Product.java                    ← products table (with POINT)
    │   │   └── Booking.java                    ← bookings table (30-min token)
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── ShopRepository.java
    │   │   ├── ProductRepository.java          ← ST_Distance_Sphere query
    │   │   └── BookingRepository.java          ← expiry sweep query
    │   ├── dto/
    │   │   └── Dtos.java                       ← All request/response DTOs
    │   ├── service/
    │   │   ├── AuthService.java                ← register, login
    │   │   ├── ProductService.java             ← list product, nearby search
    │   │   └── BookingService.java             ← reserve, verify, expire
    │   ├── scheduler/
    │   │   └── TokenExpiryScheduler.java       ← runs every 60 seconds
    │   ├── controller/
    │   │   └── Controllers.java                ← Auth, Product, Booking REST
    │   └── exception/
    │       ├── AppException.java               ← custom exception with status
    │       └── GlobalExceptionHandler.java     ← converts exceptions → JSON
    └── resources/
        ├── application.properties              ← DB, JWT, business config
        └── static/
            ├── index.html                      ← Customer homepage
            ├── css/style.css                   ← Full design system
            ├── js/
            │   ├── auth.js                     ← JWT storage, API helper
            │   ├── products.js                 ← Geolocation + product cards
            │   └── booking.js                  ← Token modal + countdown
            └── pages/
                ├── login.html                  ← Login form
                ├── register.html               ← Register (Customer/Retailer)
                ├── dashboard.html              ← Retailer dashboard
                └── my-tokens.html             ← Customer token history
```

---

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+

### Step 1: Database
```sql
-- Run the schema file in MySQL Workbench or CLI
mysql -u root -p < sql/schema.sql
```

### Step 2: Configure
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
app.jwt.secret=YourVeryLongSecretKeyHere
```

### Step 3: Run
```bash
mvn spring-boot:run
```

Open: **http://localhost:8080**

---

## API Endpoints

### Auth (Public)
| Method | URL                        | Body                | Response           |
|--------|----------------------------|---------------------|--------------------|
| POST   | `/api/v1/auth/register`    | RegisterRequest     | AuthResponse (JWT) |
| POST   | `/api/v1/auth/login`       | LoginRequest        | AuthResponse (JWT) |

### Products
| Method | URL                          | Auth      | Description              |
|--------|------------------------------|-----------|--------------------------|
| GET    | `/api/v1/products/nearby`    | Public    | `?lat=X&lng=Y` → nearby  |
| POST   | `/api/v1/products`           | RETAILER  | List a new product       |
| GET    | `/api/v1/products/mine`      | RETAILER  | Retailer's own products  |

### Bookings
| Method | URL                         | Auth      | Description              |
|--------|-----------------------------|-----------|--------------------------|
| POST   | `/api/v1/bookings/reserve`  | CUSTOMER  | Create 30-min token      |
| POST   | `/api/v1/bookings/verify`   | RETAILER  | Mark order complete      |
| GET    | `/api/v1/bookings/mine`     | CUSTOMER  | Customer token history   |
| GET    | `/api/v1/bookings/shop`     | RETAILER  | Shop's incoming tokens   |

---

## Key Design Decisions

### 1. Spatial Indexing
Product locations are **denormalised** onto the `products` table so that `ST_Distance_Sphere` queries hit a single indexed table without JOINs. The `SPATIAL INDEX` on `shop_location` makes 1km radius searches run in milliseconds.

### 2. Pessimistic Locking
`SELECT ... FOR UPDATE` is applied to the Product row during booking. This prevents two customers from booking the last unit simultaneously — one waits, sees stock=0, and gets a clear error.

### 3. Dual-Timer Architecture
The frontend runs a `setInterval` countdown for a smooth UI. The backend `@Scheduled` job sweeps every minute to expire tokens and restore stock — the authoritative fail-safe regardless of frontend state.

### 4. Stateless JWT Auth
No server-side sessions. Every request carries the JWT in `Authorization: Bearer <token>`. The JWT contains the user's role, eliminating a DB lookup on most requests.

---

## Demo Credentials

| Role     | Email               | Password    |
|----------|---------------------|-------------|
| Retailer | retailer@demo.com   | password123 |
| Customer | customer@demo.com   | password123 |
