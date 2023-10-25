package no.nav.familie.prosessering.internal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization

@Component
class TaskTransactionSynchronization(private val taskStepExecutorService: TaskStepExecutorService) :
    TransactionSynchronization {

    private val logger = LoggerFactory.getLogger(javaClass)
    override fun afterCommit() {
        logger.debug("Kaller på pollAndExecute")
        taskStepExecutorService.pollAndExecute()
    }
}
