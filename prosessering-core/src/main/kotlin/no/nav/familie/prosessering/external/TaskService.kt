package no.nav.familie.prosessering.external

import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.internal.TaskStepExecutorService

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class TaskService(val taskRepository: TaskRepository,
                  @Qualifier("taskExecutor") private val taskExecutor: TaskExecutor,
                  private val taskStepExecutorService: TaskStepExecutorService)  {

    fun save(task: ITask): ITask {
        return taskRepository.save(task)
    }

    @Transactional(propagation = Propagation.NESTED)
    fun saveAndRun(task: ITask) {
        val taskToExcecute = taskRepository.save(task)

        taskStepExecutorService.executeWork(taskToExcecute)
    }
}