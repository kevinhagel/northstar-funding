package com.northstar.funding.searchadapters.config;

import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.brave.BraveSearchAdapter;
import com.northstar.funding.searchadapters.searxng.SearXNGAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring configuration for search engine adapters.
 *
 * <p>Configures SearchAdapter beans based on application.yml properties.
 * Adapters are automatically registered if they have a configured API key or endpoint.
 *
 * <p>Configuration Pattern:
 * <ul>
 *   <li>Each adapter checks if it's enabled (has required config)</li>
 *   <li>Disabled adapters are not added to the adapter list</li>
 *   <li>SearchWorkflowService auto-discovers all registered adapters</li>
 * </ul>
 *
 * <p>Example application.yml:
 * <pre>
 * northstar:
 *   search:
 *     brave:
 *       api-key: ${BRAVE_API_KEY:}
 *     serper:
 *       api-key: ${SERPER_API_KEY:}
 *     searxng:
 *       base-url: http://localhost:8080
 * </pre>
 */
@Configuration
public class SearchAdapterConfiguration {

    private final SearchAdapterProperties properties;

    public SearchAdapterConfiguration(SearchAdapterProperties properties) {
        this.properties = properties;
    }

    /**
     * Create list of enabled SearchAdapter beans.
     *
     * <p>Adapters are conditionally registered based on configuration:
     * - Brave: Requires API key
     * - SearXNG: Requires base URL
     *
     * <p>Note: Serper adapter not yet implemented.
     *
     * @return List of enabled adapters
     */
    @Bean
    public List<SearchAdapter> searchAdapters() {
        List<SearchAdapter> adapters = new ArrayList<>();

        // Brave Search
        SearchAdapterProperties.BraveConfig braveConfig = properties.getBrave();
        if (braveConfig.getApiKey() != null && !braveConfig.getApiKey().isEmpty()) {
            adapters.add(new BraveSearchAdapter(braveConfig));
        }

        // SearXNG (Self-hosted)
        SearchAdapterProperties.SearxngConfig searxngConfig = properties.getSearxng();
        if (searxngConfig.getApiUrl() != null && !searxngConfig.getApiUrl().isEmpty()) {
            adapters.add(new SearXNGAdapter(searxngConfig));
        }

        return adapters;
    }
}
