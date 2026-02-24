package com.pluxity.common.auth.permission

import org.springframework.data.jpa.repository.JpaRepository

interface DomainPermissionRepository : JpaRepository<DomainPermission, Long>
