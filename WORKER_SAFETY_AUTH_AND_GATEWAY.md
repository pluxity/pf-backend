# 인증 보강 + safers-collect 구현 정리 (phase 1 — HQ 코로케이션)

> 기준 문서: `공사현장_관제시스템_아키텍처_v0.4.pdf` + `WORKER_SAFETY_API_DRAFT.md`
> 본 문서의 목적: v0.4 아키텍처에서 새로 도입된 **벤더키/장비키 하이브리드 인증** 과 **수집 모듈(`safers-collect`)** 구현 범위를 코드/스키마 단위로 정리.
> 이전 draft 의 "사이트별 단일 X-Api-Key" 모델은 폐기되고, **벤더 단위 + 장비 단위 2계층 키 모델** 로 교체됨.
> **phase 1 배포 모델**: v0.4 PDF 가 정의한 사이트별 필드 GW 가 아니라, **HQ 서버에 `safers` 와 함께 별도 프로세스로 코로케이션**. SQLite 영속 buffer 제거, in-memory queue + `messageId` UUID idempotency 로 대체. 사이트별 배포(필드 GW)는 phase 2.

---

## 1. 변경 요약 — v0.3 (draft) vs v0.4 (PDF)

| 항목 | 기존 draft (`WORKER_SAFETY_API_DRAFT.md`) | v0.4 PDF |
| --- | --- | --- |
| 인증 단위 | **사이트당 1키** (`X-Api-Key`) | **벤더당 1키 + 장비당 1키** (2계층) |
| 키 형식 | 미정 | `sf_{vk\|dk}_live_<43-char Base62>` (= 32-byte 엔트로피, §2.2) |
| 키 저장 | "키 → site.id 매핑" 테이블 (위치 미정) | **벤더키**: `vendor_key` 테이블 (`(vendor_id, site_id)` 단위 — site 직접 보유). **장비키**: `device` 테이블에 흡수 (1:1, phase 1 회전 없음). 모두 Hash-only |
| 디바이스 라이프사이클 | `ACTIVE` / `OFFLINE` 2상태 | `pending` → `active` ↔ `inactive` → `maintenance` / `retired` 5상태 |
| 헬스체크 | `last_seen_at` 만 | **Heartbeat + Watchdog** (장비별 임계치 차등) |
| 디바이스 등록 | 운영자 UI 또는 수집모듈 직접 | **벤더 셀프 등록 → 본사 승인** 워크플로우 |
| 현장 컴포넌트 명칭 | `safers-collect` (수집모듈) | **Pluxity Gateway** (벤더 어댑터). **HQ 서버 코로케이션** — phase 1 은 사이트 배포 X, `safers` 와 같은 호스트에 별도 프로세스로 구동 |
| 전송 프로토콜 (HQ↔collect) | HTTPS + 사이트별 X-Api-Key | **localhost HTTP, 인증 없음** — 같은 호스트 내 loopback 호출. HQ ingest 엔드포인트는 127.0.0.1 바인딩으로 외부 차단 (§2.10) |
| 전송 프로토콜 (장비↔collect) | HTTPS only | **HTTP / MQTT 양쪽 지원** (벤더별 어댑터) — 장비는 인터넷 경유로 collect 도메인에 접속, **응용 인증(vendor/device key)은 collect 에서 종결** |
| 중복 방어 | 없음 | **`messageId` UUID 기반 idempotency** — §2.11 / §3.3 |
| 영속 buffer | (해당 없음) | **제거됨** — 코로케이션이라 WAN 단절 시나리오 소멸. in-memory bounded queue + MQTT QoS 1 redeliver 로 충분 |

> 즉, **v0.4 의 핵심 변경점 두 개**: ① 인증 모델이 사이트→벤더/장비 2계층으로 정밀화 ② `safers-collect` 의 책임은 벤더 어댑터 + MQTT 인입 + idempotent forward 로 재정의 (영상·SQLite buffer·필드 배포는 phase 1 범위 외).

---

## 2. 인증 보강 — 벤더키 + 장비키 하이브리드

### 2.1 개념

| 키 종류 | 발급 단위 | 용도 | 만료 |
| --- | --- | --- | --- |
| **벤더 키** (`sf_vk_live_*`) | **(벤더 × 사이트) 1개** | 해당 사이트 디바이스 등록 API 호출 | **1년** |
| **장비 키** (`sf_dk_live_*`) | 장비 1개 (`device` 행에 흡수) | 텔레메트리·이벤트 송신 | **5년** (회전은 phase 2) |

> 권한은 **키 종류 × 헤더 이름 × endpoint path** 로 결정. phase 1 에서 키 종류와 endpoint 가 1:1 이라 별도 scope 컬럼/검증 없이 필터에서 헤더-path 매칭만으로 강제 (§2.4). scope 가 필요해지는 시점은 §4 phase 2 참조.

> 두 키는 **분리된 책임**. 벤더키가 유출되어도 **해당 사이트의 등록 한도** 만큼만 피해, 장비키 유출은 해당 장비 1대만 영향. site-level isolation 은 vendor_key.site_id 가 강제.
> 장비키는 `device_key` 테이블 분리 없이 **device 행 안에 흡수** (phase 1 1:1 강제). phase 2 회전 도입 시 별도 테이블로 분리 마이그레이션 (§4).

### 2.2 키 형식 (Format)

```
sf _ vk _ live _ 3a8f9k2dB7Mq4WnP6tR8sHxV9aZ1cD2eF3gH4iJ5kL
│    │    │      └── 43-char Base62 (= 32-byte 엔트로피)
│    │    └── 환경 (live | stage | dev)
│    └── 키 종류 (vk = vendor / dk = device)
└── 시스템 prefix (sf = safers)
```

- 전체 키 길이: prefix 11자 (`sf_vk_live_`) + Base62 43자 = **54자**
- 평문 노출은 **발급 시 1회 응답에서만**.
- DB 에는 **SHA-256 + per-key salt 해시** 만 저장. 평문 절대 미보존.
- **`key_id` = prefix 20자** (예: `sf_vk_live_3a8f9k2dB`) — lookup index 용. (DB `VARCHAR(20)` 와 일치)

### 2.3 검증 흐름 (collect 측, 요청 1건당)

> 응용 인증(vendor/device key) 의 검증은 **모두 collect 에서 종결**. HQ ↔ collect 는 localhost loopback 으로 인증 없이 통신 (§2.10). 따라서 HQ ingest 컨트롤러는 키 검증 책임이 없고, 대신 **HQ 가 키의 권위(authority) — 발급/회전/폐기/조회의 system of record**.
>
> | 키 | collect 측 | HQ 측 |
> | --- | --- | --- |
> | `X-Vendor-Key` (`sf_vk_live_*`) | **`VendorKeyCache` 검증** — hash 비교 + `key.site_id == body.siteId` 검증 후 통과만 HQ 에 forward (헤더 strip) | `vendor_key` 테이블이 system of record |
> | `X-Device-Key` (`sf_dk_live_*`) | **`DeviceCredentialStore` 검증** — (a) MQTT 브로커 인증 콜백 (§3.5.2), (b) HTTP 인입 시 헤더 검증 | `device` 테이블의 `key_*` 컬럼이 system of record. revoke/재발급 시 collect 캐시 무효화 |
>
> **권한·만료·회전·폐기의 권위는 HQ**. collect 캐시는 sync 지연이 있을 수 있으므로 변경 즉시 HQ 가 collect 에 invalidate 신호 (localhost call) 발송.

```text
collect inbound (HTTP 또는 MQTT 콜백)
  └─ Header: X-Vendor-Key 또는 X-Device-Key: sf_(vk|dk)_live_<...>
     (MQTT 의 경우 username/password 로 device key 전달 — §3.5.2)
                              │
0) request path → expected key type 결정 (`/v1/devices/register` → VENDOR, `/v1/collect/**` → DEVICE)
   기대 헤더 (`X-Vendor-Key` 또는 `X-Device-Key`) 누락/불일치 → 401
1) prefix 20자로 key_id 추출
2) (VendorKeyCache | DeviceCredentialStore) 에서 key_id 로 row lookup
3) 평문을 row.salt 와 함께 SHA-256 → row.key_hash 와 상수시간 비교
4) expires_at, status 검증
5) Rate limit 체크 (key_id 단위 분당 호출)
6) 키 종류별 추가 검증:
   - vendor key: body.siteId 가 있다면 무시 (key.site_id 가 권위) — 또는 body 에 아예 없도록 스펙 강제
   - device key: device.status == ACTIVE + 인입 path/topic 의 deviceId 와 캐시의 deviceId 일치
7) last_used_at 갱신 (collect 로컬 → 비동기로 HQ 에 audit 반영)
   ↓
collect controller / MqttSubscriber 진입
   ↓
adapter → envelope (messageId 부여, siteId 채움) → ForwardQueue
   ↓
forwarder → HQ ingest (no auth, localhost)
   ↓
HQ ingest controller 진입
   ↓
8) payload.messageId 로 idempotency 검증 — §2.11
9) Influx/PG 적재
```

> **키 → site 매핑**:
> - **vendor key**: `vendor_key.site_id` 가 직접 보유 — 벤더가 등록 시 임의 siteId 명시 못함
> - **device key**: `device.site_id` 가 권위 (단일 출처). 캐시는 `(deviceId, siteId, key_hash, ...)` 로 materialize 보관 → 핫패스 JOIN 없음
> HQ 는 collect 가 채워준 envelope 의 siteId 를 신뢰하고 그대로 적재.

### 2.4 보관/검증 체크리스트

| 항목 | 정책 | 구현 위치 |
| --- | --- | --- |
| 평문 보관 금지 | SHA-256 + per-key salt | `vendor_key.key_hash`, `device.key_hash` |
| 발급 시 1회 노출 | 응답 DTO 의 `apiKey` 필드, 이후 조회 API 에는 없음 | `VendorKeyService.issue()`, `DeviceService.register()` (device 행에 키 함께 채움) |
| key_id 인덱싱 | prefix 20자, B-tree unique | `vendor_key.key_id`, `device.key_id` |
| 만료 | `expires_at` 체크, 만료 시 401 | 검증 필터 |
| 권한 분기 | 헤더 이름(`X-Vendor-Key`/`X-Device-Key`) ↔ endpoint path 매칭. 잘못된 헤더 = 401 | `ApiKeyAuthFilter` 가 path → expected key type 매핑 |
| 벤더 한도 | `vendor_key.device_quota` — `(vendor, site)` 단위 등록 시 카운트 비교 | `DeviceRegisterService.register()` |
| Rate limit | Redis 기반 sliding window, 키별 분당 한도 | `ApiKeyRateLimiter` (필터 안) |
| 감사 로그 | `last_used_at` + 호출 이력(별도 테이블 or Redis stream) | `ApiKeyAuditWriter` |

> **회전·폐기는 phase 2 로 이연** (§4 참조). 본 단계에선 `status` 컬럼만 두되 값은 `ACTIVE` 1종만 사용.

### 2.5 신규/변경 도메인 모델 (v0.4 ERD 반영)

```
Vendor (1) ───── (N) VendorKey ────── (1) Site    -- VendorKey 가 (vendor, site) 단위
Site   (1) ───── (N) VendorKey
Vendor (1) ───── (N) Device
Site   (1) ───── (N) Device                       -- device.site_id 가 site 권위의 단일 출처
Device (1) ───── (1) [장비키 흡수]                -- device 행 안에 key_id/key_hash/... 컬럼
Device (1) ───── (N) Event   (event.device_id nullable — CCTV/사이트 단위 이벤트도 있어서)
```

> **invariant**: `device.site_id` 가 site 권위의 단일 출처. device 의 장비키는 device 행에 1:1 흡수 (phase 1). vendor_key 만 `(vendor_id, site_id)` 직접 보유 — vendor entity 가 site 속성을 가지지 않기 때문.

**`vendor`**
```sql
CREATE TABLE vendor (
  id             BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  vendor_id      VARCHAR(64)  NOT NULL,            -- 외부 노출용 식별자 (UUID 또는 slug)
  name           VARCHAR(128) NOT NULL,
  contact_email  VARCHAR(255),
  status         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SUSPENDED
  created_at     TIMESTAMP    NOT NULL,
  updated_at     TIMESTAMP    NOT NULL,
  CONSTRAINT uk_vendor_id UNIQUE (vendor_id)
);
```

**`vendor_key`**
```sql
CREATE TABLE vendor_key (
  id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  key_id        VARCHAR(20)  NOT NULL,             -- prefix 20자 (sf_vk_live_ 11자 + base62 9자), lookup용
  vendor_id     BIGINT       NOT NULL REFERENCES vendor(id),
  site_id       BIGINT       NOT NULL REFERENCES site(id),  -- (vendor, site) 단위 발급
  key_hash      CHAR(64)     NOT NULL,             -- SHA-256 hex
  salt          CHAR(32)     NOT NULL,
  device_quota  INTEGER      NOT NULL DEFAULT 1000,  -- (vendor, site) 단위 등록 한도
  status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  expires_at    TIMESTAMP    NOT NULL,
  last_used_at  TIMESTAMP,
  created_at    TIMESTAMP    NOT NULL,
  CONSTRAINT uk_vendor_key_key_id UNIQUE (key_id)
);
CREATE INDEX idx_vendor_key_vendor_site ON vendor_key (vendor_id, site_id);
-- (vendor, site) 별 active 키 1개 권장 (회전 시 잠시 2개 허용 — phase 2 에서 제거)
CREATE UNIQUE INDEX uk_vendor_key_active ON vendor_key (vendor_id, site_id) WHERE status = 'ACTIVE';
```

**`device` (v0.4 ERD 반영)**
- 기존 `device.metadata jsonb` 는 유지. **벤더/모델/시리얼/펌웨어** 는 jsonb 가 아닌 **정규 컬럼** 으로 승격 (조회·필터 빈도 ↑).
- 기존 `V20260429_001__create_device.sql` 은 **그대로 두고**, 별도 마이그레이션 파일에서 컬럼 추가 + status default 변경 + 부분 unique 인덱스 추가.

**신규 마이그레이션 — `V20260507_002__alter_device_add_vendor_and_key_fields.sql`**

```sql
-- v0.4: 벤더/모델/시리얼/펌웨어/설치일 정규 컬럼화 + status enum 5상태 확장 + 장비키 흡수
ALTER TABLE device
    ADD COLUMN vendor_id        BIGINT      REFERENCES vendor(id),
    ADD COLUMN model            VARCHAR(64),
    ADD COLUMN serial_number    VARCHAR(64),
    ADD COLUMN firmware_version VARCHAR(32),
    ADD COLUMN location         VARCHAR(128),
    ADD COLUMN installed_at     TIMESTAMP,
    -- 장비키 흡수 (phase 1 1:1). PENDING 상태에선 null 허용, ACTIVE 진입 시 NOT NULL 강제
    ADD COLUMN key_id           VARCHAR(20),
    ADD COLUMN key_hash         CHAR(64),
    ADD COLUMN key_salt         CHAR(32),
    ADD COLUMN key_expires_at   TIMESTAMP,
    ADD COLUMN key_last_used_at TIMESTAMP;

-- status default 변경: 'ACTIVE' → 'PENDING' (벤더 셀프 등록 → 본사 승인 → ACTIVE 워크플로우)
-- 값 도메인: PENDING | ACTIVE | INACTIVE | MAINTENANCE | RETIRED
ALTER TABLE device ALTER COLUMN status SET DEFAULT 'PENDING';

-- (vendor_id, serial_number) 부분 unique — 벤더당 시리얼 중복 금지
CREATE UNIQUE INDEX uk_device_vendor_serial ON device (vendor_id, serial_number)
    WHERE vendor_id IS NOT NULL AND serial_number IS NOT NULL;

-- 장비키 lookup index (collect 의 캐시 mass-fetch + 검증용)
CREATE UNIQUE INDEX uk_device_keyid ON device (key_id) WHERE key_id IS NOT NULL;

-- ACTIVE 상태에선 키 컬럼 모두 NOT NULL 강제
ALTER TABLE device ADD CONSTRAINT ck_device_active_has_key
  CHECK (status <> 'ACTIVE' OR (key_id IS NOT NULL AND key_hash IS NOT NULL AND key_salt IS NOT NULL));
```

> 실행 순서: `V20260507_001__create_vendor_and_keys.sql` (vendor 테이블 생성) → `V20260507_002__alter_device_add_vendor_fields.sql` (vendor_id FK 참조). Flyway 가 파일명 순으로 실행하므로 `001` → `002` 순서 보장됨.

**최종 device 모양 (참조용)**

```sql
-- 두 마이그레이션 적용 후의 device 테이블 — entity 매핑 시 참조
device (
  id                BIGINT PK,
  device_id         VARCHAR(64),
  type              VARCHAR(16),
  site_id           BIGINT,
  vendor_id         BIGINT REFERENCES vendor(id),    -- v0.4 추가
  name              VARCHAR(128),
  model             VARCHAR(64),                     -- v0.4 추가
  serial_number     VARCHAR(64),                     -- v0.4 추가
  firmware_version  VARCHAR(32),                     -- v0.4 추가
  location          VARCHAR(128),                    -- v0.4 추가
  status            VARCHAR(16) DEFAULT 'PENDING',   -- v0.4: default 변경
  last_seen_at      TIMESTAMP,
  installed_at      TIMESTAMP,                       -- v0.4 추가
  -- 장비키 흡수 (v0.4, phase 1 1:1)
  key_id            VARCHAR(20),                     -- prefix 20자, lookup용
  key_hash          CHAR(64),                        -- SHA-256 hex
  key_salt          CHAR(32),
  key_expires_at    TIMESTAMP,
  key_last_used_at  TIMESTAMP,
  metadata          JSONB,
  created_at        TIMESTAMP, updated_at TIMESTAMP,
  created_by        VARCHAR(255), updated_by VARCHAR(255),
  UNIQUE (device_id, site_id),
  UNIQUE (vendor_id, serial_number) WHERE 둘 다 NOT NULL,
  UNIQUE (key_id) WHERE key_id IS NOT NULL,
  CHECK (status <> 'ACTIVE' OR (key_id IS NOT NULL AND key_hash IS NOT NULL AND key_salt IS NOT NULL))
)
```

- `DeviceStatus.kt` 도 `PENDING | ACTIVE | INACTIVE | MAINTENANCE | RETIRED` 로 정의.
- `Device` entity 안에 `@Embedded DeviceKey` value object 로 매핑 권장 (key_* 컬럼들을 한 묶음으로 노출). 분리 마이그레이션 시 entity 만 분리하면 됨.
- `key_scopes` 컬럼은 phase 1 에서 **추가하지 않음** — 권한이 키 종류 = device key 로 결정되어 redundant. phase 2 scope 도입 시 컬럼 추가 마이그레이션.

### 2.6 등록 플로우 (v0.4 시퀀스)

```
1. 본사 Admin → Vendor: 사이트별 벤더키 사전 발급 (1회, 오프라인 전달)
   POST /v1/sites/{siteId}/vendors/{id}/keys → { apiKey, vendorId, siteId, expiresAt }
2. Vendor → collect API: POST /v1/devices/register
     Header: X-Vendor-Key: sf_vk_live_<...>
     Body:   { type, vendor, model, serialNumber, ... }   -- siteId 는 키에서 도출, body 에 포함 X
3. collect: VendorKeyCache 검증 (key.site_id 추출) → 통과 시 헤더 strip + body 에 siteId 보강 후
   HQ 로 forward (localhost, no auth)
4. HQ: 단일 트랜잭션으로 device 행 INSERT
     - status = PENDING
     - vendor_id, site_id, model, serial_number, ... 채움
     - key_id, key_hash, key_salt, key_expires_at 도 동시에 채움 (sf_dk_live_*)
5. HQ → collect → Vendor: { deviceId, deviceApiKey: sf_dk_live_<...> }   ← 1회 노출
   (collect 가 응답 가로채서 DeviceCredentialStore 에 (deviceId, siteId, hash) 캐시)
6. Vendor: 장비 펌웨어/설정에 deviceApiKey 주입 (OTA 또는 출고 시점)
7. HQ Admin Dashboard: status=PENDING 디바이스 목록 표시
8. Admin → HQ API: PATCH /v1/sites/{siteId}/devices/{id}/approve  → status=ACTIVE
   (관리자 세션 인증, HQ 가 즉시 collect 캐시에 status 변경 통지)
9. 장비 → collect: 텔레메트리/이벤트 송신 시작
     HTTP: Header X-Device-Key: sf_dk_live_<...>
     MQTT: username=deviceId, password=sf_dk_live_<...> (§3.5.2)
10. collect → HQ: forward (no auth, localhost) → InfluxDB/PG 적재
11. (이후) HQ Watchdog: last_seen_at 임계 초과 시 INACTIVE 자동 전환 + 알람 이벤트 생성
```

> **siteId 결정의 권위**: 벤더키 → site (path/key 에서 도출). body 에 siteId 명시 X — 벤더가 임의로 다른 사이트 등록 시도 차단.

### 2.7 신규 컨트롤러/엔드포인트

**HQ (`apps/safers`) — 인증은 관리자 세션 또는 인증 없음 (localhost only)**

| 엔드포인트 | Method | 인증 | 설명 |
| --- | --- | --- | --- |
| `/v1/vendors` | POST | 관리자 세션 | 벤더 등록 |
| `/v1/sites/{siteId}/vendors/{id}/keys` | POST | 관리자 세션 | **(vendor, site) 단위** 벤더키 발급 (응답에 평문 1회) |
| `/v1/sites/{siteId}/vendors/{id}/keys/{keyId}` | DELETE | 관리자 세션 | 벤더키 폐기 (phase 2 — phase 1 은 endpoint 만 존재, 실제 호출 X) |
| `/v1/sites/{siteId}/devices/{id}/approve` | PATCH | 관리자 세션 | PENDING → ACTIVE 승인 |
| `/v1/sites/{siteId}/devices/keys/sync` | GET | 관리자 세션 | collect 캐시 재구성용 — 사이트 active device 의 키 일괄 조회 (해시만) |
| `/v1/sites/{siteId}/vendor-keys/sync` | GET | 관리자 세션 | collect 의 VendorKeyCache 재구성용 — 사이트 active 벤더키 일괄 조회 (해시) |
| `/v1/internal/devices/register` | POST | localhost only | collect 가 vendor key 검증 후 forward — device 생성 + 키 발급 단일 트랜잭션 |
| `/v1/internal/sites/{siteId}/telemetry` | POST | localhost only | collect 가 forward — Influx 적재 |
| `/v1/internal/sites/{siteId}/events` | POST | localhost only | collect 가 forward — 이벤트 저장 |
| `/v1/internal/sites/{siteId}/devices/{id}/ping` | POST | localhost only | heartbeat — last_seen_at 갱신 |
| `/v1/internal/sites/{siteId}/devices/{id}` | PATCH | localhost only | 장비 자가 보고 (firmware version 등) |

**collect (`apps/safers-collect`) — 응용 인증 (vendor/device key) 종결 지점**

| 엔드포인트 | Method | 인증 | 설명 |
| --- | --- | --- | --- |
| `/v1/devices/register` | POST | 벤더키 (`X-Vendor-Key`) | 벤더 셀프 등록 — collect 검증 후 HQ `/v1/internal/devices/register` 로 forward |
| `/v1/collect/telemetry` | POST | 장비키 (`X-Device-Key`) | HTTP 인입 (MQTT 옵션 §3.5) |
| `/v1/collect/events` | POST | 장비키 | 이벤트 인입 |
| `/v1/collect/ping` | POST | 장비키 | 빈도 낮은 장비 heartbeat |
| `/v1/collect/devices/{id}` | PATCH | 장비키 | 장비 자가 보고 |
| `/internal/mqtt-auth` | POST | broker 공유 토큰 | EMQX 인증 콜백 (§3.5.2) |
| `/internal/mqtt-acl` | POST | broker 공유 토큰 | EMQX ACL 콜백 (§3.5.3) |

> HQ `/v1/internal/*` 는 Spring `server.address: 127.0.0.1` 또는 별도 connector 로 loopback 바인딩. collect 외에는 호출 불가하므로 인증 생략 정당화.

### 2.8 헬스 — Heartbeat + Watchdog

| 메커니즘 | 출처 | 동작 |
| --- | --- | --- |
| **Heartbeat** | 장비 → 본사 | 텔레메트리 자체를 heartbeat 로 간주. 빈도 낮은 장비(화재감지기 등)는 별도 `POST /v1/sites/{siteId}/devices/{id}/ping` 10분 간격 |
| **Watchdog** | 본사 자동 | 스케줄러가 주기적으로 `last_seen_at > threshold` 인 디바이스를 `ACTIVE → INACTIVE` 전환 + `BAND_OFFLINE` 같은 알람 이벤트 자동 생성 |

**장비별 임계치 (`device_type` → threshold)**

```kotlin
// safers/ingest/health/WatchdogProperties.kt (제안)
@ConfigurationProperties("watchdog")
data class WatchdogProperties(
    val thresholds: Map<DeviceType, Duration> = mapOf(
        DeviceType.BAND       to Duration.ofMinutes(5),    // 웨어러블
        DeviceType.GAS        to Duration.ofMinutes(30),   // 가스/화재
        DeviceType.CCTV       to Duration.ofMinutes(5),
        DeviceType.VIDEO_AI   to Duration.ofMinutes(1),
    ),
    val scanInterval: Duration = Duration.ofMinutes(1),
)
```

### 2.9 모듈별 구현 추가/변경 항목

#### HQ (`apps/safers`) — 키의 권위 (system of record), 응용 인증 검증 책임 없음

| 위치 | 작업 |
| --- | --- |
| `safers/ingest/auth/vendor/` | (신규) `VendorService`, `VendorKeyService` (issue/revoke), `VendorKey` 엔티티 — `(vendor_id, site_id)` 단위. 발급/조회만, 검증 X |
| `safers/ingest/entity/Device.kt` | `DeviceStatus` enum 5상태 확장 + vendor/model/serial/firmware/location/installedAt 컬럼 추가 + **`@Embedded DeviceKey` value object** (key_id/key_hash/key_salt/key_expires_at/key_last_used_at) |
| `safers/ingest/auth/device/` | (신규) `DeviceKeyIssuer` — device 등록/재발급 시 키 생성 + Device entity 의 `@Embedded` 필드에 채움. 별도 entity X |
| `safers/ingest/auth/sync/` | (**신규**) `KeySyncController` — collect 캐시 재구성용 sync endpoint, revoke/재발급 시 collect 에 invalidate 통지 (`POST collect:8081/internal/cache/invalidate`) |
| `safers/ingest/internal/` | (신규) `/v1/internal/*` 컨트롤러 — collect 가 호출하는 forward 수신 (telemetry/events/register/ping/self-PATCH). `IdempotencyFilter` (§2.11) 만 적용 |
| `safers/ingest/health/` | (신규) `WatchdogScheduler`, `WatchdogProperties` — last_seen_at 임계 초과 감지 |
| `safers/global/config/InternalEndpointBindingConfig.kt` | (**신규**) `/v1/internal/**` 는 별도 connector 로 127.0.0.1 바인딩 → 외부 노출 차단 |
| `db/migration/V20260507_001__create_vendor_and_keys.sql` | (신규) `vendor`, `vendor_key` 테이블 (vendor_key 에 site_id 포함, `uk_vendor_key_active`) — **device_key 테이블 없음** |
| `db/migration/V20260507_002__alter_device_add_vendor_and_key_fields.sql` | (신규) `device` 에 vendor 필드 + **장비키 흡수 컬럼** (key_id/key_hash/key_salt/...) + status default 변경 + 부분 unique + ACTIVE 시 키 NOT NULL CHECK |

#### collect (`apps/safers-collect`) — 응용 인증 검증의 종결 지점

| 위치 | 작업 |
| --- | --- |
| `safers-collect/auth/` | (**신규**) `ApiKeyAuthFilter` (path → expected key type → 헤더 검증), `ApiKeyResolver`, `ApiKeyAuditWriter` |
| `safers-collect/auth/vendor/` | (신규) `VendorKeyCache` — HQ 에서 sync 한 vendor key hash 캐시 (in-memory, 기동 시 + revoke 통지 시 갱신) |
| `safers-collect/auth/device/` | (신규) `DeviceCredentialStore` — 등록 응답 가로채기 + sync API 로 캐시 구성. revoke 통지 수신 시 즉시 무효화 |
| `safers-collect/auth/internal/` | (신규) `CacheInvalidateController` — `POST /internal/cache/invalidate` HQ 의 revoke 통지 수신 (localhost) |

### 2.10 HQ ↔ collect 통신 — 인증 없음 (loopback)

> phase 1 은 `safers` 와 `safers-collect` 가 **같은 호스트의 별도 프로세스**. 둘 사이 통신은 localhost loopback 으로 한정되며, **응용 인증(vendor/device key)은 collect 단에서 종결**되므로 HQ 측에 별도 전송 인증을 두지 않는다.

#### 2.10.1 모델

| 계층 | 키 | 보유처 | 검증 위치 |
| --- | --- | --- | --- |
| 응용 (등록) | 벤더키 `sf_vk_live_*` | 벤더사 → HTTP 헤더 | **collect** (`VendorKeyCache`) — HQ 에 forward 전 strip |
| 응용 (장비 신원) | 장비키 `sf_dk_live_*` | 장비 (펌웨어) | **collect** (`DeviceCredentialStore`) — MQTT auth 콜백 / HTTP 헤더 |
| 전송 (HQ↔collect) | — | — | **인증 없음** — loopback 바인딩으로 외부 차단 |

#### 2.10.2 격리

HQ 의 `/v1/internal/**` 라우트는 외부 노출되면 안 되므로 두 가지 중 하나로 강제:

- (권장) **별도 Tomcat connector 를 127.0.0.1 에 바인딩** — `/v1/internal/**` 는 이 connector 로만 매핑. 외부 요청은 라우트 자체에 도달 불가.
- 또는 application 전체를 `server.address: 127.0.0.1` 로 묶고, 외부 트래픽은 reverse proxy (nginx) 가 collect 로만 보냄.

```kotlin
// safers/global/config/InternalEndpointBindingConfig.kt
@Configuration
class InternalEndpointBindingConfig {
    @Bean
    fun internalConnector(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> =
        WebServerFactoryCustomizer { factory ->
            val connector = Connector("org.apache.coyote.http11.Http11NioProtocol").apply {
                port = 8081
                setProperty("address", "127.0.0.1")
            }
            factory.addAdditionalTomcatConnectors(connector)
        }
}
```

#### 2.10.3 헤더 사용 패턴

| 호출 | Header |
| --- | --- |
| 장비 → collect (HTTP 텔레메트리/이벤트/ping/self-PATCH) | `X-Device-Key: sf_dk_live_*` |
| 장비 → collect (MQTT) | username=deviceId, password=장비키 (§3.5.2) |
| 벤더 → collect (등록) | `X-Vendor-Key: sf_vk_live_*` |
| collect → HQ (`/v1/internal/*` 전체) | **헤더 없음** — body 안의 `deviceId` / `vendorId` 가 신원 표지 |

> collect 의 `ApiKeyAuthFilter` 는 응용 키 1단계 검증 후 통과만 forward. 검증 실패 시 401/403 → 벤더/장비에 직접 응답하며 HQ 까지 안 감.

#### 2.10.4 추후 확장 — 사이트별 GW 분리 시

phase 2 에서 collect 가 사이트별 필드 GW 로 분리되면 HQ ↔ GW 구간이 WAN 을 타게 됨. 그 시점에 **사이트별 GW키** 도입 (구조는 §2.5 `vendor_key` 와 유사, `gw_key` 테이블 + `site_id` FK + `rate_limit_per_min`). **본 단계 범위 외**, §4 phase 2 항목 참조.

### 2.11 idempotency — 중복 인입 방어

at-least-once 배달(MQTT QoS 1 redeliver / HTTP retry / collect→HQ retry) 때문에 **같은 메시지가 두 번 이상 도착할 수 있음**. 페이로드 내용 비교가 아니라 **명시적 `messageId` (UUID)** 로 dedup.

#### 2.11.1 책임 분리

| 측 | 책임 |
| --- | --- |
| 인입측 (장비 / 벤더 / collect) | envelope 에 `messageId: UUID` 부여 (§3.3.1). retry 해도 같은 ID 유지 |
| HQ ingest | `messageId` 본 적 있는지 확인 → 처음이면 처리, 중복이면 skip + 200 OK 반환 |

#### 2.11.2 검증 흐름 (HQ 측 — §2.3 controller 진입 직후)

```text
controller 진입 (인증 통과 후)
  ↓
1) payload.messageId 추출 (없으면 400)
2) Redis SETNX idem:{messageId} = "1" EX 300
   ├─ 성공 (첫 도착)  → Influx/PG 적재 → 200 OK
   └─ 실패 (이미 있음) → 적재 skip → 200 OK + duplicatesAccepted 메트릭 증가
```

**TTL 5분 근거**: MQTT QoS 1 redeliver 와 HTTP retry 는 통상 수초~수분 내. 5분이면 95%+ 케이스 커버. window 밖 중복은 매우 드물고 발생해도 시계열 데이터 1건 중복일 뿐 — 인전 손실보다 비용 우선.

#### 2.11.3 같은 키 / 다른 페이로드 케이스

벤더 버그로 같은 `messageId` 를 다른 페이로드에 재사용할 수 있음. 정책:

- 첫 페이로드의 hash 를 함께 캐시 (`SET idem:{id} = sha256(payload) EX 300`)
- 두 번째 도착 시 hash 비교 → 같으면 200 (정상 dedup), 다르면 **409 Conflict + 보안 알람**

#### 2.11.4 구현 위치

| 컴포넌트 | 위치 |
| --- | --- |
| `IdempotencyFilter` (인증 필터 다음 단계) | `safers/ingest/auth/IdempotencyFilter.kt` |
| Redis key 네임스페이스 | `idem:{messageId}` |
| 메트릭 | `safers_ingest_dedup_total{result=hit\|miss\|conflict}` |

#### 2.11.5 envelope 필수 필드 — `messageId` 누락 처리

신규 spec 이 박히기 전 운영 중인 벤더 펌웨어 / API 클라이언트 호환성:

- phase 1 시작 시점: collect 의 adapter 가 **누락된 `messageId` 자체 부여** (`UUID.randomUUID()`). 단 이 경우 **collect 재기동 시 동일 메시지에 다른 ID** 가 부여되어 dedup 효과 없음 → 운영 로그로 누락 비율 추적 + 벤더 펌웨어 갱신 push.
- phase 1 안정화 후: 누락 시 400 reject 로 강제.

---

## 3. `safers-collect` 구현 정리 (phase 1)

> v0.4 PDF 의 "Pluxity Gateway" 명명은 **필드 GW 모델** 을 전제로 하지만, phase 1 은 **HQ 코로케이션 별도 프로세스**. 명칭은 `safers-collect` 로 통일하고, "Pluxity Gateway" 는 phase 2 (사이트 배포) 에서 다시 등장.

### 3.1 역할 재정의 (phase 1 — HQ 코로케이션)

> **배포 모델 변경**: v0.4 PDF 는 `safers-collect` 를 사이트별 필드 GW 로 정의하지만, phase 1 은 **HQ 서버에 `safers` 와 함께 별도 프로세스로 구동**. 사이트별 배포는 phase 2 (회선 안정성 이슈가 실제로 발생한 사이트만 도입). 이 결정으로 SQLite 영속 buffer / 필드 OOB 관리 등이 범위에서 빠지고, 대신 **idempotency key 로 중복 방어** 가 들어옴.

| 책임 | v0.3 draft | v0.4 PDF (필드 GW) | **phase 1 (HQ 코로케이션)** |
| --- | --- | --- | --- |
| 벤더 어댑터 (정규화) | ✓ | ✓ | ✓ |
| `site_id` 부착 | ✓ | forwarder 가 path siteId 채움 | 등록 시점에 device→site 매핑 결정. forwarder 가 path 에 채움 |
| 회선 단절 buffer | in-memory + 디스크 spill | 48h SQLite 큐 | **제거** — in-memory bounded queue 만, MQTT QoS 1 redeliver 로 at-least-once 보장 |
| 장비 등록 API | ✓ | ✓ | ✓ |
| MQTT 인입 | (범위 외) | ✓ (broker 필드) | ✓ — broker 도 HQ 측에 위치 (장비는 인터넷 경유로 broker 도메인에 접속) |
| **idempotency** | 없음 | 없음 | **`messageId` UUID 인입측 부여 → HQ 가 Redis 5분 윈도우로 dedup** (§2.11) |

> 영상(현장 mediamtx / RTSP·RTMP·SRT / WebRTC), 방송장비 역방향 제어(HQ Proxy 경유) 는 본 모듈 범위에서 제외. `safers-collect` 는 센서/이벤트/등록만 책임.

### 3.2 패키지 구조 (v1 prefix 패키지 단위 — `WebConfig.addPathPrefix("/v1")`)

```
apps/safers-collect/.../
├── SafersCollectApplication.kt
├── config/
│   ├── WebConfig.kt
│   ├── OpenApiConfig.kt
│   └── GatewayProperties.kt       ★ central/queue/forwarder 설정 (site.id 없음 — 모든 사이트 처리)
├── v1/
│   ├── collect/                   (기존) 가스/밴드/SOS 수신
│   │   ├── controller/  dto/  enums/
│   │   └── adapter/               ★ 벤더 raw → 정규화 envelope (envelope 에 messageId 부여)
│   └── devices/                   (기존) 디바이스 CRUD
│       ├── controller/  dto/  enums/
│       └── adapter/
├── mqtt/                          ★ MQTT 인입 (HQ 측 EMQX broker 연계)
│   ├── MqttAuthController.kt      /internal/mqtt-auth, /internal/mqtt-acl (broker 콜백, localhost-only 바인딩)
│   ├── MqttSubscriber.kt          EMQX 구독 → 메시지 검증 → queue enqueue → HQ forward → broker ACK
│   └── MqttProperties.kt          broker URL / 토픽 패턴
├── queue/                         ★ in-memory bounded queue (SQLite 제거됨)
│   ├── ForwardQueue.kt            BlockingQueue 2개 — normal / immediate (SOS 등)
│   ├── BackpressurePolicy.kt      queue full 시 drop-oldest + metric 증가
│   └── QueueProperties.kt         capacity, dropPolicy
├── forwarder/                     ★ queue → HQ push (localhost HTTP)
│   ├── TelemetryHttpForwarder.kt  텔레메트리 batch → POST /v1/sites/{siteId}/telemetry
│   ├── EventHttpForwarder.kt      이벤트 → POST /v1/sites/{siteId}/events
│   ├── DeviceHttpForwarder.kt     디바이스 등록 pass-through / 자가 PATCH
│   ├── ImmediatePolicy.kt         SOS/낙상/임계초과 → immediate queue 로 라우팅
│   └── BackoffPolicy.kt           HQ 일시 다운 시 지수 백오프 (Resilience4j)
└── auth/                          ★ 응용 인증 검증의 종결 지점
    ├── ApiKeyAuthFilter.kt        path → expected key type 매핑 → 헤더(X-Vendor-Key / X-Device-Key) 검증 dispatch
    ├── ApiKeyResolver.kt          헤더 → key_id 추출 → 캐시 lookup → hash 비교
    ├── ApiKeyAuditWriter.kt       last_used_at 업데이트 비동기 → HQ 로 전송
    ├── vendor/
    │   └── VendorKeyCache.kt      HQ 에서 sync 한 (key_id, vendor_id, site_id, hash, salt, expires_at) 캐시.
    │                              검증 통과 시 request 에 site_id 주입 (downstream 이 신뢰)
    ├── device/
    │   └── DeviceCredentialStore.kt 장비 캐시 — (key_id, deviceId, siteId, hash, salt, expires_at, status)
    │                              materialized 보관 → 핫패스 lookup 1번. 등록 응답 가로채기 + sync API 로 구성
    └── internal/
        └── CacheInvalidateController.kt POST /internal/cache/invalidate (HQ 의 변경 통지 수신, localhost only)
```

> **변경점 요약**: ① `buffer/` 패키지 통째로 제거, `queue/` 로 대체 ② `mqtt/MqttSubscriber` 의 ACK 시점이 "queue enqueue 후" → **"HQ forward 성공 후"** 로 이동 (at-least-once 보장의 핵심) ③ adapter 가 envelope 만들 때 `messageId = UUID.randomUUID()` 부여 책임 명시 ④ **응용 인증 검증이 collect 단으로 이동** — `ApiKeyAuthFilter`/`VendorKeyCache` 신설, HQ↔collect 공유 키 인터셉터 폐기.

### 3.3 in-memory queue + idempotency

> v0.4 PDF 의 48h SQLite buffer 는 **HQ 코로케이션으로 의미 소멸** → 제거. 대신 **in-memory bounded queue + `messageId` UUID idempotency** 로 at-least-once 처리.

#### 3.3.1 envelope 스펙 (모든 인입 채널 공통)

```jsonc
{
  "messageId": "01JCXP-3a8f9k2dB7Mq4WnP",   // ULID/UUID — 인입측이 부여 (필수)
  "deviceId":  "BAND-A1B2C3",
  "siteId":    42,
  "type":      "TELEMETRY",                  // TELEMETRY | EVENT | LWT
  "ts":        "2026-05-06T10:14:50.123Z",
  "data":      { /* 정규화된 페이로드 */ }
}
```

`messageId` 발급 책임:

| 인입 | 발급자 | 비고 |
|---|---|---|
| 장비 → MQTT broker | **장비** (벤더 펌웨어) | publish 페이로드에 박아서. 누락 시 broker ACL 통과해도 collect 에서 reject |
| 벤더 → collect HTTP | **벤더** | API 스펙으로 강제. 누락 시 400 |
| collect → HQ forward | (재발급 X) | 인입 시 받은 값 그대로 전달 — retry 해도 같은 ID |
| collect 가 자체 생성하는 메시지 (LWT 변환 등) | **collect** | UUID 자체 생성 |

`siteId` 결정 책임:

| 인입 | siteId 출처 |
|---|---|
| 장비 → MQTT / HTTP | **collect adapter 가 부착** — `DeviceCredentialStore` 에 보관된 device→site 매핑 lookup. 장비는 `deviceId` 만 보고하면 됨 |
| 벤더 → collect (등록) | request body 의 `siteId` (벤더가 명시) |
| collect → HQ forward | envelope 그대로 — HQ 는 collect 가 채운 siteId 신뢰 |

> 장비가 자기 siteId 를 모르는 게 정상. device 가 어느 site 에 속하는지는 등록 시점에 결정되어 HQ device 테이블 + collect 캐시 양쪽에 박힘.

#### 3.3.2 in-memory queue 정책

```
인입 → adapter → envelope (messageId 검증/부여) → ForwardQueue
                                                     │
                                          ┌──────────┴──────────┐
                                          │ normal (LinkedBlockingQueue, cap=10000) │
                                          │ immediate (LinkedBlockingQueue, cap=1000) │
                                          └──────────┬──────────┘
                                                     │
                                          forwarder thread × N
                                                     │
                                                     ▼
                                          POST localhost:8080/v1/...
```

- **bounded** — capacity 초과 시 `drop-oldest` (동시에 metric 증가, 운영 알람). 무한 큐는 OOM 위험.
- **immediate queue 우선** — SOS/낙상/임계초과는 normal queue 무시하고 즉시 dispatch (~100ms 목표)
- **재시도** — HQ 일시 다운 시 Resilience4j 지수 백오프, 큐에 다시 enqueue. attempts 카운터는 envelope metadata 에.
- **broker ACK 시점 (MQTT 한정)** — `forwarder.dispatch()` 가 HQ 200 응답 받은 후에만 broker 에 ACK. 그 전에 collect 죽으면 broker 가 redeliver → HQ idempotency 가 중복 차단.

#### 3.3.3 손실 시나리오 vs SQLite 모델 비교

| 시나리오 | SQLite (구) | in-memory queue (신) |
|---|---|---|
| HQ 다운 (분 단위) | SQLite 가 흡수 | queue 가 흡수 (capacity 한계) |
| HQ 다운 (시간 단위) | 48h 까지 보존 | queue 가득 차면 drop. **현재 같은 호스트라 시나리오 비현실적** |
| collect 크래시 후 재기동 | 디스크에서 복구 | in-flight 메시지는 broker redeliver (MQTT) / 벤더 retry (HTTP) 로 회복 |
| HQ 와 collect 동시 다운 | 인입측 retry 에 의존 | 인입측 retry 에 의존 (동등) |

→ phase 1 환경에서는 SQLite 가 보호하던 시나리오가 모두 다른 메커니즘으로 커버됨.

### 3.4 forwarder — 분기 전송

| 메시지 종류 | 전송 채널 | 주소 | 헤더 |
| --- | --- | --- | --- |
| 텔레메트리 batch | **127.0.0.1:8081** | `POST /v1/internal/sites/{siteId}/telemetry` | 없음 (body 의 `deviceId` / `messageId` 가 신원·idempotency 키) |
| 이벤트 (가스 임계 / SOS / 낙상) | 127.0.0.1:8081 | `POST /v1/internal/sites/{siteId}/events` | 없음 |
| 디바이스 등록 (벤더 pass-through) | 127.0.0.1:8081 | `POST /v1/internal/devices/register` | 없음 — collect 가 X-Vendor-Key 검증 후 strip |
| 디바이스 자가 PATCH | 127.0.0.1:8081 | `PATCH /v1/internal/sites/{siteId}/devices/{id}` | 없음 — collect 가 X-Device-Key 검증 후 strip |
| heartbeat (빈도 낮은 장비) | 127.0.0.1:8081 | `POST /v1/internal/sites/{siteId}/devices/{id}/ping` | 없음 |

> HQ ↔ collect 구간은 loopback (127.0.0.1:8081) 으로 한정 (§2.10.2). 응용 키는 collect 단에서 검증 종결 → HQ forward 시 헤더 strip. MQTT 는 장비 ↔ HQ-측 broker 구간에서만 사용 — §3.5 참조.

**immediate forward** (SOS·낙상·임계초과): `ForwardQueue.immediate` 로 enqueue, forwarder 가 batch 무시하고 즉시 dispatch (~100ms 목표). batch 와 immediate 는 별도 worker thread.

**재시도/dedup** — HQ 가 5xx / timeout 응답 시 forwarder 가 같은 envelope 으로 retry. `messageId` 가 동일하므로 HQ 가 중복으로 적재하지 않음 (§2.11). 재시도 소진 정책은 §3.6 참조.

### 3.5 MQTT 인입 채널 — 장비 ↔ HQ-측 broker

장비 → collect 의 인입 채널 옵션. HTTP 와 양쪽 지원. broker 는 **HQ 서버의 Docker 컨테이너 (EMQX 권장)** 로 운영 — 장비는 인터넷을 통해 broker 도메인(예: `mqtt.safers.pluxity.com:8883`) 에 접속. collect 는 같은 호스트 docker network 안에서 broker 의 인증 콜백 수신 + 메시지 subscribe 두 가지로 연계.

> 필드 GW 모델과의 차이: broker 가 HQ 측에 있으므로 **TLS (8883) 필수**, 외부 노출 포트, 클라이언트 인증서 옵션 검토. 1883 평문 포트는 운영 환경에서 차단.

#### 3.5.1 토픽 구조

```
device/{deviceType}/{deviceId}/data       텔레메트리
device/{deviceType}/{deviceId}/event      이벤트 (SOS / 낙상 / 임계초과)
device/{deviceType}/{deviceId}/lwt        last-will (연결 끊김)
```

collect `MqttSubscriber` 는 와일드카드 1개씩 구독:

```
device/+/+/data    device/+/+/event    device/+/+/lwt
```

> 토픽에 timestamp / messageId 같은 매번 바뀌는 값 절대 금지 — broker subscription 매칭 trie 폭증. **`messageId` 는 페이로드 안에** 박는다 (§3.3.1 envelope 참조).

#### 3.5.2 인증 — `username = deviceId`, `password = 평문 장비키`

장비 CONNECT:

```
clientId: BAND-A1B2C3
username: BAND-A1B2C3                                   ← deviceId
password: sf_dk_live_3a8f9k2dB7Mq4WnP6tR8sHxV9...        ← 평문 장비키
```

EMQX 가 매 CONNECT 시 collect 의 HTTP auth 콜백을 호출. collect 는 `DeviceCredentialStore` 로 검증.

```kotlin
// mqtt/MqttAuthController.kt — localhost-only 바인딩, 외부 노출 금지
@PostMapping("/internal/mqtt-auth")
fun authenticate(@RequestBody req: AuthRequest): ResponseEntity<Void> {
    val deviceId = req.username
    if (deviceId != req.clientid) return ResponseEntity.status(401).build()
    val cred = store.findActiveByDeviceId(deviceId) ?: return ResponseEntity.status(401).build()
    if (!cred.matches(req.password)) return ResponseEntity.status(401).build()
    if (cred.expired) return ResponseEntity.status(401).build()
    return ResponseEntity.ok().build()
}
```

> phase 1 은 **1 device = 1 active key** 가정 — `findActiveByDeviceId` single 반환. 회전·폐기 도입 시 list 반환 + sync 메커니즘으로 확장 (§4 참조).

#### 3.5.3 ACL — 자기 deviceId 토픽만 publish

```kotlin
private val topicPattern = Regex("""^device/[^/]+/([^/]+)/(data|event|lwt)$""")

@PostMapping("/internal/mqtt-acl")
fun authorize(@RequestBody req: AclRequest): ResponseEntity<Void> {
    if (req.action != "publish") return ResponseEntity.status(403).build()
    val match = topicPattern.matchEntire(req.topic) ?: return ResponseEntity.status(403).build()
    val topicDeviceId = match.groupValues[1]
    return if (topicDeviceId == req.username) ResponseEntity.ok().build()
           else ResponseEntity.status(403).build()
}
```

> `contains("/${username}/")` 같은 substring 검사는 username 에 `/` 가 들어가거나 토픽이 변형되면 false-allow 위험. **regex strict match** 로 토픽 구조 + deviceId 일치를 동시에 강제.

EMQX 결과 캐싱(기본 5분) 으로 매 CONNECT/publish 마다 호출되지 않음.

#### 3.5.4 EMQX 설정 예시

`docker-compose.yml` (HQ 호스트 — `safers` / `safers-collect` 와 동일 네트워크):

```yaml
services:
  emqx:
    image: emqx/emqx:5.7
    ports:
      - "8883:8883"           # TLS 만 외부 노출
      # 1883 (평문) 외부 미노출 — 운영 환경에선 publish 시도 자체 차단
    volumes:
      - ./emqx.conf:/opt/emqx/etc/emqx.conf:ro
      - ./certs:/opt/emqx/etc/certs:ro
    networks: [pluxity-hq]

  safers-collect:
    # ... (생략)
    networks: [pluxity-hq]    # 같은 네트워크 — broker 의 auth 콜백이 collect:8080 으로 접근
```

`emqx.conf`:

```hocon
authentication = [
  {
    mechanism = password_based
    backend   = http
    method    = post
    url       = "http://safers-collect:8080/internal/mqtt-auth"
    body { username = "${username}", password = "${password}", clientid = "${clientid}" }
    headers {
      content-type = "application/json"
      x-broker-shared-token = "${SAFERS_BROKER_SHARED_TOKEN}"   # broker→collect 추가 인증
    }
  }
]

authorization {
  sources = [
    {
      type   = http
      method = post
      url    = "http://safers-collect:8080/internal/mqtt-acl"
      body { username = "${username}", topic = "${topic}", action = "${action}" }
    }
  ]
}
```

> **보호 레이어**: ① `/internal/*` 는 컨테이너 내부 네트워크에서만 reachable (외부 publish 차단), ② `x-broker-shared-token` 헤더로 broker→collect 호출 출처 검증 (네트워크 내 다른 컨테이너 도용 방지), ③ collect 의 `MqttAuthController` 는 token 헤더 검증 후에만 처리.

#### 3.5.5 LWT — 연결 끊김 즉시 감지

장비가 CONNECT 시 미리 등록 → broker 가 1.5×keepalive 후 dead 판정 시 자동 publish.

```
LWT topic:   device/band/BAND-A1B2C3/lwt
LWT payload: { "type":"OFFLINE", "reason":"unclean-disconnect" }
qos: 1, retain: false
```

collect `MqttSubscriber` 가 `device/+/+/lwt` 구독 → `BAND_OFFLINE` 이벤트로 변환 (`messageId = UUID.randomUUID()` 자체 부여) → ForwardQueue enqueue → HQ forward. **Watchdog 임계(30분) 기다릴 필요 없이 즉시 감지.**

#### 3.5.6 yml 설정

```yaml
gateway:
  mqtt:
    enabled: true
    broker-url: tcp://emqx:1883             # 같은 docker network 내부 — collect → broker 구독용 (장비는 외부에서 8883/TLS)
    client-id: collect-subscriber           # 단일 인스턴스. HA 도입 시 instance ID 부여
    broker-shared-token: ${SAFERS_BROKER_SHARED_TOKEN:dev-broker-token-change-me}
    subscribe-topics:
      - device/+/+/data
      - device/+/+/event
      - device/+/+/lwt
```

### 3.6 queue/forwarder 설정 (yml 예시)

```yaml
gateway:
  central:
    base-url: ${SAFERS_INTERNAL_BASE_URL:http://127.0.0.1:8081}  # HQ 의 loopback connector (§2.10.2)
    # api-key 없음 — HQ↔collect 인증 생략 (loopback)
  queue:
    normal-capacity: 10000
    immediate-capacity: 1000
    drop-policy: DROP_OLDEST                                  # 큐 가득 시 가장 오래된 메시지 drop + metric 증가
  forwarder:
    batch-size: 100
    flush-interval: 1s
    gzip: true
    max-retry-attempts: 5                                     # HQ 5xx 시 지수 백오프 후 결정
    on-retry-exhausted:
      telemetry: DROP_AND_ALARM                               # 시계열 1건 손실 허용 + metric/알람
      event:     DLQ_FILE                                     # SOS/낙상 등은 로컬 파일 DLQ 로 보존, 운영자 수동 복구
    immediate-event-types:
      - SOS_TRIGGERED
      - GAS_THRESHOLD_EXCEEDED
      - BAND_FALL_DETECTED
```

> `gateway.site.id` **제거됨** — collect 가 모든 사이트를 처리. siteId 는 envelope / device 등록 매핑에서 결정.
>
> **재시도 소진 정책**: 같은 호스트라 HQ 가 5번 연속 5xx 인 케이스는 보통 HQ 프로세스 다운/배포 중. 텔레메트리는 1건 손실 허용 (다음 배치가 곧 옴), **immediate event 는 DLQ 파일로 보존** — SOS/낙상 1건 누락은 운영 사고이므로 절대 silent drop 금지.

### 3.7 의존성 추가 (`apps/safers-collect/build.gradle.kts`)

```kotlin
dependencies {
    // 현재
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // 추가 — phase 1 (HQ 코로케이션)
    implementation("com.hivemq:hivemq-mqtt-client:1.3.3")                   // MQTT 5 client (HQ-측 EMQX subscribe)
    implementation("org.springframework.boot:spring-boot-starter-webflux")  // forwarder WebClient (localhost HTTP)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0") // backoff/circuit
}
```

> phase 1 은 영속 저장소 X — `sqlite-jdbc`, `HikariCP` 모두 불필요. 기존 build.gradle 의 PostgreSQL/Flyway/JPA 제외 정책 유지.

### 3.8 헬스 노출 (운영 모니터링용)

`GET /v1/health` — 다음 항목을 반환해 본사 모니터링이 collect 상태 확인.

```json
{
  "uptime": "PT53H12M",
  "queue": {
    "normalDepth": 12, "normalCapacity": 10000,
    "immediateDepth": 0, "immediateCapacity": 1000,
    "droppedTotal": 0
  },
  "forwarder": {
    "lastSuccessAt": "2026-05-06T10:14:51",
    "consecutiveFailures": 0,
    "duplicatesAccepted": 3      // HQ 가 중복으로 판정해 dedup 한 건수 (정보용)
  },
  "mqtt": { "connected": true, "lastReceivedAt": "2026-05-06T10:14:50" }
}
```

> `siteId` 필드 제거됨 — phase 1 의 collect 는 모든 사이트를 처리. 사이트별 통계가 필요하면 `GET /v1/health/sites/{siteId}` 별도 엔드포인트로 분리 (phase 2).

---

## 4. 미정/협의 항목 (PDF Open Items 반영)

### 인증
- [ ] 벤더키 발급 채널 (관리자 UI vs CLI vs OOB)
- [ ] HQ → collect vendor/device key sync 메커니즘 — push webhook (HQ→collect localhost call) vs polling vs SSE. 기동 시 일괄 fetch + 변경 시 invalidate 가 단순
- [ ] collect 재기동 시 `DeviceCredentialStore` 재구성 — HQ 의 `GET /v1/sites/{siteId}/devices/keys/sync` 응답 포맷 (해시만 노출, 평문 X)
- [ ] 감사 로그 저장 위치 — `api_key_audit` 테이블 vs Redis stream + S3 archival
- [ ] collect 의 `ApiKeyAuthFilter` rate limit 저장소 — Redis 의존 vs 로컬 token bucket

### 인증 — phase 2 (회전·폐기 + device_key 분리)
> phase 1 에선 키 회전·폐기 자체를 미구현. 운영 안정화 후 다음 항목 일괄 도입:
- [ ] **`device_key` 테이블 분리 마이그레이션** — phase 1 은 device 행에 흡수, 회전 도입 시점에 별도 테이블로 추출:
  ```sql
  CREATE TABLE device_key ( id, device_id, key_id, key_hash, key_salt, status, expires_at, last_used_at, created_at, revoked_at );  -- scope 도입 시 key_scopes 추가
  INSERT INTO device_key (device_id, key_id, key_hash, ...)
    SELECT id, key_id, key_hash, ... FROM device WHERE key_id IS NOT NULL;
  ALTER TABLE device DROP COLUMN key_id, DROP COLUMN key_hash, ...;
  -- @Embedded DeviceKey 제거 → @OneToMany DeviceKey 로 entity 변경
  ```
- [ ] 키 회전 정책 — 주기 (벤더키 1년 / 장비키 5년 default?), 신구 동시 유효 윈도우 (제안: 7일)
- [ ] 키 폐기 워크플로우 — `status='REVOKED'` + `revoked_at` 컬럼, `/v1/sites/{siteId}/vendors/{id}/keys/{keyId}` DELETE, `/v1/sites/{siteId}/devices/{id}/keys/rotate` 추가
- [ ] **회전 윈도우 동안 1 device = 2 active key** 허용 → device_key 분리 후 `uk_device_key_active` 제거 + §3.5.2 MQTT auth 가 list 반환으로 둘 중 매칭 통과
- [ ] **회전 윈도우 동안 (vendor, site) = 2 active vendor key** 허용 → `uk_vendor_key_active` 제거
- [ ] HQ → collect 캐시 sync — revoke 즉시 invalidate, rotate 시 둘 다 push
- [ ] **scope 부활** — 다음 중 하나라도 발생 시 scope 컬럼 + `@RequiredScope` 어노테이션 + `ScopeCheckInterceptor` 동시 도입:
  - 읽기 전용 device key 추가 (모니터링 도구용 — `telemetry:read`)
  - vendor 권한 분리 (일부 벤더는 `device:register` 만, 일부는 `device:revoke` 도)
  - OAuth-style 위임 (외부 사용자에게 일부 권한만 제공)

### safers-collect (phase 1)
- [ ] MQTT broker — EMQX self-hosted vs HiveMQ SaaS (현재 §3.5 는 EMQX 가정)
- [ ] broker 외부 노출 도메인 / TLS 인증서 (Let's Encrypt vs 사설 CA)
- [ ] `messageId` 누락 시 phase 1 정책 — collect 자체 부여 vs 즉시 400 reject (§2.11.5)
- [ ] DLQ 파일 포맷 / 위치 / rotation — §3.6 `on-retry-exhausted: DLQ_FILE` 의 구체 스펙

### MQTT broker 보안 (phase 1, 인터넷 노출)
- [ ] CONNECT rate limit — 같은 deviceId 분당 N회 초과 시 일시 차단 (벤더키 brute force 방지)
- [ ] mTLS 옵션 — phase 1 은 deviceId/장비키만으로 갈지, 클라이언트 cert 도 강제할지
- [ ] abuse 모니터링 — 인증 실패 spike 자동 알람 + IP/clientId 차단
- [ ] EMQX shared subscription 도입 시점 — collect HA 1→2대 확장 시

### Pluxity Gateway (phase 2 — 사이트 배포)
- [ ] 도입 트리거 — 어느 사이트가 회선 안정성 이슈로 GW 가 필요한지 판정 기준
- [ ] 폼팩터 — 산업용 IPC vs 일반 PC, 전원, 0~50°C 작동 인증
- [ ] OOB 관리망 / OTA 업데이트 SLA
- [ ] phase 1 → phase 2 마이그레이션 — 영속 buffer 재도입 (SQLite), HTTPS 외부 도메인 전환, **사이트별 GW키** (§2.10.4 — `gw_key` 테이블 도입, GW→HQ WAN 구간 인증 부활)

### 운영
- [ ] 관제실 24×7 인력 / 사고 대응 SOP
- [ ] DR / 이중화 범위
- [ ] RPO/RTO (현재 PDF 는 PostgreSQL Primary+Standby 스트리밍 복제, RPO 수초)
