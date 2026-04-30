-- 안전 이벤트(가스/SOS/스마트밴드)를 기존 events 테이블에 통합 적재.
-- category=SAFETY 디스크리미네이터로 CCTV(category=DETECTION/ROI) 와 구분.
-- 모든 DDL 은 idempotent — 부분 적용된 환경에서도 재실행 안전.

-- 안전 이벤트는 trackId 가 없으므로 NOT NULL 완화 (이미 NULL 허용이면 no-op)
ALTER TABLE events ALTER COLUMN track_id DROP NOT NULL;

-- 안전 이벤트 전용 컬럼 (CCTV 행은 모두 NULL)
ALTER TABLE events ADD COLUMN IF NOT EXISTS severity   VARCHAR(16);
ALTER TABLE events ADD COLUMN IF NOT EXISTS source     VARCHAR(16);
ALTER TABLE events ADD COLUMN IF NOT EXISTS device_id  VARCHAR(64);
ALTER TABLE events ADD COLUMN IF NOT EXISTS band_id    VARCHAR(64);
ALTER TABLE events ADD COLUMN IF NOT EXISTS lat        DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS lon        DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS alt        DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS accuracy_m DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS payload    JSONB;

-- 인덱스: partial index 로 CCTV 행 제외해 인덱스 크기 절약
CREATE INDEX IF NOT EXISTS idx_events_device   ON events (device_id) WHERE device_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_band     ON events (band_id)   WHERE band_id   IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_severity ON events (severity, event_timestamp DESC) WHERE severity IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_payload  ON events USING GIN (payload jsonb_path_ops) WHERE payload IS NOT NULL;
