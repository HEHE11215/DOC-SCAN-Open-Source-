# Minimalist shadcn/ui Redesign, Crisp B&W Clear Scan & Next-Gen Scanner Suite

This plan overhauls the application into a refined, minimalistic **Light & Dark adaptive Zinc `shadcn/ui`** design system with a new geometric brand mark, upgrades **Clear Scan** to produce ultra-crisp black-and-white print quality, and adds four new document and scanner workflows.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> All core product and aesthetic decisions below reflect your confirmed preferences from the clarification step.

- **Confirmed Decision 1 — New Scanner & PDF Suite Features**:
  1. **ID Card & Passport 2-Side A4 Combiner**: Dedicated studio to capture or pick front + back ID/Passport images and compose them onto a print-ready A4 sheet with corner crop marks and optional B&W print enhancement.
  2. **QR & Barcode Scanner + Generator Studio**: Live/image barcode & QR reader paired with a real-time **QR & Barcode Generator** (URL, Wi-Fi, vCard, Text) that exports high-resolution PNGs or inserts the generated QR directly onto any PDF page.
  3. **Batch Page Numbering & Custom Watermarks**: Multi-document / multi-page batch processor with configurable watermark text, opacity, angle, and page number position (`Bottom Right`, `Bottom Center`, `Top Right`, `Page X of Y` format).
  4. **Smart Receipt & Table Data Extractor to CSV**: Structured parser that extracts line items, dates, totals, and tabular rows from scanned receipts or tables and exports them directly as a shareable `.csv` spreadsheet file.
- **Confirmed Decision 2 — Light & Dark Adaptive Zinc `shadcn/ui` Theme**:
  - Replaces heavy neon gradients and multi-colored glass orbs with crisp `1dp` Zinc borders (`#E4E4E7` in Light, `#27272A` in Dark), pure monochrome surfaces (`#FFFFFF` / `#09090B`), muted Zinc secondary containers (`#F4F4F5` / `#18181B`), and tight `8dp–12dp` border radii.
- **Confirmed Decision 3 — Minimal Monochrome Buttons & New Brand Logo**:
  - Converts all action buttons, chips, and feature cards (including the Camera shutter dock and Direct PDF Editor buttons) to clean monochrome `shadcn/ui` primary (`#18181B` on light / `#FAFAFA` on dark) and outline button styles with a subtle `1dp` Zinc border and crisp vector iconography.
  - Introduces a new minimalistic geometric scanner reticle & folded document brand logo across the adaptive launcher icon and in-app headers.
- **Confirmed Decision 4 — Clear Mode as Crisp Black & White Print Quality**:
  - Upgrades `ScanFilter.CLEAR` into a high-definition **Black & White Print** document engine using continuous Gaussian illumination normalization, adaptive Sauvola-inspired soft contrast mapping, pure `#FFFFFF` paper background whitening, deep `#0A0A0A` ink darkening, and a 3x3 unsharp-mask pass for laser-printer clarity.

---

## 1. Overview & Core Concept

- **What It Does**: Combines an ultra-clean, distraction-free `shadcn/ui` document workspace with laser-sharp black-and-white document scanning and a complete suite of productivity tools (Direct PDF editing, 2-side ID/Passport A4 layout, QR generation + embedding, batch page numbering/watermarking, and receipt/table-to-CSV export).
- **Target Audience / Persona**: Professionals, students, and everyday users who want a fast, modern, zero-clutter scanner and PDF studio whose output looks indistinguishable from a physical flatbed laser scanner.
- **Key Value**: Eliminates visual noise with a cohesive monochrome Zinc interface while turning uneven phone camera photos into razor-sharp black-and-white printable documents and structured data.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Home & Library (`shadcn/ui` Workspace)**:
   - Minimal top bar featuring the new geometric wireframe logo mark, clean monochrome search bar with `1dp` Zinc border, subtle pill category tabs, and a monochrome floating dock with a crisp high-contrast Camera trigger.
2. **Clear Scan (`B&W Print` Default)**:
   - Capturing or importing a document automatically applies the upgraded **Clear (B&W Print)** pipeline, rendering pure white paper and razor-sharp black text with zero block artifacts and readable bottom tool buttons (`Crop`, `Rotate`, `Sign`, `Stamp`, `Tune`).
3. **ID Card & Passport 2-Side A4 Combiner**:
   - Accessible from the PDF Tools & Scanners suite: users either scan live or pick Front & Back images, preview the centered A4 two-up layout (with toggleable B&W Print filter and cut guide marks), and save directly as a printable PDF.
4. **QR & Barcode Studio (Scan + Generate)**:
   - Users can scan QR/barcodes or switch to the **Generate QR** tab to enter a URL, text, Wi-Fi config, or contact card, preview the crisp monochrome QR code, share it as PNG, or stamp it onto an existing document.
5. **Batch Page Numbering & Custom Watermarks**:
   - Users select one or multiple documents, choose watermark text + opacity + diagonal/horizontal placement, configure page number formatting (`1 / N` or `Page 1`) and corner alignment, and apply across all pages in one tap.
6. **Smart Receipt & Table Extractor to CSV**:
   - Users pick any scanned document or receipt, run structured OCR table/receipt parsing to preview detected rows (`Item / Description`, `Date`, `Amount / Columns`), edit cells if needed, and export/share a standard `.csv` file.

### Visual Identity & Theme (`shadcn/ui` Zinc System)
- **Aesthetic Direction**: Utilitarian editorial minimalism inspired by **shadcn/ui** and Linear—crisp `1dp` structural hairlines, flat high-contrast surfaces, generous whitespace, and zero rainbow/neon clutter.
- **Color Palette & Mood**:
  - **Light Mode (Zinc Light)**:
    - `--background`: `#FFFFFF` (Pure White)
    - `--card` / `--surface`: `#FFFFFF` with `#E4E4E7` (`Zinc-200`) `1dp` border
    - `--muted` / `--secondary`: `#F4F4F5` (`Zinc-100`)
    - `--muted-foreground`: `#71717A` (`Zinc-500`)
    - `--primary`: `#18181B` (`Zinc-900`) with `--primary-foreground`: `#FAFAFA`
    - `--border`: `#E4E4E7` (`Zinc-200`)
  - **Dark Mode (Zinc Dark)**:
    - `--background`: `#09090B` (`Zinc-950`)
    - `--card` / `--surface`: `#09090B` / `#121215` with `#27272A` (`Zinc-800`) `1dp` border
    - `--muted` / `--secondary`: `#18181B` (`Zinc-900`)
    - `--muted-foreground`: `#A1A1AA` (`Zinc-400`)
    - `--primary`: `#FAFAFA` (`Zinc-50`) with `--primary-foreground`: `#18181B`
    - `--border`: `#27272A` (`Zinc-800`)
- **Typography & Hierarchy**: Clean geometric sans-serif hierarchy with tight letter-spacing on headings (`FontWeight.SemiBold` / `Bold`), high-legibility body copy, and tabular numerals for page counts, dates, and CSV tables.
- **Component Styling & Layout**:
  - `8.dp` to `12.dp` corner radii across cards, inputs, dialogs, and buttons.
  - All primary buttons use solid monochrome fill (`Zinc-900` in Light, `Zinc-50` in Dark); secondary/outline buttons use transparent/surface fill with a `1dp` `Zinc-200`/`Zinc-800` border.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Unified Monochrome `shadcn/ui` Component Primitives**
  - *Chosen Approach*: Refactor the shared theme primitives (`GlassScreenBackground`, `FrostedGlassCard`, `GlassActionButton`, `DynamicRgbActionButton`, `DynamicRgbGlowCard`, `DynamicRgbChip`) in the theme layer to render authentic `shadcn/ui` Zinc surfaces, `1dp` borders, and monochrome button states.
  - *Why*: Instantly transforms every screen, dialog, chip, and toolbar into a cohesive minimalistic `shadcn/ui` aesthetic without breaking existing screen layouts or test tags.
  - *Alternatives Considered*: Keeping RGB animations on primary buttons was rejected per your preference to switch all buttons to minimal monochrome `shadcn/ui` style.
- **Decision 2: Continuous Bilinear Illumination + High-Contrast B&W Print Curve for `ScanFilter.CLEAR`**
  - *Chosen Approach*: Convert pixel luminance relative to a 4-pass Gaussian-smoothed local illumination grid sampled via bilinear interpolation, then apply a steep S-curve threshold with anti-aliased gray transitions on character edges and a 3x3 unsharp mask.
  - *Why*: Produces pure `#FFFFFF` paper and rich `#0A0A0A` black print typography with smooth anti-aliased letterforms (no jagged 1-bit pixelation and no blocky shadows).
- **Decision 3: On-Device Deterministic QR Matrix + Structured Receipt/Table CSV Parser**
  - *Chosen Approach*: Pair ML Kit barcode scanning with a built-in QR/Code-128 bitmap renderer and a multi-column OCR spatial line grouper that clusters bounding boxes by vertical row coordinates (`Y`-overlap) and horizontal column tab stops (`X`-alignment) into CSV rows.
  - *Why*: Works 100% offline with instant response times and exports clean, standards-compliant `.csv` files directly via Android's share sheet.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   Minimalist Zinc shadcn/ui Theme Layer                    │
│  • Adaptive Light (#FFFFFF / #E4E4E7) & Dark (#09090B / #27272A) Tokens    │
│  • Monochrome Primary & 1dp Outline Buttons, Cards, Badges & Minimal Logo  │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        ▼                             ▼                             ▼
┌───────────────────┐     ┌───────────────────────┐     ┌────────────────────┐
│  Clear Scan B&W   │     │   PDF & Scanner Hub   │     │ Library & Direct   │
│   Print Engine    │     │  (4 New Studio Tools) │     │    PDF Studio      │
├───────────────────┤     ├───────────────────────┤     ├────────────────────┤
│ • 4-Pass Gaussian │     │ 1. ID/Passport 2-Side │     │ • Minimal Brand    │
│   Bg Estimation   │     │    A4 Sheet Composer  │     │   Header & Logo    │
│ • Bilinear Sample │     │ 2. QR & Barcode Scan  │     │ • Monochrome Dock  │
│ • Crisp B&W Print │     │    + QR PNG/PDF Gen   │     │ • Smooth Pinch &   │
│   S-Curve + Sharp │     │ 3. Batch Watermark &  │     │   Double-Tap Zoom  │
│ • Full-Text Tools │     │    Page Numbering     │     │ • Non-Truncated    │
│   (No "..." Crop) │     │ 4. Receipt/Table OCR  │     │   Action Buttons   │
│                   │     │    to CSV Exporter    │     │                    │
└─────────┬─────────┘     └───────────┬───────────┘     └──────────┬─────────┘
          │                           │                            │
          └───────────────────────────┼────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                 ViewModel & Local Persistence / Export Layer               │
│  • Room Database (ScannedDocumentEntity) + File Provider Share Helpers     │
│  • PDF Renderer / Canvas Page Stamping + CSV File Writer & Share Intent    │
└────────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping
- **Minimal Brand Mark & Adaptive Launcher Icon**:
  - Custom vector logo (`ShadcnBrandLogo`) rendered in the top bar and compiled into the Android adaptive icon (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`, and all `mipmap-*` PNG densities) featuring a geometric monochrome document frame and scanner corner reticles on a `#09090B` Zinc background.
- **Clear Scan (`ScanFilter.CLEAR`) B&W Print Pipeline**:
  - `applyDocumentClearScan` computes pixel luminance, normalizes against the bilinearly interpolated local background illumination, maps paper tones above `0.86` relative illumination to pure white `255`, deepens ink below `0.62` to rich black `0..25` with smooth anti-aliased edge ramp, and runs a 3x3 unsharp mask (`amount = 0.65f`) for crisp print legibility.
- **ID Card & Passport 2-Side A4 Studio**:
  - Interactive dialog/sheet in PDF Tools allowing users to pick or scan **Front Image** and **Back Image**, toggle **Apply Clear B&W Print**, toggle **Show Crop Marks**, preview the combined A4 canvas (`1240 x 1754` px), and save as a new PDF document in the library.
- **QR & Barcode Scanner + Generator**:
  - Adds a **QR Generator & Scanner Studio** dialog in PDF Tools with mode tabs (`Generate QR`, `Scan Camera`), content type presets (`URL`, `Text`, `Wi-Fi`, `Contact`), live bitmap preview, **Share QR PNG**, and **Insert into Document**.
- **Batch Page Numbering & Custom Watermarks**:
  - Upgrades the Watermark & Page Numbering workflow to support multi-document batch selection, watermark text, opacity slider (`10%–60%`), orientation (`Diagonal` / `Horizontal`), page number format (`Page X of Y`, `X / Y`, `#X`), and position (`Bottom Right`, `Bottom Center`, `Top Right`).
- **Smart Receipt & Table Data Extractor to CSV**:
  - Adds a **Receipt & Table to CSV** tool card in PDF Tools that runs ML Kit text recognition on the selected document's pages, clusters text blocks by row (`Y` proximity) and column (`X` alignment) plus receipt key-value pairs (`Merchant`, `Date`, `Total`, `Line Items`), displays an interactive table preview, and exports a `.csv` file via `ShareExportHelper`.
