/**
 * Candidate status values - mirrors CandidateStatus from backend.
 */
export const CandidateStatus = {
  NEW: 'NEW',
  PENDING_CRAWL: 'PENDING_CRAWL',
  CRAWLED: 'CRAWLED',
  ENHANCED: 'ENHANCED',
  JUDGED: 'JUDGED',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  SKIPPED_LOW_CONFIDENCE: 'SKIPPED_LOW_CONFIDENCE',
  BLACKLISTED: 'BLACKLISTED'
} as const

export type CandidateStatusType = typeof CandidateStatus[keyof typeof CandidateStatus]
