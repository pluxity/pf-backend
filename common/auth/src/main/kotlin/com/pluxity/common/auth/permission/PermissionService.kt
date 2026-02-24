package com.pluxity.common.auth.permission

import com.pluxity.common.auth.permission.dto.PermissionCreateRequest
import com.pluxity.common.auth.permission.dto.PermissionResponse
import com.pluxity.common.auth.permission.dto.PermissionUpdateRequest
import com.pluxity.common.auth.permission.dto.ResourceTypeResponse
import com.pluxity.common.auth.permission.dto.toResponse
import com.pluxity.common.auth.user.repository.RolePermissionRepository
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PermissionService(
    private val permissionRepository: PermissionRepository,
    private val resourcePermissionRepository: ResourcePermissionRepository,
    private val domainPermissionRepository: DomainPermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val resourceDataProviders: List<ResourceDataProvider>,
    private val resourceTypeRegistry: ResourceTypeRegistry,
) {
    @Transactional
    fun create(request: PermissionCreateRequest): Long {
        if (permissionRepository.existsByName(request.name)) {
            throw CustomException(ErrorCode.DUPLICATE_PERMISSION_NAME, request.name)
        }

        val permission = Permission(name = request.name, description = request.description)
        val requestedResourceKeys = mutableSetOf<ResourceKey>()
        val requestedDomainKeys = mutableSetOf<String>()
        request.permissions.forEach { permissionRequest ->
            val resourceName = resourceTypeRegistry.resolve(permissionRequest.resourceType)
            val resourceIds = permissionRequest.resourceIds
            val level = permissionRequest.level
            val hasDomainRequest = resourceIds.isEmpty()

            if (hasDomainRequest) {
                if (!requestedDomainKeys.add(resourceName)) {
                    throw CustomException(
                        ErrorCode.DUPLICATE_RESOURCE_ID,
                        "리소스 타입 '$resourceName'에 중복된 도메인 권한이 포함되어 있습니다.",
                    )
                }
                val domainPermission =
                    DomainPermission(
                        resourceName = resourceName,
                        level = level,
                    )
                permission.addDomainPermission(domainPermission)
            }

            resourceIds
                .forEach { id ->
                    val key = ResourceKey(resourceName, id)
                    if (!requestedResourceKeys.add(key)) {
                        throw CustomException(
                            ErrorCode.DUPLICATE_RESOURCE_ID,
                            "리소스 타입 '$resourceName'에 중복된 ID가 포함되어 있습니다.",
                        )
                    }
                    val resourcePermission =
                        ResourcePermission(
                            resourceName = resourceName,
                            resourceId = id,
                            level = level,
                        )
                    permission.addResourcePermission(resourcePermission)
                }
        }

        return permissionRepository.save(permission).requiredId
    }

    fun findById(id: Long): PermissionResponse = findPermissionById(id).toResponse()

    fun findAll(): List<PermissionResponse> = permissionRepository.findAll().map { it.toResponse() }

    @Transactional
    fun update(
        id: Long,
        request: PermissionUpdateRequest,
    ) {
        val permission = findPermissionById(id)

        request.name?.takeIf { it.isNotBlank() && it != permission.name }?.let { newName ->
            if (permissionRepository.existsByNameAndIdNot(newName, id)) {
                throw CustomException(ErrorCode.DUPLICATE_PERMISSION_NAME, newName)
            }
            permission.changeName(newName)
        }

        request.description?.let { permission.changeDescription(it) }

        val existingResourceMap =
            permission.resourcePermissions
                .associateBy { ResourceKey(it.resourceName, it.resourceId) }
        val existingDomainMap =
            permission.domainPermissions
                .associateBy { it.resourceName }

        val requestedResourceKeys = mutableSetOf<ResourceKey>()
        val requestedDomainKeys = mutableSetOf<String>()
        request.permissions.forEach { permissionRequest ->
            val resourceName = resourceTypeRegistry.resolve(permissionRequest.resourceType)
            val level = permissionRequest.level
            val resourceIds = permissionRequest.resourceIds
            val hasDomainRequest = resourceIds.isEmpty()

            if (hasDomainRequest) {
                if (!requestedDomainKeys.add(resourceName)) {
                    throw CustomException(
                        ErrorCode.DUPLICATE_RESOURCE_ID,
                        "리소스 타입 '$resourceName'에 중복된 도메인 권한이 포함되어 있습니다.",
                    )
                }
                existingDomainMap[resourceName]?.changeLevel(level)
                    ?: permission.addDomainPermission(DomainPermission(resourceName = resourceName, level = level))
            }

            resourceIds.forEach { resourceId ->
                val key = ResourceKey(resourceName, resourceId)
                if (!requestedResourceKeys.add(key)) {
                    throw CustomException(
                        ErrorCode.DUPLICATE_RESOURCE_ID,
                        "리소스 타입 '$resourceName'에 중복된 ID가 포함되어 있습니다.",
                    )
                }
                existingResourceMap[key]?.changeLevel(level)
                    ?: permission.addResourcePermission(
                        ResourcePermission(resourceName = resourceName, resourceId = resourceId, level = level),
                    )
            }
        }

        existingDomainMap
            .filterKeys { it !in requestedDomainKeys }
            .values
            .forEach { domainPermission ->
                permission.removeDomainPermission(domainPermission)
                domainPermissionRepository.delete(domainPermission)
            }

        existingResourceMap
            .filterKeys { it !in requestedResourceKeys }
            .values
            .forEach { resourcePermission ->
                permission.removeResourcePermission(resourcePermission)
                resourcePermissionRepository.delete(resourcePermission)
            }
    }

    @Transactional
    fun delete(id: Long) {
        val permission = findPermissionById(id)
        rolePermissionRepository.deleteAllByPermission(permission)
        resourcePermissionRepository.deleteAll(permission.resourcePermissions)
        domainPermissionRepository.deleteAll(permission.domainPermissions)
        permissionRepository.delete(permission)
    }

    fun findPermissionById(id: Long): Permission =
        permissionRepository.findByIdOrNull(id)
            ?: throw CustomException(ErrorCode.NOT_FOUND_PERMISSION, id)

    fun findAllResourceTypes(): List<ResourceTypeResponse> {
        val providerMap = resourceDataProviders.associateBy { it.resourceType }

        return resourceTypeRegistry
            .allEntries()
            .map { typeInfo ->
                val resources = providerMap[typeInfo.key]?.findAllResources() ?: emptyList()
                typeInfo.toResponse(resources)
            }
    }
}
