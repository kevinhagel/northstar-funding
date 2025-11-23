/**
 * Search engine type values - mirrors SearchEngineType from backend.
 */
export const SearchEngineType = {
  BRAVE: 'BRAVE',
  TAVILY: 'TAVILY',
  PERPLEXITY: 'PERPLEXITY',
  SEARXNG: 'SEARXNG',
  BROWSERBASE: 'BROWSERBASE'
} as const

export type SearchEngineTypeValue = typeof SearchEngineType[keyof typeof SearchEngineType]
