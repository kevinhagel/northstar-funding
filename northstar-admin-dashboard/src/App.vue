<template>
  <div id="app">
    <header class="app-header">
      <div class="container">
        <h1 class="logo">
          <i class="pi pi-star-fill"></i>
          NorthStar Funding Discovery
        </h1>
        <nav>
          <router-link to="/" class="nav-link">Review Queue</router-link>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <footer class="app-footer">
      <div class="container">
        <p>&copy; 2025 NorthStar Funding Discovery. Admin Dashboard.</p>
      </div>
    </footer>
  </div>
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #f5f7fa;
  color: #333;
  line-height: 1.6;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 2rem;
}

.app-header {
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 1rem 0;
}

.app-header .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 1.5rem;
  font-weight: 700;
  color: #3b82f6;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.logo i {
  color: #f59e0b;
}

nav {
  display: flex;
  gap: 1.5rem;
}

.nav-link {
  text-decoration: none;
  color: #666;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: #eff6ff;
  color: #3b82f6;
}

.app-main {
  flex: 1;
  padding: 2rem 0;
}

.app-footer {
  background: #1f2937;
  color: white;
  padding: 1.5rem 0;
  text-align: center;
}

.app-footer p {
  color: #9ca3af;
}
</style>
