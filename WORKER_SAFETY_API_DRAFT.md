# 근로자 안전 데이터 연동 API 규격 (초안)

> **수집모듈(외부 현장 배포)** 과 **중앙 ingest(`safers`)** 의 2단계 구조.
> - **수집모듈** 은 센서 종류별로 데이터 / 이벤트 / 디바이스 CRUD 엔드포인트를 노출 (외부에 다양하게).
> - **중앙(`safers`)** 은 정규화된 통합 단일 API 3개만 노출 (모두 `/v1/sites/{siteId}/...` 하위): `telemetry`, `events`, `devices` (안쪽은 단일하게).
> - 새 센서 종류 도입 = **수집모듈에 어댑터 1세트 추가**. 중앙 코드 / DB 스키마 / 운영 UI 무변경.
> - 인증: **센서 ↔ 수집모듈은 인증 없음** (사이트 LAN 격리로 보호), **수집모듈 ↔ 중앙은 `X-Api-Key`**.

```text
[현장 — 외부 / 사이트 LAN 내부]                              [중앙 — 사내]
┌───────────────┐                                            ┌─────────────────────────────────┐
│ 가스센서      │──HTTP (인증 없음)─┐                        │ pf-backend (safers)             │
│ 스마트밴드    │──HTTP (인증 없음)─┤                        │                                 │
│ SOS 디바이스  │──HTTP (인증 없음)─┤    수집모듈            │  POST /v1/sites/{siteId}/       │──▶ InfluxDB
│ (벤더 GW)     │                  ├──▶ /collect/gas        │       telemetry                  │
└───────────────┘                  │   /collect/band        │  POST /v1/sites/{siteId}/        │──▶ events (PG, CCTV+SAFETY 통합)
                                    │   /collect/sos/events │       events                     │
                                    │   /devices/gas  CRUD  │  CRUD /v1/sites/{siteId}/        │──▶ device (PG)
                                    │   /devices/bands CRUD │       devices                    │
                                    │   /devices/sos  CRUD  │   X-Api-Key 검증                  │
                                    └──HTTPS + X-Api-Key────┘   path siteId 강제 (위변조 차단) │
                                                            └─────────┬───────────────────────┘
                                                                      ▼
                                                              보강 + STOMP 브로드캐스트
```

> **왜 forward 모델?** 사이트는 외부 망, 중앙 DB 는 사내 망이라 사이트에서 DB 직접 접근 불가. 수집모듈이 raw 를 받아 정규화 + `site_id` 부착 후 중앙으로 push, 적재는 중앙에서만.

---

## 1. 연동 대상

| 구분 | 설명 | 3D맵 표출 |
| --- | --- | --- |
| 유해가스 측정경보 | 밀폐장소(맨홀, 탱크 내부 등)에 설치된 가스센서 측정값 | 밀폐장소 객체에 실시간 가스 농도 / 경보 상태 표출 |
| SOS 알림 | 근로자가 직접 발신하는 긴급호출 (밴드 SOS 버튼, 모바일 등) | 발신자 위치를 3D 좌표로 즉시 표출 |
| 스마트 밴드 | 근로자 착용 웨어러블 — 위치 / 체온 / 심박수 주기 전송 | 근로자 아이콘 위치 + 생체정보 라벨, 이상치 시 알림 표출 |

---

## 2. 설계 원칙

### 2.1 외부는 다양하게, 안쪽은 단일하게 (Adapter 경계)

수집모듈이 **벤더별 차이를 흡수하는 어댑터** 역할을 합니다. 중앙은 정규화된 단일 API 만 받습니다.

| 계층 | 다양한 부분 | 단일한 부분 |
| --- | --- | --- |
| 센서 / 벤더 GW → 수집모듈 | 프로토콜·payload·단위·시간 포맷 (벤더별) | — |
| 수집모듈 내부 | 어댑터 N개 (센서 종류만큼) | — |
| 수집모듈 → 중앙 | — | 정규화된 envelope 1종 + `sourceType` 디스크리미네이터 |
| 중앙 ingest API | — | `/v1/sites/{siteId}/{telemetry,events,devices}` 3개 |
| InfluxDB writer | — | 1개. `measurement` 만 envelope 에서 결정 |
| 디바이스 저장 | — | 단일 `device` 테이블 + `metadata jsonb` |

> **새 센서 추가 시 수정 범위**: 수집모듈에 어댑터 클래스 + 컨트롤러 1세트. 중앙·DB·관리 UI **무변경**.

### 2.2 3개 채널 (telemetry / events / devices)

저장소 특성과 호출 패턴이 다르므로 채널을 분리합니다.

| 채널 | 목적 | 호출 빈도 | 적재 대상 | 처리 방식 | 응답 |
| --- | --- | --- | --- | --- | --- |
| **데이터수집** (telemetry) | 정상 범위 측정값 시계열 적재 | 高 (수 초 ~ 수 분) | **InfluxDB** | 내부 버퍼 → 배치 write | `200 OK` |
| **이벤트수집** (events) | 임계 초과·SOS·낙상 등 사건 | 低 (사건 발생 시) | **PostgreSQL** | 동기 INSERT | `200 OK` |
| **디바이스 관리** (devices) | 등록·수정·삭제 (운영자 화면) | 매우 낮음 | **PostgreSQL** | 동기 CRUD | `200/201 OK` |

### 2.3 수집모듈은 "현장 게이트웨이"

수집모듈을 두는 본질적 이유는 단순 forwarding 이 아니라 **현장 컨텍스트 부여(site_id 부착)** 입니다.

- 디바이스(가스센서·밴드·SOS) 와 그 앞단의 벤더 게이트웨이는 자기 `device_id`/`bandId` 만 알고 **자기가 어느 현장에 있는지(`site_id`) 모름**.
- 수집모듈이 `site_id` 를 자동 부착해 중앙으로 forward → 중앙은 패킷만으로 현장 식별 끝.
- 부수 효과: ① 회선 단절 시 buffer ② 사이트별 배치 + gzip 으로 중앙 부하 ↓ ③ 잘못된 사이트 디바이스 송신 차단.

### 2.4 인증 모델 (확정)

| 구간 | 인증 | 비고 |
| --- | --- | --- |
| 센서 / 벤더 GW → 수집모듈 | **없음** | 사이트 LAN 내부에만 노출. 외부 인터넷 차단 (방화벽/네트워크 격리) |
| 수집모듈 → 중앙(`safers`) | **`X-Api-Key`** + HTTPS | 사이트별 1키. 키 → `site.id` 매핑은 중앙이 보유. 인증 통과 시 그 키의 `site.id` 를 중앙이 강제 부착 |
| 운영자 → 중앙 관리 화면 | 기존 사용자 인증 (관리자) | 본 문서 범위 외 |

> 센서/GW 측 인증을 두지 않는 대신 **네트워크 경계로 보호**. 수집모듈 포트는 외부 인터넷 노출 금지.
> 별도 IP allowlist 등 추가 강화는 §13 협의 항목.

---

## 3. 공통 규약

### 3.1 시간 포맷
- 모든 timestamp 는 **ISO-8601 (KST)**: `2026-04-24T10:15:30`
- 서버 수신 시각이 아니라 **장비 측정/발생 시각**.

### 3.2 명명 규약
- JSON 필드: **`camelCase`** (`eventId`, `workerId`, `occurredAt`)
- PostgreSQL 컬럼 / InfluxDB 태그·필드: 각 스토어 컨벤션에 따라 `snake_case` 유지 → 영속 계층에서 매핑.

### 3.3 위치 정보 — Raw vs Enriched

수집 단계에서는 **기기가 실제로 알 수 있는 정보(raw) 만** 받습니다. 3D 맵 표출에 필요한 보강 좌표(`facilityId`, `floor`, `local{x,y,z}`)는 **pf-backend 가 마스터 데이터와 join 해서 생성** (§7).

```json
{ "rawPosition": { "lat": 37.501234, "lng": 127.039876, "accuracyM": 3.5 } }
```

- 가스센서처럼 위치가 고정인 기기는 `rawPosition` 미전송 (설치 위치는 디바이스 마스터에서 조회).
- 실내 측위(BLE/UWB) 정보가 있다면 별도 협의 후 필드 확장.

### 3.4 응답 코드

| 코드 | 적용 | 의미 |
| --- | --- | --- |
| `200 OK` | telemetry / events 수집, devices 조회/수정/삭제 | 정상 접수 |
| `201 Created` | devices 등록 | 신규 생성 |
| `400 Bad Request` | 공통 | 스키마/검증 실패 |
| `401 Unauthorized` | 중앙 ↔ 수집모듈 구간 | API Key 없음/무효 |
| `404 Not Found` | devices | 존재하지 않는 디바이스 |
| `500 Internal Server Error` | 공통 | 서버 내부 오류 |

오류 응답 본문(예시):
```json
{ "code": "VALIDATION_ERROR", "message": "samples[0].deviceId is required", "timestamp": "2026-04-24T10:15:30" }
```

---

## 4. 수집모듈 API — 데이터수집 (센서별)

> 인증: **없음** (사이트 LAN 격리). Base path: `/v1/collect`.
> `/v1` prefix 는 `WebConfig.addPathPrefix` 로 자동 부여 — 컨트롤러는 `/collect/...` 만 선언 (§11.6).
> `samples[]` 배열로 단건/다건 모두 지원. **권장 운영은 측정 즉시 1건씩 송신** (실시간성 우선). 배치는 모듈 → 중앙 forward 단계에서만 (§12.5).

### 4.1 유해가스 측정값 — `POST /v1/collect/gas`

```json
{
  "samples": [
    {
      "deviceId": "GAS-MH203-01",
      "timestamp": "2026-04-24T10:15:30",
      "measurements": [
        { "gas": "O2",  "value": 20.8, "unit": "PERCENT" },
        { "gas": "H2S", "value": 3.1,  "unit": "PPM" },
        { "gas": "CO",  "value": 12.0, "unit": "PPM" },
        { "gas": "LEL", "value": 4.0,  "unit": "PERCENT" }
      ],
      "battery": 87,
      "signalRssi": -68
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `samples[].deviceId` | string | ✅ | 가스센서 고유 ID |
| `samples[].timestamp` | string | ✅ | 측정 시각 (ISO-8601) |
| `samples[].measurements[].gas` | enum | ✅ | `O2`, `H2S`, `CO`, `LEL`, `CO2`, `CH4` 등 |
| `samples[].measurements[].value` | number | ✅ | 측정값 |
| `samples[].measurements[].unit` | enum | ✅ | `PPM`, `PERCENT` |
| `samples[].battery` | int | ❌ | 0~100 |
| `samples[].signalRssi` | int | ❌ | dBm |

### 4.2 스마트밴드 측정값 — `POST /v1/collect/band`

```json
{
  "samples": [
    {
      "bandId": "BAND-A1B2C3",
      "timestamp": "2026-04-24T10:15:30",
      "rawPosition": { "lat": 37.501234, "lng": 127.039876, "accuracyM": 4.0 },
      "vitals": {
        "heartRateBpm": 88,
        "bodyTempC": 36.7,
        "spo2": 97,
        "step": 4321
      },
      "battery": 73,
      "wearState": "WORN"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `samples[].bandId` | string | ✅ | 밴드 시리얼 |
| `samples[].timestamp` | string | ✅ | 측정 시각 |
| `samples[].rawPosition` | object | ❌ | 기기 측정 raw 위치 |
| `samples[].vitals.heartRateBpm` | int | ❌ | 심박수 |
| `samples[].vitals.bodyTempC` | number | ❌ | 체온 (℃) |
| `samples[].vitals.spo2` | int | ❌ | 산소포화도 (%) |
| `samples[].wearState` | enum | ❌ | `WORN`, `OFF`, `UNKNOWN` |

> SOS 는 "사건" 이므로 데이터수집 API 에는 포함되지 않음 (§5.3 참조).

### 4.x 새 센서 종류 추가 시
센서 종류가 늘어나면 어댑터 컨트롤러를 한 개 추가합니다 — `POST /v1/collect/<type>`. 내부적으로 정규화 envelope 로 변환 후 동일한 buffer/forwarder 파이프라인을 탑니다. 중앙 코드는 무변경.

---

## 5. 수집모듈 API — 이벤트수집 (센서별)

> 인증: **없음**. 알림/대응이 필요한 사건만 전송. 수신 즉시 STOMP 토픽으로 브로드캐스트하여 3D맵에서 즉각 표출.

### 5.1 공통 이벤트 envelope

```json
{
  "eventId": "EVT-20260424-0001",
  "eventType": "GAS_THRESHOLD_EXCEEDED",
  "severity": "CRITICAL",
  "occurredAt": "2026-04-24T10:15:30",
  "source": "GAS_SENSOR",
  "rawPosition": { "lat": 37.501234, "lng": 127.039876, "accuracyM": 3.5 },
  "payload": { "...": "이벤트별 상세 (식별자 + 사건 정보)" }
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `eventId` | string | ✅ | 외부 시스템 발급 ID |
| `eventType` | enum | ✅ | `GAS_THRESHOLD_EXCEEDED`, `SOS_TRIGGERED`, `BAND_VITAL_ABNORMAL`, `BAND_FALL_DETECTED`, `BAND_OFFLINE` |
| `severity` | enum | ✅ | `INFO`, `WARNING`, `CRITICAL` |
| `source` | enum | ✅ | `GAS_SENSOR`, `SOS_DEVICE`, `SMART_BAND` |
| `rawPosition` | object | ❌ | 기기가 알 수 있는 raw 위치 |
| `payload` | object | ✅ | 이벤트 종류별 상세. **반드시 식별자(`deviceId`/`bandId`/`workerId`) 포함** |

### 5.2 가스 임계 초과 — `POST /v1/collect/gas/events`

가스센서는 위치가 고정이라 `rawPosition` 미전송. 중앙이 `deviceId` → 설치 위치 join.

```json
{
  "eventId": "GAS-EVT-20260424-0001",
  "eventType": "GAS_THRESHOLD_EXCEEDED",
  "severity": "CRITICAL",
  "occurredAt": "2026-04-24T10:15:30",
  "source": "GAS_SENSOR",
  "payload": {
    "deviceId": "GAS-MH203-01",
    "triggered": [
      { "gas": "H2S", "value": 25.4, "unit": "PPM",     "threshold": 10.0,  "level": "DANGER" },
      { "gas": "O2",  "value": 17.8, "unit": "PERCENT", "threshold": 19.5,  "level": "WARNING" }
    ]
  }
}
```

### 5.3 SOS 긴급호출 — `POST /v1/collect/sos/events`

```json
{
  "eventId": "SOS-EVT-20260424-0001",
  "eventType": "SOS_TRIGGERED",
  "severity": "CRITICAL",
  "occurredAt": "2026-04-24T10:15:30",
  "source": "SOS_DEVICE",
  "rawPosition": { "lat": 37.501234, "lng": 127.039876, "accuracyM": 3.5 },
  "payload": {
    "bandId": "BAND-A1B2C3",
    "trigger": "BUTTON_LONG_PRESS",
    "vitals": { "heartRateBpm": 132, "bodyTempC": 37.4 },
    "memo": null
  }
}
```

| 필드 | 설명 |
| --- | --- |
| `payload.bandId` | 발신 밴드 — 중앙이 `workerId`/`workerName` 보강 |
| `payload.trigger` | `BUTTON_LONG_PRESS`, `MOBILE_APP`, `MANUAL_DISPATCH` 등 |
| `payload.vitals` | 직전 측정값 동봉 |
| `rawPosition` | 옵션. 없으면 중앙이 직전 밴드 측정값 위치 사용 |

### 5.4 스마트밴드 이상 — `POST /v1/collect/band/events`

체온/심박수 이상, 낙상, 통신두절 등을 한 엔드포인트에서 받음 (`eventType` 으로 구분).

```json
{
  "eventId": "BAND-EVT-20260424-0001",
  "eventType": "BAND_VITAL_ABNORMAL",
  "severity": "WARNING",
  "occurredAt": "2026-04-24T10:15:30",
  "source": "SMART_BAND",
  "rawPosition": { "lat": 37.501234, "lng": 127.039876 },
  "payload": {
    "bandId": "BAND-A1B2C3",
    "abnormal": [
      { "metric": "HEART_RATE", "value": 168, "unit": "BPM",     "thresholdHigh": 150 },
      { "metric": "BODY_TEMP",  "value": 38.6, "unit": "CELSIUS", "thresholdHigh": 38.0 }
    ]
  }
}
```

| `eventType` | `payload` 형태 |
| --- | --- |
| `BAND_VITAL_ABNORMAL` | 위 예시 (`abnormal[]`) |
| `BAND_FALL_DETECTED` | `{ "bandId", "impactG": 4.2 }` |
| `BAND_OFFLINE` | `{ "bandId", "lastSeenAt": "..." }` (위치 정보 없음) |

---

## 6. 수집모듈 API — 디바이스 CRUD (센서별)

> 인증: **없음** (사이트 LAN 격리). 운영자 / 현장 도구가 호출.
> 수집모듈은 호출을 받으면 ① 자기 `site_id` 를 부착해 ② 중앙 `/v1/sites/{siteId}/devices` 로 forward, ③ 응답 캐시(로컬)도 갱신. 중앙이 SoT(source of truth).

### 6.1 가스센서 — `/v1/devices/gas`

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/v1/devices/gas` | 신규 등록 |
| `GET` | `/v1/devices/gas` | 목록 (현 사이트) — 옵션: `?status=ACTIVE` |
| `GET` | `/v1/devices/gas/{deviceId}` | 단건 조회 |
| `PATCH` | `/v1/devices/gas/{deviceId}` | 부분 수정 |
| `DELETE` | `/v1/devices/gas/{deviceId}` | 폐기/삭제 |

**Body 예시 (등록)**
```json
{
  "deviceId": "GAS-MH203-01",
  "name": "맨홀 203 가스센서",
  "facilityId": "MH-203",
  "installLocal": { "x": 12.5, "y": 4.2, "z": -3.0 },
  "floor": "B1",
  "metadata": {
    "vendor": "Acme",
    "model": "GX-9",
    "calibratedAt": "2026-03-01"
  }
}
```

### 6.2 스마트밴드 — `/v1/devices/bands`

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/v1/devices/bands` | 신규 등록 |
| `GET` | `/v1/devices/bands` | 목록 (현 사이트) |
| `GET` | `/v1/devices/bands/{bandId}` | 단건 조회 |
| `PATCH` | `/v1/devices/bands/{bandId}` | 부분 수정 (워커 재할당 등) |
| `DELETE` | `/v1/devices/bands/{bandId}` | 폐기/삭제 |

**Body 예시 (등록)**
```json
{
  "bandId": "BAND-A1B2C3",
  "name": "1조 작업자",
  "assignedWorkerId": "W102",
  "metadata": {
    "vendor": "Acme",
    "model": "BandPro2",
    "firmware": "1.2.3"
  }
}
```

### 6.3 SOS 디바이스 — `/v1/devices/sos`

밴드와 별도 SOS 단말(펜던트 등) 을 운용할 때 사용. 밴드와 동일한 패턴.

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/v1/devices/sos` | 신규 등록 |
| `GET` | `/v1/devices/sos` | 목록 (현 사이트) |
| `GET` | `/v1/devices/sos/{deviceId}` | 단건 조회 |
| `PATCH` | `/v1/devices/sos/{deviceId}` | 부분 수정 |
| `DELETE` | `/v1/devices/sos/{deviceId}` | 폐기/삭제 |

**Body 예시 (등록)**
```json
{
  "deviceId": "SOS-X1Y2Z3",
  "name": "1조 SOS 펜던트",
  "ownerWorkerId": "W102",
  "metadata": {
    "vendor": "Acme",
    "model": "SOS-Pendant-1",
    "firmware": "1.0.0"
  }
}
```

### 6.x 새 센서 종류 추가 시
센서별 컨트롤러 1개 추가 — `POST/GET/PATCH/DELETE /v1/devices/<type>`. 내부적으로 통합 envelope 로 변환해 중앙 `/v1/sites/{siteId}/devices` 로 forward (siteId 는 수집모듈 자체 설정값). **중앙·DB 무변경**.

---

## 7. 중앙(`safers`) API — 통합

> 인증: **`X-Api-Key`** (사이트별 1키) + HTTPS. Base path: `/v1/sites/{siteId}`.
> URL path 의 `{siteId}` 가 권위. body 에는 `siteId` 를 두지 않음 (위변조 방지).
> 수집모듈만 호출. 운영자 화면은 별도 사용자 인증 API 사용 (본 문서 범위 외).

### 7.1 데이터수집 — `POST /v1/sites/{siteId}/telemetry`

수집모듈이 정규화한 통합 envelope. `sourceType` 으로 분기.

```json
{
  "sourceType": "GAS",
  "sourceId":   "GAS-MH203-01",
  "timestamp":  "2026-04-24T10:15:30",
  "measurement": "gas_reading",
  "tags": {
    "device_id": "GAS-MH203-01",
    "gas":       "H2S"
  },
  "fields": {
    "value":       3.1,
    "unit":        "PPM",
    "battery":     87,
    "signal_rssi": -68
  }
}
```

| 필드 | 설명 |
| --- | --- |
| `sourceType` | `GAS`, `BAND`, ... — 라우팅 키 |
| `sourceId` | 디바이스 식별자 (`deviceId`/`bandId`) |
| `measurement` | 어댑터가 결정 — InfluxDB measurement 이름 |
| `tags` | InfluxDB tag (인덱싱 대상). **`site_id` 는 보내지 않음** — writer 가 path `{siteId}` 를 자동 주입 |
| `fields` | InfluxDB field (값) |

**`site_id` 자동 주입 (writer 정책)**
- InfluxDB writer 는 line protocol 작성 시 path `{siteId}` 를 `tags.site_id` 로 **무조건 덮어씀**.
- 수집모듈이 envelope.tags 에 `site_id` 를 보내도 무시되거나 path 값으로 교체됨 → 위변조 차단.
- 인증된 키의 `site.id` ≠ path `{siteId}` 면 사전에 `403` 으로 거부 (§2.4).

**왜 통합 envelope?**
- writer 1개로 모든 센서 종류 처리 (measurement 만 envelope 에서 가져와서 line protocol 빌드)
- 새 센서 추가 시 중앙은 무변경

> 가스 측정값 한 sample 에 N 개 가스가 있는 경우, 수집모듈은 가스 단위로 1 envelope 씩 분할해서 N 건 forward. 또는 Influx line protocol 에 다중 field 로 한 번에 보낼 수도 있음 — 운영 협의 (§13).

### 7.2 이벤트수집 — `POST /v1/sites/{siteId}/events`

```json
{
  "eventId":    "GAS-EVT-20260424-0001",
  "eventType":  "GAS_THRESHOLD_EXCEEDED",
  "severity":   "CRITICAL",
  "source":     "GAS_SENSOR",
  "occurredAt": "2026-04-24T10:15:30",
  "deviceId":   "GAS-MH203-01",
  "bandId":     null,
  "rawPosition": null,
  "payload": {
    "triggered": [
      { "gas": "H2S", "value": 25.4, "unit": "PPM", "threshold": 10.0, "level": "DANGER" }
    ]
  }
}
```

- 페이로드 구조는 §5 와 동일하되, 식별자(`deviceId`, `bandId`)가 평탄화(top-level)되어 PostgreSQL 컬럼으로 직접 매핑.
- **`site_id` 는 body 에 두지 않음** — INSERT 시 path `{siteId}` 를 컬럼에 직접 사용 (telemetry 와 동일한 정책).
- `payload` 는 종류별 상세를 그대로 JSONB 로 저장.
- **적재 위치**: 별도 `safety_event` 테이블이 아니라 **기존 `events` 테이블에 `category=SAFETY` 로 INSERT** (§9.2). 운영자 UI 의 `GET /events` 가 CCTV 와 안전 이벤트를 한 화면에서 보여주는 게 목적. 안전 이벤트도 `snapshot_file_id` / `video_file_id` 를 채워 CCTV 영상을 attach 가능.

### 7.3 디바이스 관리 — `/v1/sites/{siteId}/devices`

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/v1/sites/{siteId}/devices` | 등록 |
| `GET` | `/v1/sites/{siteId}/devices` | 목록 — 필터: `?type=&status=` |
| `GET` | `/v1/sites/{siteId}/devices/{id}` | 단건 |
| `PATCH` | `/v1/sites/{siteId}/devices/{id}` | 부분 수정 |
| `DELETE` | `/v1/sites/{siteId}/devices/{id}` | 폐기/삭제 |

> **식별 키**: `(id, siteId)` 가 비즈니스 유일 키. **같은 `id` 라도 사이트가 다르면 별개 row 로 등록 가능** (PK 는 surrogate Long, `(device_id, site_id)` 에 unique 제약).

**등록 Body (siteId 는 path 에 있어 body 에 없음)**
```json
{
  "id":        "BAND-A1B2C3",
  "type":      "BAND",
  "name":      "1조 작업자",
  "metadata": {
    "assignedWorkerId": "W102",
    "vendor": "Acme",
    "firmware": "1.2.3"
  }
}
```

**응답 Body (siteId / status / lastSeenAt 포함)**
```json
{
  "id":        "BAND-A1B2C3",
  "type":      "BAND",
  "siteId":    42,
  "name":      "1조 작업자",
  "status":    "ACTIVE",
  "lastSeenAt": "2026-04-24T10:15:30",
  "metadata":  { "assignedWorkerId": "W102", "vendor": "Acme", "firmware": "1.2.3" }
}
```

| 필드 | 설명 |
| --- | --- |
| `id` | 디바이스 ID (`bandId`/`deviceId` 그대로) — 사이트 내 유일 |
| `type` | `GAS`, `BAND`, `SOS` — 디스크리미네이터 |
| `status` | `ACTIVE`, `OFFLINE` (영구 폐기는 `DELETE` 엔드포인트로 hard delete) |
| `lastSeenAt` | telemetry/events 수신 시 자동 갱신 |
| `metadata` | type 별 자유 필드 (JSONB). 필수 필드는 type 별로 검증 |

**type 별 필수 metadata 예시**
```text
GAS  : { facilityId, installLocal{x,y,z}, floor }
BAND : { assignedWorkerId? }
SOS  : { ownerWorkerId? }
```

> 한 화면에서 **사이트 전체 디바이스 통합 조회 + 상태별 필터** 가능. 새 센서 type 추가 시 metadata 매핑만 추가 — 테이블 스키마 무변경.

---

## 8. Enum 정의

```text
SourceType     : GAS, BAND, SOS                     (ingest 라우팅 키)
DeviceType     : GAS, BAND, SOS                     (디바이스 마스터)
DeviceStatus   : ACTIVE, OFFLINE
GasType        : O2, H2S, CO, CO2, CH4, LEL
GasUnit        : PPM, PERCENT
WearState      : WORN, OFF, UNKNOWN

# 통합 events 테이블 enum — 기존 CCTV enum 에 SAFETY 항목을 추가하는 형태
EventCategory  : DETECTION (CCTV), SAFETY (가스/SOS/밴드)
EventType      : # CCTV (기존)
                 NO_HELMET, NO_VEST, FIRE, SMOKE, FALLING, ...,
                 # SAFETY (신규 추가)
                 GAS_THRESHOLD_EXCEEDED, SOS_TRIGGERED,
                 BAND_VITAL_ABNORMAL, BAND_FALL_DETECTED, BAND_OFFLINE
EventSeverity  : INFO, WARNING, CRITICAL                  (안전 이벤트만 사용)
EventSource    : GAS_SENSOR, SOS_DEVICE, SMART_BAND       (안전 이벤트만 사용)

SosTrigger     : BUTTON_LONG_PRESS, MOBILE_APP, MANUAL_DISPATCH
ThresholdLevel : WARNING, DANGER
VitalMetric    : HEART_RATE, BODY_TEMP, SPO2
```

> `FIRE`, `SMOKE` 는 이미 CCTV `EventType` 에 있고, 안전 이벤트의 가스 감지(`GAS_THRESHOLD_EXCEEDED`)와는 `category` 디스크리미네이터로 구분되므로 충돌 없음.

---

## 9. 저장 스키마

### 9.1 InfluxDB (시계열 — telemetry)

각 측정값은 **raw 그대로** 적재. 시설/사이트/3D 좌표 등 보강 정보는 저장하지 않음 (조회 시 pf-backend 에서 join).

| measurement | tags | fields | timestamp |
| --- | --- | --- | --- |
| `gas_reading` | `site_id`, `device_id`, `gas` | `value`, `unit`, `battery`, `signal_rssi` | `timestamp` |
| `band_reading` | `site_id`, `band_id` | `heart_rate_bpm`, `body_temp_c`, `spo2`, `step`, `lat`, `lng`, `accuracy_m`, `battery`, `wear_state` | `timestamp` |

- `measurement` 는 envelope 에서 결정. writer 코드는 1개. 새 센서는 새 measurement 이름만 추가하면 끝.
- 권장 retention: 원본 30~90일 + 1분/5분 다운샘플링 버킷.

### 9.2 PostgreSQL (이벤트 — 통합 `events` 테이블)

> **결정사항**: 안전 이벤트(가스/SOS/밴드)는 **별도 `safety_event` 테이블을 만들지 않고 기존 `events` 테이블을 확장** 해서 한 곳에 적재. 이유는 ① 운영자 UI 가 CCTV 이벤트와 안전 이벤트를 한 화면에서 보고 ② STOMP 단일 토픽으로 브로드캐스트 ③ `GET /events` 한 번에 통합 조회가 가능하기 때문. 안전 이벤트도 필요 시 CCTV 스냅샷/영상을 attach 할 수 있다는 부수 효과도 있음.

**기존 컬럼 (CCTV 이벤트 — 그대로 유지)**
- `id`, `event_id`, `event_timestamp`, `category`, `type`, `track_id`, `name`, `bbox`, `center_x/y`, `confidence`, `path`, `site_id`, `snapshot_file_id`, `video_file_id`, audit 컬럼

**확장 컬럼 (안전 이벤트용 — 모두 nullable)**

```sql
-- V20260430_001__extend_events_for_safety.sql (예시)

-- 안전 이벤트는 trackId 가 없으므로 nullable 로 완화
ALTER TABLE events ALTER COLUMN track_id DROP NOT NULL;

-- 안전 이벤트 전용 컬럼 추가 (CCTV 이벤트는 모두 NULL)
ALTER TABLE events
    ADD COLUMN severity     VARCHAR(16),                    -- INFO | WARNING | CRITICAL
    ADD COLUMN source       VARCHAR(16),                    -- GAS_SENSOR | SOS_DEVICE | SMART_BAND
    ADD COLUMN device_id    VARCHAR(64),
    ADD COLUMN band_id      VARCHAR(64),
    ADD COLUMN lat          DOUBLE PRECISION,
    ADD COLUMN lng          DOUBLE PRECISION,
    ADD COLUMN accuracy_m   DOUBLE PRECISION,
    ADD COLUMN payload      JSONB;                           -- 이벤트 종류별 상세 (triggered/abnormal 등)

-- 인덱스 (안전 이벤트 조회용 — partial index 로 CCTV 행 제외해 인덱스 크기 절약)
CREATE INDEX idx_events_device   ON events (device_id) WHERE device_id IS NOT NULL;
CREATE INDEX idx_events_band     ON events (band_id)   WHERE band_id   IS NOT NULL;
CREATE INDEX idx_events_severity ON events (severity, event_timestamp DESC) WHERE severity IS NOT NULL;
CREATE INDEX idx_events_payload  ON events USING GIN (payload jsonb_path_ops) WHERE payload IS NOT NULL;
```

**`category` 디스크리미네이터로 두 종류 구분**

| `category` | 의미 | 사용되는 컬럼 묶음 |
| --- | --- | --- |
| `DETECTION` | CCTV AI 감지 | `track_id`, `bbox`, `center_x/y`, `confidence`, `path`, `name`, `snapshot_file_id`, `video_file_id` |
| `SAFETY` | 가스/SOS/밴드 사건 | `severity`, `source`, `device_id` 또는 `band_id`, `lat/lng/accuracy_m`, `payload jsonb` |

> 양쪽 모두 `event_id`, `event_timestamp`, `site_id`, `category`, `type`, `name` 은 공통 사용. 안전 이벤트도 `snapshot_file_id` / `video_file_id` 첨부 가능 (예: SOS 발생 시 인근 CCTV 스냅샷 자동 연결).

**조회 / 알림**
- `GET /events` (기존) 엔드포인트가 자동으로 둘 다 반환 — `category` 필터 추가만 운영자 UI 에서 처리
- `LISTEN/NOTIFY events` 단일 채널로 실시간 알림

### 9.3 PostgreSQL (디바이스 마스터 — devices)

식별: surrogate Long PK + 비즈니스 키 `(device_id, site_id)` 복합 unique. 같은 `device_id` 라도 사이트가 다르면 별개 row.

```sql
CREATE TABLE device (
  id            BIGINT       GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  device_id     VARCHAR(64)  NOT NULL,             -- "BAND-A1B2C3", "GAS-MH203-01"
  type          VARCHAR(16)  NOT NULL,             -- GAS | BAND | SOS
  site_id       BIGINT       NOT NULL,
  name          VARCHAR(128),
  status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  last_seen_at  TIMESTAMP,
  metadata      JSONB        NOT NULL DEFAULT '{}'::jsonb,
  created_at    TIMESTAMP    NOT NULL,
  updated_at    TIMESTAMP    NOT NULL,
  created_by    VARCHAR(255),
  updated_by    VARCHAR(255),
  CONSTRAINT uk_device_id_site UNIQUE (device_id, site_id)
);

CREATE INDEX idx_device_site_status ON device (site_id, status);
CREATE INDEX idx_device_type        ON device (type);
CREATE INDEX idx_device_metadata    ON device USING GIN (metadata jsonb_path_ops);
```

- type 별 특이 필드는 `metadata jsonb` — DB 스키마 변경 없이 새 type 도입 가능.
- API 응답의 `id` 필드는 `device_id` 컬럼값 (사용자 친화 식별자). surrogate Long pk 는 외부에 노출하지 않음.
- 응답/요청 DTO 에서 `type` 으로 polymorphic 매핑 (Jackson `@JsonTypeInfo`) → 코드 레벨 타입 안전 확보.
- 6.x 의 센서별 `/v1/devices/<type>` (수집모듈) 호출은 내부적으로 이 테이블의 row 1개로 매핑됨.

---

## 10. 보강(Enrichment) & 후속 시스템 — pf-backend 책임

수집 단계는 raw 적재까지만 책임짐. 3D 맵 표출에 필요한 보강·STOMP 브로드캐스트는 pf-backend 가 담당.

### 10.1 보강 마스터

| 마스터 | 키 | 보강 필드 | 비고 |
| --- | --- | --- | --- |
| `sites` (기존) | `id` | `name`, `region`, `address`, `location`, `nx`, `ny` | 이미 존재 (`apps/safers/.../site/entity/Site.kt`) |
| 사이트 인증 키 | `site.id` | `api_key_hash` (+ 회전 시 `valid_from/until`) | 위치는 §13 협의 — `sites` 컬럼 추가 vs 별도 테이블 |
| **`device` (§9.3)** | `id` | type 별 `metadata`(facility, installLocal, assignedWorker 등) | 통합 디바이스 마스터 — gas/band/sos 가 하나의 테이블 |
| `worker_master` | `worker_id` | `worker_name`, `team`, `phone` | 근로자 정보 |
| `site_coord_transform` | `site_id` | WGS84 ↔ 현장 3D 모델 로컬좌표 변환 | 옥외 GPS → 3D 모델 좌표 변환 |

### 10.2 보강 흐름

```text
raw event   (PG events, category=SAFETY) ─┐
raw reading (Influx)                       ─┤  pf-backend 조회 시
                                            ├─→ device.id 로 device join
device (gas/band/sos 통합)                ─┤      ├─ GAS:  metadata.facilityId, installLocal 사용
worker_master                             ─┤      ├─ BAND: rawPosition → site_coord_transform → local{x,y,z}, floor
site_coord_transform           ─┘      └─ SOS:  rawPosition 우선, 없으면 직전 band_reading 위치
                                       ↓
                                3D 표출용 enriched 모델 → STOMP 브로드캐스트
```

### 10.3 신규 이벤트 구독

| 방식 | 비고 |
| --- | --- |
| pf-backend 가 PostgreSQL `LISTEN/NOTIFY` 구독 | 가장 단순. ingest 가 INSERT 후 `NOTIFY events` (CCTV 와 안전 이벤트 단일 채널) |
| pf-backend 폴링 | `received_at > last_seen` 조건으로 N초 간격 조회 |

### 10.4 미등록 디바이스 처리 정책

| 옵션 | 동작 | 장단 |
| --- | --- | --- |
| **A. 느슨 (v1 권장)** | ingest 그냥 적재. pf-backend 보강 시 `device` 에 없으면 unknown 으로 표출 + 미등록 알람 | 단순. 데이터 손실 없음 |
| B. 엄격 | 중앙 ingest 가 `device` lookup → 미등록 시 `400` 거부 | 정합성 ↑. 신규 설치 직후 race condition 주의 |
| C. 사이트 화이트리스트 | 수집모듈이 자기 사이트 디바이스 목록을 중앙에서 주기 fetch → 미등록 차단 | 사이트 단위 격리 강함 |

> v1 은 A. 운영 안정화 후 잡음/위협이 되면 B 로 강화.

---

## 11. 컴포넌트 구성

### 11.1 기술 스택 (양쪽 공통)

| 항목 | 선택 | 비고 |
| --- | --- | --- |
| 언어 / 프레임워크 | **Spring Boot + Kotlin** | pf-backend 와 동일 |
| 웹 계층 | **Spring MVC (blocking)** | 동시연결 수만 단위로 커지면 재평가 |
| 인증 (중앙 ↔ 모듈) | `X-Api-Key` (Filter / Interceptor) | §2.4 |
| InfluxDB 클라이언트 | `com.influxdb:influxdb-client-java` | 중앙에서만 — 배치 + 비동기 write |
| PostgreSQL 클라이언트 | Spring Data JPA + HikariCP | 중앙에서만 — JSONB 매핑 (Hibernate `JsonType`) |

### 11.2 수집모듈 (각 현장에 배포)

`apps/safers-collect` — pf-backend 모노레포 안의 독립 Spring Boot 앱 (`:common:*` 의존성 0).
**DB 클라이언트 없음 (중앙으로 push 만)**.

**패키지 구조 (v1 패키지 + 도메인별 어댑터 = Option B)**

```
apps/safers-collect/.../
├── SafersCollectApplication.kt
├── config/
│   ├── WebConfig.kt              패키지 단위 addPathPrefix("/v1") 자동 부여
│   └── OpenApiConfig.kt          GroupedOpenApi("v1") — Swagger 그룹 분리
├── v1/
│   ├── collect/                  센서별 수신 (telemetry + events)
│   │   ├── controller/
│   │   │   ├── GasCollectController          POST /v1/collect/gas, /v1/collect/gas/events
│   │   │   ├── BandCollectController         POST /v1/collect/band, /v1/collect/band/events
│   │   │   └── SosEventController            POST /v1/collect/sos/events
│   │   ├── dto/                              CollectDtos, EventDtos
│   │   └── adapter/                          벤더 raw → 정규화 telemetry/event envelope
│   └── devices/                  센서별 디바이스 CRUD
│       ├── controller/
│       │   ├── GasDeviceController           CRUD /v1/devices/gas
│       │   ├── BandDeviceController          CRUD /v1/devices/bands
│       │   └── SosDeviceController           CRUD /v1/devices/sos
│       ├── dto/                              GasDeviceDtos, BandDeviceDtos, SosDeviceDtos
│       └── adapter/                          CRUD payload → 중앙 /v1/devices envelope
├── buffer/                       로컬 buffer (in-memory + 옵션 디스크 spill)
└── forwarder/                    buffer → 중앙 /v1/telemetry, /v1/events, /v1/devices push
```

> **Option B (도메인 안에 어댑터 동거)**: 컨트롤러·DTO·어댑터가 한 폴더에 모임. 새 센서 추가 시 `v1/collect/<type>` 와 `v1/devices/<type>` 두 폴더에 컨트롤러+DTO+어댑터 한 세트씩만 추가하면 끝 — 다른 센서 코드 무영향.

**핸들러 흐름 (수집모듈)**
1. 센서 / 벤더 GW → 수집모듈 HTTP POST 수신 (인증 없음, LAN 격리)
2. 어댑터가 통합 envelope 변환 + 자기 `site_id` 부착 → buffer enqueue → 즉시 `200`
3. forwarder 가 배치(예: 1초 또는 100건)로 묶어 중앙 `/v1/telemetry` (또는 `/v1/events`) 에 POST
4. 중앙이 `200` 응답하면 buffer 에서 제거, 실패면 백오프 후 재시도

**buffer 정책 (yml 예시)**
```yaml
safety-collector:
  site:
    id: 42                              # safers 의 sites.id
  central:                              # 수집모듈 → safers
    ingest-url: https://safers.pluxity.com/v1
    api-key: ${INGEST_API_KEY}
  buffer:
    in-memory-max: 10000
    spill-to-disk: true
    disk-path: /var/lib/safety-collector/buffer
    retry:
      backoff-initial: 1s
      backoff-max: 60s
  forwarder:
    batch-size: 100
    flush-interval: 1s
    gzip: true
    immediate-event-types:              # 긴급 이벤트 즉시 forward (~100ms)
      - SOS_TRIGGERED
      - GAS_THRESHOLD_EXCEEDED
      - BAND_FALL_DETECTED
```

> 디바이스 → 수집모듈 구간은 **측정 즉시 1건씩 실시간 송신**. 1초 윈도우 배치는 모듈 → 중앙 forward 단계에서만 적용.

### 11.3 중앙 ingest API (`safers` 내부 신규 모듈)

```
apps/safers/.../ingest/
├── controller/
│   ├── TelemetryController     POST /v1/sites/{siteId}/telemetry
│   ├── EventIngestController   POST /v1/sites/{siteId}/events     (기존 /events 조회 API 와 별도)
│   └── DeviceController        CRUD /v1/sites/{siteId}/devices
├── dto/                        TelemetryRequest, EventIngestRequest, Device*Request/Response, enum
├── entity/                     Device (IdentityIdEntity 상속, deviceId+siteId 복합 unique)
├── repository/                 DeviceRepository (existsByDeviceIdAndSiteId 등)
├── service/                    DeviceService (CRUD + lastSeenAt 자동 갱신)
├── auth/                       X-Api-Key 검증 + 키→site.id 매핑값 vs path siteId 일치 검증
├── persistence/
│   └── influx/                 InfluxDB writer (배치, async). path siteId 를 tags.site_id 로 강제 주입
└── config/                     influx url/token/bucket, datasource
```

> 중앙 ingest 컨트롤러는 `@RequestMapping("/v1/...")` 에 prefix 를 직접 명시 (수집모듈처럼 패키지 단위 prefix 미사용). **이유는 §11.6 의 버저닝 전략 차이 참조**.

**핸들러 흐름 (중앙)**
- `/v1/telemetry`: 검증 → InfluxDB 배치 큐 enqueue → 즉시 `200`
- `/v1/events`: 검증 → PG `events` 테이블에 동기 INSERT (`category=SAFETY`) → `200` (필요 시 `NOTIFY events`)
- `/v1/devices`: JPA CRUD → `device.last_seen_at` 은 telemetry/events 핸들러가 별도 갱신
- 컨트롤러는 분리 (인증/모니터링/Rate-limit 정책 분리 용이)

### 11.4 외부 인프라

- **중앙**: InfluxDB 2.x (bucket: `safety`, retention 협의), PostgreSQL (`safers` DB 별도 스키마 권장)
- **수집모듈**: DB 없음. 디스크 buffer 용 작은 디렉토리 (`/var/lib/safety-collector/buffer`)

### 11.5 새 센서 종류 추가 — 변경 범위 정리

본 설계의 핵심 약속이 "수집모듈에서 변화를 흡수, 중앙은 type 만 추가" 임을 코드 변경 단위로 정리.

| 영역 | 새 센서(예: 온습도계) 추가 시 | 변경 위치 |
| --- | --- | --- |
| 수집모듈 컨트롤러 | 신규 `TempCollectController` + `TempDeviceController` | `v1/collect/`, `v1/devices/` |
| 수집모듈 DTO | 신규 `TempDtos`, `TempDeviceDtos` | 동일 도메인 폴더 |
| 수집모듈 어댑터 | 신규 `TempAdapter`, `TempDeviceAdapter` | 동일 도메인 폴더 (Option B) |
| 수집모듈 forwarder/buffer | **무변경** | 정규화 envelope 단일 파이프라인 |
| 중앙 enum | `SourceType.TEMP`, `DeviceType.TEMP` 추가 (이벤트 있으면 `EventSource`/`EventTypeKind` 도 1줄) | `apps/safers/.../ingest/dto/` |
| 중앙 컨트롤러/DTO 클래스 | **무변경** | `/v1/telemetry`, `/v1/events`, `/v1/devices` 통합 envelope 그대로 |
| 중앙 InfluxDB writer | **무변경** | `measurement` 문자열만 envelope 에서 받아 line protocol 작성 |
| 중앙 PostgreSQL 스키마 | **무변경** | `device.metadata jsonb` 가 type 별 자유 필드 흡수 |
| 운영자 UI 디바이스 화면 | **무변경** | 단일 `device` 테이블 조회로 자동 표출 |
| 다른 사이트 수집모듈 | **무변경** | 사이트별 독립 배포 |

> 즉 새 센서 도입 = **수집모듈에 도메인 폴더 1개 + 중앙 enum 값 1~2줄**. 사이트 단위 재배포는 신규 센서를 도입하는 사이트만 수행.

### 11.6 버저닝 전략 (수집모듈 vs 중앙)

두 모듈의 **변경 빈도가 다르므로 전략도 다르게** 적용.

| 측면 | 수집모듈 (`safers-collect`) | 중앙 (`safers/ingest`) |
| --- | --- | --- |
| 변경 빈도 | 高 (벤더·센서 추가마다) | 거의 변하지 않음 (변하면 안 됨) |
| 변경 비용 | 사이트별 배포로 흡수 가능 | 수집모듈 동시 업그레이드 필요 = 빅뱅 |
| 버저닝 방식 | **패키지 분리 + WebConfig prefix** | **`@RequestMapping("/v1/...")` 어노테이션 직접 명시** |
| 패키지 | `com.pluxity.safersCollect.v1.*` | `com.pluxity.safers.ingest.controller.*` (버전 패키지 없음) |
| 새 버전 도입 | `v2/` 폴더 + WebConfig 한 줄 추가 — v1 무수정 | 가능한 한 v2 자체를 만들지 않음. 변경은 envelope/enum 확장으로 흡수 |
| 동시 살아있는 버전 | ≤ 2개 (Sunset 정책) | 사실상 v1 영구 유지 목표 |

**수집모듈 (`safers-collect`) — Option A: 패키지 분리**
```kotlin
// config/WebConfig.kt
configurer.addPathPrefix("/v1") {
    it.isAnnotationPresent(RestController::class.java) &&
        it.packageName.startsWith("com.pluxity.safersCollect.v1")
}
// 향후:
// configurer.addPathPrefix("/v2") {
//     ... .startsWith("com.pluxity.safersCollect.v2")
// }
```
- 컨트롤러 자체는 `@RequestMapping("/collect/gas")` 만 — prefix 신경 X
- v2 도입: `v1/` 안 깨진 컨트롤러는 그대로 두고 변경된 것만 `v2/` 에 새로 작성
- Swagger 는 `GroupedOpenApi("v1")`, `GroupedOpenApi("v2")` 로 페이지 분리

**중앙 (`safers/ingest`) — Option B: 어노테이션 직접**
```kotlin
@RequestMapping("/v1/telemetry")   // ← prefix 직박
class TelemetryController { ... }
```
- safers 의 다른 도메인(`/events` 조회, `/sites`, `/weather`) 들과 동일한 컨벤션 유지 — 모듈 안 일관성
- 컨트롤러가 3개뿐이라 패키지 분리 비용 > 이익
- v2 가 정말 필요해질 만큼 envelope 자체가 깨지는 변경은 거의 발생하지 않는 게 정상

**왜 중앙은 v2 가 거의 발생 안 하는가**
- envelope 자체가 generic 함: `measurement: String`, `tags: Map`, `fields: Map`, `metadata: Map` — 새 필드/타입은 그냥 추가
- enum 값 추가는 호환됨 (forward-compat). breaking change 가 아님
- "필드 의미 자체가 바뀌는" 근본적 변경이 있을 때만 v2 등장

---

## 12. 배포 토폴로지 & 규모 가이드

### 12.1 왜 DB 는 중앙에만 두는가

| 이유 | 설명 |
| --- | --- |
| 사이트 운영 인프라 부재 | 현장 PC/소형 서버에 collector 만 띄움 |
| pf-backend 통합 조회 | 3D 맵에 "전 사이트 통합 뷰" — 사이트별 DB 면 federation 필요 |
| 데이터 안전성 | 중앙 IDC 는 백업/HA 갖춤 |
| 운영 단순함 | 백업·모니터링·스키마 마이그레이션 1세트만 |

### 12.2 회선 단절 대비 (수집모듈 buffer)

| 단절 시간 | 흡수 방법 |
| --- | --- |
| 수 분 | in-memory queue (1만 건) |
| 수 시간 ~ 수 일 | 디스크 spill |
| 수 일 이상 | 사이트별 로컬 DB 도입 검토 |

### 12.3 규모별 운영 가이드

**가정 (1,000세대 표준 아파트 현장)**: 가스센서 100대 × 30초 + 스마트밴드 1,000대 × 10초 ≈ **~110 req/s** (디바이스 → 모듈). 모듈이 1초 윈도우로 배치 forward → 중앙 입장 사이트당 ~1 req/s.

| 사이트 수 | 디바이스 → 모듈 | 중앙 ingest | 중앙 인프라 권장 |
| --- | --- | --- | --- |
| 1 ~ 30 (현재 목표) | 110 req/s × N | 30 req/s 내외 | 중앙 ingest 1대, InfluxDB/PG 단일 노드 — 100배 여유 |
| 50 ~ 100 | | 100 req/s 내외 | 인스턴스 2~3대 + LB. DB 단일 |
| 200+ | | 수백 req/s | InfluxDB cluster, PG 분할 검토 |

### 12.4 수집모듈(게이트웨이) 성능 (2 vCPU / 2GB)

| 항목 | 처리량 / 한도 |
| --- | --- |
| 디바이스 → 모듈 수신 | **2,000 ~ 5,000 req/s** |
| in-memory buffer | 10,000 건 (~50~100MB heap) |
| 디스크 spill 활용 | 수백만 건 |
| forwarder → 중앙 | 수십 req/s (배치 후) |

### 12.5 게이트웨이 배치 효과

> 디바이스 → 모듈 구간은 **실시간 1건씩**. 모듈 → 중앙만 1초 윈도우 배치 + gzip.

| 항목 | 모듈이 즉시 forward | 모듈이 배치 forward (100건/1초) | 감소율 |
| --- | --- | --- | --- |
| 사이트당 중앙 요청 수 | 100 req/s | ~1 req/s | **99% ↓** |
| 사이트당 네트워크 트래픽 | ~100 KB/s | ~5~10 KB/s (gzip) | **90% ↓** |
| 30 사이트 합산 중앙 RPS | 3,000 req/s | **~30 req/s** | — |
| 추가 지연 (3D 맵 체감) | 0 | ~0~1초 | 무시 가능 (긴급 우회) |

### 12.6 End-to-end 지연 예산

| 구간 | 일반적 지연 |
| --- | --- |
| 디바이스 → 수집모듈 (LAN) | 1~10 ms |
| 모듈 핸들러 (어댑터 + buffer enqueue) | < 1 ms |
| 모듈 buffer 대기 (배치 flush) | **0 ~ 1,000 ms** |
| 모듈 → 중앙 (WAN) | 20~100 ms |
| 중앙 ingest (검증 + Influx enqueue / PG INSERT) | 1~20 ms |
| pf-backend 보강 + STOMP | 5~50 ms |
| 클라이언트 수신 → 3D 렌더 | 5~20 ms |
| **합계** | **~50~1,200 ms** |

긴급 이벤트는 즉시 forward 모드 → 100ms 미만 도달 가능.

### 12.7 단일 인스턴스 한계 (중앙)

| 컴포넌트 | 처리량 (단일 노드) |
| --- | --- |
| 중앙 ingest API (Spring MVC blocking) | 2,000 ~ 5,000 req/s |
| InfluxDB 2.x | 100,000+ points/s |
| PostgreSQL (이벤트 INSERT) | 5,000+ /s |

→ **30 사이트 규모에선 단일 노드로 100배 여유.**

---

## 13. 미정/협의 항목

**인프라**
- [ ] InfluxDB 인스턴스 — 신규 vs 기존 공용, retention 정책
- [ ] PostgreSQL — `safers` DB 별도 스키마 vs 전용 DB
- [ ] pf-backend 가 이벤트를 가져오는 방식 (LISTEN/NOTIFY vs polling)

**수집모듈 운영**
- [ ] buffer 크기 / 디스크 spill 한도 / 단절 SLA
- [ ] 사이트별 회선 안정성 실측 (in-memory 만 OK 인지)
- [ ] 수집모듈 배포 형태 (도커 / 베어메탈 / Windows 서비스 …)

**벤더 / 센서 ↔ 수집모듈 (인증 없음)**
- [ ] 사이트 LAN 격리 정책 — 수집모듈 포트 외부 노출 차단 검증 (방화벽 룰 표준화)
- [ ] 벤더 GW 송신 JSON 포맷이 §4·§5 와 다를 경우 어댑터 위치 (벤더 협의 vs 수집모듈 변환)
- [ ] 센서 종류별 어댑터 모듈 추가 절차 / 코드 컨벤션

**수집모듈 ↔ 중앙 인증**
- [ ] 키 → `site.id` 매핑 보관 위치: `sites` 테이블 컬럼 추가 vs 별도 `site_ingest_credential` 테이블 (회전 윈도우 지원)
- [ ] 키 회전 정책 (구키/신키 동시 유효 기간)
- [ ] 추가 방어선으로 IP allowlist 적용 여부 (도입 시 `sites.base_url` 또는 별도 컬럼 활용)

**이벤트 통합 운영 (§9.2 결정사항 후속)**
- [ ] CCTV 이벤트의 `track_id NOT NULL` 제약 완화 마이그레이션 시점 / 운영 영향 확인
- [ ] 안전 이벤트의 `name` 컬럼 자동 생성 규칙 (예: "유해가스 임계 초과 — H2S 25.4 PPM")
- [ ] 안전 이벤트에 CCTV 영상 자동 attach 정책 — 가까운 CCTV 매칭 룰 (`device_master.facility_id` ↔ `cctv.site_id` ↔ 거리 기준 등)
- [ ] STOMP 토픽 분리 여부 — `/topic/events` 단일 vs `/topic/events/safety` 분리

**비즈니스 정책**
- [ ] 가스 임계값(WARNING/DANGER) 기준 — 송신값 신뢰 vs 중앙에서 판정
- [ ] 스마트밴드 측정 주기 (3s / 10s / 1분?) → 배치 size · 회선 비용 영향
- [ ] 좌표 변환 룰: 현장별 WGS84 → 3D 모델 로컬좌표 변환 매트릭스/층 매핑
- [ ] 디바이스 신규 설치 워크플로우: 수집모듈 `/devices/<type>` 직접 등록 vs 운영자 UI 만 사용
- [ ] 미등록 디바이스 처리 정책 — §10.4 옵션 A/B/C 중 선택 시점
- [ ] 밴드 ↔ 근로자 재할당 이력 관리 (이벤트 시점 매핑 보존 필요한지)
- [ ] 실내 측위(BLE/UWB) 도입 시 `rawPosition` 스키마 확장
- [ ] SOS 발신 후 재발신 정책 (동일 worker 반복 시)
- [ ] 밴드 오프라인 판단 책임 (송신측 inactivity timeout vs 수집모듈/중앙 자체 판단)
