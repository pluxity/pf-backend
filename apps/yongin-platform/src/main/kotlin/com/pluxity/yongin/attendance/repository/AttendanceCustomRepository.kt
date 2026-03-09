package com.pluxity.yongin.attendance.repository

import com.pluxity.yongin.attendance.entity.Attendance
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AttendanceCustomRepository {
    fun findAllOrderByDateDescIdAsc(pageable: Pageable): Page<Attendance>
}
