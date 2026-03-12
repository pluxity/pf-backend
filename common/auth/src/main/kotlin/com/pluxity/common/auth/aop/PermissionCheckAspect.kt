package com.pluxity.common.auth.aop

import com.pluxity.common.auth.annotation.CheckPermission
import com.pluxity.common.auth.constant.SecurityConstants
import com.pluxity.common.auth.permission.PermissionLevel
import com.pluxity.common.auth.user.entity.Permissible
import com.pluxity.common.auth.user.entity.PermissionAction
import com.pluxity.common.auth.user.entity.PermissionStrategy
import com.pluxity.common.auth.user.entity.RoleType
import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.service.UserResourcePermissionService
import com.pluxity.common.auth.user.service.UserService
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

// @Profile("local")
@Aspect
@Component
class PermissionCheckAspect(
    private val userService: UserService,
    private val permissionStrategy: PermissionStrategy,
    private val userResourcePermissionService: UserResourcePermissionService,
) {
    @Before("@annotation(checkPermission)")
    fun beforeExecute(
        joinPoint: JoinPoint,
        checkPermission: CheckPermission,
    ) {
        val user = getCurrentUserIfApplicable() ?: return

        when (checkPermission.action) {
            PermissionAction.CREATE -> {
                val resourceType = checkPermission.resourceType
                if (!user.canAccessDomain(resourceType, PermissionLevel.WRITE)) {
                    throw CustomException(ErrorCode.PERMISSION_DENIED)
                }
            }

            PermissionAction.UPDATE,
            PermissionAction.DELETE,
            -> {
                val resource = resolveArgumentResource(joinPoint, checkPermission)
                val requiredLevel =
                    if (checkPermission.action == PermissionAction.UPDATE) {
                        PermissionLevel.WRITE
                    } else {
                        PermissionLevel.ADMIN
                    }
                if (!permissionStrategy.check(user, resource, requiredLevel)) {
                    throw CustomException(ErrorCode.PERMISSION_DENIED)
                }
            }

            PermissionAction.READ_SINGLE,
            PermissionAction.READ_LIST,
            -> Unit
        }
    }

    @Around("@annotation(checkPermission)")
    fun execute(
        joinPoint: ProceedingJoinPoint,
        checkPermission: CheckPermission,
    ): Any? {
        val user = getCurrentUserIfApplicable() ?: return joinPoint.proceed()
        val isRead =
            checkPermission.action == PermissionAction.READ_SINGLE ||
                checkPermission.action == PermissionAction.READ_LIST

        if (!isRead) {
            return joinPoint.proceed()
        }

        val returnObject = joinPoint.proceed()

        if (checkPermission.action == PermissionAction.READ_SINGLE) {
            if (!permissionStrategy.check(user, returnObject, checkPermission.level)) {
                throw CustomException(ErrorCode.PERMISSION_DENIED)
            }
            return returnObject
        }
        // READ_LIST
        return when (returnObject) {
            is MutableCollection<*> -> {
                returnObject.removeIf { it == null || !permissionStrategy.check(user, it, checkPermission.level) }
                returnObject
            }

            is Collection<*> -> {
                returnObject
                    .filterNotNull()
                    .filter { permissionStrategy.check(user, it, checkPermission.level) }
            }

            else -> {
                error("READ_LIST expects a Collection but got: ${returnObject?.javaClass?.name}")
            }
        }
    }

    @AfterReturning(pointcut = "@annotation(checkPermission)", returning = "returnObject")
    fun afterExecute(
        joinPoint: JoinPoint,
        checkPermission: CheckPermission,
        returnObject: Any?,
    ) {
        if (checkPermission.action != PermissionAction.CREATE &&
            checkPermission.action != PermissionAction.DELETE
        ) {
            return
        }

        val user = getCurrentUserIfApplicable() ?: return
        val resourceId =
            when (checkPermission.action) {
                PermissionAction.CREATE -> returnObject?.toString()
                PermissionAction.DELETE -> joinPoint.args.firstOrNull()?.toString()
            } ?: return

        when (checkPermission.action) {
            PermissionAction.CREATE -> {
                userResourcePermissionService.create(user.requiredId, checkPermission.resourceType, resourceId)
            }

            PermissionAction.DELETE -> {
                userResourcePermissionService.delete(checkPermission.resourceType, resourceId)
            }
        }
    }

    private fun resolveArgumentResource(
        joinPoint: JoinPoint,
        checkPermission: CheckPermission,
    ): Any {
        val args = joinPoint.args
        val index = checkPermission.idParamIndex

        return object : Permissible {
            override val resourceType = checkPermission.resourceType
            override val resourceId = args[index].toString()
        }
    }

    private fun getCurrentUserIfApplicable(): User? {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw CustomException(ErrorCode.PERMISSION_DENIED)

        if (!authentication.isAuthenticated || SecurityConstants.ANONYMOUS_USER == authentication.principal) {
            throw CustomException(ErrorCode.PERMISSION_DENIED)
        }

        val user = userService.findUserByUsername(authentication.name)

        return if (user.getRoles().any { it.auth == RoleType.ADMIN.roleName }) null else user
    }
}
