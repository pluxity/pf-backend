package com.pluxity.yongin.attendance.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.yongin.attendance.entity.Attendance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface AttendanceRepository :
    JpaRepository<Attendance, Long>,
    KotlinJdslJpqlExecutor {
    fun findByAttendanceDateAndDeviceNameIn(
        attendanceDate: LocalDate,
        deviceNames: List<String>,
    ): List<Attendance>

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = (SELECT MAX(a2.attendanceDate) FROM Attendance a2) ORDER BY a.id ASC")
    fun findAllByLatestDate(): List<Attendance>
}
