package com.pluxity.yonginplatform.announcement.service

import com.pluxity.yonginplatform.announcement.dto.dummyAnnouncementRequest
import com.pluxity.yonginplatform.announcement.entity.Announcement
import com.pluxity.yonginplatform.announcement.entity.dummyAnnouncement
import com.pluxity.yonginplatform.announcement.repository.AnnouncementRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class AnnouncementServiceTest :
    BehaviorSpec({

        val repository: AnnouncementRepository = mockk()
        val service = AnnouncementService(repository)

        Given("안내사항 조회") {

            When("저장된 안내사항이 있으면") {
                val entity = dummyAnnouncement(content = "저장된 안내사항")

                every { repository.findByIdOrNull(Announcement.SINGLETON_ID) } returns entity

                val result = service.getAnnouncement()

                Then("안내사항이 반환된다") {
                    result.content shouldBe "저장된 안내사항"
                    result.updatedAt shouldNotBe null
                }
            }

            When("저장된 안내사항이 없으면") {
                every { repository.findByIdOrNull(Announcement.SINGLETON_ID) } returns null

                val result = service.getAnnouncement()

                Then("빈 응답이 반환된다") {
                    result.content shouldBe null
                    result.updatedAt shouldBe null
                }
            }
        }

        Given("안내사항 저장") {

            When("기존 안내사항이 없으면") {
                val request = dummyAnnouncementRequest(content = "새로운 안내사항")
                val savedEntity = dummyAnnouncement(content = "새로운 안내사항")

                every { repository.findByIdOrNull(Announcement.SINGLETON_ID) } returns null
                every { repository.save(any()) } returns savedEntity

                service.saveAnnouncement(request)

                Then("새로운 안내사항이 저장된다") {
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("기존 안내사항이 있으면") {
                val existingEntity = dummyAnnouncement(content = "기존 안내사항")
                val request = dummyAnnouncementRequest(content = "수정된 안내사항")

                every { repository.findByIdOrNull(Announcement.SINGLETON_ID) } returns existingEntity

                service.saveAnnouncement(request)

                Then("기존 안내사항이 수정된다") {
                    existingEntity.content shouldBe "수정된 안내사항"
                    verify(exactly = 0) { repository.save(any()) }
                }
            }
        }
    })
