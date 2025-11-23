import type { Candidate } from './Candidate'

/**
 * Paginated candidate response - mirrors CandidatePageDTO from backend.
 */
export interface CandidatePage {
  content: Candidate[]
  totalElements: number
  totalPages: number
  currentPage: number
}
