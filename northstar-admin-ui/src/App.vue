<template>
  <div id="app">
    <header>
      <h1>NorthStar Funding Discovery - Admin Dashboard</h1>
    </header>
    <main>
      <CandidateReviewQueue />
    </main>
  </div>
</template>

<script>
import CandidateReviewQueue from './components/CandidateReviewQueue.vue'

export default {
  name: 'App',
  components: {
    CandidateReviewQueue
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  background-color: #f5f5f5;
  color: #333;
}

#app {
  min-height: 100vh;
}

header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

header h1 {
  font-size: 1.8rem;
  font-weight: 600;
}

main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}
</style>
