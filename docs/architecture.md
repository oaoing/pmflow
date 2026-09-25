# 구현 아키텍처

[domain-model.md](domain-model.md)와 [ADR 0001~0005](adr/README.md)를 코드 구조로 옮기기 위한 방향 문서.
결정이 필요한 지점은 📌로 표시한다.

---

## 1. 기술 스택

| 영역 | 선택 | 근거 / 참조 |
|------|------|-------------|
| 언어 / 런타임 | Java 25 (LTS) | `record`·패턴 매칭·가상 스레드 활용. **Spring Boot 버전을 Java 25 지원 라인(Boot 3.5+ / 4.0)으로 맞출 것** |
| 프레임워크 | Spring Boot (Java 25 지원 라인) | `ProblemDetail`, `@ServiceConnection` 등 활용 |
| 빌드 | Gradle (Kotlin DSL) | |
| DB | PostgreSQL | [ADR-0003](adr/0003-numbering-strategy.md) |
| 마이그레이션 | Flyway | [ADR-0005](adr/0005-schema-migration-tool.md) |
| 영속 | Spring Data JPA / Hibernate | `ddl-auto=none` |
| API 문서 | springdoc-openapi | MVP 컷라인: REST + OpenAPI |
| 테스트 | JUnit 5, Testcontainers, k6 | [ADR-0004](adr/0004-integration-testing-with-testcontainers.md) |
| 관측 | Actuator + Micrometer + Prometheus + Grafana | MVP 볼거리 |
| 인증 | API key 필터 (프로파일로 on/off) 또는 생략 | MVP 컷라인 |

---

## 2. 패키지 구조

기능(엔티티)별 수직 분할 + 공용 엔진은 `common`으로 추출.

```
com.pmflow
├── PmflowApplication
│
├── common
│   ├── domain
│   │   ├── BaseEntity            // 감사 컬럼 (created_at/by, modified_at/by)
│   │   ├── Status                // CREATE·CONFIRM·INPROGRESS·CLOSE·CANCEL
│   │   └── Unit                  // DAY·WEEK·MONTH·YEAR
│   ├── statemachine
│   │   ├── StateMachine<S>       // canTransition / transition / nextStates
│   │   ├── TransitionTable<S>    // EnumMap<S, EnumSet<S>> 빌더
│   │   └── IllegalStateTransitionException
│   ├── numbering
│   │   ├── NumberingService      // next(scopeKey): long  — REQUIRES_NEW
│   │   ├── NumberingSequence     // 엔티티 (scope_key PK, last_seq)
│   │   ├── NumberingSequenceRepository
│   │   ├── NumberFormatter       // prefix + YYYYMM + seq 조립 + 자릿수 검증
│   │   └── NumberingOverflowException
│   ├── error
│   │   ├── DomainException       // 최상위
│   │   ├── NotModifiableException / HasChildrenException / PreconditionViolationException
│   │   └── GlobalExceptionHandler // @RestControllerAdvice → ProblemDetail
│   └── config
│       ├── OpenApiConfig
│       ├── ApiKeyFilter          // 프로파일 조건부
│       └── SchedulingConfig
│
├── equipment                    // EQUIP — seed only, 읽기 전용
│   ├── domain/Equipment
│   ├── EquipmentRepository
│   ├── EquipmentService
│   └── web/EquipmentController + dto
│
├── plan                         // PLAN
│   ├── domain/Plan              // confirm()/cancel()/close() — 자기 불변식 + StateMachine 호출
│   ├── PlanRepository
│   ├── PlanService              // 오케스트레이션 (§5 템플릿)
│   ├── PlanStateMachine         // 4.1 매트릭스
│   └── web/PlanController + dto
│
├── check                        // CHECK
│   ├── domain/Check
│   ├── CheckRepository
│   ├── CheckService
│   ├── CheckGenerationService   // cron: CONFIRM 계획 → 주기 도래 시 CHECK 생성
│   ├── CheckStateMachine        // 4.2 매트릭스
│   └── web/CheckController + dto
│
├── order                        // ORDER (정비의뢰)
│   ├── domain/RepairOrder
│   ├── RepairOrderRepository
│   ├── RepairOrderService
│   ├── RepairOrderStateMachine  // 4.3 매트릭스
│   └── web/RepairOrderController + dto
│
└── scheduler
    ├── CheckGenerationJob       // @Scheduled → CheckGenerationService 위임
    └── PlanExpiryJob            // @Scheduled → valid_date 경과 계획 CLOSE
```

> `order`는 SQL 예약어이므로 테이블명은 `repair_order`, 클래스는 `RepairOrder`로 둔다.

---

## 3. 레이어링 규칙

```
web (Controller, DTO)  →  service  →  repository  →  DB
```

- **Controller**: 요청/응답 DTO 변환, 검증(`@Valid`), 서비스 호출만. 엔티티를 직접 반환하지 않는다.
- **Service**: 트랜잭션 경계. 유스케이스 1개 = 메서드 1개. §5 템플릿을 따른다.
- **Entity**: 자기 자신만으로 판단 가능한 불변식은 엔티티 메서드로 (`plan.confirm()` 내부에서 `PlanStateMachine.transition(status, CONFIRM)` 호출). 다른 애그리거트를 봐야 하는 가드는 서비스가 담당.
- **Repository**: 쿼리만. 비즈니스 로직 없음.
- 엔티티 간 참조는 **ID 값**으로 (`plan.equipId`), JPA 연관관계(`@ManyToOne`) 남용하지 않는다 — R02 삭제 검증은 `countByPlanId(...)` 같은 명시 쿼리로. (§3.1 대안 참조)

### 3.1 연관관계 매핑: ID 참조 vs JPA 연관관계

| | 채택: ID 참조 | 대안: JPA 연관관계 |
|--|--------------|-------------------|
| 필드 | `Long equipId` | `@ManyToOne Equipment equip` / `@OneToMany List<Check> checks` |
| R02 자식 확인 | `checkRepository.existsByPlanId(planId)` — 의도가 쿼리에 드러남 | `!plan.getChecks().isEmpty()` — 컬렉션 전체 로딩 유발, `LazyInitializationException` 위험 |
| 삭제 차단 | 서비스에서 명시 검증 후 거부 | `orphanRemoval`/`CascadeType`에 의존 → 실수로 연쇄 삭제되기 쉬움 |
| 조회 성능 | 필요한 것만 별도 조회 (N+1 없음) | fetch 전략 튜닝 필요, `@EntityGraph` 등 부가 작업 |
| 애그리거트 경계 | 명확 — 각 엔티티가 독립 애그리거트 루트 | 흐려짐 — 큰 객체 그래프 |
| 트레이드오프 | 조인 조회 시 코드가 다소 장황, 참조 무결성은 DB FK + 서비스 검증으로 보장 | 객체 탐색은 편하지만 상태머신·정합성 로직에서 부작용 통제가 어려움 |

**대안을 택할 경우**: 최소한 모든 연관을 `FetchType.LAZY`로 고정하고, `CascadeType`·`orphanRemoval`을 쓰지 않으며(R02를 프레임워크에 위임하지 않음), 컬렉션 순회 대신 `count`/`exists` 파생 쿼리를 별도로 두어야 한다. 즉 대안을 택해도 R02 검증 코드는 거의 동일하게 필요하므로, 이득이 크지 않다.

---

## 4. 상태 전이 엔진 (ADR-0002 구체화)

```java
interface StateMachine<S extends Enum<S>> {
    boolean canTransition(S from, S to);
    S transition(S from, S to);          // 허용 시 to 반환, 아니면 IllegalStateTransitionException
    Set<S> nextStates(S from);
}
```

- 엔티티별 구현이 **허용 전이 테이블만** 선언한다. 예 (PLAN):

  ```
  CREATE  → { CONFIRM, CANCEL }
  CONFIRM → { CLOSE }
  CLOSE   → { }
  CANCEL  → { }
  ```

- **가드(선행조건)는 테이블에 넣지 않는다.** "상위 PLAN이 CONFIRM인가", "자식이 없는가"는 서비스가 검증.
- 순수 함수 → domain-model 부록의 전이 테스트가 스프링·DB 없이 여기에 직결.

---

## 5. 서비스 오케스트레이션 템플릿

모든 상태 변경 유스케이스가 따르는 순서:

```
1. 대상 로드            repository.findById(...) or throw NotFound
2. 수정 가능성 검증      R01: status ∈ {CLOSE, CANCEL} 이면 NotModifiableException
3. 교차 애그리거트 가드   예: CHECK 생성 시 상위 PLAN.status == CONFIRM
4. 상태 전이 판정        stateMachine.transition(current, target)
5. 부수효과             채번(NumberingService.next), 타임스탬프(approved_at 등)
6. 저장                 repository.save(...)  → BaseEntity가 modified_* 갱신
7. (선택) 메트릭        state_transition_total{result=...} 증가
```

- 2~6은 하나의 `@Transactional` 안에서. 단 5의 채번은 `NumberingService`가 `REQUIRES_NEW`로 분리 커밋([ADR-0003](adr/0003-numbering-strategy.md)).
- 이 순서를 강제하는 얇은 헬퍼(`StatefulUseCase` 유틸 메서드)를 둘 수 있으나, 과한 추상화는 지양하고 서비스마다 명시적으로 작성하는 편을 우선한다.

---

## 6. 채번 (ADR-0003 구체화)

| 엔티티 | scope_key | formatter 출력 |
|--------|-----------|----------------|
| PLAN | `PM-{YYYYMM}` | `PM-YYYYMM-nnn` |
| CHECK | `PM-{계획YYYYMM}-{nnn}` | `PM-YYYYMM-nnn-mmmm` |
| ORDER | `OD-{YYYYMM}` | `OD-YYYYMM-nnn` |

```java
@Transactional(propagation = REQUIRES_NEW)
long next(String scopeKey) {
    int updated = repo.increment(scopeKey);          // UPDATE ... SET last_seq = last_seq + 1
    if (updated == 0) { repo.insertStart(scopeKey);  // 최초 1건, 경합 시 unique 제약으로 1건만
                        return next(scopeKey); }
    return repo.findLastSeq(scopeKey);
}
```

- `NumberFormatter`가 `nnn > 999 || mmmm > 9999` 이면 `NumberingOverflowException`.
- 각 엔티티 테이블의 `code` 컬럼에 `UNIQUE` — 최종 안전망.

---

## 7. API 표면

상태 전이는 `PATCH status`가 아니라 **행위 하위 리소스**(`POST /{id}/{action}`)로 노출한다 — 상태머신과 1:1.

| 리소스 | 엔드포인트 |
|--------|-----------|
| 설비 | `GET /api/equipments`, `GET /api/equipments/{id}` |
| 계획 | `POST /api/plans`, `GET /api/plans`, `GET /api/plans/{id}`, `POST /api/plans/{id}/confirm`, `POST /api/plans/{id}/cancel`, `POST /api/plans/{id}/close` |
| 실행 | `GET /api/checks`, `GET /api/checks/{id}`, `POST /api/checks/{id}/start`, `POST /api/checks/{id}/complete`, `POST /api/checks/{id}/cancel` |
| 정비의뢰 | `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/confirm`, `POST /api/orders/{id}/start`, `POST /api/orders/{id}/complete`, `POST /api/orders/{id}/cancel` |
| 삭제 | `DELETE /api/plans/{id}`, `DELETE /api/checks/{id}` (R02 검증) |

- 일반 필드 수정: `PATCH /api/plans/{id}` 등 (R01 검증).
- 에러 응답: RFC 7807 `application/problem+json`. 예외→상태코드→`type` 매핑표는 `api-conventions.md`에서 관리.

---

## 8. Flyway 마이그레이션 (ADR-0005)

```
src/main/resources/db/migration/
  V1__create_equipment.sql
  V2__create_plan.sql
  V3__create_check.sql
  V4__create_repair_order.sql
  V5__create_numbering_sequence.sql

src/main/resources/db/seed/           # local·test 프로파일에서만 로드
  R__seed_equipment.sql
```

- 각 `V` 파일에 해당 테이블의 PK·FK·UNIQUE·인덱스를 함께 포함.
- `code` 컬럼 `UNIQUE`, FK 컬럼에 인덱스.

---

## 9. 스케줄러

| 잡 | 주기 | 동작 | 멱등성 |
|----|------|------|--------|
| `CheckGenerationJob` | 예: 매시 정각 | `status=CONFIRM` 이고 `next_scheduled_at <= now` 인 계획에 CHECK 생성 후 `next_scheduled_at` 전진 | `next_scheduled_at` 기준 조회 → 중복 생성 없음 (결정 E) |
| `PlanExpiryJob` | 예: 매일 00:10 | `valid_date < now` 인 `CONFIRM` 계획을 `CLOSE` | 이미 `CLOSE`면 skip |

- 실제 생성/만료 로직은 `*Service`에 두고 잡은 위임만 → 스케줄러 없이 테스트 가능.

---

## 10. 관측

- `spring-boot-starter-actuator` + `micrometer-registry-prometheus`, `/actuator/prometheus` 노출.
- 커스텀 메트릭:
  - `pm_state_transition_total{entity, from, to, result}` — counter
  - `pm_numbering_duration_seconds{scope_prefix}` — timer (ADR-0003 병목 측정용)
  - `pm_check_generation_batch_size` — gauge/summary
- Grafana 대시보드 JSON은 `ops/grafana/`에 저장.
- k6 스크립트는 `ops/k6/`, 계획 생성·상태 전이 시나리오로 p95 측정.

---

## 11. 테스트 배치 (ADR-0004)

| 파일 | 유형 | 대상 |
|------|------|------|
| `*StateMachineTest` | 단위 | 전이 테이블 (허용/금지/터미널) — domain-model 부록 ~30개 |
| `NumberFormatterTest` | 단위 | 포맷 조립, 오버플로 예외 |
| `NumberingConcurrencyIT` | 통합 | N스레드 동시 채번 → 중복 0 (CountDownLatch) |
| `PlanLifecycleIT` / `CheckLifecycleIT` / `RepairOrderLifecycleIT` | 통합 | 해피패스 + 가드 위반 |
| `ConsistencyRuleIT` | 통합 | R01 수정 거부, R02 삭제 거부, R03 분리 |
| `CheckGenerationIT` | 통합 | cron 로직 (스케줄러 우회 호출) |

---

## 12. 구현 순서 (증분)

각 단계가 끝나면 테스트가 초록이고 앱이 뜬다.

1. **스켈레톤** — Gradle, Boot, `application*.yml`, Flyway V1~V5, Actuator, `EquipmentController`(seed 읽기), OpenAPI 노출.
2. **공용 엔진** — `common/statemachine`, `common/numbering`, `common/error` + 단위 테스트.
3. **PLAN 수직** — 엔티티·마이그레이션 확인, `create/confirm/cancel/close`, `PlanLifecycleIT`.
4. **CHECK 수직** — `start/complete/cancel` + `CheckGenerationJob` + `CheckGenerationIT`.
5. **ORDER 수직** — `create/confirm/start/complete/cancel` + IT.
6. **정합성 규칙 통합** — R01/R02/R03을 서비스에 배선, `ConsistencyRuleIT`, `GlobalExceptionHandler` 완성.
7. **동시성·부하** — `NumberingConcurrencyIT`, k6 스크립트, Prometheus 메트릭·Grafana 대시보드.
8. **CI/CD** — GitHub Actions: build → 단위 → 통합(TC) → 이미지 빌드 → 배포.

---

## 결정 완료 (2026-09-07)

| # | 항목 | 결정 |
|---|------|------|
| A | PK 전략 | **대리키(`Long`, IDENTITY) + `code VARCHAR UNIQUE`**. 업무번호는 `code` 컬럼. FK도 대리키(`plan_id BIGINT`) 참조. domain-model 2장 ERD를 이에 맞게 갱신 |
| B | Java / 빌드 | **Java 25 (LTS) + Gradle KTS**. Spring Boot는 Java 25 지원 라인으로 |
| C | 인증 | **API key 필터 추가, `local`/`test` 프로파일에서 비활성.** 감사 주체(`created_by`/`modified_by`)는 API key 주체로 채움 — 아래 §C |
| D | 연관관계 | **ID 참조, `@ManyToOne` 배제** (§3.1에 대안·트레이드오프 정리) |
| E | CHECK 예정일 | **PLAN에 `next_scheduled_at` 보관.** cron이 `next_scheduled_at <= now` 계획을 조회 → CHECK 생성(`scheduled_at = plan.next_scheduled_at`) → `plan.next_scheduled_at += period·unit` 로 전진. 멱등·조회 단순 |

### C. 감사 주체(auditor) 해석

`BaseEntity`의 `created_by` / `modified_by`는 **요청을 인증한 API key의 주체 문자열**로 채운다. 흐름:

```
ApiKeyFilter (주체 해석) → ActorContext (ThreadLocal) → AuditorAware → Spring Data JPA Auditing
```

1. **`ApiKeyFilter`** (`OncePerRequestFilter`): `X-API-Key` 헤더를 설정값(`pm.api-keys`, `Map<발급키, 주체이름>`)과 대조. 유효하면 주체 이름을 `ActorContext.set(...)`, 무효/누락이고 인증 필수 프로파일이면 401. **키 값이 아니라 주체 이름(클라이언트 식별자)을 저장한다.**
2. **`ActorContext`**: `ThreadLocal<String>` 홀더. 필터가 `try/finally`로 감싸 요청 종료 시 반드시 `clear()` (풀 스레드 누수 방지).
3. **`AuditorAware<String>`**: `ActorContext.get().orElse("system")` 반환. 요청 밖(cron 잡, seed, 테스트)에서는 항상 `"system"`.
4. **Spring Data JPA Auditing으로 통일**: 손으로 만든 `@PrePersist`/`@PreUpdate`를 걷어내고 `BaseEntity`에 `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`/`@LastModifiedBy`. 설정 클래스에 `@EnableJpaAuditing(auditorAwareRef = "auditorAware")`. 날짜와 주체를 한 메커니즘이 처리.
5. **cron 잡**: 요청 컨텍스트가 없어 자동으로 `"system"`. 구분이 필요하면 잡 진입 시 `ActorContext.set("batch")` + `finally clear()`.

`local` / `test` 프로파일은 인증 비활성이므로 주체 미설정 → 감사값은 `"system"`.

> 이는 상태·전이 로직을 우회하는 `@Modifying` 벌크 UPDATE에는 적용되지 않는다(라이프사이클 콜백 미실행). 벌크 쿼리를 쓸 경우 `modified_at`·`modified_by`를 쿼리 안에서 직접 갱신해야 한다 — 그래서 이 프로젝트는 벌크 대신 엔티티 순회를 기본으로 한다(§4·§5).
