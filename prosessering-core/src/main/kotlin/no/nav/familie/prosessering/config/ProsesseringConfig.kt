package no.nav.familie.prosessering.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
@EnableAsync
class ProsesseringConfig(
    @Value("\${prosessering.queue.capacity:20}") private val køstørrelse: Int,
    @Value("\${prosessering.pool.size:4}") private val poolSize: Int
) : SchedulingConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLog = LoggerFactory.getLogger("secureLogger")

    @Bean(name = ["taskExecutor"])
    fun threadPoolTaskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = poolSize
        executor.maxPoolSize = poolSize
        executor.threadNamePrefix = "TaskWorker-"
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(20)
        executor.setQueueCapacity(køstørrelse)
        executor.initialize()
        return executor
    }

    @Bean
    fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler {
        val executor = object : ThreadPoolTaskScheduler() {
            override fun destroy() {
                this.scheduledThreadPoolExecutor.executeExistingDelayedTasksAfterShutdownPolicy = false
                super.destroy()
            }
        }
        executor.threadNamePrefix = "TaskScheduler-"
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(20)
        executor.setErrorHandler { e ->
            log.error(
                "TaskScheduler feilet, se secureLogs. exception=${e.javaClass.simpleName}" +
                    " cause=${e.cause?.javaClass?.simpleName}"
            )
            secureLog.error("TaskScheduler feilet", e)
        }
        executor.initialize()
        return executor
    }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setScheduler(threadPoolTaskScheduler())
    }
}
