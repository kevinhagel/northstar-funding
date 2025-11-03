package com.northstar.funding.querygeneration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

/**
 * Configuration for Java 25 Virtual Threads.
 *
 * <p>Virtual Threads provide lightweight concurrency perfect for:
 * <ul>
 *   <li>Parallel query generation across multiple providers</li>
 *   <li>Non-blocking I/O operations (LM Studio HTTP calls)</li>
 *   <li>Async persistence operations</li>
 * </ul>
 *
 * <p>Benefits over platform threads:
 * <ul>
 *   <li>Millions of threads without memory overhead</li>
 *   <li>Blocking I/O doesn't block platform threads</li>
 *   <li>Perfect for CompletableFuture-based async APIs</li>
 * </ul>
 */
@Configuration
public class VirtualThreadConfig {

    /**
     * Creates async task executor using Virtual Threads.
     *
     * <p>Each task runs on a new virtual thread, allowing high concurrency
     * without platform thread exhaustion.
     *
     * @return Virtual thread-based async executor
     */
    @Bean(name = "asyncTaskExecutor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(
                Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
