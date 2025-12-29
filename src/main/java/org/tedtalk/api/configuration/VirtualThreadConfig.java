package org.tedtalk.api.configuration;

import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;


@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * Configure Spring to use Virtual Threads for async operations.
     *
     * Java 21 Virtual Threads are perfect for I/O-bound operations like:
     * - Database queries
     * - File uploads
     * - Network calls
     * - CSV processing
     *
     * Unlike traditional threads, we can create them on-demand without worrying about
     * resource exhaustion. The JVM handles the scheduling efficiently.
     */
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        // Java 21: Create a virtual thread executor
        // This replaces traditional thread pools with lightweight virtual threads
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}

