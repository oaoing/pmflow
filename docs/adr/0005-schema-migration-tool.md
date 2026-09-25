# ADR-0005: 스키마 마이그레이션 도구로 Flyway를 사용한다

- 상태: 수용됨 (Accepted)
- 날짜: 2026-09-06
- 관련: [ADR-0003](0003-numbering-strategy.md)(`numbering_sequence` 스키마) · [ADR-0004](0004-integration-testing-with-testcontainers.md)(테스트에 마이그레이션 적용) · [domain-model.md](../domain-model.md) 2장(엔티티)

## 컨텍스트

스키마를 코드로 버전 관리해야 한다.

- [ADR-0004](0004-integration-testing-with-testcontainers.md)에서 `ddl-auto`를 쓰지 않기로 했고, 통합테스트도 프로덕션과 동일한 마이그레이션 경로로 스키마를 만든다.
- 관리 대상: 엔티티 4개 테이블, `numbering_sequence` 테이블, 유니크/외래키 제약, 설비 마스터 seed 데이터.
- DB는 단일 엔진(PostgreSQL, [ADR-0003](0003-numbering-strategy.md) 전제).
- 배포는 GitHub Actions 파이프라인.

## 결정 동인

1. 진입장벽과 러닝커브 — MVP.
2. SQL 가시성 — 리뷰·학습 관점(포트폴리오)에서 스키마 변경이 그대로 읽히는가.
3. Spring Boot 통합 정도.
4. 롤백 요구 — 실제로 필요한 수준.

## 검토한 선택지

| 선택지 | 평가 |
|--------|------|
| **A. Flyway** | ✅ **채택**. SQL-first, 순차 버전 파일. Spring Boot 자동 실행. 러닝커브 낮음. 단점: 자동 롤백은 상용(Teams) 기능 — OSS는 forward-only |
| B. Liquibase | XML/YAML changelog + DB 추상화 + 롤백 기본 제공. preconditions·contexts 등 기능 풍부. 단점: 추상화 레이어 러닝커브, changelog가 장황하고 SQL이 한 겹 가려진다. 단일 DB엔 이점이 상쇄됨 |
| C. JPA `ddl-auto` | ❌ [ADR-0004](0004-integration-testing-with-testcontainers.md)에서 이미 배제. 프로덕션 부적합 |
| D. 수동 SQL 스크립트 | ❌ 적용 이력·멱등성을 직접 관리 → 취약, 팀/CI에서 재현 불가 |

## 결정

**A. Flyway**를 사용한다.

### 방침

- 위치·네이밍: `src/main/resources/db/migration/V{n}__{snake_case_desc}.sql` (예: `V1__create_equipment.sql`, `V2__create_plan.sql`, `V6__create_numbering_sequence.sql`).
- **forward-only**: 자동 롤백을 쓰지 않는다. 잘못된 마이그레이션은 되돌리는 새 버전(`V{n+1}`)을 추가해 교정한다. MVP 규모에서 이 규율로 충분하다.
- **불변 규칙**: 이미 적용된 `V` 파일은 수정하지 않는다(Flyway checksum 검증). 모든 변경은 새 파일.
- **통합테스트**: Testcontainers 컨테이너 기동 후 동일 마이그레이션을 자동 적용([ADR-0004](0004-integration-testing-with-testcontainers.md)). 별도 테스트 스키마 정의를 두지 않는다.
- **seed 데이터**: 설비 마스터 seed는 버전 마이그레이션(`V`)에 넣지 않는다. `db/seed/` 별도 위치에 두고 `local`·`test` 프로파일에서만 로드한다(프로덕션 오염 방지). 데이터 변경이 잦으므로 repeatable(`R__`) 또는 `ApplicationRunner`로 관리.
- baseline 불필요 — 신규 프로젝트.

## 결과

### 긍정적

- 스키마 변경 이력이 순수 SQL로 저장소에 남아 리뷰·추적이 쉽다.
- Spring Boot가 기동 시 자동 적용 — 별도 실행 단계 없음.
- 로컬·CI·프로덕션이 동일한 마이그레이션 경로를 탄다.

### 트레이드오프

- **자동 롤백 없음**: forward-fix 규율에 의존한다. 파괴적 변경(컬럼 삭제 등)은 다단계로 나눠 배포하는 습관이 필요하다.
- SQL이 PostgreSQL 방언에 종속된다. 단일 DB 전제이므로 현재는 문제되지 않는다.

### 재검토 트리거

- 다중 DB 엔진 지원이 요구되면 → Liquibase 또는 방언 분리 재검토.
- 조건부 마이그레이션·자동 롤백·환경별 대량 분기가 필요해지면 → Liquibase 재검토.
