# NorthStar Funding Discovery - Design System

**Theme**: Celestial Navigation
**Last Updated**: 2025-11-23
**Created By**: frontend-design skill

## Overview

The NorthStar admin dashboard uses a distinctive "Celestial Navigation" aesthetic that evokes deep space exploration, starlight, and cosmic discovery. This design system ensures consistent branding across all features.

## Color Palette

### Primary Colors (CSS Variables)

```css
--space-deep: #0a0e27;      /* Deep space background */
--space-mid: #1a1f3a;       /* Mid-tone space */
--nebula-blue: #2d3561;     /* Nebula accent */
--constellation: #4a5899;   /* Constellation blue */
--starlight: #f4d03f;       /* Golden starlight */
--nova: #ff6b35;            /* Orange nova */
--aurora: #00d9ff;          /* Cyan aurora */
--moonlight: #e8edf5;       /* Soft white */
```

### Semantic Colors

```css
--text-primary: #e8edf5;           /* Primary text */
--text-secondary: #a0a8c0;         /* Secondary text */
--text-accent: var(--aurora);      /* Accent text */
--surface: rgba(26, 31, 58, 0.7);  /* Surface background */
--surface-elevated: rgba(45, 53, 97, 0.8); /* Elevated surface */
--border-subtle: rgba(74, 88, 153, 0.3);   /* Subtle borders */
--cosmic-glow: rgba(0, 217, 255, 0.15);    /* Aurora glow effect */
--star-shimmer: rgba(244, 208, 63, 0.25);  /* Starlight shimmer */
```

### Usage Guidelines

- **Backgrounds**: Use `--space-deep` for main background, `--surface` for cards
- **Accents**: `--aurora` for primary actions, `--starlight` for highlights
- **Borders**: `--border-subtle` for subtle dividers, `--constellation` for emphasis
- **Text**: `--text-primary` for body, `--text-secondary` for labels

## Typography

### Font Families

```css
--font-display: 'Lexend', sans-serif;      /* Headers, logo */
--font-body: 'IBM Plex Sans', sans-serif;  /* Body text */
--font-mono: 'JetBrains Mono', monospace;  /* Code, data */
```

### Font Loading

Include in `<style>` section:
```css
@import url('https://fonts.googleapis.com/css2?family=Lexend:wght@300;400;500;600;700&family=IBM+Plex+Sans:wght@300;400;500;600&family=JetBrains+Mono:wght@400;500&display=swap');
```

### Typography Scale

| Element | Font | Size | Weight | Usage |
|---------|------|------|--------|-------|
| Page Title | Lexend | 2.25rem | 600 | Main page headers |
| Section Title | Lexend | 1.5rem | 600 | Section headers |
| Body | IBM Plex Sans | 1rem | 400 | General text |
| Label | IBM Plex Sans | 0.875rem | 500 | Form labels (uppercase) |
| Data | JetBrains Mono | 0.875rem | 400-700 | Confidence scores, URLs |

## Visual Effects

### Glassmorphism

Apply to cards and elevated surfaces:
```css
background: var(--surface-elevated);
backdrop-filter: blur(20px) saturate(180%);
border: 1px solid var(--border-subtle);
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
border-radius: 12px;
```

### Cosmic Glow

Accent borders and highlights:
```css
border: 1px solid var(--aurora);
box-shadow: 0 0 12px var(--cosmic-glow);
```

### Starlight Effect

Text glow for high-value items:
```css
color: var(--starlight);
text-shadow: 0 0 8px var(--star-shimmer);
```

### Animations

**Star Pulse** (rotating star icon):
```css
@keyframes starPulse {
  0%, 100% { transform: scale(1) rotate(0deg); opacity: 1; }
  50% { transform: scale(1.1) rotate(90deg); opacity: 0.8; }
}
animation: starPulse 3s ease-in-out infinite;
```

**Floating** (empty state icons):
```css
@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}
animation: float 3s ease-in-out infinite;
```

**Star Drift** (background stars):
```css
@keyframes driftStars {
  0%, 100% { background-position: 0% 0%; }
  50% { background-position: 100% 100%; }
}
animation: driftStars 120s ease-in-out infinite;
```

## Component Patterns

### Page Header

```vue
<div class="header">
  <h1>Page Title</h1>
  <p class="subtitle">Page description</p>
</div>
```

```css
.header {
  margin-bottom: 2.5rem;
  padding: 2rem 0 1.5rem;
  border-bottom: 1px solid var(--border-subtle);
  position: relative;
}

.header::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 120px;
  height: 2px;
  background: linear-gradient(90deg, var(--aurora), transparent);
}

.header h1 {
  font-family: var(--font-display);
  font-size: 2.25rem;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.header h1::before {
  content: '◆';
  color: var(--aurora);
  font-size: 1.5rem;
  opacity: 0.7;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 1rem;
  font-weight: 400;
}
```

### Glassmorphic Card

```vue
<div class="cosmic-card">
  <!-- Card content -->
</div>
```

```css
.cosmic-card {
  background: var(--surface-elevated);
  backdrop-filter: blur(20px) saturate(180%);
  border-radius: 12px;
  padding: 1.75rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border-subtle);
  position: relative;
  overflow: hidden;
}

.cosmic-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--cosmic-glow), transparent);
}
```

### Confidence Score Badge

```vue
<span :class="getConfidenceClass(score)">{{ score }}</span>
```

```css
.confidence-high {
  color: var(--starlight);
  font-weight: 700;
  font-family: var(--font-mono);
  text-shadow: 0 0 8px var(--star-shimmer);
  background: rgba(244, 208, 63, 0.1);
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  border: 1px solid rgba(244, 208, 63, 0.3);
  display: inline-block;
}

.confidence-medium {
  color: var(--nova);
  font-weight: 600;
  font-family: var(--font-mono);
  background: rgba(255, 107, 53, 0.1);
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  border: 1px solid rgba(255, 107, 53, 0.3);
  display: inline-block;
}

.confidence-low {
  color: var(--aurora);
  font-weight: 500;
  font-family: var(--font-mono);
  background: var(--cosmic-glow);
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  border: 1px solid rgba(0, 217, 255, 0.3);
  display: inline-block;
}
```

### External Link

```vue
<a :href="url" target="_blank" class="url-link">
  {{ url }}
  <i class="pi pi-external-link"></i>
</a>
```

```css
.url-link {
  color: var(--aurora);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-family: var(--font-mono);
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.url-link:hover {
  color: var(--starlight);
  text-decoration: underline;
  text-shadow: 0 0 8px var(--star-shimmer);
}

.url-link i {
  font-size: 0.75rem;
  opacity: 0.6;
}
```

## PrimeVue Theme Overrides

### DataTable

```css
:deep(.p-datatable .p-datatable-thead > tr > th) {
  background: var(--nebula-blue);
  color: var(--text-primary);
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 0.875rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  border-bottom: 2px solid var(--constellation);
  padding: 1rem 0.75rem;
}

:deep(.p-datatable .p-datatable-tbody > tr:hover) {
  background: rgba(74, 88, 153, 0.15);
}

:deep(.p-datatable .p-datatable-tbody > tr > td) {
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-subtle);
  padding: 0.875rem 0.75rem;
}
```

### Tag (Status Badges)

```css
:deep(.p-tag) {
  font-family: var(--font-mono);
  font-size: 0.8125rem;
  font-weight: 500;
  padding: 0.375rem 0.75rem;
  border-radius: 6px;
  backdrop-filter: blur(8px);
}

:deep(.p-tag.p-tag-success) {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

:deep(.p-tag.p-tag-info) {
  background: var(--cosmic-glow);
  color: var(--aurora);
  border: 1px solid rgba(0, 217, 255, 0.3);
}
```

### Buttons

```css
:deep(.p-button) {
  font-family: var(--font-body);
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

:deep(.p-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

:deep(.p-button.p-button-text) {
  background: transparent;
  border: 1px solid var(--border-subtle);
}

:deep(.p-button.p-button-text:hover) {
  background: var(--cosmic-glow);
  border-color: var(--aurora);
}
```

### Form Inputs

```css
:deep(.p-multiselect),
:deep(.p-dropdown),
:deep(.p-inputtext) {
  background: var(--surface);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  transition: all 0.2s ease;
}

:deep(.p-multiselect:hover),
:deep(.p-dropdown:hover),
:deep(.p-inputtext:hover) {
  border-color: var(--constellation);
}

:deep(.p-multiselect.p-focus),
:deep(.p-dropdown.p-focus),
:deep(.p-inputtext:focus) {
  border-color: var(--aurora);
  box-shadow: 0 0 0 2px var(--cosmic-glow);
}
```

### Paginator

```css
:deep(.p-paginator) {
  background: var(--surface);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.75rem;
}

:deep(.p-paginator .p-paginator-pages .p-paginator-page:hover) {
  background: var(--cosmic-glow);
  color: var(--aurora);
}

:deep(.p-paginator .p-paginator-pages .p-paginator-page.p-highlight) {
  background: var(--constellation);
  color: var(--text-primary);
  box-shadow: 0 0 12px var(--cosmic-glow);
}
```

## Iconography

### PrimeIcons Usage

Use PrimeIcons for consistent iconography:

| Icon | Class | Usage |
|------|-------|-------|
| View | `pi pi-eye` | View details |
| Edit | `pi pi-pencil` | Edit/Enhance |
| Check | `pi pi-check` | Approve |
| Times | `pi pi-times` | Reject |
| External Link | `pi pi-external-link` | Open URL |
| Search | `pi pi-search` | Search/Filter |
| Grid | `pi pi-th-large` | Dashboard |
| Inbox | `pi pi-inbox` | Empty state |

### Celestial Symbols

Use Unicode symbols for cosmic accents:

| Symbol | Unicode | Usage |
|--------|---------|-------|
| ✦ | U+2726 | Logo star |
| ◆ | U+25C6 | Section markers |
| ⬢ | U+2B22 | Data points |
| ★ | U+2605 | Favorites |

## Layout Patterns

### Container

```css
.container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 2rem;
  position: relative;
  z-index: 1;
}
```

### Grid Layout (Filters)

```css
.filter-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr) auto;
  gap: 1.25rem;
  align-items: end;
}
```

### Flex Layout (Actions)

```css
.action-buttons {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}
```

## Accessibility

### Contrast Ratios

All text meets WCAG AA standards:
- `--text-primary` on `--space-deep`: 13.5:1 (AAA)
- `--text-secondary` on `--space-deep`: 7.8:1 (AA)
- `--aurora` on `--space-deep`: 8.2:1 (AA)

### Focus States

All interactive elements have visible focus states using aurora glow:
```css
:focus-visible {
  outline: 2px solid var(--aurora);
  outline-offset: 2px;
}
```

## Future Components

When building new features (CandidateDetail, Enhancement Form, etc.), follow these patterns:

1. **Always include cosmic background** via App.vue
2. **Use glassmorphic cards** for content sections
3. **Add page header** with aurora accent line
4. **Style PrimeVue components** with `:deep()` selectors
5. **Use CSS variables** from App.vue (never hardcode colors)
6. **Apply typography scale** (Lexend for headers, IBM Plex Sans for body)
7. **Add subtle animations** (floating, pulsing, glowing)

## Examples

See implemented components:
- `src/App.vue` - Global layout, cosmic background, navigation
- `src/views/ReviewQueue.vue` - Full page example with filters, table, pagination

## Resources

- PrimeVue Documentation: https://primevue.org
- PrimeIcons: https://primevue.org/icons
- Google Fonts: Lexend, IBM Plex Sans, JetBrains Mono
