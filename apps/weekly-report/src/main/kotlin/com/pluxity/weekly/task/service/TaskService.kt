package com.pluxity.weekly.task.service

import com.pluxity.common.auth.annotation.CheckPermission
import com.pluxity.common.auth.user.entity.PermissionAction
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.task.dto.TaskRequest
import com.pluxity.weekly.task.dto.TaskResponse
import com.pluxity.weekly.task.dto.toResponse
import com.pluxity.weekly.task.entity.Task
import com.pluxity.weekly.task.repository.TaskRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// TODO: Role별 llm context 생성 메서드
@Service
@Transactional(readOnly = true)
class TaskService(
    private val taskRepository: TaskRepository,
    private val epicRepository: EpicRepository,
) {
    @CheckPermission(action = PermissionAction.READ_LIST, resourceType = "task")
    fun findAll(): List<TaskResponse> = taskRepository.findAll().map { it.toResponse() }

    @CheckPermission(action = PermissionAction.READ_SINGLE, resourceType = "task")
    fun findById(id: Long): TaskResponse = getTaskById(id).toResponse()

    @CheckPermission(action = PermissionAction.CREATE, resourceType = "task")
    @Transactional
    fun create(request: TaskRequest): Long =
        taskRepository
            .save(
                Task(
                    epic = getEpicById(request.epicId),
                    name = request.name,
                    description = request.description,
                    status = request.status,
                    progress = request.progress,
                    startDate = request.startDate,
                    dueDate = request.dueDate,
                ),
            ).requiredId

    @CheckPermission(action = PermissionAction.UPDATE, resourceType = "task")
    @Transactional
    fun update(
        id: Long,
        request: TaskRequest,
    ) {
        getTaskById(id).update(
            epic = getEpicById(request.epicId),
            name = request.name,
            description = request.description,
            status = request.status,
            progress = request.progress,
            startDate = request.startDate,
            dueDate = request.dueDate,
        )
    }

    @CheckPermission(action = PermissionAction.DELETE, resourceType = "task")
    @Transactional
    fun delete(id: Long) {
        taskRepository.delete(getTaskById(id))
    }

    private fun getTaskById(id: Long): Task =
        taskRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TASK, id)

    private fun getEpicById(id: Long): Epic =
        epicRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_EPIC, id)
}
