package com.northstar.funding.querygeneration.template;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * LangChain4j prompt templates for query generation.
 *
 * <p>Part of NorthStar Ubiquitous Language:
 * <ul>
 *   <li><b>Keyword Search</b> - Short keyword-based queries for traditional search engines</li>
 *   <li><b>Prompt Search</b> - Engineered prompts for AI-powered search engines</li>
 * </ul>
 *
 * <p>Provides two templates:
 * <ul>
 *   <li>KEYWORD_QUERY_TEMPLATE: For Keyword Search (Brave, Serper, SearXNG)</li>
 *   <li>PROMPT_SEARCH_TEMPLATE: For Prompt Search (Perplexica, future AI searchers)</li>
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
     * Template for generating Prompt Search queries for AI-powered search engines.
     *
     * <p>Prompt Search queries are engineered prompts that include:
     * <ul>
     *   <li>Positive criteria (what we're looking for)</li>
     *   <li>Negative criteria (exclusions)</li>
     *   <li>Format requirements</li>
     *   <li>Context tuned to the AI search engine's LLM</li>
     * </ul>
     *
     * <p>Variables:
     * <ul>
     *   <li>{{categories}}: Comma-separated conceptual category descriptions</li>
     *   <li>{{geographic}}: Geographic contextual description</li>
     *   <li>{{maxQueries}}: Number of queries to generate</li>
     * </ul>
     */
    public static final PromptTemplate PROMPT_SEARCH_TEMPLATE = PromptTemplate.from("""
            You are a search query expert specialized in finding educational funding opportunities.

            Generate {{maxQueries}} detailed, engineered search prompts for AI-powered search engines like Perplexica.

            **Requirements:**
            - Each query should be 15-40 words (full sentence or question)
            - Include rich context about the funding type and purpose
            - Incorporate geographic and economic context
            - Include negative criteria (exclusions) where appropriate
            - Vary query structure (statements, questions, specific scenarios)
            - Target funding for: {{categories}}
            - Geographic focus: {{geographic}}

            **Query Style Examples:**
            - "What are the current EU funding programs available for educational institutions in Bulgaria that support teacher training and STEM education initiatives? Exclude gambling and non-educational sites."
            - "Find grant opportunities for modernizing school infrastructure in Eastern European countries, particularly those supporting technology integration and facility upgrades in post-transition economies."
            - "Scholarship programs available for educators in the Balkans region to pursue professional development, excluding commercial loan programs and for-profit training services."

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
     * Fallback queries for Prompt Search when LLM is unavailable.
     */
    public static final String[] PROMPT_SEARCH_FALLBACK_QUERIES = {
        "What are the current educational funding opportunities for schools and programs supporting student learning and development? Exclude commercial and for-profit programs.",
        "Find grant programs for teacher professional development and educational capacity building initiatives in Eastern Europe, excluding loan-based financing.",
        "What scholarship and financial aid opportunities are available for students pursuing educational advancement in EU member states and candidate countries?"
    };

    private PromptTemplates() {
        // Utility class - prevent instantiation
    }
}
