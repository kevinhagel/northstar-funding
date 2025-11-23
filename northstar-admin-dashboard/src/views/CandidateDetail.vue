<template>
  <div class="placeholder-page">
    <Card>
      <template #title>Candidate Detail</template>
      <template #subtitle>Coming in Feature 014</template>
      <template #content>
        <p>This page will display detailed information about a funding source candidate.</p>
        <p><strong>Candidate ID:</strong> {{ route.params.id }}</p>
        <Button label="Back to Review Queue" icon="pi pi-arrow-left" @click="goBack" />
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'

const route = useRoute()
const router = useRouter()

function goBack() {
  router.push('/')
}
</script>

<style scoped>
.placeholder-page {
  max-width: 800px;
  margin: 2rem auto;
  padding: 2rem;
}

p {
  margin-bottom: 1rem;
}
</style>
