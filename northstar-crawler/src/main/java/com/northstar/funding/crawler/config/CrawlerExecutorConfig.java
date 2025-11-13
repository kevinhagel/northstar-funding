package com.northstar.funding.crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Virtual Thread executor configuration for parallel I/O operations.
 *
 * Uses Java 25 Virtual Threads (Project Loom) to enable efficient
 * concurrent search execution across multiple providers without
 * blocking platform threads.
 *
 * Constitutional requirement: NO WebFlux/Reactive - use Virtual Threads instead.
 */
@Configuration
public class CrawlerExecutorConfig {

    /**
     * Creates an unbounded Virtual Thread executor for search operations.
     *
     * Virtual Threads are lightweight and managed by the JVM, making
     * them suitable for high-concurrency I/O-bound operations like
     * parallel HTTP requests to multiple search providers.
     *
     * @return ExecutorService backed by Virtual Threads
     */
    @Bean(name = "searchExecutor", destroyMethod = "shutdown")
    public ExecutorService searchExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Creates a fixed-size Virtual Thread executor for anti-spam filtering.
     *
     * Uses a fixed pool size to limit parallel processing during
     * fuzzy string matching operations.
     *
     * @return ExecutorService backed by Virtual Threads with fixed concurrency
     */
    @Bean(name = "filterExecutor", destroyMethod = "shutdown")
    public ExecutorService filterExecutor() {
        // Limit to 10 concurrent filtering operations to avoid excessive CPU usage
        // during fuzzy matching operations
        return Executors.newFixedThreadPool(10, Thread.ofVirtual().factory());
    }
}
