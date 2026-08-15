# Kasir Pro — Android POS & Cashflow

A complete, offline-first cashier (POS) app for Android. Built with Kotlin, Jetpack Compose
and Material 3, backed by a Room database. No server, no account, no subscription — the shop
owns its data.

The design brief came from what Indonesian POS apps actually ship
([Moka](https://www.hashmicro.com/id/blog/aplikasi-kasir-toko-terbaik-untuk-membantu-bisnis-anda/),
[Kasir Warung](https://play.google.com/store/apps/details?id=com.dikalabs.kasirwarung),
[Griyo Pos](https://play.google.com/store/apps/details/Griyo_Pos_POS_and_Cashflow?id=com.griyosolusi.griyopos))
crossed with the standard cash-register feature checklist
([Square](https://squareup.com/us/en/the-bottom-line/operating-your-business/important-cash-register-features-for-small-businesses),
[Mobile Transaction](https://www.mobiletransaction.org/pos-system-features-list/)).

---

## Features

### Cashier (POS)
- Product grid with search by **name, SKU or barcode**, plus category filter chips
- **Camera barcode scanner** (ML Kit + CameraX) with manual-entry fallback
- Cart with per-line quantity, per-line discount and per-line note
- **Hold / resume orders** — park a cart for table 4, serve the next customer, come back
- Whole-order discount, percent or fixed amount
- **Split payment** — stack cash + QRIS + card on one bill, one receipt
- Cash quick-tender buttons (exact money, round up to 50k/100k…) and a large on-screen keypad
- Automatic change calculation
- Receipt: on-screen preview, **share as text**, and **print** through the Android print
  framework (works with any installed printer service; "Save as PDF" comes free)

### Stock management — wired into sales
- Selling **deducts stock inside the same transaction as the sale**; it cannot half-apply
- Voiding a sale **puts the stock back** and posts a reversing cash entry
- **Stock in** (goods received) with optional cost-price update and optional cash-out posting
- **Stock take** — enter what you counted, the difference is recorded as a signed adjustment
- **Write-off** for damaged / expired / lost goods
- **Movement ledger**: an append-only audit trail — what moved, when, why, balance before
  and after
- Low-stock alerts driven by a per-product reorder point
- Stock valuation at cost and at retail, with potential profit

### Cashflow (cash in / cash out)
- Every sale posts to the ledger automatically, split by tender
- Manual cash in / cash out with preset categories (stock purchase, salary, rent, utilities,
  owner draw…), free-text notes and a big amount keypad
- **`affectsCashDrawer` flag** — a QRIS sale is income but never touches the till, so
  cash-on-hand stays honest. This is the single detail that makes a drawer count reconcile
- Cash-on-hand, money in / money out / net, expense breakdown by category
- Money-in vs money-out chart per day
- Auto-posted rows are read-only; manual rows are editable and deletable

### Temporary discounts (promos)
Time-boxed discounts that apply themselves at checkout. A promo must clear three
independent gates:
1. a **calendar window** (start date → end date),
2. an optional **weekday set** (e.g. Mon–Fri only),
3. an optional **time-of-day window** — this is the happy-hour part, and it wraps past
   midnight correctly (22:00 → 02:00).

Scope is store-wide, one category, or one product. Percent or fixed amount, with a minimum
quantity and an optional discount cap. When several promos qualify, the largest discount
wins, ties broken toward the most specific scope. The POS re-evaluates once a minute, so a
happy hour starts and stops on its own without leaving the screen.

### Statistics
- **Best sellers and slowest movers side by side**, ranked by quantity, revenue or profit
- Both tables come from one query and one sort, so a product can never appear in both
- **Not sold this period** — active products with zero sales, i.e. capital sitting on a shelf
- Sales by hour (busiest hour), sales by category
- Per-product margin

### Reports
Period presets (today / yesterday / 7 days / month / year / all) applied consistently
everywhere. Net sales with period-over-period deltas, gross profit and margin, order count,
average order value, sales chart, payment-method mix, and a cashflow summary.

### Shifts
Open a shift with a cash float, close it by counting the drawer. Expected cash is **derived
from the ledger**, not from a running counter, so it stays correct even if the app was
killed mid-shift. A non-zero variance is posted as its own ledger row, so the next shift
starts from reality.

### Also
- Transaction history with search, day grouping and daily totals
- Void with reason (stock restored, cash reversed, revenue excluded)
- Products, categories with colour coding, archive instead of delete
- Store profile, tax (inclusive or exclusive), service charge, total rounding, receipt
  header/footer, invoice prefix
- Full dark mode
- **English and Indonesian** (`values/` and `values-in/`, 366 strings each, in parity)

---

## Getting started

**No Android Studio required.** This project builds from the command line.

### Prerequisites

| Piece | Version |
|---|---|
| JDK | 17 (e.g. [Microsoft OpenJDK 17](https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip)) |
| Gradle | [8.11.1](https://services.gradle.org/distributions/gradle-8.11.1-bin.zip) |
| Android SDK | API 35, build-tools 35.0.0, platform-tools |

Unzip each wherever you like — nothing needs to be installed system-wide.

### Configure your paths

Two files hold machine-specific locations. Both are gitignored, so your paths never
reach the repository.

```bat
copy build.env.example.bat build.env.bat
```

Edit `build.env.bat` and point the three variables at your own installs:

```bat
set "JAVA_HOME=C:\path\to\jdk-17"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "GRADLE_BIN=C:\path\to\gradle-8.11.1\bin\gradle.bat"
```

You can skip this entirely if `JAVA_HOME` and `ANDROID_HOME` are already exported and
`gradle` is on your `PATH` — `build.bat` falls back to those.

Then create `local.properties` in the project root so Gradle can find the SDK:

```properties
sdk.dir=C:\\path\\to\\Android\\Sdk
```

Your system-wide `java` can be any version — the build scripts set their own `JAVA_HOME`
and never touch it.

### Build

```bat
build.bat              :: debug APK
build.bat release      :: release APK (unsigned)
build.bat install      :: build + install on a connected device
build.bat clean        :: wipe build output
build.bat lint         :: anything else is passed through to Gradle
```

The APK lands at `app\build\outputs\apk\debug\app-debug.apk`.

The first build downloads roughly 1 GB of dependencies (Compose, Room, ML Kit, CameraX) and
takes several minutes. Later builds are incremental and take seconds.

`gradlew` / `gradle-wrapper.jar` are deliberately **not** generated: the wrapper would
re-download its own copy of Gradle on first use. `build.bat` calls the installed Gradle
directly instead. If you want the standard wrapper anyway, run `gradle wrapper` once from
the project root.

**Verified build** — Gradle 8.11.1 / JDK 17 / AGP 8.7.3, `assembleDebug`
**BUILD SUCCESSFUL, 0 errors, 0 warnings**. Output: 45 MB debug APK,
`com.rzk.kasirpro.debug`, minSdk 26 / targetSdk 35. The APK is large because the ML Kit
barcode model is bundled, so scanning works offline from the very first launch.

### Put it on a phone

**Over USB** — enable *Developer options → USB debugging* on the phone, plug it in, then:
```bat
build.bat install
```
Or manually: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
(`adb.exe` lives in `%LOCALAPPDATA%\Android\Sdk\platform-tools`.)

**Without a cable** — copy `app-debug.apk` to the phone (USB storage, WhatsApp to yourself,
Google Drive) and tap it. Android will ask you to allow installing from that app; accept and
it installs.

### Getting the Android SDK without Android Studio
Download the [Android command-line tools](https://developer.android.com/studio#command-line-tools-only),
unzip to `<sdk>\cmdline-tools\latest`, then:
```bat
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```
Point `local.properties` and `ANDROID_HOME` at `<sdk>` and you are done.

### First launch
The app seeds itself with 5 categories and 17 realistic products (one out of stock, two low
on stock, so the alerts have something to show). Wipe them any time from **Settings → Clear
all business data**, or bring them back with **Reload sample catalogue**.

---

## Architecture

Single module, MVVM, unidirectional data flow.

```
app/src/main/java/com/rzk/kasirpro/
├── KasirApplication.kt          Creates the AppContainer
├── MainActivity.kt              Edge-to-edge Compose host
├── core/                        Formatters, TimePeriod, receipt rendering, printing
├── domain/                      Cart maths (CartCalculator) and PromoEngine — pure Kotlin
├── data/
│   ├── model/                   Enums + query projections
│   ├── local/                   Room database, 10 entities, 8 DAOs, converters, seeder
│   └── repository/              7 repositories — the only place that writes
├── di/                          AppContainer + AppViewModelProvider
└── ui/
    ├── theme/                   Colour scheme, semantic colours, type scale, shapes
    ├── components/              Cards, charts, inputs, keypad, chips, empty states
    ├── navigation/              Routes + bottom-nav destinations
    └── screens/                 16 screens, each with its own ViewModel
```

**Dependency injection is hand-rolled** (`AppContainer`). The graph is small and entirely
singleton-shaped, so Hilt would add an annotation processor and a layer of indirection for
no benefit. Everything is lazy — the database isn't opened until a screen asks for it.

**Charts are hand-drawn on Compose `Canvas`.** No charting dependency, no version risk,
and full control over the marks.

### Two decisions worth knowing about

**Money is `Long`, in whole rupiah.** Integers keep totals exact across discount → promo →
order discount → service charge → tax → rounding. If you localise to a currency with cents,
switch to minor units (store cents) rather than to `Double`.

**Checkout is one database transaction.** The sale, its lines, its tenders, the stock
deductions, the invoice number and the cash-ledger postings all land or none do. Stock is
re-read *inside* that transaction, so two cashiers can't both sell the last unit. A partial
checkout is the one bug that makes a POS untrustworthy.

### The cash model
```
cash on hand = Σ(IN where affectsCashDrawer) − Σ(OUT where affectsCashDrawer)
```
- Cash sale → `IN`, affects drawer, **netted of change given**
- QRIS / card / transfer sale → `IN`, does *not* affect drawer
- Manual entry → user chooses (default: affects drawer)
- Shift float → `IN`, affects drawer
- Void → reversing `OUT` (never a delete — the history stays)

### Chart colours
The 8-step categorical palette in `ui/theme/Color.kt` was validated against **both** the
light surface (`#F5FBF7`) and the dark surface (`#0E1513`): lightness band, chroma floor,
colour-vision-deficiency separation, normal-vision separation, and 3:1 contrast all pass in
both modes. Series colour is assigned by **entity identity** (e.g. payment-method enum
position), never by rank, so filtering never repaints the survivors. Charts also carry
legends and direct labels, so identity is never colour alone. Re-validate before editing
those values.

---

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3 |
| Database | Room 2.6.1 + KSP |
| Navigation | Navigation Compose 2.8.5 |
| Barcode | ML Kit Barcode Scanning + CameraX 1.4.1 |
| Build | AGP 8.7.3, Gradle 8.11.1, JDK 17 |
| min / target SDK | 26 / 35 |

Versions live in `gradle/libs.versions.toml`. They are a deliberately conservative,
known-good set — bump them there in one place once you have a build running and can verify.

---

## Extending it

Natural next steps, in rough order of value:

- **Customers & loyalty** — a `CustomerEntity` plus a FK on `sales`; the sale already
  carries a customer name and phone
- **CSV / Excel export** — `SaleDao` projections are already report-shaped; add a writer and
  share via the existing `FileProvider`
- **ESC/POS thermal printing** — `ReceiptFormatter.buildText()` already emits 32-column
  monospaced output ready for a byte stream
- **Multi-device sync** — every entity has an `id` and a timestamp; add `updatedAt` +
  a `syncState` column and a remote data source behind the existing repositories
- **Purchase orders & suppliers** — `StockMovementEntity.PURCHASE_IN` is the hook
- **User accounts and permissions** — shifts already record a cashier name

### Migrations
`exportSchema = true` writes a JSON snapshot to `app/schemas/` on every build. When you
change an entity, bump `version` in `KasirDatabase` and add a `Migration` — don't reach for
`fallbackToDestructiveMigration()` on an app that holds a shop's books.
