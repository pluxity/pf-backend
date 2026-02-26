# pf-backend

Pluxity 백엔드 모노레포. 공통 인프라를 `common/` 모듈로 통합하고 각 프로젝트를 `apps/`에 배치.

## 기술 스택

| 항목          | 버전     |
|-------------|--------|
| Kotlin      | 2.2.10 |
| Java        | 21     |
| Spring Boot | 3.5.9  |
| Gradle      | 8.14.3 |

## 프로젝트 구조

```
pf-backend/
├── common/
│   ├── core/           # 공통 인프라 (entity, exception, response, utils, config)
│   ├── auth/           # JWT, Security, User, Role, Permission
│   ├── file/           # S3, Local 파일 업로드
│   ├── messaging/      # WebSocket, STOMP
│   └── test-support/   # 테스트 헬퍼, Dummy 팩토리
├── shared/
│   └── cctv/           # CCTV 도메인 (여러 앱에서 공유)
├── apps/
│   ├── safers/         # 기존 safers-api
│   └── yongin-platform/# 기존 plug-siteguard-api
└── gradle/
    └── libs.versions.toml  # 의존성 버전 카탈로그
```

### 모듈 의존 관계

```
common/messaging ──implementation──→ common/auth ──api──→ common/core
common/file      ──api─────────────→ common/core
```

- `common/` 모듈은 라이브러리로 빌드 (`bootJar` 비활성화)
- `shared/` 모듈은 여러 앱에서 공유하는 도메인 라이브러리 (`bootJar` 비활성화)
- `apps/` 모듈은 배포 가능한 Spring Boot 앱 (`bootJar` 활성화)

### 모듈 구성 가이드

#### `api` vs `implementation` 의존성

| 선언 방식            | 전이 노출              | 용도              |
|------------------|--------------------|-----------------|
| `api`            | 의존하는 모듈에도 클래스가 노출됨 | 공개 API에 사용되는 타입 |
| `implementation` | 내부에서만 사용, 외부에 숨김   | 내부 구현 상세        |

#### 앱 모듈 의존성 설정

각 앱에서 직접 사용하는 모듈은 **명시적으로 선언**해야 합니다:

```kotlin
// apps/safers/build.gradle.kts
dependencies {
    implementation(project(":common:auth"))        // User, Role, Permission, JWT
    implementation(project(":common:file"))        // FileService, StorageStrategy
    implementation(project(":common:messaging"))   // WebSocket, SessionManager

    testImplementation(project(":common:test-support"))
}
```

> `common/core`는 auth, file이 `api`로 노출하므로 **별도 선언 불필요**.
> auth나 file을 넣으면 core의 ErrorCode, CustomException, BaseResponse 등이 자동으로 사용 가능.

#### 전이적 의존성 정리

| 앱에서 선언                  | core       | auth                  | file | messaging |
|-------------------------|------------|-----------------------|------|-----------|
| `common/auth`만          | O (api 전이) | O                     | X    | X         |
| `common/file`만          | O (api 전이) | X                     | O    | X         |
| `common/messaging`만     | O (전이)     | X (implementation 숨김) | X    | O         |
| auth + file + messaging | O          | O                     | O    | O         |

> `common/messaging`은 내부적으로 `common/auth`를 사용하지만 `implementation`으로 선언되어 있어,
> 앱에서 auth 기능을 직접 사용하려면 **별도로 `common/auth`를 선언**해야 합니다.

#### Cross-module 빈 주입 경고 suppress

멀티모듈 구조에서 `common/` 모듈의 빈을 `apps/`에서 주입받을 때, IntelliJ가 빈을 찾지 못한다는 경고를 표시합니다.
런타임에는 `@PlatformApplication`의 `scanBasePackages`로 정상 주입되므로 `@Suppress`로 경고를 무시합니다:

```kotlin
@Component
class WeatherApiClient(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    webClientFactory: WebClientFactory,  // common/core에서 제공
)
```

> `@Suppress("SpringJavaInjectionPointsAutowiringInspection")`는 다른 모듈의 빈을 주입받는 생성자 파라미터에 붙입니다.

#### @ConfigurationProperties 등록

각 common/shared 모듈은 `@EnableConfigurationProperties`로 자체 Properties를 등록하므로,
앱에서는 `application.yml`에 설정값만 제공하면 됩니다:

| 모듈          | 등록 위치                  | Properties                                |
|-------------|------------------------|-------------------------------------------|
| common/auth | `CommonSecurityConfig` | `JwtProperties`, `UserProperties`         |
| common/auth | `CommonRedisConfig`    | `RedisProperties`                         |
| common/file | `FileStorageConfig`    | `FileProperties`, `S3Properties`          |
| shared/cctv | `CctvConfig`           | `CctvProperties`, `MediaServerProperties` |

## 빌드 & 실행

```bash
# 전체 빌드
./gradlew build

# 특정 앱만 빌드
./gradlew :apps:safers:build

# 특정 앱 실행
./gradlew :apps:safers:bootRun

# 실행 가능한 JAR 생성
./gradlew :apps:safers:bootJar
# → apps/safers/build/libs/safers-*.jar

# 코드 포맷 자동 수정 (ktlint)
./gradlew spotlessApply

# Docker 이미지 빌드 (프로젝트 루트에서 실행)
docker build -f apps/safers/Dockerfile -t safers-api .
```

## CI/CD (GitHub Actions)

`main` 브랜치에 push하면 **변경된 모듈만** 자동 배포됩니다.

### 동작 방식

1. **변경 감지** — `git diff`로 변경된 파일 경로를 분석
2. **모듈 필터링** — `.github/deploy-config.json` 설정을 읽어 변경된 모듈만 matrix로 구성
3. **병렬 배포** — Docker 이미지 빌드 → tar 저장 → 서버 전송 → `deploy.sh` 실행

### 배포 트리거 조건

| 변경 경로                                                               | 배포 대상               |
|---------------------------------------------------------------------|---------------------|
| `apps/safers/**`                                                    | safers만 배포          |
| `apps/yongin-platform/**`                                           | yongin-platform만 배포 |
| `common/**`, `gradle/**`, `build.gradle.kts`, `settings.gradle.kts` | **전체 모듈** 배포        |

### 모듈 배포 설정 (`.github/deploy-config.json`)

```json
[
  {
    "name": "safers",
    "dir": "safers-demo",
    "dockerfile": "apps/safers/Dockerfile",
    "image": "safers-api"
  }
]
```

| 필드           | 설명                                |
|--------------|-----------------------------------|
| `name`       | 모듈명 (`apps/` 하위 디렉토리명과 일치)        |
| `dir`        | 서버 배포 디렉토리 (`$DOCKER_PATH/{dir}`) |
| `dockerfile` | Docker 빌드에 사용할 Dockerfile 경로      |
| `image`      | Docker 이미지명                       |

> 새 앱 모듈 추가 시 이 파일에 항목만 추가하면 워크플로우 수정 없이 자동 배포됩니다.

## 패키지 규칙

| 모듈                   | 패키지                              |
|----------------------|----------------------------------|
| common/core          | `com.pluxity.common.core.*`      |
| common/auth          | `com.pluxity.common.auth.*`      |
| common/file          | `com.pluxity.common.file.*`      |
| common/messaging     | `com.pluxity.common.messaging.*` |
| shared/cctv          | `com.pluxity.cctv.*`             |
| apps/safers          | `com.pluxity.safers.*`           |
| apps/yongin-platform | `com.pluxity.yongin.*`           |

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

### @PlatformApplication

모든 앱 모듈에서 공통으로 필요한 4가지 어노테이션을 묶은 메타 어노테이션:

```kotlin
@EnableScheduling   // 앱별로 필요한 경우에만 추가
@PlatformApplication
class SafersApplication
```

`@PlatformApplication`은 아래 4개를 포함:

| 포함 어노테이션                                                     | 역할                               |
|--------------------------------------------------------------|----------------------------------|
| `@SpringBootApplication(scanBasePackages = ["com.pluxity"])` | common 모듈 컴포넌트 스캔                |
| `@EntityScan(basePackages = ["com.pluxity"])`                | common 모듈 JPA Entity 스캔          |
| `@EnableJpaRepositories(basePackages = ["com.pluxity"])`     | common 모듈 JPA Repository 스캔      |
| `@ConfigurationPropertiesScan`                               | `@ConfigurationProperties` 자동 등록 |

> `@SpringBootApplication`의 `scanBasePackages`는 `@ComponentScan`만 제어하고, JPA Entity/Repository 스캔은 별도로 지정해야 합니다.
> `@PlatformApplication`이 이를 한 번에 해결합니다.

### Swagger (ApiConfigurer)

`ApiConfigurer` 인터페이스를 구현하여 앱별 Swagger 설정을 커스터마이징:

```kotlin
@Configuration
class SafersApiConfigurer : ApiConfigurer {
    override fun openApiInfo(): Info =
        Info()
            .title("Safers API")
            .description("Safers Platform API Documentation")
            .version("1.0.0")

    @Bean
    fun siteApi(): GroupedOpenApi = apiGroup("5. 현장", "/sites/**")

    @Bean
    fun eventApi(): GroupedOpenApi = apiGroup("6. 이벤트", "/events/**")
}
```

- `ApiConfigurer`를 구현하지 않으면 `DefaultApiConfigurer`가 기본값("Pluxity Platform API") 제공
- `CommonApiConfig`가 공통 그룹(전체, 인증, 파일관리, 사용자)을 자동 등록
- 앱 전용 그룹은 `@Bean` 메서드로 직접 등록
- `apiGroup()` 헬퍼 함수로 `GroupedOpenApi`를 간결하게 생성

### WebClientFactory

외부 API 호출용 WebClient를 생성하는 팩토리. `spring-boot-starter-webflux`가 클래스패스에 있을 때만 활성화 (`@ConditionalOnClass`):

```kotlin
@Component
class WeatherApiClient(
    webClientFactory: WebClientFactory,
) {
    private val client = webClientFactory.createClient(
        baseUrl = "https://api.weather.go.kr",
        connectionTimeoutMs = 5000,   // 기본값
        responseTimeoutMs = 30000,    // 기본값
        maxInMemorySize = 50 * 1024 * 1024, // 기본값 50MB
    )
}
```

앱에서 사용하려면 `build.gradle.kts`에 webflux 의존성 추가:

```kotlin
implementation(rootProject.libs.spring.boot.starter.webflux)
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

### Security 허용 경로 확장 (SecurityPermitConfigurer)

`SecurityPermitConfigurer` 인터페이스를 구현하여 앱별로 `permitAll` 경로를 추가:

```kotlin
@Configuration
class YonginSecurityPermitConfigurer : SecurityPermitConfigurer {
    override fun permitPaths(): List<String> =
        listOf(
            "/weather/webhook",
        )
}
```

- 구현하지 않으면 기본 경로만 적용 (actuator, swagger, docs 등)
- 구현하면 `기본 경로 + 앱 경로`가 합쳐져 `permitAll` 처리

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

## common/file 제공 기능

### 파일 업로드/다운로드

- `FileController` (`/files/**`) — 업로드, 조회, Pre-signed URL 생성
- `FileService` — 파일 업로드(MultipartFile / byte[]), 영구 저장(finalize), 조회
- 파일 상태 관리: `TEMP` → `COMPLETE` (2단계 업로드)
- ZIP 파일 자동 분석: 루트 항목 메타데이터를 `ZipContentEntry` 테이블에 저장

### Storage Strategy (전략 패턴)

`file.storage-strategy` 설정에 따라 스토리지 구현체가 자동 선택:

| 설정값     | 구현체                    | 설명                                      |
|---------|------------------------|-----------------------------------------|
| `local` | `LocalStorageStrategy` | 로컬 파일시스템 저장 (`/files/**` 정적 리소스 서빙)     |
| `s3`    | `S3StorageStrategy`    | AWS S3 업로드 (ZIP 자동 해제 + Pre-signed URL) |

### S3 설정

- `S3ClientConfig` — S3Client 빈 (endpoint override, path style, Pinpoint 헤더 제거)
- `S3PresignerConfig` — S3Presigner 빈 (Pre-signed URL 생성용)
- `FileWebConfig` — 로컬 모드 시 정적 리소스 핸들러

### 유틸리티

| 클래스                     | 기능                                                       |
|-------------------------|----------------------------------------------------------|
| `FileUtils`             | 임시 파일 생성, 디렉토리 삭제, Content-Type 감지, 확장자 추출, 파일명 sanitize |
| `ZipUtils`              | ZIP 압축/해제 (경로 순회 공격 방어 포함)                               |
| `UUIDUtils`             | UUID 생성, 8자리 Short UUID                                  |
| `FileServiceExtensions` | `getFileMapById()`, `getFileMapByIds()` 확장 함수            |

### 설정 (application.yml)

```yaml
# 파일 업로드
file:
  storage-strategy: local  # local 또는 s3
  local:
    path: /data/uploads     # 로컬 저장 경로

  # S3 설정 (storage-strategy: s3 일 때)
  s3:
    bucket: my-bucket
    region: ap-northeast-2
    endpoint-url: https://s3.ap-northeast-2.amazonaws.com
    public-url: https://cdn.example.com
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}
    pre-signed-url-expiration: 3600  # 초 단위
```

## common/messaging 제공 기능

### WebSocket/STOMP 인프라

| 클래스                              | 역할                                                                        |
|----------------------------------|---------------------------------------------------------------------------|
| `WebSocketConfig`                | STOMP 엔드포인트(`/stomp/platform`), 메시지 브로커(`/topic`, `/queue`), Heartbeat 설정 |
| `AsyncConfig`                    | `@EnableAsync` + ThreadPool (core=5, max=10, queue=500) + Heartbeat 스케줄러  |
| `SessionManager`                 | 사용자별 WebSocket 세션 관리 (ConcurrentHashMap 기반)                               |
| `StompPrincipal`                 | STOMP 세션 Principal (UUID 기반)                                              |
| `MyDefaultHandshakeHandler`      | JWT 쿠키에서 사용자 인증 후 세션 속성에 username 저장                                      |
| `SocketApplicationEventListener` | 세션 연결/해제 이벤트 → SessionManager 등록/해제                                       |

### 앱별 확장 (각 앱에서 구현)

`StompMessageSender`와 `MessageHandler`는 앱마다 토픽/큐가 다르므로 각 앱에서 직접 구현:

```kotlin
// 브로드캐스트 전송
@Component
class StompMessageSender(private val messageTemplate: SimpMessagingTemplate) {
    fun sendEventCreated(payload: EventResponse) {
        messageTemplate.convertAndSend("/topic/events", payload)
    }
}

// 사용자 대상 전송 예시
@Component
class StompMessageSender(
    private val messageTemplate: SimpMessagingTemplate,
    private val sessionManager: SessionManager,  // common/messaging에서 제공
) {
    fun sendToUser(payload: Any, userIds: List<String>) {
        userIds.flatMap { sessionManager.findPrincipalByUserId(it) }
            .forEach { messageTemplate.convertAndSendToUser(it.name, "/queue/messages", payload) }
    }
}
```

### 모듈 의존 관계

```
common/messaging → common/auth (JWT 인증) → common/core
```

## shared/cctv 모듈

CCTV 도메인(엔티티, 서비스, 컨트롤러, 미디어서버 연동)을 여러 앱에서 공유하기 위한 모듈.

### 사용법

`build.gradle.kts`에 의존성 추가:

```kotlin
implementation(project(":shared:cctv"))
```

`application.yml`에 미디어서버 URL 설정 (필수):

```yaml
media-server:
  url: http://192.168.10.181:9997
```

즐겨찾기 최대 개수 변경 (선택, 기본값 4):

```yaml
cctv:
  max-bookmark-count: 4
```

## 앱별 고유 의존성

| 앱               | 고유 의존성                                |
|-----------------|---------------------------------------|
| safers          | Hibernate Spatial + JTS (Polygon WKT) |
| yongin-platform | —                                     |
