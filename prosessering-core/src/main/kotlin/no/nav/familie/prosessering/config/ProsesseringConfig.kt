package no.nav.familie.prosessering.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class ProsesseringConfig(@Value("\${prosessering.queue.capacity:20}") private val køstørrelse: Int,
                         @Value("\${prosessering.pool.size:4}") private val poolSize: Int) {

    @Bean(name = ["taskExecutor"])
    fun threadPoolTaskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = poolSize
        executor.maxPoolSize = poolSize
        executor.threadNamePrefix = "TaskWorker-"
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setQueueCapacity(køstørrelse)
        executor.initialize()
        return executor
    }
}