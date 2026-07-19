# 🏠 StayEase — Complete Interview Preparation Guide

## What is StayEase?

StayEase is a **full-stack Airbnb-like vacation rental platform** built with Node.js, Express, MongoDB, and EJS. It allows users to list properties, browse and book stays, leave reviews, and manage their profile — all with role-based access for **guests** and **hosts**.

---

## 🧱 Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Runtime | Node.js (v23) | Non-blocking, async I/O |
| Framework | Express v5 | Routing, middleware pipeline |
| Database | MongoDB + Mongoose | Flexible schema, document model |
| Templating | EJS + ejs-mate | Server-side rendering with layouts |
| Auth | Passport.js (Local Strategy) | Session-based authentication |
| Storage | Cloudinary + Multer | Cloud image uploads |
| Maps | MapTiler SDK | Geocoding + interactive maps |
| Security | Helmet, express-rate-limit | HTTP headers, brute-force protection |
| Sessions | express-session + connect-mongo | Persistent sessions stored in MongoDB |
| Validation | Joi | Server-side schema validation |
| Module System | ES Modules (`"type": "module"`) | Modern `import/export` syntax |

---

## 🗂️ Project Architecture (MVC Pattern)

```
StayEase/
├── app.js              ← Entry point, middleware stack, error handling
├── CloudConfig.js      ← Cloudinary + Multer storage config
├── middleware.js       ← Custom middleware (auth, authorization, validation)
├── schema.js           ← Joi validation schemas
├── models/             ← Mongoose schemas (Listing, Review, User, Booking)
├── controllers/        ← Business logic for each feature
├── routes/             ← Express Router definitions
├── views/              ← EJS templates (listings, users, bookings, etc.)
├── public/             ← Static assets (CSS, JS, images)
├── utils/              ← Helper utilities (wrapAsync, ExpressError)
└── init/               ← DB seed data
```

**The MVC flow:** `Route → Middleware → Controller → Model → View`

---

## 📦 Data Models (Mongoose Schemas)

### User
```js
{ email, username, password (hashed), role: ["guest"|"host"], wishlist: [Listing ref] }
```
- Uses `passport-local-mongoose` plugin which automatically adds `username`, `hash`, `salt` fields and methods like `register()`, `authenticate()`, `serializeUser()`, `deserializeUser()`
- `role` field enables **role-based access control (RBAC)**

### Listing
```js
{ title, description, image: {url, filename}, price, location, country,
  category (enum), reviews: [Review ref], owner: User ref,
  geometry: GeoJSON Point, isAvailable: Boolean }
```
- **GeoJSON `geometry`** field stores `{ type: "Point", coordinates: [lng, lat] }` for map display
- **Virtual field `avgRating`** — computed from populated reviews (not stored in DB)
- **Post middleware** `findOneAndDelete` — cascades review deletion when a listing is deleted
- `isAvailable` flag — hosts can toggle bookability

### Review
```js
{ comment, rating (1–5), createdAt, author: User ref }
```
- Embedded as ObjectId refs inside Listing's `reviews` array

### Booking
```js
{ listing: Listing ref, guest: User ref, startDate, endDate, nights,
  totalPrice, paymentId, paymentStatus: ["pending"|"paid"|"failed"],
  status: ["confirmed"|"cancelled"], cancelledAt, createdAt }
```
- **Pre-save hook** auto-computes `nights` from date diff
- `paymentId` stores a mock transaction ID (e.g., `TXN_ABC123XY`)
- Date overlap check at booking time prevents double-booking

---

## 🔐 Authentication & Authorization

### Authentication Flow (Passport.js)
1. User submits login form → `passport.authenticate("local")` runs
2. Passport calls `User.authenticate()` (from `passport-local-mongoose`) — compares hashed password
3. On success → `passport.serializeUser()` saves only `user._id` to session
4. On every request → `passport.deserializeUser()` fetches full user from DB using session's `_id`
5. `req.user` is available in all routes and `res.locals.currUser` in all views

### Key Middleware
| Middleware | Purpose |
|---|---|
| `isLoggedIn` | Checks `req.isAuthenticated()`, saves `redirectUrl` to session, redirects to `/login` |
| `saveRedirectUrl` | Copies `session.redirectUrl` → `res.locals` so it survives Passport's session regeneration |
| `isOwner` | Fetches listing and compares `listing.owner._id` to `currUser._id` |
| `isHost` | Checks `currUser.role === "host"`, blocks guests from creating listings |
| `isReviewAuthor` | Allows review deletion only by the review author OR the listing owner |
| `validateListing` | Runs Joi schema validation on `req.body`, throws `ExpressError(400)` if invalid |
| `validatereviews` | Same for review data |

### Why `saveRedirectUrl` is needed
Passport's `req.login()` regenerates the session, wiping `session.redirectUrl`. So before authentication, the URL is moved to `res.locals` to persist it.

---

## 🌐 Routing Structure

```
GET  /                         → Home page (featured listings)
GET  /listings                 → All listings (with filters/search/pagination)
GET  /listings/new             → New listing form [host only]
POST /listings                 → Create listing [host only]
GET  /listings/:id             → Show listing + map + booked dates
GET  /listings/:id/edit        → Edit form [owner only]
PUT  /listings/:id             → Update listing [owner only]
DELETE /listings/:id           → Delete listing (cascades reviews) [owner only]
POST /listings/:id/toggle-availability → Toggle isAvailable [owner only]

POST /listings/:id/reviews     → Add review [logged in]
DELETE /listings/:id/reviews/:reviewId → Delete review [author or listing owner]

GET  /listings/:id/bookings/checkout   → Checkout page
POST /listings/:id/bookings/book       → Confirm booking
GET  /listings/:id/bookings/receipt/:bookingId → Booking receipt
POST /listings/:id/bookings/:bookingId/cancel  → Cancel booking

POST /wishlist/:listingId/toggle  → Toggle wishlist (returns JSON)
GET  /wishlist                    → Show wishlist

GET  /signup / POST /signup   → Register
GET  /login  / POST /login    → Login
GET  /logout                  → Logout
GET  /profile                 → User dashboard (role-aware)
```

**`mergeParams: true`** is used in booking and review routers so they can access `:id` from the parent route.

---

## 🔍 Search, Filter & Pagination

In `listings.js` controller (`index` function):
- **Text search** via `$regex` on title, location, country, category (case-insensitive)
- **Category filter** — exact enum match
- **Price range** using `$gte` / `$lte` on the price field
- **Sort** — price ascending/descending, or newest first (`_id: -1`)
- **Pagination** — `skip()` and `limit()` with `ITEMS_PER_PAGE = 12`

---

## 📸 Image Upload (Cloudinary + Multer)

1. `multer` intercepts `multipart/form-data` requests
2. `multer-storage-cloudinary` streams the file directly to Cloudinary (no local disk storage)
3. Cloudinary returns `{ path (url), filename }` on `req.file`
4. The URL is stored in `listing.image.url`; filename allows future deletion
5. For edit preview: the URL is transformed with `/upload/w_250` to get a thumbnail

---

## 🗺️ Map & Geocoding (MapTiler SDK)

- On `createListing`: `geocoding.forward(location, { limit: 1 })` converts address → `GeoJSON geometry`
- `geometry` is saved to the Listing document
- On the show page: `mapToken` is passed to the EJS template and used client-side to render an interactive map pinning the listing's coordinates

---

## 📅 Booking System

### Checkout Flow
1. User picks dates on the listing page (Flatpickr calendar with **disabled booked dates**)
2. Redirects to `/checkout` with `startDate` and `endDate` as query params
3. Server calculates: `nights`, `basePrice = price × nights`, `gst = basePrice × 18%`, `totalPrice`
4. User confirms → POST to `/book`

### Booking Logic (`processBooking`)
- **Overlap check** using MongoDB query:
  ```js
  { listing: id, status: "confirmed", startDate: { $lt: end }, endDate: { $gt: start } }
  ```
- Prevents double-booking before saving
- A mock `paymentId` (`TXN_XXXXXXX`) is generated (no real payment gateway)
- After booking → redirected to **receipt page**

### Receipt + QR Code
- Receipt page shows all booking details
- `qrcode` package generates a Base64 Data URL encoding booking details (guest, listing, dates, txn ID) as a scannable QR code

### Cancellation
- Guest can cancel from their profile
- Sets `status = "cancelled"` and `cancelledAt = new Date()`
- Does NOT refund (mock payment)

---

## 🔒 Security Features

| Feature | Implementation |
|---|---|
| **Helmet** | Sets secure HTTP headers (XSS protection, content-type sniffing, etc.) |
| **Rate Limiting** | `express-rate-limit` — max 20 requests per 15 min on `/login` and `/signup` |
| **NoSQL Injection Prevention** | Custom inline middleware that removes keys with `$` or `.` from `req.body` |
| **Session Security** | `httpOnly: true` cookie, sessions stored in MongoDB (not in-memory), encrypted with `SESSION_SECRET` |
| **CSRF Mitigation** | Sessions are `httpOnly`, re-authentication enforced on sensitive operations |
| **Input Validation** | Joi schemas validate all form submissions server-side before DB operations |

---

## ⚠️ Error Handling

### `wrapAsync` Utility
```js
// utils/wrapAsync.js
module.exports = (fn) => (req, res, next) => fn(req, res, next).catch(next);
```
Wraps async route handlers so thrown errors are automatically forwarded to Express's error middleware — no need for try/catch in every route.

### `ExpressError` Class
A custom error class with `statusCode` and `message` fields, extends `Error`.

### Global Error Handler (bottom of `app.js`)
Catches all errors, renders `error.ejs` with the status code and message. Also ensures `res.locals` are always defined to prevent EJS `ReferenceError`.

### 404 Handler
A catch-all middleware before the error handler creates a `new ExpressError(404, "Page Not Found")`.

---

## 👤 User Roles (RBAC)

| Action | Guest | Host |
|---|---|---|
| Browse listings | ✅ | ✅ |
| Search & filter | ✅ | ✅ |
| Book a listing | ✅ | ✅ |
| Add to wishlist | ✅ | ✅ |
| Leave a review | ✅ | ✅ |
| Create a listing | ❌ | ✅ |
| Edit/Delete own listing | ❌ | ✅ |
| Toggle availability | ❌ | ✅ |
| View earnings dashboard | ❌ | ✅ |

---

## 📊 Host Dashboard (Profile)

For **hosts** (`showProfile` controller):
- Fetches all their listings
- Fetches all confirmed bookings across those listings
- Calculates `totalEarnings` (sum of `totalPrice` for confirmed bookings)
- Builds **last 6 months** of monthly earnings data for a chart

For **guests**:
- Shows their booking history (sorted newest first)
- Shows their wishlist

---

## ❓ Common Interview Questions & Answers

**Q: Why use MongoDB over SQL here?**
> Listings have varied attributes (categories, nested image objects, geometry). MongoDB's flexible schema suits this better. Reviews are embedded as refs inside listings, making it easy to cascade-delete.

**Q: How does Passport.js session authentication work?**
> On login, only the user's `_id` is serialized into the session cookie. On every request, the `_id` is deserialized by querying MongoDB for the full user object — that's `req.user`.

**Q: How do you prevent double bookings?**
> Before saving a new booking, a MongoDB query checks for any confirmed booking on the same listing where `startDate < newEnd AND endDate > newStart` — the standard interval overlap condition.

**Q: What is `mergeParams: true`?**
> When a child router is mounted with a `:param` in the parent's path (e.g., `/listings/:id/reviews`), `mergeParams: true` allows the child router to access `:id` from `req.params`.

**Q: What is the role of `wrapAsync`?**
> It's a higher-order function that wraps async route handlers and calls `.catch(next)` so that any rejected promise is automatically passed to Express's error-handling middleware instead of causing an unhandled promise rejection.

**Q: How do you secure against NoSQL injection?**
> A custom middleware scans all `req.body` keys and removes any that contain `$` or `.` — these are MongoDB operators that could be injected to manipulate queries (e.g., `{ "$gt": "" }` as a password).

**Q: How is image transformation done for the edit page?**
> Cloudinary supports URL-based transformations. The stored image URL is modified by inserting `/w_250` after `/upload` to get a 250px-wide thumbnail — no separate API call needed.

**Q: What is a Mongoose virtual?**
> A virtual is a computed property not stored in MongoDB. `avgRating` is a virtual on Listing — it calculates the average from populated `reviews` on the fly. Must call `.toObject({ virtuals: true })` to include it in plain objects.

**Q: Why `touchAfter: 24 * 3600` in MongoStore?**
> This lazily updates the session in MongoDB only if it hasn't been touched in 24 hours — reduces unnecessary DB writes for every single request.

**Q: How does QR code generation work?**
> The `qrcode` library encodes a JSON string with booking metadata into a Base64 PNG data URL using `QRCode.toDataURL()`. This is embedded directly in the HTML `<img src="...">` — no file is written to disk.

---

## 🔑 Key Concepts to Know Cold

- **ES Modules** — `import/export`, why `__dirname` needs `fileURLToPath` workaround
- **Mongoose middleware** — `pre("save")`, `post("findOneAndDelete")` hooks
- **Passport flow** — serialize → session → deserialize → `req.user`
- **Method Override** — HTML forms only support GET/POST; `?_method=PUT` fakes PUT/DELETE
- **Flashs messages** — set with `req.flash("success", "msg")`, consumed once via `res.locals`
- **Joi validation** — server-side, validates shape/type/range of incoming data before DB
- **Cloudinary** — cloud storage, transformations via URL params, no local disk usage
- **Rate limiting** — protects auth endpoints from brute-force attacks
- **GeoJSON Point** — `{ type: "Point", coordinates: [longitude, latitude] }` — used for map pins
- **Flatpickr** — date picker library that disables already-booked date ranges client-side
