package com.northstar.funding.querygeneration.template;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * LangChain4j prompt templates for query generation.
 *
 * <p>Provides two templates:
 * <ul>
 *   <li>KEYWORD_QUERY_TEMPLATE: For traditional search engines (Brave, Serper, SearXNG)</li>
 *   <li>PERPLEXICA_QUERY_TEMPLATE: For AI-optimized search (Perplexica)</li>
 * </ul>
 *
 * <p>Templates use LangChain4j variable substitution with {{variableName}} syntax.
 */
public class PromptTemplates {

    /**
     * Template for generating keyword queries for traditional search engines.
     *
     * <p>Variables:
     * <ul>
     *   <li>{{categories}}: Comma-separated category keywords</li>
     *   <li>{{geographic}}: Geographic location keywords</li>
     *   <li>{{maxQueries}}: Number of queries to generate</li>
     * </ul>
     */
    public static final PromptTemplate KEYWORD_QUERY_TEMPLATE = PromptTemplate.from("""
            You are a search query expert specialized in finding educational funding opportunities.

            Generate {{maxQueries}} concise, keyword-focused search queries for traditional search engines.

            **Requirements:**
            - Each query should be 3-8 words maximum
            - Focus on keywords, not full sentences
            - Include relevant geographic terms
            - Vary the keyword combinations for diversity
            - Target funding for: {{categories}}
            - Geographic focus: {{geographic}}

            **Query Style Examples:**
            - "Bulgaria education infrastructure grants"
            - "EU scholarship programs Eastern Europe"
            - "teacher training funding Balkans"

            **Output Format:**
            Return ONLY the queries, one per line, numbered 1-{{maxQueries}}.
            Do not include explanations, just the queries.

            1.""");

    /**
     * Template for generating AI-optimized queries for Perplexica search.
     *
     * <p>Variables:
     * <ul>
     *   <li>{{categories}}: Comma-separated conceptual category descriptions</li>
     *   <li>{{geographic}}: Geographic contextual description</li>
     *   <li>{{maxQueries}}: Number of queries to generate</li>
     * </ul>
     */
    public static final PromptTemplate PERPLEXICA_QUERY_TEMPLATE = PromptTemplate.from("""
            You are a search query expert specialized in finding educational funding opportunities.

            Generate {{maxQueries}} detailed, AI-optimized search queries for semantic search engines.

            **Requirements:**
            - Each query should be 15-30 words (full sentence or question)
            - Include rich context about the funding type and purpose
            - Incorporate geographic and economic context
            - Vary query structure (statements, questions, specific scenarios)
            - Target funding for: {{categories}}
            - Geographic focus: {{geographic}}

            **Query Style Examples:**
            - "Educational infrastructure funding opportunities for modernizing schools and facilities in Bulgaria and post-transition Eastern European countries"
            - "What scholarship programs are available for teachers in the Balkans region to pursue professional development and advanced degrees?"
            - "Grant opportunities supporting STEM education initiatives and technology integration in EU candidate countries like Serbia and North Macedonia"

            **Output Format:**
            Return ONLY the queries, one per line, numbered 1-{{maxQueries}}.
            Do not include explanations, metadata, or commentary.

            1.""");

    /**
     * Fallback queries for keyword-based search when LLM is unavailable.
     */
    public static final String[] KEYWORD_FALLBACK_QUERIES = {
        "education grants funding",
        "school program grants",
        "teacher training funding",
        "student scholarship opportunities",
        "educational infrastructure grants"
    };

    /**
     * Fallback queries for AI-optimized search when LLM is unavailable.
     */
    public static final String[] PERPLEXICA_FALLBACK_QUERIES = {
        "Educational funding opportunities for schools and programs supporting student learning and development",
        "Grant programs for teacher professional development and educational capacity building initiatives",
        "Scholarship and financial aid opportunities for students pursuing educational advancement"
    };

    private PromptTemplates() {
        // Utility class - prevent instantiation
    }
}
