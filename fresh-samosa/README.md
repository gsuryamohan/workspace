# Fresh Samosa

A single-page web application themed around India’s favourite evening tea-time snack. Built with **React** and **CSS3**, with a responsive layout for mobile and desktop.

## Features

- **Single-page layout**: Hero, About, Snacks, and Contact sections with smooth scroll navigation
- **Food-themed design**: Warm palette (golden, cream, brown, terracotta), appetizing typography (Playfair Display + Source Sans 3)
- **Responsive (RWD)**:
  - Mobile-first CSS
  - Fluid typography with `clamp()`
  - Flexible grid for snack cards (1 → 2 → 3 columns)
  - Sticky header and touch-friendly tap targets

## Run locally

```bash
cd fresh-samosa
npm install   # if not already done
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Resize the window or use DevTools device mode to check mobile and desktop layouts.

## Build for production

```bash
npm run build
```

Output is in the `dist/` folder, ready to deploy to any static host.
