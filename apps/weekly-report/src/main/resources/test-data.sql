-- ============================================
-- Weekly Report 테스트 데이터
-- 실행: psql -h localhost -U pluxity -d weekly_report -f test-data.sql
-- ============================================

-- 기존 데이터 정리 (역순 삭제)
DELETE FROM tasks;
DELETE FROM epic_assignments;
DELETE FROM epics;
DELETE FROM projects;
DELETE FROM team_members;
DELETE FROM teams;
DELETE FROM resource_permission;
DELETE FROM domain_permission;
DELETE FROM role_permission;
DELETE FROM user_role;
DELETE FROM permission;
DELETE FROM roles;
-- users는 유지 (id 1,2,3 이미 존재)

-- ── Teams ──
INSERT INTO teams (id, name, leader_id, created_at, updated_at, created_by, updated_by) VALUES
(1, '백엔드팀',   1, NOW(), NOW(), 'system', 'system'),
(2, '프론트팀',   2, NOW(), NOW(), 'system', 'system'),
(3, '인프라팀',   NULL, NOW(), NOW(), 'system', 'system');

-- ── Team Members (users 테이블에 존재하는 user_id 사용) ──
-- user 1 = 백엔드팀, user 2 = 프론트팀
INSERT INTO team_members (id, team_id, user_id, created_at, updated_at, created_by, updated_by) VALUES
(1, 1, 1, NOW(), NOW(), 'system', 'system'),
(2, 2, 2, NOW(), NOW(), 'system', 'system');

-- ── Projects ──
INSERT INTO projects (id, name, description, status, start_date, due_date, pm_id, created_at, updated_at, created_by, updated_by) VALUES
(1, 'SAFERS',       'CCTV 안전 관제 플랫폼',       'IN_PROGRESS', '2026-01-01', '2026-06-30', 1, NOW(), NOW(), 'system', 'system'),
(2, '용인 플랫폼',  '용인시 스마트시티 플랫폼',     'IN_PROGRESS', '2026-02-01', '2026-08-31', 2, NOW(), NOW(), 'system', 'system'),
(3, '주간보고 시스템', 'PMS 주간보고 자동화',        'TODO',        '2026-03-01', '2026-05-31', NULL, NOW(), NOW(), 'system', 'system');

-- ── Epics ──
-- SAFERS 에픽
INSERT INTO epics (id, project_id, name, description, status, start_date, due_date, team_id, created_at, updated_at, created_by, updated_by) VALUES
(1, 1, '인증 개발',       '로그인/권한 관련 기능',      'IN_PROGRESS', '2026-01-15', '2026-03-31', 1, NOW(), NOW(), 'system', 'system'),
(2, 1, '대시보드',        '메인 대시보드 UI 개발',      'TODO',        '2026-04-01', '2026-05-31', 2, NOW(), NOW(), 'system', 'system'),
(3, 1, 'CCTV 연동',      'CCTV 스트리밍 연동',         'TODO',        NULL, NULL, 1, NOW(), NOW(), 'system', 'system');

-- 용인 플랫폼 에픽
INSERT INTO epics (id, project_id, name, description, status, start_date, due_date, team_id, created_at, updated_at, created_by, updated_by) VALUES
(4, 2, '출역 관리',       '출역현황 관리 기능',         'IN_PROGRESS', '2026-02-15', '2026-04-30', 2, NOW(), NOW(), 'system', 'system'),
(5, 2, '날씨 API',        '기상청 API 연동',            'DONE',        '2026-02-01', '2026-02-28', 1, NOW(), NOW(), 'system', 'system');

-- 주간보고 시스템 에픽
INSERT INTO epics (id, project_id, name, description, status, start_date, due_date, team_id, created_at, updated_at, created_by, updated_by) VALUES
(6, 3, 'LLM 채팅',        '자연어 CRUD 채팅 기능',     'IN_PROGRESS', '2026-03-01', '2026-04-15', 1, NOW(), NOW(), 'system', 'system');

-- ── Tasks ──
-- SAFERS > 인증 개발 (user 1 = 백엔드 담당)
INSERT INTO tasks (id, epic_id, name, description, status, progress, start_date, due_date, assignee_id, created_at, updated_at, created_by, updated_by) VALUES
(1,  1, '로그인 API',       'JWT 쿠키 기반 로그인',       'IN_PROGRESS', 70,  '2026-01-15', '2026-02-28', 1, NOW(), NOW(), 'system', 'system'),
(2,  1, '권한 체크',        'RBAC 권한 검증 AOP',         'IN_PROGRESS', 40,  '2026-02-01', '2026-03-15', 1, NOW(), NOW(), 'system', 'system'),
(3,  1, '회원가입 API',     '사용자 등록 기능',           'DONE',        100, '2026-01-15', '2026-02-15', 1, NOW(), NOW(), 'system', 'system');

-- SAFERS > 대시보드 (user 2 = 프론트 담당)
INSERT INTO tasks (id, epic_id, name, description, status, progress, start_date, due_date, assignee_id, created_at, updated_at, created_by, updated_by) VALUES
(4,  2, '메인 화면 레이아웃', '대시보드 메인 페이지',     'TODO',         0,  NULL, '2026-04-30', 2, NOW(), NOW(), 'system', 'system'),
(5,  2, '차트 컴포넌트',     'Recharts 기반 차트',        'TODO',         0,  NULL, '2026-05-15', 2, NOW(), NOW(), 'system', 'system');

-- SAFERS > CCTV 연동 (미배정)
INSERT INTO tasks (id, epic_id, name, description, status, progress, start_date, due_date, assignee_id, created_at, updated_at, created_by, updated_by) VALUES
(6,  3, 'RTSP 스트리밍',     'CCTV RTSP 프로토콜 연동',  'TODO',         0,  NULL, NULL, NULL, NOW(), NOW(), 'system', 'system');

-- 용인 플랫폼 > 출역 관리 (user 2 = 프론트 담당)
INSERT INTO tasks (id, epic_id, name, description, status, progress, start_date, due_date, assignee_id, created_at, updated_at, created_by, updated_by) VALUES
(7,  4, '출역현황 컴포넌트', '출역 현황 대시보드 UI',     'IN_PROGRESS', 60, '2026-02-15', '2026-03-31', 2, NOW(), NOW(), 'system', 'system'),
(8,  4, '출역 통계 API',     '출역 데이터 집계 API',      'TODO',         0, NULL, '2026-04-15', 1, NOW(), NOW(), 'system', 'system');

-- 용인 플랫폼 > 날씨 API (user 1 = 백엔드 담당)
INSERT INTO tasks (id, epic_id, name, description, status, progress, start_date, due_date, assignee_id, created_at, updated_at, created_by, updated_by) VALUES
(9,  5, '기상청 API 연동',   '단기예보 API 호출',         'DONE',        100, '2026-02-01', '2026-02-20', 1, NOW(), NOW(), 'system', 'system'),
(10, 5, '날씨 위젯',         '날씨 표시 컴포넌트',        'DONE',        100, '2026-02-10', '2026-02-28', 2, NOW(), NOW(), 'system', 'system');

-- 주간보고 시스템 > LLM 채팅 (user 1 = 백엔드, user 3 = 미배정)
INSERT INTO tasks (id, epic_id, name, description, status, progress, start_date, due_date, assignee_id, created_at, updated_at, created_by, updated_by) VALUES
(11, 6, 'Ollama 연동',       'LLM API 호출 서비스',       'DONE',        100, '2026-03-01', '2026-03-05', 1, NOW(), NOW(), 'system', 'system'),
(12, 6, 'ActionHandler 구현', '액션 라우팅 로직',          'IN_PROGRESS', 80, '2026-03-03', '2026-03-10', 1, NOW(), NOW(), 'system', 'system'),
(13, 6, '프론트 채팅 UI',     '채팅 인터페이스 개발',      'TODO',         0, NULL, '2026-04-15', NULL, NOW(), NOW(), 'system', 'system');

-- ── Roles ──
-- ADMIN: auth='ADMIN' → PermissionCheckAspect에서 모든 권한 검사 바이패스
-- WRITER: auth='USER' + WRITE 레벨 도메인 권한 → 조회/생성/수정 가능, 삭제 불가
-- READER: auth='USER' + READ 레벨 도메인 권한 → 조회만 가능
INSERT INTO roles (id, name, description, auth, created_at, updated_at, created_by, updated_by) VALUES
(1, 'ADMIN',  '관리자 (모든 권한)', 'ADMIN', NOW(), NOW(), 'system', 'system'),
(2, 'WRITER', '편집자 (조회/생성/수정)', 'USER', NOW(), NOW(), 'system', 'system'),
(3, 'READER', '뷰어 (조회만)', 'USER', NOW(), NOW(), 'system', 'system');

-- ── User-Role 매핑 ──
-- user 1 → ADMIN, user 2 → WRITER, user 3 → READER
INSERT INTO user_role (id, user_id, role_id, created_at, updated_at, created_by, updated_by) VALUES
(1, 1, 1, NOW(), NOW(), 'system', 'system'),
(2, 2, 2, NOW(), NOW(), 'system', 'system'),
(3, 3, 3, NOW(), NOW(), 'system', 'system');

-- ── Permissions (역할별 권한 그룹) ──
INSERT INTO permission (id, name, description, created_at, updated_at, created_by, updated_by) VALUES
(1, 'WRITER_PERM', '편집자 권한 그룹', NOW(), NOW(), 'system', 'system'),
(2, 'READER_PERM', '뷰어 권한 그룹',   NOW(), NOW(), 'system', 'system');

-- ── Role-Permission 매핑 ──
-- ADMIN(role 1)은 권한 그룹 불필요 (AOP에서 바이패스)
-- WRITER(role 2) → WRITER_PERM, READER(role 3) → READER_PERM
INSERT INTO role_permission (id, role_id, permission_id) VALUES
(1, 2, 1),
(2, 3, 2);

-- ── Domain Permissions ──
-- WRITER_PERM: 5개 리소스 × WRITE 레벨 (조회/생성/수정 가능, 삭제는 ADMIN 레벨 필요)
INSERT INTO domain_permission (id, permission_id, resource_name, level, created_at, updated_at, created_by, updated_by) VALUES
(1,  1, 'USER',    'WRITE', NOW(), NOW(), 'system', 'system'),
(2,  1, 'TEAM',    'WRITE', NOW(), NOW(), 'system', 'system'),
(3,  1, 'PROJECT', 'WRITE', NOW(), NOW(), 'system', 'system'),
(4,  1, 'EPIC',    'WRITE', NOW(), NOW(), 'system', 'system'),
(5,  1, 'TASK',    'WRITE', NOW(), NOW(), 'system', 'system');

-- READER_PERM: 5개 리소스 × READ 레벨 (조회만 가능)
INSERT INTO domain_permission (id, permission_id, resource_name, level, created_at, updated_at, created_by, updated_by) VALUES
(6,  2, 'USER',    'READ', NOW(), NOW(), 'system', 'system'),
(7,  2, 'TEAM',    'READ', NOW(), NOW(), 'system', 'system'),
(8,  2, 'PROJECT', 'READ', NOW(), NOW(), 'system', 'system'),
(9,  2, 'EPIC',    'READ', NOW(), NOW(), 'system', 'system'),
(10, 2, 'TASK',    'READ', NOW(), NOW(), 'system', 'system');

-- 시퀀스 재설정
SELECT setval('teams_id_seq', (SELECT MAX(id) FROM teams));
SELECT setval('team_members_id_seq', (SELECT MAX(id) FROM team_members));
SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));
SELECT setval('epics_id_seq', (SELECT MAX(id) FROM epics));
SELECT setval('tasks_id_seq', (SELECT MAX(id) FROM tasks));
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('user_role_id_seq', (SELECT MAX(id) FROM user_role));
SELECT setval('permission_id_seq', (SELECT MAX(id) FROM permission));
SELECT setval('role_permission_id_seq', (SELECT MAX(id) FROM role_permission));
SELECT setval('domain_permission_id_seq', (SELECT MAX(id) FROM domain_permission));


