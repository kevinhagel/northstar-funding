<template>
  <div class="candidate-card" :class="statusClass">
    <div class="card-header">
      <div class="title-section">
        <h3>{{ candidate.title || 'Untitled' }}</h3>
        <a :href="candidate.url" target="_blank" class="url">
          {{ candidate.url }}
          <span class="external-icon">↗</span>
        </a>
      </div>
      <div class="meta-section">
        <span class="badge status-badge">{{ candidate.status }}</span>
        <span class="badge engine-badge">{{ candidate.searchEngine }}</span>
      </div>
    </div>

    <div class="card-body">
      <div class="confidence-bar">
        <label>Confidence Score:</label>
        <div class="bar-container">
          <div
            class="bar-fill"
            :style="{ width: confidencePercent + '%' }"
            :class="confidenceClass"
          ></div>
          <span class="bar-label">{{ candidate.confidenceScore }}</span>
        </div>
      </div>

      <div class="metadata">
        <div class="meta-item">
          <strong>Discovered:</strong>
          {{ formatDate(candidate.createdAt) }}
        </div>
        <div class="meta-item">
          <strong>ID:</strong>
          <code>{{ candidate.id }}</code>
        </div>
      </div>
    </div>

    <div class="card-actions" v-if="showActions">
      <button
        @click="$emit('approve', candidate.id)"
        class="btn-approve"
        :disabled="candidate.status === 'APPROVED'"
      >
        ✓ Approve
      </button>
      <button
        @click="$emit('reject', candidate.id)"
        class="btn-reject"
        :disabled="candidate.status === 'REJECTED'"
      >
        ✗ Reject
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CandidateCard',
  props: {
    candidate: {
      type: Object,
      required: true
    }
  },
  computed: {
    confidencePercent() {
      return parseFloat(this.candidate.confidenceScore) * 100
    },
    confidenceClass() {
      const score = parseFloat(this.candidate.confidenceScore)
      if (score >= 0.8) return 'high'
      if (score >= 0.6) return 'medium'
      return 'low'
    },
    statusClass() {
      return `status-${this.candidate.status.toLowerCase().replace(/_/g, '-')}`
    },
    showActions() {
      return !['APPROVED', 'REJECTED'].includes(this.candidate.status)
    }
  },
  methods: {
    formatDate(dateString) {
      if (!dateString) return 'N/A'
      const date = new Date(dateString)
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.candidate-card {
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
  background: white;
  transition: all 0.2s;
}

.candidate-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-color: #667eea;
}

.status-approved {
  border-left: 4px solid #27ae60;
}

.status-rejected {
  border-left: 4px solid #e74c3c;
  opacity: 0.7;
}

.status-pending-crawl {
  border-left: 4px solid #3498db;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  margin-bottom: 1rem;
  gap: 1rem;
}

.title-section {
  flex: 1;
}

.title-section h3 {
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
  color: #333;
}

.url {
  color: #667eea;
  text-decoration: none;
  font-size: 0.9rem;
  word-break: break-all;
}

.url:hover {
  text-decoration: underline;
}

.external-icon {
  margin-left: 0.25rem;
  font-size: 0.8rem;
}

.meta-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-end;
}

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-badge {
  background: #667eea;
  color: white;
}

.engine-badge {
  background: #f0f0f0;
  color: #666;
}

.card-body {
  margin-bottom: 1rem;
}

.confidence-bar {
  margin-bottom: 1rem;
}

.confidence-bar label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #555;
  font-size: 0.9rem;
}

.bar-container {
  position: relative;
  height: 28px;
  background: #f0f0f0;
  border-radius: 14px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  transition: width 0.3s;
  border-radius: 14px;
}

.bar-fill.high {
  background: linear-gradient(90deg, #27ae60, #2ecc71);
}

.bar-fill.medium {
  background: linear-gradient(90deg, #f39c12, #f1c40f);
}

.bar-fill.low {
  background: linear-gradient(90deg, #e67e22, #e74c3c);
}

.bar-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-weight: 600;
  font-size: 0.85rem;
  color: #333;
}

.metadata {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 4px;
}

.meta-item {
  font-size: 0.85rem;
  color: #666;
}

.meta-item strong {
  color: #333;
  display: block;
  margin-bottom: 0.25rem;
}

.meta-item code {
  font-size: 0.75rem;
  background: white;
  padding: 0.125rem 0.375rem;
  border-radius: 3px;
  color: #667eea;
}

.card-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f0f0f0;
}

.btn-approve, .btn-reject {
  flex: 1;
  padding: 0.75rem;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-approve {
  background: #27ae60;
  color: white;
}

.btn-approve:hover:not(:disabled) {
  background: #229954;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(39, 174, 96, 0.3);
}

.btn-reject {
  background: #e74c3c;
  color: white;
}

.btn-reject:hover:not(:disabled) {
  background: #c0392b;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(231, 76, 60, 0.3);
}

.btn-approve:disabled,
.btn-reject:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}
</style>
