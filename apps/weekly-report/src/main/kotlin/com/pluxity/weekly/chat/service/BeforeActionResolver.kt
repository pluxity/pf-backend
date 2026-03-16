package com.pluxity.weekly.chat.service

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.weekly.chat.dto.BeforeAction
import com.pluxity.weekly.chat.dto.Candidate
import com.pluxity.weekly.chat.dto.LlmAction
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.task.repository.TaskRepository
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class BeforeActionResolver(
    private val projectRepository: ProjectRepository,
    private val epicRepository: EpicRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
) {
    fun resolve(action: LlmAction): List<BeforeAction> {
        val missingFields = action.missingFields ?: emptyList()
        val candidateIds = action.candidates ?: emptyList()
        val result = mutableListOf<BeforeAction>()

        for (field in missingFields) {
            val beforeAction = when (field) {
                "id" -> resolveIdCandidates(action.target, candidateIds)
                "project_id" -> resolveProjectCandidates(candidateIds)
                "epic_id" -> resolveEpicCandidates(candidateIds)
                else -> null
            }
            if (beforeAction != null) result.add(beforeAction)
        }

        addSelectFields(action, result)

        return result
    }

    private fun resolveIdCandidates(
        target: String?,
        candidateIds: List<Long>,
    ): BeforeAction? {
        if (candidateIds.isEmpty()) return null
        val candidates = when (target) {
            "task" -> candidateIds.mapNotNull { id ->
                taskRepository.findByIdOrNull(id)?.let { task ->
                    val epic = epicRepository.findByIdOrNull(task.epic.id!!)
                    val project = epic?.let { projectRepository.findByIdOrNull(it.project.id!!) }
                    Candidate(id, "${task.name} (${project?.name ?: ""}/${epic?.name ?: ""})")
                }
            }
            "epic" -> candidateIds.mapNotNull { id ->
                epicRepository.findByIdOrNull(id)?.let { epic ->
                    val project = projectRepository.findByIdOrNull(epic.project.id!!)
                    Candidate(id, "${epic.name} (${project?.name ?: ""})")
                }
            }
            "project" -> candidateIds.mapNotNull { id ->
                projectRepository.findByIdOrNull(id)?.let { Candidate(id, it.name) }
            }
            else -> return null
        }
        return BeforeAction(field = "id", candidates = candidates)
    }

    private fun resolveProjectCandidates(candidateIds: List<Long>): BeforeAction? {
        val candidates = if (candidateIds.isNotEmpty()) {
            candidateIds.mapNotNull { id ->
                projectRepository.findByIdOrNull(id)?.let { Candidate(id, it.name) }
            }
        } else {
            projectRepository.findAll().map { Candidate(it.requiredId, it.name) }
        }
        if (candidates.isEmpty()) return null
        return BeforeAction(field = "projectId", candidates = candidates)
    }

    private fun resolveEpicCandidates(candidateIds: List<Long>): BeforeAction? {
        val candidates = if (candidateIds.isNotEmpty()) {
            candidateIds.mapNotNull { id ->
                epicRepository.findByIdOrNull(id)?.let { epic ->
                    val project = projectRepository.findByIdOrNull(epic.project.id!!)
                    Candidate(id, "${epic.name} (${project?.name ?: ""})")
                }
            }
        } else {
            epicRepository.findAll().map { epic ->
                val project = projectRepository.findByIdOrNull(epic.project.id!!)
                Candidate(epic.requiredId, "${epic.name} (${project?.name ?: ""})")
            }
        }
        if (candidates.isEmpty()) return null
        return BeforeAction(field = "epicId", candidates = candidates)
    }

    private fun resolveUserCandidates(
        field: String,
        roleName: String? = null,
    ): BeforeAction {
        val users = userRepository.findAllBy(Sort.by("name"))
        val candidates = if (roleName != null) {
            users.filter { user -> user.userRoles.any { it.role.name.uppercase() == roleName } }
        } else {
            users
        }.map { Candidate(it.requiredId, it.name) }
        return BeforeAction(field = field, candidates = candidates)
    }

    private fun addSelectFields(
        action: LlmAction,
        result: MutableList<BeforeAction>,
    ) {
        if (action.action != "create") return

        val existingFields = result.map { it.field }.toSet()

        when (action.target) {
            "project" -> {
                if ("pmId" !in existingFields) {
                    result.add(resolveUserCandidates("pmId", "PM"))
                }
            }
            "epic" -> {
                if ("projectId" !in existingFields) {
                    result.add(resolveProjectCandidates(emptyList()) ?: return)
                }
                if ("userIds" !in existingFields) {
                    result.add(resolveUserCandidates("userIds"))
                }
            }
            "task" -> {
                if ("epicId" !in existingFields) {
                    result.add(resolveEpicCandidates(emptyList()) ?: return)
                }
            }
            "team" -> {
                if ("leaderId" !in existingFields) {
                    result.add(resolveUserCandidates("leaderId"))
                }
            }
        }
    }
}
