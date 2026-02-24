package com.pluxity.common.auth.user.service

import com.pluxity.common.auth.permission.PermissionService
import com.pluxity.common.auth.user.dto.RoleCreateRequest
import com.pluxity.common.auth.user.dto.RoleResponse
import com.pluxity.common.auth.user.dto.RoleUpdateRequest
import com.pluxity.common.auth.user.dto.toResponse
import com.pluxity.common.auth.user.entity.Role
import com.pluxity.common.auth.user.entity.RolePermission
import com.pluxity.common.auth.user.entity.RoleType
import com.pluxity.common.auth.user.repository.RolePermissionRepository
import com.pluxity.common.auth.user.repository.RoleRepository
import com.pluxity.common.auth.user.repository.UserRoleRepository
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import jakarta.persistence.EntityManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RoleService(
    private val roleRepository: RoleRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val userRoleRepository: UserRoleRepository,
    private val permissionService: PermissionService,
    private val em: EntityManager,
) {
    @Transactional
    fun save(
        request: RoleCreateRequest,
        authentication: Authentication,
    ): Long {
        if (request.authority == RoleType.ADMIN && authentication.authorities.none { it.authority == "ROLE_${RoleType.ADMIN.name}" }) {
            throw CustomException(ErrorCode.PERMISSION_DENIED)
        }
        val role =
            roleRepository.save(
                Role(
                    name = request.name,
                    description = request.description,
                    auth = request.authority.name,
                ),
            )

        request.permissionIds.let { permissionIds ->
            if (permissionIds.isNotEmpty()) {
                val newRolePermissions =
                    permissionIds.map { permissionId ->
                        val permission = permissionService.findPermissionById(permissionId)
                        RolePermission(
                            role = role,
                            permission = permission,
                        )
                    }

                rolePermissionRepository.saveAll(newRolePermissions)
                newRolePermissions.forEach { rolePermission ->
                    role.addRolePermission(rolePermission)
                }
            }
        }

        return role.requiredId
    }

    fun findById(id: Long): RoleResponse = findRoleById(id).toResponse()

    fun findAll(): List<RoleResponse> = roleRepository.findAllByOrderByCreatedAtDesc().map { it.toResponse() }

    @Transactional
    fun update(
        id: Long,
        request: RoleUpdateRequest,
    ) {
        val role = findRoleById(id)

        request.name?.takeIf { it.isNotBlank() }?.let {
            role.changeRoleName(it)
        }
        request.description?.let { role.changeDescription(request.description) }

        request.permissionIds?.let { syncPermissions(role, request.permissionIds) }
    }

    private fun syncPermissions(
        role: Role,
        requestedPermissionIds: List<Long>,
    ) {
        val currentPermissionIds = role.rolePermissions.map { it.permission.id }.toSet()
        val requestedPermissionIdsSet = requestedPermissionIds.toSet()

        val rolePermissionsToRemove =
            role.rolePermissions
                .filter { !requestedPermissionIdsSet.contains(it.permission.id) }

        if (rolePermissionsToRemove.isNotEmpty()) {
            rolePermissionRepository.deleteAllInBatch(rolePermissionsToRemove)
            rolePermissionsToRemove.forEach { rolePermission ->
                role.removeRolePermission(rolePermission)
            }
        }

        val idsToAdd = requestedPermissionIdsSet.filter { !currentPermissionIds.contains(it) }

        if (idsToAdd.isNotEmpty()) {
            val rolePermissionsToAdd =
                idsToAdd.map { permissionId ->
                    val permission = permissionService.findPermissionById(permissionId)
                    RolePermission(
                        role = role,
                        permission = permission,
                    )
                }

            rolePermissionRepository.saveAll(rolePermissionsToAdd).forEach { rolePermission ->
                role.addRolePermission(rolePermission)
            }
        }
    }

    @Transactional
    fun delete(id: Long) {
        val role = findRoleById(id)
        rolePermissionRepository.deleteAllByRole(role)
        userRoleRepository.deleteAllByRole(role)
        em.flush()
        em.clear()
        roleRepository.deleteById(role.requiredId)
    }

    fun findRoleById(id: Long): Role =
        roleRepository.findWithInfoById(id)
            ?: throw CustomException(ErrorCode.NOT_FOUND_ROLE, id)
}
