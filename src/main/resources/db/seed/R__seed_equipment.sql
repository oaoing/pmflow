-- 설비 마스터 seed. local / test 프로파일에서만 로드된다 (ADR-0005).
-- repeatable 마이그레이션(R__): 파일 내용이 바뀔 때마다 재실행되므로 멱등해야 한다.

INSERT INTO equipment (code, name, period, unit, description,
                       created_at, created_by, modified_at, modified_by)
VALUES
    ('EQ-0001', '1호기 공조유닛',   30, 'DAY',   '2층 클린룸 급기',        now(), 'seed', now(), 'seed'),
    ('EQ-0002', '2호기 냉동기',     90, 'DAY',   '지하 기계실',            now(), 'seed', now(), 'seed'),
    ('EQ-0003', '컴프레서 A',        4, 'WEEK',  '도장 라인 공압 공급',    now(), 'seed', now(), 'seed'),
    ('EQ-0004', '컨베이어 벨트 L1',  6, 'MONTH', '조립 1라인',             now(), 'seed', now(), 'seed'),
    ('EQ-0005', '옥상 항온항습기',   1, 'YEAR',  '전산실 항온항습',        now(), 'seed', now(), 'seed')
ON CONFLICT (code) DO NOTHING;
