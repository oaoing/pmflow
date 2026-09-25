# PMFlow — 설비 점검(PM) 상태머신 API

설비 예방 점검의 계획–실행–정비 흐름을 상태머신으로 관리하는 REST API.
상태 전이 규칙과 정합성 규칙을 코드·테스트로 엄격하게 강제하는 것이 목표다.

> 상태: 설계 완료 / 구현 착수 전. 아래 실행·관측 항목은 대부분 _(예정)_ 이다.

---

## 개요

설비마다 주기적 점검 **계획(PLAN)** 을 세우고, 승인되면 주기마다 점검 **실행(CHECK)** 이 생성된다.
점검 중 이상이 발견되면 **정비의뢰(ORDER)** 가 파생되어 독립적으로 처리된다.

```
계획 CREATE ─승인→ CONFIRM ─만료→ CLOSE
                    │
                    └─(주기)→ 실행 CREATE ─착수→ INPROGRESS ─완료→ CLOSE
                                                    │
                                                    └─(이상)→ 정비의뢰 CREATE → CONFIRM → INPROGRESS → CLOSE
```

전체 전이 매트릭스·정합성 규칙은 [docs/domain-model.md](docs/domain-model.md) 참조.

## 범위 (MVP 컷라인)

**포함**

- 엔티티 4개: 설비 / 점검계획 / 점검실행 / 정비의뢰
- 상태머신: `CREATE → CONFIRM → INPROGRESS → CLOSE` (+ `CANCEL`), 정비의뢰 파생
- 정합성 규칙: 완료 건 수정 불가(R01) / 하위 데이터 있으면 삭제 불가(R02) / 점검·정비 라이프사이클 분리(R03)
- 채번 엔진: `PM-YYYYMM-nnn` 형식, 서비스 계층 단일 지점, 동시성 제어
- REST + OpenAPI 문서

**제외**

- UI (프론트엔드 없음)
- 인증/인가 — 단순 API key 또는 생략 (`local`/`test` 프로파일 비활성)
- 설비 마스터 CRUD — seed 데이터로만 제공
- 점검 항목(체크리스트) 상세, 설비 가동상태 연동, 상태 전이 이력

## 기술 스택

| 영역 | 선택 |
|------|------|
| 언어 | Java 25 (LTS) |
| 프레임워크 | Spring Boot (Java 25 지원 라인) |
| 빌드 | Gradle (Kotlin DSL) |
| DB | PostgreSQL |
| 마이그레이션 | Flyway |
| API 문서 | springdoc-openapi |
| 테스트 | JUnit 5, Testcontainers, k6 |
| 관측 | Actuator + Micrometer + Prometheus + Grafana |

근거는 [docs/adr/](docs/adr/README.md).

## 아키텍처

- 기능(엔티티)별 수직 분할 + 상태머신·채번 엔진은 `common`으로 추출
- 레이어: `web → service → repository`, 엔티티 간 참조는 ID 값 (`@ManyToOne` 배제)
- 모든 상태 변경은 `로드 → R01 검증 → 교차 가드 → 전이 판정 → 부수효과 → 저장` 템플릿

상세: [docs/architecture.md](docs/architecture.md)

## 실행법 _(예정)_

**요구사항**: JDK 25, Docker (Testcontainers / 로컬 DB)

```bash
# 로컬 DB 기동 (예정 — docker compose)
docker compose up -d db

# 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 기본 포트: `8080` _(예정)_
- 프로파일: `local` (seed 로드, 인증 비활성) / `test` (Testcontainers) / `prod`

## API _(예정)_

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui.html`
- 엔드포인트 목록: [docs/architecture.md](docs/architecture.md) §7
- 에러 응답 규약: `docs/api-conventions.md` _(예정)_

## 테스트 _(예정)_

```bash
./gradlew test              # 단위: 상태 전이 테이블, 채번 포맷
./gradlew integrationTest   # 통합: Testcontainers (동시성, 정합성 규칙, cron)
```

- 상태 전이 케이스 도출: [docs/domain-model.md](docs/domain-model.md) 부록 (~37개)
- 전략: [docs/adr/0004-integration-testing-with-testcontainers.md](docs/adr/0004-integration-testing-with-testcontainers.md)

## 관측 _(예정)_

- 메트릭: `GET /actuator/prometheus`
  - `pm_state_transition_total{entity,from,to,result}`
  - `pm_numbering_duration_seconds{scope_prefix}`
- Grafana 대시보드: `ops/grafana/`
- k6 부하 시나리오 및 p95 결과: `ops/k6/` — 수치 `TBD`

## 문서

| 문서 | 내용 |
|------|------|
| [docs/domain-model.md](docs/domain-model.md) | 엔티티, 상태 전이 매트릭스, 정합성·채번 규칙 |
| [docs/architecture.md](docs/architecture.md) | 패키지 구조, 레이어링, 구현 순서 |
| [docs/adr/](docs/adr/README.md) | 아키텍처 결정 기록 (0001~0005) |
| `docs/api-conventions.md` | 에러 코드·HTTP 매핑 _(예정)_ |

## 로드맵

1. 스켈레톤 (Gradle, Boot, Flyway V1~V5, 설비 seed·조회, Actuator)
2. 공용 엔진 (`statemachine`, `numbering`, `error`) + 단위 테스트
3. PLAN 수직 슬라이스
4. CHECK 수직 슬라이스 + cron 생성/만료 잡
5. ORDER 수직 슬라이스
6. 정합성 규칙 배선 + 통합 테스트
7. 동시성·부하 테스트, Prometheus/Grafana
8. GitHub Actions (build → test → 이미지 → 배포)
