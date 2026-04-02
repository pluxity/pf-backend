-- events 테이블에 site_id 컬럼 추가
ALTER TABLE events ADD COLUMN site_id BIGINT;

-- 기존 데이터: path(스트림명) → cctv_stream → site_id 매핑
UPDATE events e
SET site_id = (
    SELECT c.site_id
    FROM cctv_stream c
    WHERE c.stream_name = e.path
    LIMIT 1
)
WHERE e.site_id IS NULL
  AND e.path IS NOT NULL
  AND e.path != '';

-- NOT NULL 제약 추가
ALTER TABLE events ALTER COLUMN site_id SET NOT NULL;

-- 인덱스 추가
CREATE INDEX idx_events_site_id ON events (site_id);
