<template>
  <div class="review-queue">
    <div class="filters">
      <h2>Review Queue Filters</h2>

      <div class="filter-group">
        <label>Status:</label>
        <select v-model="filters.status" multiple>
          <option value="PENDING_CRAWL">Pending Crawl</option>
          <option value="CRAWLED">Crawled</option>
          <option value="ENHANCED">Enhanced</option>
          <option value="JUDGED">Judged</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      <div class="filter-group">
        <label>Min Confidence:</label>
        <input
          type="range"
          v-model.number="filters.minConfidence"
          min="0"
          max="1"
          step="0.05"
        >
        <span class="confidence-value">{{ filters.minConfidence.toFixed(2) }}</span>
      </div>

      <div class="filter-group">
        <label>Search Engine:</label>
        <select v-model="filters.searchEngine">
          <option value="">All</option>
          <option value="TAVILY">Tavily</option>
          <option value="SEARXNG">SearXNG</option>
          <option value="BRAVE">Brave</option>
          <option value="PERPLEXITY">Perplexity</option>
        </select>
      </div>

      <div class="filter-actions">
        <button @click="loadCandidates" class="btn-primary">Apply Filters</button>
        <button @click="resetFilters" class="btn-secondary">Reset</button>
      </div>
    </div>

    <div class="results">
      <div class="results-header">
        <h2>Candidates ({{ totalElements }} total)</h2>
        <div class="pagination-info">
          Page {{ currentPage + 1 }} of {{ totalPages || 1 }}
        </div>
      </div>

      <div v-if="loading" class="loading">Loading candidates...</div>

      <div v-else-if="error" class="error">
        Error: {{ error }}
      </div>

      <div v-else-if="candidates.length === 0" class="empty-state">
        No candidates found matching your filters.
      </div>

      <div v-else class="candidate-list">
        <CandidateCard
          v-for="candidate in candidates"
          :key="candidate.id"
          :candidate="candidate"
          @approve="approveCandidate"
          @reject="rejectCandidate"
        />
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button
          @click="prevPage"
          :disabled="currentPage === 0"
          class="btn-secondary"
        >
          Previous
        </button>
        <span class="page-info">Page {{ currentPage + 1 }} / {{ totalPages }}</span>
        <button
          @click="nextPage"
          :disabled="currentPage >= totalPages - 1"
          class="btn-secondary"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import CandidateCard from './CandidateCard.vue'

export default {
  name: 'CandidateReviewQueue',
  components: {
    CandidateCard
  },
  data() {
    return {
      candidates: [],
      loading: false,
      error: null,
      filters: {
        status: [],
        minConfidence: 0.6,
        searchEngine: ''
      },
      currentPage: 0,
      pageSize: 20,
      totalElements: 0,
      totalPages: 0
    }
  },
  mounted() {
    this.loadCandidates()
  },
  methods: {
    async loadCandidates() {
      this.loading = true
      this.error = null

      try {
        const params = {
          page: this.currentPage,
          size: this.pageSize
        }

        if (this.filters.status.length > 0) {
          params.status = this.filters.status
        }
        if (this.filters.minConfidence > 0) {
          params.minConfidence = this.filters.minConfidence
        }
        if (this.filters.searchEngine) {
          params.searchEngine = this.filters.searchEngine
        }

        console.log('[CandidateReviewQueue] Loading candidates with params:', params)
        const response = await axios.get('/api/candidates', { params })
        console.log('[CandidateReviewQueue] Received response:', response.data)

        this.candidates = response.data.content
        this.totalElements = response.data.totalElements
        this.totalPages = response.data.totalPages
        this.currentPage = response.data.currentPage
      } catch (err) {
        console.error('[CandidateReviewQueue] Error loading candidates:', err)
        this.error = err.response?.data?.message || err.message
      } finally {
        this.loading = false
      }
    },

    async approveCandidate(candidateId) {
      try {
        console.log('[CandidateReviewQueue] Approving candidate:', candidateId)
        await axios.put(`/api/candidates/${candidateId}/approve`)
        console.log('[CandidateReviewQueue] Candidate approved successfully')
        await this.loadCandidates() // Reload to show updated status
      } catch (err) {
        console.error('[CandidateReviewQueue] Error approving candidate:', err)
        alert('Failed to approve candidate: ' + (err.response?.data?.message || err.message))
      }
    },

    async rejectCandidate(candidateId) {
      try {
        console.log('[CandidateReviewQueue] Rejecting candidate:', candidateId)
        await axios.put(`/api/candidates/${candidateId}/reject`)
        console.log('[CandidateReviewQueue] Candidate rejected successfully')
        await this.loadCandidates() // Reload to show updated status
      } catch (err) {
        console.error('[CandidateReviewQueue] Error rejecting candidate:', err)
        alert('Failed to reject candidate: ' + (err.response?.data?.message || err.message))
      }
    },

    resetFilters() {
      this.filters = {
        status: [],
        minConfidence: 0.6,
        searchEngine: ''
      }
      this.currentPage = 0
      this.loadCandidates()
    },

    prevPage() {
      if (this.currentPage > 0) {
        this.currentPage--
        this.loadCandidates()
      }
    },

    nextPage() {
      if (this.currentPage < this.totalPages - 1) {
        this.currentPage++
        this.loadCandidates()
      }
    }
  }
}
</script>

<style scoped>
.review-queue {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 2rem;
  align-items: start;
}

.filters {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 2rem;
}

.filters h2 {
  font-size: 1.2rem;
  margin-bottom: 1.5rem;
  color: #667eea;
}

.filter-group {
  margin-bottom: 1.5rem;
}

.filter-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #555;
}

.filter-group select,
.filter-group input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.filter-group select[multiple] {
  height: 150px;
}

.confidence-value {
  display: inline-block;
  margin-left: 0.5rem;
  font-weight: 600;
  color: #667eea;
}

.filter-actions {
  display: flex;
  gap: 0.5rem;
}

.results {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #f0f0f0;
}

.results-header h2 {
  font-size: 1.2rem;
  color: #667eea;
}

.pagination-info {
  color: #666;
  font-size: 0.9rem;
}

.loading, .error, .empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
  font-size: 1.1rem;
}

.error {
  color: #e74c3c;
}

.candidate-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px solid #f0f0f0;
}

.page-info {
  font-weight: 600;
  color: #667eea;
}

.btn-primary, .btn-secondary {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  flex: 1;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
  flex: 1;
}

.btn-secondary:hover:not(:disabled) {
  background: #f8f9ff;
}

.btn-secondary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .review-queue {
    grid-template-columns: 1fr;
  }

  .filters {
    position: static;
  }
}
</style>
