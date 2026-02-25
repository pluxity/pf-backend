package com.pluxity.yongin.notice.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDate

@Entity
@Table(name = "notice")
class Notice(
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "content", length = 1000)
    var content: String?,
    @Column(name = "is_visible", nullable = false)
    @ColumnDefault("false")
    var isVisible: Boolean = false,
    @Column(name = "is_always", nullable = false)
    @ColumnDefault("false")
    var isAlways: Boolean = false,
    @Column(name = "start_date")
    var startDate: LocalDate? = null,
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
) : IdentityIdEntity() {
    fun update(
        title: String,
        content: String,
        isVisible: Boolean,
        isAlways: Boolean,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) {
        this.title = title
        this.content = content
        this.isVisible = isVisible
        this.isAlways = isAlways
        this.startDate = startDate
        this.endDate = endDate
    }
}
