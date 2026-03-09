package com.pluxity.yongin.attendance.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.attendance.entity.Attendance
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class AttendanceCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : AttendanceCustomRepository {
    override fun findAllOrderByDateDescIdAsc(pageable: Pageable): Page<Attendance> =
        executor.findPageNotNull(pageable) {
            select(entity(Attendance::class))
                .from(entity(Attendance::class))
                .orderBy(
                    path(Attendance::attendanceDate).desc(),
                    path(Attendance::id).asc(),
                )
        }
}
