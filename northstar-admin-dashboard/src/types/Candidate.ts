/**
 * Candidate interface - mirrors CandidateDTO from backend.
 * All fields are String types for consistent JSON serialization.
 */
export interface Candidate {
  id: string
  url: string
  title: string
  confidenceScore: string
  status: string
  searchEngine: string
  createdAt: string
  domainName?: string
}
