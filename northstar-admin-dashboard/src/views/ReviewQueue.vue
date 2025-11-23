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
/* Celestial Navigation Theme - Review Queue */

.review-queue {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 2rem 2rem;
}

/* Page Header with Constellation Styling */
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
  margin-bottom: 0.625rem;
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
  letter-spacing: 0.01em;
}

/* Glassmorphic Filter Card */
.filters-card {
  background: var(--surface-elevated);
  backdrop-filter: blur(20px) saturate(180%);
  border-radius: 12px;
  padding: 1.75rem;
  margin-bottom: 2rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border-subtle);
  position: relative;
  overflow: hidden;
}

.filters-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--cosmic-glow), transparent);
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr) auto;
  gap: 1.25rem;
  align-items: end;
}

.filter-group label {
  display: block;
  font-family: var(--font-body);
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--text-secondary);
  font-size: 0.875rem;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.filter-input {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 0.625rem;
}

/* Data Table Enhancements */
.candidates-table {
  font-family: var(--font-body);
  font-size: 0.9375rem;
}

/* URL Links with Aurora Accent */
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

/* Confidence Score Badges with Cosmic Colors */
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

/* Action Buttons with Hover Effects */
.action-buttons {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

/* Empty State with Celestial Styling */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--text-secondary);
}

.empty-state i {
  font-size: 4rem;
  color: var(--constellation);
  display: block;
  margin-bottom: 1.5rem;
  opacity: 0.4;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.empty-state p {
  font-family: var(--font-body);
  font-size: 1.125rem;
  margin-bottom: 1.5rem;
  color: var(--text-secondary);
}

/* Pagination Styling */
.pagination {
  margin-top: 1.5rem;
}

/* PrimeVue Component Overrides for Dark Theme */
:deep(.p-card) {
  background: var(--surface-elevated);
  backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--border-subtle);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  border-radius: 12px;
}

:deep(.p-card .p-card-body) {
  padding: 1.5rem;
}

:deep(.p-card .p-card-content) {
  padding: 0;
}

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

:deep(.p-datatable .p-datatable-tbody > tr) {
  background: transparent;
  transition: all 0.2s ease;
}

:deep(.p-datatable .p-datatable-tbody > tr:hover) {
  background: rgba(74, 88, 153, 0.15);
}

:deep(.p-datatable .p-datatable-tbody > tr > td) {
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-subtle);
  padding: 0.875rem 0.75rem;
}

:deep(.p-datatable .p-datatable-tbody > tr.p-row-odd) {
  background: rgba(26, 31, 58, 0.3);
}

:deep(.p-datatable .p-datatable-tbody > tr.p-row-odd:hover) {
  background: rgba(74, 88, 153, 0.2);
}

/* PrimeVue Tag Component */
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

:deep(.p-tag.p-tag-danger) {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

:deep(.p-tag.p-tag-info) {
  background: var(--cosmic-glow);
  color: var(--aurora);
  border: 1px solid rgba(0, 217, 255, 0.3);
}

:deep(.p-tag.p-tag-warning) {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

/* PrimeVue Button Component */
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

:deep(.p-button.p-button-success) {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

:deep(.p-button.p-button-success:hover) {
  background: rgba(16, 185, 129, 0.3);
  border-color: #10b981;
}

:deep(.p-button.p-button-danger) {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

:deep(.p-button.p-button-danger:hover) {
  background: rgba(239, 68, 68, 0.3);
  border-color: #ef4444;
}

/* PrimeVue Input Components */
:deep(.p-multiselect),
:deep(.p-dropdown) {
  background: var(--surface);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  transition: all 0.2s ease;
}

:deep(.p-multiselect:hover),
:deep(.p-dropdown:hover) {
  border-color: var(--constellation);
}

:deep(.p-multiselect.p-focus),
:deep(.p-dropdown.p-focus) {
  border-color: var(--aurora);
  box-shadow: 0 0 0 2px var(--cosmic-glow);
}

:deep(.p-multiselect .p-multiselect-label),
:deep(.p-dropdown .p-dropdown-label) {
  color: var(--text-primary);
  font-family: var(--font-body);
}

/* PrimeVue Paginator */
:deep(.p-paginator) {
  background: var(--surface);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.75rem;
  margin-top: 1.5rem;
}

:deep(.p-paginator .p-paginator-pages .p-paginator-page) {
  color: var(--text-secondary);
  border-radius: 6px;
  transition: all 0.2s ease;
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

/* Loading Spinner */
:deep(.p-progress-spinner) {
  opacity: 0.8;
}

:deep(.p-progress-spinner-circle) {
  stroke: var(--aurora);
}
</style>
