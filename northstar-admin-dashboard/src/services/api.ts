import axios from 'axios'
import type { Candidate } from '@/types/Candidate'
import type { CandidatePage } from '@/types/CandidatePage'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

export interface CandidateFilters {
  status?: string[]
  minConfidence?: number
  searchEngine?: string[]
  startDate?: string
  endDate?: string
  sortBy?: string
  sortDirection?: string
  page?: number
  size?: number
}

export const candidateApi = {
  /**
   * List candidates with filters and pagination
   */
  async listCandidates(filters: CandidateFilters): Promise<CandidatePage> {
    const { data } = await api.get<CandidatePage>('/candidates', {
      params: filters
    })
    return data
  },

  /**
   * Approve a candidate
   */
  async approveCandidate(id: string): Promise<Candidate> {
    const { data } = await api.put<Candidate>(`/candidates/${id}/approve`)
    return data
  },

  /**
   * Reject a candidate and blacklist domain
   */
  async rejectCandidate(id: string): Promise<Candidate> {
    const { data } = await api.put<Candidate>(`/candidates/${id}/reject`)
    return data
  }
}

export default api
