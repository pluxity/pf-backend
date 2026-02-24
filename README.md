# pf-backend

Pluxity 백엔드 모노레포. 공통 인프라를 `common/` 모듈로 통합하고 각 프로젝트를 `apps/`에 배치.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Kotlin | 2.2.10 |
| Java | 21 |
| Spring Boot | 3.5.9 |
| Gradle | 8.14.3 |

## 프로젝트 구조

```
pf-backend/
├── common/
│   ├── core/           # 공통 인프라 (entity, exception, response, utils, config)
│   ├── auth/           # JWT, Security, User, Role, Permission
│   ├── file/           # S3, Local 파일 업로드
│   ├── messaging/      # WebSocket, STOMP
│   └── test-support/   # 테스트 헬퍼, Dummy 팩토리
├── apps/
│   ├── gs-auth/        # 기존 plug-platform-api project/gs/refactor/permission-check
│   ├── safers/         # 기존 safers-api
│   └── yongin-platform/# 기존 plug-siteguard-api
└── gradle/
    └── libs.versions.toml  # 의존성 버전 카탈로그
```

### 모듈 의존 관계

```
apps/*  →  common/auth  →  common/core
        →  common/file  →  common/core
        →  common/messaging → common/core
        →  common/test-support (testImplementation)
```

- `common/` 모듈은 라이브러리로 빌드 (`bootJar` 비활성화)
- `apps/` 모듈은 배포 가능한 Spring Boot 앱 (`bootJar` 활성화)

## 빌드 & 실행

```bash
# 전체 빌드
./gradlew build

# 특정 앱만 빌드
./gradlew :apps:safers:build

# 특정 앱 실행
./gradlew :apps:safers:bootRun

# 코드 포맷 자동 수정 (ktlint)
./gradlew spotlessApply
```

## 패키지 규칙

| 모듈 | 패키지 |
|---|---|
| common/core | `com.pluxity.common.core.*` |
| common/auth | `com.pluxity.common.auth.*` |
| common/file | `com.pluxity.common.file.*` |
| common/messaging | `com.pluxity.common.messaging.*` |
| apps/gs-auth | `com.pluxity.gsauth.*` |
| apps/safers | `com.pluxity.safers.*` |
| apps/yongin-platform | `com.pluxity.yonginplatform.*` |

> ktlint 규칙: 패키지명에 언더스코어(`_`) 사용 금지. `safety_equipment` (X) → `safetyequipment` (O)

## common/core 제공 기능

### Entity 베이스 클래스

```kotlin
// 감사 필드 자동 관리 (createdAt, updatedAt, createdBy, updatedBy)
class MyEntity : IdentityIdEntity() {
    var name: String = ""
}
```

### ErrorCode & CustomException

`CustomException`은 `Code` 인터페이스를 받으므로 앱별 에러 코드를 정의할 수 있음.

```kotlin
// common/core에 정의된 공통 에러 코드 사용
throw CustomException(ErrorCode.NOT_FOUND_USER, username)

// 앱 전용 에러 코드 정의
enum class SafersErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    NOT_FOUND_SITE(HttpStatus.NOT_FOUND, "ID가 %s인 현장을 찾을 수 없습니다."),
    ;
    override fun getHttpStatus() = httpStatus
    override fun getMessage() = message
    override fun getStatusName() = httpStatus.name
    override fun getCodeName() = name
}

throw CustomException(SafersErrorCode.NOT_FOUND_SITE, siteId)
```

### Swagger (CommonApiConfig)

기본 OpenAPI 설정이 `@ConditionalOnMissingBean`으로 등록됨.
앱에서 자체 `OpenAPI` 빈을 정의하면 자동으로 오버라이드:

```kotlin
@Configuration
class SafersApiConfig {
    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI().info(Info().title("Safers API").version("1.0.0"))
}
```

### @ResponseCreated

POST 엔드포인트에서 201 Created + Location 헤더 자동 생성:

```kotlin
@PostMapping
@ResponseCreated
fun create(@RequestBody request: CreateRequest): ResponseEntity<Long> {
    val id = service.create(request)
    return ResponseEntity.ok(id)  // AOP가 201 + Location: /{id} 로 변환
}
```

## 설정 (application.yml)

### Logbook 로그 제외 경로

기본 제외 경로: `/actuator/`, `/swagger-ui/`, `/api-docs/`, `/.well-known/`, `/springwolf/`

앱별로 추가 제외 경로를 설정:

```yaml
pluxity:
  logbook:
    exclude-paths:
      - /weather/webhook
      - /health/custom
```

## common/auth 제공 기능

### 인증 (JWT Cookie 기반)

- 로그인/로그아웃/회원가입/토큰 갱신 API (`/auth/**`)
- Access Token + Refresh Token (Redis 저장) 쿠키 기반 인증
- `JwtAuthenticationFilter`로 요청마다 자동 인증
- `WhiteListPath`에 등록된 경로는 인증 생략

### 사용자 & 역할 관리

- User, Role, UserRole, RolePermission 엔티티
- `UserController` (`/users/me`) — 본인 정보 조회/수정/비밀번호 변경
- `AdminUserController` (`/admin/users`) — 관리자용 CRUD
- `RoleController` (`/roles`) — 역할 CRUD

### 권한 시스템 (RBAC)

```
User → UserRole → Role → RolePermission → Permission
                                            ├── DomainPermission (리소스 타입 단위)
                                            └── ResourcePermission (개별 리소스 단위)
```

- `PermissionLevel`: READ → WRITE → ADMIN (상위 레벨이 하위 포함)
- `@CheckPermission` AOP로 컨트롤러 메서드에 권한 검사 적용
- `PermissionController` (`/permissions`) — 권한 CRUD + 리소스 타입 조회

### ResourceType 확장

`ResourceType`은 앱마다 다르므로 `ResourceTypeRegistry` 인터페이스로 추상화.
각 앱에서 자체 리소스 타입을 등록:

```kotlin
// apps/safers에서 ResourceTypeRegistry 구현
@Component
class SafersResourceTypeRegistry : ResourceTypeRegistry {
    enum class SafersResourceType(
        val resourceName: String,
        val endpoint: String,
    ) {
        USER("사용자관리", "users"),
        ATTENDANCE_STATUS("출역현황", "attendances"),
        PROCESS_STATUS("공정현황", "process-statuses"),
    }

    override fun resolve(name: String): String =
        SafersResourceType.entries
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.name ?: throw CustomException(ErrorCode.INVALID_RESOURCE_TYPE, name)

    override fun allEntries(): List<ResourceTypeInfo> =
        SafersResourceType.entries.map {
            ResourceTypeInfo(key = it.name, resourceName = it.resourceName, endpoint = it.endpoint)
        }
}
```

### 설정 (application.yml)

```yaml
# JWT
jwt:
  access-token:
    name: access_token
    secret: your-secret-key
    expiration: 3600        # 초 단위
  refresh-token:
    name: refresh_token
    secret: your-refresh-secret-key
    expiration: 604800

# Redis
spring:
  data:
    redis:
      host: localhost
      port: 6379

# 비밀번호 초기화 기본값
user:
  init-password: "changeme123"
```

## 앱별 고유 의존성

| 앱 | 고유 의존성 |
|---|---|
| gs-auth | Flyway, Cron Utils, Actuator + Prometheus |
| safers | Hibernate Spatial + JTS (Polygon WKT) |
| yongin-platform | — |
