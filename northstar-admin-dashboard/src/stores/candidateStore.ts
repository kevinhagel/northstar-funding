import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Candidate } from '@/types/Candidate'
import type { CandidatePage } from '@/types/CandidatePage'
import { candidateApi, type CandidateFilters } from '@/services/api'

export const useCandidateStore = defineStore('candidate', () => {
  // State
  const candidates = ref<Candidate[]>([])
  const totalElements = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Filters
  const filters = ref<CandidateFilters>({
    status: [],
    minConfidence: 0.6,
    searchEngine: [],
    sortBy: 'createdAt',
    sortDirection: 'DESC',
    page: 0,
    size: 20
  })

  // Actions
  async function fetchCandidates() {
    loading.value = true
    error.value = null

    try {
      const result: CandidatePage = await candidateApi.listCandidates(filters.value)

      candidates.value = result.content
      totalElements.value = result.totalElements
      totalPages.value = result.totalPages
      currentPage.value = result.currentPage
    } catch (err: any) {
      error.value = err.response?.data?.message || err.message || 'Failed to fetch candidates'
      console.error('Error fetching candidates:', err)
    } finally {
      loading.value = false
    }
  }

  async function approveCandidate(id: string) {
    try {
      await candidateApi.approveCandidate(id)
      await fetchCandidates() // Refresh list
      return { success: true }
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || 'Failed to approve candidate'
      error.value = message
      console.error('Error approving candidate:', err)
      return { success: false, error: message }
    }
  }

  async function rejectCandidate(id: string) {
    try {
      await candidateApi.rejectCandidate(id)
      await fetchCandidates() // Refresh list
      return { success: true }
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || 'Failed to reject candidate'
      error.value = message
      console.error('Error rejecting candidate:', err)
      return { success: false, error: message }
    }
  }

  function setFilters(newFilters: Partial<CandidateFilters>) {
    filters.value = { ...filters.value, ...newFilters }
  }

  function clearFilters() {
    filters.value = {
      status: [],
      minConfidence: 0.6,
      searchEngine: [],
      sortBy: 'createdAt',
      sortDirection: 'DESC',
      page: 0,
      size: 20
    }
    fetchCandidates()
  }

  function setPage(page: number) {
    filters.value.page = page
    fetchCandidates()
  }

  return {
    // State
    candidates,
    totalElements,
    totalPages,
    currentPage,
    loading,
    error,
    filters,
    // Actions
    fetchCandidates,
    approveCandidate,
    rejectCandidate,
    setFilters,
    clearFilters,
    setPage
  }
})
