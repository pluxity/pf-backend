package com.pluxity.yongin.attendance.service

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.attendance.client.AttendanceApiClient
import com.pluxity.yongin.attendance.dto.AttendanceExternalData
import com.pluxity.yongin.attendance.dto.AttendanceUpdateRequest
import com.pluxity.yongin.attendance.dto.dummyAttendance
import com.pluxity.yongin.attendance.entity.Attendance
import com.pluxity.yongin.attendance.repository.AttendanceRepository
import com.pluxity.yongin.global.constant.YonginErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class AttendanceServiceTest :
    BehaviorSpec({

        mockkStatic("com.pluxity.common.core.utils.KotlinJdslExtensionsKt")

        val repository: AttendanceRepository = mockk(relaxed = true)
        val apiClient: AttendanceApiClient = mockk()
        val service = AttendanceService(repository)
        val facade = AttendanceFacade(service, apiClient)

        Given("최신 출역현황 조회") {

            When("데이터가 존재하면") {
                val entities =
                    listOf(
                        dummyAttendance(id = 1L, deviceName = "1공구"),
                        dummyAttendance(id = 2L, deviceName = "2공구"),
                    )

                every { repository.findAllByLatestDate() } returns entities

                val result = service.findLatest()

                Then("출역현황 목록이 반환된다") {
                    result.size shouldBe 2
                    result[0].deviceName shouldBe "1공구"
                    result[1].deviceName shouldBe "2공구"
                }
            }

            When("데이터가 없으면") {
                every { repository.findAllByLatestDate() } returns emptyList()

                val result = service.findLatest()

                Then("빈 목록이 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("작업내용 수정") {

            When("출역현황이 존재하면") {
                val entity = dummyAttendance(id = 1L, workContent = null)
                val request = AttendanceUpdateRequest(workContent = "콘크리트 타설 작업")

                every { repository.findByIdOrNull(1L) } returns entity

                service.updateWorkContent(1L, request)

                Then("작업내용이 수정된다") {
                    entity.workContent shouldBe "콘크리트 타설 작업"
                }
            }

            When("출역현황이 존재하지 않으면") {
                every { repository.findByIdOrNull(999L) } returns null

                val request = AttendanceUpdateRequest(workContent = "작업내용")

                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.updateWorkContent(999L, request)
                        }
                    exception.code shouldBe YonginErrorCode.NOT_FOUND_ATTENDANCE
                }
            }
        }

        Given("출역현황 조회 및 동기화") {
            val today = LocalDate.now()
            val request = PageSearchRequest(page = 1, size = 10)

            When("외부 API에 신규 데이터가 있으면") {
                val externalData =
                    listOf(
                        AttendanceExternalData(deviceName = "1공구", attendanceCount = 10),
                        AttendanceExternalData(deviceName = "2공구", attendanceCount = 20),
                    )
                val savedEntities =
                    listOf(
                        dummyAttendance(id = 1L, deviceName = "1공구", attendanceCount = 10),
                        dummyAttendance(id = 2L, deviceName = "2공구", attendanceCount = 20),
                    )

                every { apiClient.fetchAttendanceData() } returns externalData
                every { repository.findByAttendanceDateAndDeviceNameIn(today, any()) } returns emptyList()
                every {
                    repository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Attendance>>>(),
                    )
                } returns PageImpl(savedEntities)

                val result = facade.findAllWithSync(request)

                Then("동기화 후 목록이 반환된다") {
                    result.content.size shouldBe 2
                    verify { repository.saveAll(any<List<Attendance>>()) }
                }
            }

            When("외부 API에 기존 데이터 업데이트가 있으면") {
                val existingEntity = dummyAttendance(id = 1L, deviceName = "1공구", attendanceCount = 5)
                val externalData =
                    listOf(
                        AttendanceExternalData(deviceName = "1공구", attendanceCount = 15),
                    )

                every { apiClient.fetchAttendanceData() } returns externalData
                every { repository.findByAttendanceDateAndDeviceNameIn(today, any()) } returns listOf(existingEntity)
                every {
                    repository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Attendance>>>(),
                    )
                } returns PageImpl(listOf(existingEntity))

                facade.findAllWithSync(request)

                Then("기존 데이터가 수정된다") {
                    existingEntity.attendanceCount shouldBe 15
                    verify { repository.saveAll(any<List<Attendance>>()) }
                }
            }

            When("외부 API 데이터가 없으면") {
                every { apiClient.fetchAttendanceData() } returns emptyList()
                every {
                    repository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Attendance>>>(),
                    )
                } returns PageImpl(emptyList())

                facade.findAllWithSync(request)

                Then("동기화가 수행되지 않는다") {
                    verify(exactly = 0) { repository.saveAll(any<List<Attendance>>()) }
                }
            }
        }
    })
