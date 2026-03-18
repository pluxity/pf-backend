package com.pluxity.weekly.chat.service

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.weekly.chat.dto.SelectField
import com.pluxity.weekly.chat.dto.Candidate
import com.pluxity.weekly.chat.dto.LlmAction
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.task.repository.TaskRepository
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class SelectFieldResolver(
    private val projectRepository: ProjectRepository,
    private val epicRepository: EpicRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
) {
    fun resolve(action: LlmAction): List<SelectField> {
        val missingFields = action.missingFields ?: emptyList()
        val candidateIds = action.candidates ?: emptyList()
        val result = mutableListOf<SelectField>()

        for (field in missingFields) {
            val selectField =
                when (field) {
                    "id" -> resolveIdCandidates(action.target, candidateIds)
                    "project_id" -> resolveProjectCandidates(candidateIds)
                    "epic_id" -> resolveEpicCandidates(candidateIds)
                    else -> null
                }
            if (selectField != null) result.add(selectField)
        }

        addSelectFields(action, result)

        return result
    }

    private fun resolveIdCandidates(
        target: String?,
        candidateIds: List<Long>,
    ): SelectField? {
        if (candidateIds.isEmpty()) return null
        val candidates =
            when (target) {
                "task" ->
                    candidateIds.mapNotNull { id ->
                        taskRepository.findByIdOrNull(id)?.let { task ->
                            val epic = epicRepository.findByIdOrNull(task.epic.id!!)
                            val project = epic?.let { projectRepository.findByIdOrNull(it.project.id!!) }
                            Candidate(id, "${task.name} (${project?.name ?: ""}/${epic?.name ?: ""})")
                        }
                    }
                "epic" ->
                    candidateIds.mapNotNull { id ->
                        epicRepository.findByIdOrNull(id)?.let { epic ->
                            val project = projectRepository.findByIdOrNull(epic.project.id!!)
                            Candidate(id, "${epic.name} (${project?.name ?: ""})")
                        }
                    }
                "project" ->
                    candidateIds.mapNotNull { id ->
                        projectRepository.findByIdOrNull(id)?.let { Candidate(id, it.name) }
                    }
                else -> return null
            }
        return SelectField(field = "id", candidates = candidates)
    }

    private fun resolveProjectCandidates(candidateIds: List<Long>): SelectField? {
        val candidates =
            if (candidateIds.isNotEmpty()) {
                candidateIds.mapNotNull { id ->
                    projectRepository.findByIdOrNull(id)?.let { Candidate(id, it.name) }
                }
            } else {
                projectRepository.findAll().map { Candidate(it.requiredId, it.name) }
            }
        if (candidates.isEmpty()) return null
        return SelectField(field = "projectId", candidates = candidates)
    }

    private fun resolveEpicCandidates(candidateIds: List<Long>): SelectField? {
        val candidates =
            if (candidateIds.isNotEmpty()) {
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
        return SelectField(field = "epicId", candidates = candidates)
    }

    private fun resolveUserCandidates(
        field: String,
        roleName: String? = null,
    ): SelectField {
        val users = userRepository.findAllBy(Sort.by("name"))
        val candidates =
            if (roleName != null) {
                users.filter { user -> user.userRoles.any { it.role.name.uppercase() == roleName } }
            } else {
                users
            }.map { Candidate(it.requiredId, it.name) }
        return SelectField(field = field, candidates = candidates)
    }

    private fun resolveStatusCandidates(target: String): SelectField {
        val statuses = when (target) {
            "project" -> listOf("TODO", "IN_PROGRESS", "DONE")
            else -> listOf("TODO", "IN_PROGRESS", "DONE")
        }
        return SelectField(
            field = "status",
            candidates = statuses.mapIndexed { index, name -> Candidate(index.toLong(), name) },
        )
    }

    private fun addSelectFields(
        action: LlmAction,
        result: MutableList<SelectField>,
    ) {
        if (action.action != "create") return

        val existingFields = result.map { it.field }.toSet()

        when (action.target) {
            "project" -> {
                if ("pmId" !in existingFields) {
                    result.add(resolveUserCandidates("pmId", "PM"))
                }
                if ("status" !in existingFields) {
                    result.add(resolveStatusCandidates("project"))
                }
            }
            "epic" -> {
                if ("projectId" !in existingFields) {
                    result.add(resolveProjectCandidates(emptyList()) ?: return)
                }
                if ("userIds" !in existingFields) {
                    result.add(resolveUserCandidates("userIds"))
                }
                if ("status" !in existingFields) {
                    result.add(resolveStatusCandidates("epic"))
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
