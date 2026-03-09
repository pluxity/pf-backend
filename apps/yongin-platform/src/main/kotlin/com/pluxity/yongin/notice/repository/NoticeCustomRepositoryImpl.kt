package com.pluxity.yongin.notice.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.notice.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

class NoticeCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : NoticeCustomRepository {
    override fun findAllOrderByIdDesc(pageable: Pageable): Page<Notice> =
        executor.findPageNotNull(pageable) {
            select(entity(Notice::class))
                .from(entity(Notice::class))
                .orderBy(path(Notice::id).desc())
        }

    override fun findAllActive(today: LocalDate): List<Notice> =
        executor.findAllNotNull {
            select(entity(Notice::class))
                .from(entity(Notice::class))
                .where(
                    and(
                        path(Notice::isVisible).equal(true),
                        or(
                            path(Notice::isAlways).equal(true),
                            and(
                                or(
                                    path(Notice::startDate).isNull(),
                                    path(Notice::startDate).lessThanOrEqualTo(today),
                                ),
                                or(
                                    path(Notice::endDate).isNull(),
                                    path(Notice::endDate).greaterThanOrEqualTo(today),
                                ),
                            ),
                        ),
                    ),
                ).orderBy(path(Notice::id).desc())
        }
}
