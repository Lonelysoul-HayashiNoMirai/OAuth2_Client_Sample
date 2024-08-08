package com.lonelysoul.oauth2_client_sample.request_sender.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "taskExecutor")
    public Executor constructTaskExecutor (){
        SimpleAsyncTaskExecutor virtualThreadsExecutor = new SimpleAsyncTaskExecutor ();
        virtualThreadsExecutor.setVirtualThreads (true);
        virtualThreadsExecutor.setTaskTerminationTimeout (30000);

        return virtualThreadsExecutor;
    }
}
