<template>
  <div id="app">
    <!-- Cosmic background with subtle stars -->
    <div class="cosmic-background"></div>

    <header class="app-header">
      <div class="container">
        <h1 class="logo">
          <span class="star-icon">✦</span>
          <span class="logo-text">
            <span class="logo-north">North</span><span class="logo-star">Star</span>
            <span class="logo-subtitle">Funding Discovery</span>
          </span>
        </h1>
        <nav>
          <router-link to="/" class="nav-link">
            <i class="pi pi-th-large"></i>
            Review Queue
          </router-link>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <footer class="app-footer">
      <div class="container">
        <div class="footer-content">
          <p class="footer-text">NorthStar Funding Discovery © 2025</p>
          <p class="footer-meta">Navigating the funding universe</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Lexend:wght@300;400;500;600;700&family=IBM+Plex+Sans:wght@300;400;500;600&family=JetBrains+Mono:wght@400;500&display=swap');

:root {
  /* Celestial Navigation Color Palette */
  --space-deep: #0a0e27;
  --space-mid: #1a1f3a;
  --nebula-blue: #2d3561;
  --constellation: #4a5899;
  --starlight: #f4d03f;
  --nova: #ff6b35;
  --aurora: #00d9ff;
  --moonlight: #e8edf5;
  --cosmic-glow: rgba(0, 217, 255, 0.15);
  --star-shimmer: rgba(244, 208, 63, 0.25);

  /* Semantic colors */
  --text-primary: #e8edf5;
  --text-secondary: #a0a8c0;
  --text-accent: var(--aurora);
  --surface: rgba(26, 31, 58, 0.7);
  --surface-elevated: rgba(45, 53, 97, 0.8);
  --border-subtle: rgba(74, 88, 153, 0.3);

  /* Typography */
  --font-display: 'Lexend', sans-serif;
  --font-body: 'IBM Plex Sans', sans-serif;
  --font-mono: 'JetBrains Mono', monospace;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: var(--font-body);
  background: var(--space-deep);
  color: var(--text-primary);
  line-height: 1.6;
  font-weight: 400;
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* Cosmic Background with Animated Stars */
.cosmic-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  background:
    radial-gradient(ellipse at 20% 30%, rgba(74, 88, 153, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 70%, rgba(0, 217, 255, 0.1) 0%, transparent 50%),
    var(--space-deep);

  /* Subtle star field */
  background-image:
    radial-gradient(1px 1px at 20% 30%, rgba(244, 208, 63, 0.4), transparent),
    radial-gradient(1px 1px at 60% 70%, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(2px 2px at 50% 50%, rgba(0, 217, 255, 0.3), transparent),
    radial-gradient(1px 1px at 80% 10%, rgba(244, 208, 63, 0.5), transparent),
    radial-gradient(1px 1px at 90% 60%, rgba(255, 255, 255, 0.2), transparent);
  background-size: 200% 200%, 200% 200%, 300% 300%, 250% 250%, 280% 280%;
  background-position: 0% 0%;
  animation: driftStars 120s ease-in-out infinite;
}

@keyframes driftStars {
  0%, 100% { background-position: 0% 0%, 20% 30%, 50% 50%, 80% 10%, 90% 60%; }
  50% { background-position: 100% 100%, 80% 70%, 30% 70%, 20% 90%, 10% 40%; }
}

.container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 2rem;
  position: relative;
  z-index: 1;
}

/* Header with glassmorphism */
.app-header {
  background: var(--surface);
  backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--border-subtle);
  padding: 1.25rem 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.app-header .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Logo with celestial styling */
.logo {
  font-family: var(--font-display);
  display: flex;
  align-items: center;
  gap: 0.875rem;
  text-decoration: none;
  position: relative;
}

.star-icon {
  font-size: 2rem;
  color: var(--starlight);
  filter: drop-shadow(0 0 8px var(--star-shimmer));
  animation: starPulse 3s ease-in-out infinite;
  display: block;
  line-height: 1;
}

@keyframes starPulse {
  0%, 100% {
    transform: scale(1) rotate(0deg);
    opacity: 1;
  }
  50% {
    transform: scale(1.1) rotate(90deg);
    opacity: 0.8;
  }
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.logo-north,
.logo-star {
  font-size: 1.5rem;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1;
}

.logo-north {
  color: var(--text-primary);
  display: inline;
}

.logo-star {
  color: var(--aurora);
  display: inline;
  text-shadow: 0 0 20px var(--cosmic-glow);
}

.logo-subtitle {
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--text-secondary);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-top: -0.125rem;
}

/* Navigation */
nav {
  display: flex;
  gap: 0.5rem;
}

.nav-link {
  font-family: var(--font-body);
  text-decoration: none;
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 0.9375rem;
  padding: 0.625rem 1.25rem;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
  overflow: hidden;
}

.nav-link::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--cosmic-glow), transparent);
  transition: left 0.5s;
}

.nav-link:hover::before {
  left: 100%;
}

.nav-link:hover {
  color: var(--aurora);
  border-color: var(--border-subtle);
  background: rgba(0, 217, 255, 0.05);
  transform: translateY(-1px);
}

.nav-link.router-link-active {
  color: var(--aurora);
  background: var(--surface-elevated);
  border-color: var(--constellation);
  box-shadow: 0 0 20px var(--cosmic-glow);
}

.nav-link i {
  font-size: 1rem;
}

/* Main content area */
.app-main {
  flex: 1;
  padding: 2.5rem 0;
  position: relative;
  z-index: 1;
}

/* Footer with constellation theme */
.app-footer {
  background: var(--surface);
  backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid var(--border-subtle);
  padding: 2rem 0;
  position: relative;
  z-index: 1;
}

.footer-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.footer-text {
  font-family: var(--font-display);
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 500;
}

.footer-meta {
  font-family: var(--font-body);
  color: var(--text-secondary);
  font-size: 0.75rem;
  opacity: 0.6;
  letter-spacing: 0.05em;
  font-style: italic;
}

/* Scrollbar styling */
::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: var(--space-mid);
}

::-webkit-scrollbar-thumb {
  background: var(--constellation);
  border-radius: 5px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--aurora);
}
</style>
