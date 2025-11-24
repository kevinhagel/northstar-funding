<template>
  <div class="placeholder-page">
    <Card>
      <template #title>Candidate Enhancement</template>
      <template #subtitle>Coming in Feature 015</template>
      <template #content>
        <p>This page will allow administrators to enhance candidate information with additional details.</p>
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
