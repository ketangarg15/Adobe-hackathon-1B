# 🌾 FarmFresh — Interview Preparation Guide

> A complete cheat sheet based on your actual codebase. Read this, know your project cold.

---

## 📌 Quick Project Summary (30-Second Pitch)

> *"FarmFresh is a full-stack e-commerce web app that directly connects organic farmers with consumers, eliminating middlemen. I built it with a **React + Vite** frontend and a **Node.js + Express** REST API backend, with **MongoDB** as the database. It features role-based authentication using **Passport.js**, cloud image uploads via **Cloudinary**, and a complete order-to-delivery tracking system. It's live on **Render**."*

---

## 🏗️ Architecture Overview

```
FarmFresh/
├── Backend (Node.js + Express REST API)
│   ├── app.js              ← Entry point, middleware setup
│   ├── models/             ← Mongoose schemas (User, Product, Order, Delivery, Review)
│   ├── controllers/        ← Business logic (products, orders, deliveries)
│   ├── routes/             ← API route definitions
│   ├── middleware.js        ← isLoggedIn, isFarmer, isProductAuthor guards
│   ├── config/             ← Cloudinary + Multer storage config
│   └── utils/              ← ExpressError helper
│
└── Frontend (React 19 + Vite)
    ├── src/pages/          ← 15 pages (Home, Products, Cart, Orders, Dashboard…)
    ├── src/components/     ← Navbar, Footer, PrivateRoute, Toast, PaymentModal
    └── src/App.jsx         ← Root: state, cart logic, routing
```

**Pattern:** The backend serves the compiled React app as static files. In production, `express.static('frontend/dist')` + a catch-all route handles SPA navigation. API calls from React always go to `/api/*`.

---

## 🛠️ Tech Stack — Exact Versions

| Layer | Technology | Version |
|---|---|---|
| Frontend Framework | React | 19.x |
| Build Tool | Vite | 8.x |
| Client Routing | React Router DOM | 7.x |
| Icons | Lucide React | 1.x |
| Backend | Node.js + Express | Express 5.x |
| Database | MongoDB + Mongoose | Mongoose 8.x |
| Auth | Passport.js + passport-local | 0.7.x |
| Auth Plugin | passport-local-mongoose | 8.x |
| Sessions | express-session + connect-mongo | – |
| Image Upload | Multer + Cloudinary | – |
| Validation | express-validator | 7.x |
| Deployment | Render | – |

---

## 👤 User Roles & Permissions

There are **4 roles** defined in the User schema:

| Role | What they can do |
|---|---|
| `consumer` | Browse products, add to cart, place orders, track delivery, view reviews |
| `farmer` | Create/edit/delete their own product listings, view orders for their products, see revenue dashboard |
| `admin` | View all deliveries, all orders, assign delivery partners |
| `delivery_partner` | View their assigned deliveries, update delivery status |

> **Interview insight:** The role is stored in the User document. The backend checks it via the `isFarmer` middleware and role-based dashboard logic. The frontend enforces it with a `PrivateRoute` component.

---

## 🗄️ Database Models (Deep Dive)

### User
```js
{ email, role: enum['farmer','consumer','admin','delivery_partner'], contactNumber, address, name }
// + username, hash, salt (auto-added by passport-local-mongoose)
```

### Product
```js
{ name, description, image: {url, filename}, price, quantity, category, location,
  farmer: ref('User'), reviews: [ref('Review')] }
// Post-delete middleware: automatically deletes all associated Reviews when a product is deleted
```

### Order
```js
{ consumer: ref('User'), products: [{product: ref('Product'), quantity}],
  totalAmount, status: enum['pending','confirmed','dispatched','delivered','cancelled'], createdAt }
```

### Delivery
```js
{ order: ref('Order'), partnerName, deliveryPartner: ref('User'),
  status: enum['pending','picked_up','in_transit','delivered'], updatedAt }
```

### Review
```js
{ comment, rating(1-5), createdAt, author: ref('User') }
```

---

## 🔐 Authentication — How It Works

1. **Passport.js** uses `LocalStrategy` (username + password).
2. `passport-local-mongoose` plugin automatically adds `username`, `hash`, `salt` fields and the `authenticate()`, `serializeUser()`, `deserializeUser()` methods.
3. Sessions are stored in **MongoDB** using `connect-mongo` (not in-memory), so sessions survive server restarts.
4. Session cookies are `httpOnly`, expire in **7 days** (`maxAge: 1000 * 60 * 60 * 24 * 7`).
5. Sessions are encrypted using a `secret` key from `.env`.
6. `touchAfter: 24 * 3600` — sessions are only re-saved to DB once every 24 hours (lazy update = performance win).

**Flow:**
```
POST /api/users/signup → validates → User.register(user, password) → req.login() → returns user JSON
POST /api/users/login  → passport.authenticate('local') → req.login() → returns user JSON
POST /api/users/logout → req.logout() → session destroyed
GET  /api/users/current-user → checks req.isAuthenticated() → returns user or null
```

---

## 🛡️ Middleware (Authorization Guards)

Three custom middleware functions in `middleware.js`:

| Middleware | What it does |
|---|---|
| `isLoggedIn` | Calls `req.isAuthenticated()`. Returns 401 if not logged in. |
| `isFarmer` | Checks `req.user.role === 'farmer'`. Returns 403 otherwise. |
| `isProductAuthor` | Fetches product from DB, checks `product.farmer.equals(req.user._id)`. Prevents other farmers from editing each other's products. |

**Route guard example (products):**
```js
router.post('/',   isLoggedIn, isFarmer, upload.single('image'), createProduct);
router.put('/:id', isLoggedIn, isFarmer, isProductAuthor, upload.single('image'), updateProduct);
router.delete('/:id', isLoggedIn, isFarmer, isProductAuthor, deleteProduct);
```

---

## 🖼️ Image Uploads (Cloudinary + Multer)

1. `multer-storage-cloudinary` pipes file uploads **directly from the request to Cloudinary** — no local disk storage.
2. Images land in a `FarmFresh/` folder on Cloudinary. Allowed formats: `jpeg`, `png`, `jpg`.
3. Cloudinary returns a `url` and `filename`, which are stored in the Product's `image` object.
4. **On product update:** If a new image is uploaded, the old Cloudinary file is deleted first using `cloudinary.uploader.destroy(product.image.filename)`.
5. **On product delete:** Same cleanup — old image is removed from Cloudinary before the DB record is deleted.

---

## 🛒 Cart Logic (Frontend)

The cart is managed entirely in **React state** (no backend cart model):
- Cart state lives in `App.jsx` and is passed down as props.
- It's **persisted to `localStorage`** per user with key `cart_{userId}`.
- When a user logs out, the cart is cleared.
- Cart items: `{ productId, name, price, image, quantity, farmer }`.
- `addToCart` checks for existing items and increments quantity instead of duplicating.

> **Why localStorage?** Avoids a round-trip to the server for every cart interaction. Fast, simple. Trade-off: cart is device-specific.

---

## 📦 Order Placement Logic

The `placeOrder` controller handles both scenarios:

**Single item checkout:**
```
POST /api/orders → { productId, quantity }
→ validate stock → deduct product.quantity → create Order → create Delivery → return order
```

**Cart (multi-item) checkout:**
```
POST /api/orders → { items: [{productId, quantity}, ...] }
→ for each item: validate stock, deduct, accumulate total
→ create single Order with all items → create Delivery → return order
```

A **Delivery record is automatically created** when an order is placed (status: `pending`), linked by `order._id`.

---

## 🚚 Delivery & Order Status Sync

Two status machines run in parallel:

**Order statuses:** `pending → confirmed → dispatched → delivered | cancelled`

**Delivery statuses:** `pending → picked_up → in_transit → delivered`

The `update` controller in deliveries **mirrors the delivery status to the Order**:
```js
if (status === 'picked_up' || 'in_transit') → Order.status = 'dispatched'
if (status === 'delivered')                  → Order.status = 'delivered'
```

`assignPartner` controller: Sets `deliveryPartner`, `status: 'picked_up'`, and updates Order to `dispatched`.

---

## 📊 Dashboard (Role-Based Data)

`GET /api/dashboard/data` returns different data based on role:

| Role | Stats returned |
|---|---|
| `consumer` | totalOrders, moneySaved (15% of total spend), last 5 orders |
| `farmer` | totalListings, pendingOrders, revenue (from non-cancelled orders), last 5 products |
| `admin` | totalOrders, totalDeliveries, pendingDeliveries, last 10 deliveries |
| `delivery_partner` | totalDeliveries, all their assigned deliveries |

> **moneySaved** is calculated as 15% of total spend — a business assumption that FarmFresh is 15% cheaper than supermarkets.

---

## 🌐 API Endpoints

| Method | Route | Auth | Description |
|---|---|---|---|
| GET | `/api/users/current-user` | None | Get logged-in user |
| POST | `/api/users/signup` | None | Register new user |
| POST | `/api/users/login` | None | Login |
| POST | `/api/users/logout` | None | Logout |
| GET | `/api/products` | None | Get all products (with avg rating) |
| GET | `/api/products/:id` | None | Get single product + reviews |
| POST | `/api/products` | LoggedIn + Farmer | Create product |
| PUT | `/api/products/:id` | LoggedIn + Farmer + Author | Update product |
| DELETE | `/api/products/:id` | LoggedIn + Farmer + Author | Delete product |
| POST | `/api/products/:id/reviews` | LoggedIn | Add review |
| GET | `/api/orders` | LoggedIn | Get user's orders |
| POST | `/api/orders` | LoggedIn | Place order |
| GET | `/api/orders/:id` | LoggedIn | Get specific order |
| POST | `/api/orders/:id/cancel` | LoggedIn | Cancel pending order |
| POST | `/api/orders/:id/reorder` | LoggedIn | Reorder (stub) |
| GET | `/api/deliveries` | LoggedIn | Get deliveries (role-filtered) |
| PATCH | `/api/deliveries/:id` | LoggedIn | Update delivery status |
| GET | `/api/deliveries/partners` | LoggedIn | List delivery partners |
| POST | `/api/deliveries/:id/assign` | LoggedIn | Assign delivery partner |
| GET | `/api/dashboard/data` | LoggedIn | Role-based dashboard data |

---

## ⚙️ Frontend Routing

| Path | Component | Access |
|---|---|---|
| `/` | Home | Public |
| `/products` | Products | Public |
| `/products/:id` | ProductDetail | Public |
| `/farmers` | Farmers | Public |
| `/farmers/:id` | FarmerProfile | Public |
| `/about` | About | Public |
| `/login` | Login | Public (redirects if logged in) |
| `/signup` | Signup | Public (redirects if logged in) |
| `/dashboard/consumer` | Dashboard | Consumer only |
| `/dashboard/farmer` | Dashboard | Farmer only |
| `/dashboard/admin` | Dashboard | Admin only |
| `/dashboard/delivery_partner` | Dashboard | Delivery Partner only |
| `/cart` | Cart | Consumer only |
| `/orders` | Orders | Consumer only |
| `/orders/:id` | OrderDetail | Any logged-in user |
| `/deliveries` | Deliveries | Any logged-in user |
| `/products/new` | ProductForm | Farmer only |
| `/products/:id/edit` | ProductForm | Farmer only |

**`PrivateRoute` component** wraps protected routes and checks both `user` existence and `role`.

---

## 🚀 Deployment

- **Platform:** Render
- **Build Command:** `npm run build` → installs frontend packages + runs `vite build`
- **Start Command:** `npm start` → runs `node app.js`
- **Static serving:** Express serves `frontend/dist/` as static files
- **SPA routing:** Regex catch-all `(/^(?!\/api).*/)` → sends `index.html` for all non-API routes
- **Live URL:** https://farmfresh-s9ul.onrender.com/

---

## ❓ Common Interview Questions & Model Answers

### Q1: "Walk me through the project."
> *"FarmFresh is a farm-to-consumer marketplace. The backend is an Express REST API with MongoDB. The frontend is a React SPA built with Vite. Users register as farmers, consumers, delivery partners, or admins. Farmers list products, consumers browse, add to cart, and checkout. When an order is placed, a delivery record is auto-created. Admins and delivery partners manage the delivery lifecycle. Images are stored on Cloudinary. Sessions persist in MongoDB. It's deployed on Render."*

### Q2: "How did you handle authentication?"
> *"I used Passport.js with the Local Strategy. The `passport-local-mongoose` plugin handles password hashing with PBKDF2 — I never store plain text passwords. Sessions are stored in MongoDB via `connect-mongo` so they survive server restarts. The session cookie is `httpOnly` and has a 7-day expiry. On the React side, I fetch `/api/users/current-user` on app load to hydrate the auth state."*

### Q3: "How does your authorization work?"
> *"I have three middleware layers. `isLoggedIn` checks `req.isAuthenticated()`. `isFarmer` checks the role on the user object. `isProductAuthor` queries the DB to verify the logged-in user owns the product before any update or delete. These are chained in the route definitions so unauthorized users get a 403 before the controller even runs."*

### Q4: "Why did you use MongoDB instead of SQL?"
> *"For a marketplace with products that have varying attributes — organic produce can have different metadata — a flexible document model fits well. MongoDB's nested arrays work naturally for a product's `reviews` array. Also, Mongoose's `populate()` gives ORM-like joins when needed, like populating farmer details or review authors."*

### Q5: "How does the cart work?"
> *"The cart is client-side state in React, persisted to `localStorage` with a user-specific key. This keeps cart interactions instant without server round-trips. On checkout, the entire cart is sent as an `items` array to `POST /api/orders`. The backend validates each item's stock, deducts inventory, calculates the total, and creates a single Order and Delivery record atomically."*

### Q6: "What happens when a product is deleted?"
> *"Two cleanup operations happen: first, the `isProductAuthor` middleware confirms ownership. Then in the controller, `cloudinary.uploader.destroy()` removes the image from Cloudinary using the stored filename. Finally, `findByIdAndDelete()` triggers a Mongoose post-hook (`productSchema.post('findOneAndDelete')`) that runs `Review.deleteMany()` to remove all reviews associated with that product."*

### Q7: "How did you handle image uploads?"
> *"I used Multer as the multipart form parser with `multer-storage-cloudinary` as the storage engine. This streams the file directly to Cloudinary without writing to disk. The `upload.single('image')` middleware runs before the controller. Cloudinary responds with a URL and filename, which I store in the Product document. On update, I delete the old image from Cloudinary before storing the new one."*

### Q8: "How is your SPA deployed on a single server?"
> *"The Express server serves the compiled React build from `frontend/dist` as static files. For any URL that doesn't start with `/api`, a regex catch-all route returns `index.html`. React Router then handles client-side navigation. This means the whole app runs on one Node.js process — simpler to deploy and cheaper."*

### Q9: "How does the delivery system work?"
> *"When an order is placed, a Delivery document is automatically created in `pending` status. Admins can list delivery partners (users with role `delivery_partner`) and assign one. When the delivery status changes — `picked_up`, `in_transit`, or `delivered` — the system also updates the parent Order's status. So a customer checking their order always sees an up-to-date status even though the actual tracking is in the Delivery collection."*

### Q10: "What would you improve if you had more time?"
> *"A few things: First, the order placement isn't in a database transaction — if two users order the last item simultaneously, there's a race condition. I'd add Mongoose transactions or use `findOneAndUpdate` with an atomic decrement and check. Second, the cart is localStorage-only, so it doesn't sync across devices. Third, I'd add email notifications (using Nodemailer or SendGrid) for order confirmations. Fourth, payments are currently a UI-only modal — I'd integrate Razorpay or Stripe for real transactions."*

---

## 🔥 Key Technical Decisions (Talk about these confidently)

| Decision | Why |
|---|---|
| Sessions over JWT | Passport.js local strategy works naturally with sessions; simpler revocation |
| `connect-mongo` | Session persistence across server restarts (critical for Render's free tier which spins down) |
| Cart in localStorage | Zero latency cart UX; simple for scope of project |
| Multer + Cloudinary | No server disk space used; CDN delivery of images |
| SPA + REST API | Clean separation; frontend can be deployed independently later |
| Express 5.x | Async error handling is built-in (no need for `express-async-errors` package) |
| Mongoose post-hook for reviews | Keeps data integrity without manual cleanup in every delete controller |

---

## ⚠️ Honest Weaknesses (Shows maturity — interviewers respect this)

- **No DB transactions:** Stock deduction during order placement isn't atomic. Race condition exists.
- **No payment gateway:** PaymentModal is a simulated UI.
- **`reorder` is a stub:** The controller returns `success: true` but doesn't actually recreate the cart.
- **`moneySaved` is hardcoded at 15%:** Business logic assumption, not real data.
- **No refresh token:** Session expiry kicks the user out; no silent refresh.
- **No rate limiting:** The API has no protection against brute-force login attempts.

---

## 🎯 Numbers to Remember

- **4 user roles:** farmer, consumer, admin, delivery_partner
- **5 models:** User, Product, Order, Delivery, Review
- **5 order statuses:** pending, confirmed, dispatched, delivered, cancelled
- **4 delivery statuses:** pending, picked_up, in_transit, delivered
- **15 frontend pages**
- **5 React components**
- **Session expiry: 7 days**
- **Session lazy-save: 24 hours** (`touchAfter`)
- **Revenue stat: 15% savings assumption**
- **Live on:** https://farmfresh-s9ul.onrender.com/
