package com.pluxity.yongin.attendance.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_attendance_date_device_name",
            columnNames = ["attendance_date", "device_name"],
        ),
    ],
)
class Attendance(
    @Column(name = "attendance_date", nullable = false)
    val attendanceDate: LocalDate,
    @Column(name = "device_name", nullable = false)
    val deviceName: String,
    @Column(name = "attendance_count", nullable = false)
    var attendanceCount: Int,
    @Column(name = "work_content")
    var workContent: String? = null,
) : IdentityIdEntity() {
    fun updateAttendanceCount(count: Int) {
        this.attendanceCount = count
    }

    fun updateWorkContent(content: String) {
        this.workContent = content
    }
}
