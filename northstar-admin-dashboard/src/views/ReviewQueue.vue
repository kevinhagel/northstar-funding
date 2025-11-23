<template>
  <div class="review-queue">
    <div class="header">
      <h1>Funding Source Review Queue</h1>
      <p class="subtitle">Review and manage discovered funding opportunities</p>
    </div>

    <div class="filters-card">
      <div class="filter-row">
        <div class="filter-group">
          <label>Status</label>
          <MultiSelect
            v-model="localFilters.status"
            :options="statusOptions"
            placeholder="All Statuses"
            class="filter-input"
          />
        </div>

        <div class="filter-group">
          <label>Confidence</label>
          <Dropdown
            v-model="localFilters.minConfidence"
            :options="confidenceOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All"
            class="filter-input"
          />
        </div>

        <div class="filter-group">
          <label>Search Engine</label>
          <MultiSelect
            v-model="localFilters.searchEngine"
            :options="searchEngineOptions"
            placeholder="All Engines"
            class="filter-input"
          />
        </div>

        <div class="filter-actions">
          <Button label="Apply Filters" icon="pi pi-search" @click="applyFilters" />
          <Button label="Clear" icon="pi pi-times" @click="clearFilters" severity="secondary" />
        </div>
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="store.candidates"
          :loading="store.loading"
          stripedRows
          responsiveLayout="scroll"
          class="candidates-table"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-inbox"></i>
              <p>No candidates found matching your filters.</p>
              <Button label="Clear Filters" @click="clearFilters" />
            </div>
          </template>

          <Column field="url" header="URL" style="min-width: 300px">
            <template #body="{ data }">
              <a :href="data.url" target="_blank" class="url-link">
                {{ data.url }}
                <i class="pi pi-external-link"></i>
              </a>
            </template>
          </Column>

          <Column field="title" header="Title" style="min-width: 200px" />

          <Column field="confidenceScore" header="Confidence" style="width: 120px">
            <template #body="{ data }">
              <span :class="getConfidenceClass(data.confidenceScore)">
                {{ parseFloat(data.confidenceScore).toFixed(2) }}
              </span>
            </template>
          </Column>

          <Column field="status" header="Status" style="width: 180px">
            <template #body="{ data }">
              <Tag :value="data.status" :severity="getStatusSeverity(data.status)" />
            </template>
          </Column>

          <Column field="searchEngine" header="Engine" style="width: 130px" />

          <Column field="createdAt" header="Discovered" style="width: 180px">
            <template #body="{ data }">
              {{ formatDate(data.createdAt) }}
            </template>
          </Column>

          <Column header="Actions" style="width: 280px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  label="View"
                  icon="pi pi-eye"
                  size="small"
                  @click="viewCandidate(data.id)"
                  text
                />
                <Button
                  label="Enhance"
                  icon="pi pi-pencil"
                  size="small"
                  @click="enhanceCandidate(data.id)"
                  text
                />
                <Button
                  label="Approve"
                  icon="pi pi-check"
                  size="small"
                  severity="success"
                  @click="confirmApprove(data)"
                  text
                />
                <Button
                  label="Reject"
                  icon="pi pi-times"
                  size="small"
                  severity="danger"
                  @click="confirmReject(data)"
                  text
                />
              </div>
            </template>
          </Column>
        </DataTable>

        <Paginator
          v-if="store.totalElements > 0"
          :rows="20"
          :totalRecords="store.totalElements"
          :first="store.currentPage * 20"
          @page="onPageChange"
          class="pagination"
        />
      </template>
    </Card>

    <ConfirmDialog />
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import { useCandidateStore } from '@/stores/candidateStore'
import { CandidateStatus } from '@/types/CandidateStatus'
import { SearchEngineType } from '@/types/SearchEngineType'
import type { Candidate } from '@/types/Candidate'

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Paginator from 'primevue/paginator'
import MultiSelect from 'primevue/multiselect'
import Dropdown from 'primevue/dropdown'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import ConfirmDialog from 'primevue/confirmdialog'
import Toast from 'primevue/toast'

const router = useRouter()
const confirm = useConfirm()
const toast = useToast()
const store = useCandidateStore()

// Filter options
const statusOptions = Object.values(CandidateStatus)
const searchEngineOptions = Object.values(SearchEngineType)
const confidenceOptions = [
  { label: 'All (≥0.00)', value: 0.0 },
  { label: 'Low (≥0.60)', value: 0.6 },
  { label: 'Medium (≥0.70)', value: 0.7 },
  { label: 'High (≥0.80)', value: 0.8 }
]

// Local filters state
const localFilters = ref({
  status: [] as string[],
  minConfidence: 0.6,
  searchEngine: [] as string[]
})

onMounted(() => {
  store.fetchCandidates()
})

function applyFilters() {
  store.setFilters({
    status: localFilters.value.status,
    minConfidence: localFilters.value.minConfidence,
    searchEngine: localFilters.value.searchEngine,
    page: 0
  })
  store.fetchCandidates()
}

function clearFilters() {
  localFilters.value = {
    status: [],
    minConfidence: 0.6,
    searchEngine: []
  }
  store.clearFilters()
}

function onPageChange(event: any) {
  store.setPage(event.page)
}

function getConfidenceClass(score: string): string {
  const value = parseFloat(score)
  if (value >= 0.8) return 'confidence-high'
  if (value >= 0.7) return 'confidence-medium'
  return 'confidence-low'
}

function getStatusSeverity(status: string): string {
  const severityMap: Record<string, string> = {
    NEW: 'info',
    PENDING_CRAWL: 'info',
    CRAWLED: 'info',
    ENHANCED: 'info',
    JUDGED: 'info',
    APPROVED: 'success',
    REJECTED: 'danger',
    SKIPPED_LOW_CONFIDENCE: 'warning',
    BLACKLISTED: 'danger'
  }
  return severityMap[status] || 'info'
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function viewCandidate(id: string) {
  router.push(`/candidates/${id}`)
}

function enhanceCandidate(id: string) {
  router.push(`/candidates/${id}/enhance`)
}

function confirmApprove(candidate: Candidate) {
  confirm.require({
    message: `Are you sure you want to approve this candidate?\n\n${candidate.title}`,
    header: 'Confirm Approval',
    icon: 'pi pi-check-circle',
    accept: async () => {
      const result = await store.approveCandidate(candidate.id)
      if (result.success) {
        toast.add({
          severity: 'success',
          summary: 'Approved',
          detail: 'Candidate approved successfully',
          life: 3000
        })
      } else {
        toast.add({
          severity: 'error',
          summary: 'Error',
          detail: result.error || 'Failed to approve candidate',
          life: 5000
        })
      }
    }
  })
}

function confirmReject(candidate: Candidate) {
  confirm.require({
    message: `Are you sure you want to reject this candidate and blacklist the domain?\n\n${candidate.title}\n\nDomain: ${candidate.domainName || 'unknown'}`,
    header: 'Confirm Rejection',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      const result = await store.rejectCandidate(candidate.id)
      if (result.success) {
        toast.add({
          severity: 'success',
          summary: 'Rejected',
          detail: 'Candidate rejected and domain blacklisted',
          life: 3000
        })
      } else {
        toast.add({
          severity: 'error',
          summary: 'Error',
          detail: result.error || 'Failed to reject candidate',
          life: 5000
        })
      }
    }
  })
}
</script>

<style scoped>
.review-queue {
  max-width: 1600px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  margin-bottom: 2rem;
}

.header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: #333;
}

.subtitle {
  color: #666;
  font-size: 1.1rem;
}

.filters-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr) auto;
  gap: 1rem;
  align-items: end;
}

.filter-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #555;
  font-size: 0.9rem;
}

.filter-input {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 0.5rem;
}

.candidates-table {
  font-size: 0.9rem;
}

.url-link {
  color: #3b82f6;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.url-link:hover {
  text-decoration: underline;
}

.confidence-high {
  color: #10b981;
  font-weight: 700;
}

.confidence-medium {
  color: #f59e0b;
  font-weight: 600;
}

.confidence-low {
  color: #ef4444;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.empty-state i {
  font-size: 3rem;
  color: #ccc;
  display: block;
  margin-bottom: 1rem;
}

.pagination {
  margin-top: 1rem;
}
</style>
