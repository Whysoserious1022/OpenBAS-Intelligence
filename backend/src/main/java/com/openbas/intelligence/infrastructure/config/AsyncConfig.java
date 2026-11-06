package com.openbas.intelligence.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async executor configuration.
 * Uses separate thread pools for telemetry ingestion and anomaly detection
 * so a slow anomaly evaluation never blocks telemetry throughput.
 */
@Configuration
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Thread pool for telemetry ingestion.
     * Sized for concurrent multi-device ingestion.
     * Uses CallerRunsPolicy to apply backpressure rather than losing data.
     */
    @Bean(name = "telemetryIngestionExecutor")
    public Executor telemetryIngestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("telemetry-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Telemetry ingestion executor initialized: core=10, max=20, queue=10000");
        return executor;
    }

    /**
     * Thread pool for anomaly detection.
     * Separate from ingestion so anomaly evaluation delays don't back-pressure telemetry.
     */
    @Bean(name = "anomalyDetectionExecutor")
    public Executor anomalyDetectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("anomaly-detect-");
        executor.setRejectedExecutionHandler(new LoggingDiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        log.info("Anomaly detection executor initialized: core=4, max=8, queue=5000");
        return executor;
    }

    /**
     * Thread pool for AI service calls.
     */
    @Bean(name = "aiServiceExecutor")
    public Executor aiServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-service-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Discard policy that logs the rejection rather than silently dropping.
     */
    static class LoggingDiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Task rejected from {}: queue full (capacity={}, active={})",
                executor.toString(), executor.getQueue().size(), executor.getActiveCount());
        }
    }
}
