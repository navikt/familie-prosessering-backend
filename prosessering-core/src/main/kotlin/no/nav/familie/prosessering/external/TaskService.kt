package no.nav.familie.prosessering.external

import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.internal.TaskStepExecutorService

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class TaskService(val taskRepository: TaskRepository,
                  @Qualifier("taskExecutor") private val taskExecutor: TaskExecutor,
                  private val taskStepExecutorService: TaskStepExecutorService)  {

    fun save(task: ITask): ITask {
        return taskRepository.save(task)
    }

    fun saveAndRun(task: ITask) {
        taskRepository.save(task)

        taskExecutor.execute { taskStepExecutorService.executeWork(task) }
    }
}