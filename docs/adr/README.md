# 아키텍처 결정 기록 (ADR)

이 디렉터리는 프로젝트의 주요 아키텍처·설계 결정을 기록한다.
배경과 규칙은 [ADR-0001](0001-record-architecture-decisions.md) 참조.

## 목록

| 번호 | 제목 | 상태 | 요약 |
|------|------|------|------|
| [0001](0001-record-architecture-decisions.md) | 아키텍처 결정을 ADR로 기록한다 | Accepted | `docs/adr/` + 간소화 MADR 형식 채택 |
| [0002](0002-state-machine-implementation.md) | 상태머신 구현 방식 | Accepted | enum 기반 전이 테이블 직접 구현. Spring Statemachine 미채택 |
| [0003](0003-numbering-strategy.md) | 채번 전략과 동시성 제어 | Accepted | 전용 시퀀스 테이블 + 행 잠금. 엔티티 UNIQUE 제약을 안전망으로. 결번 허용 |
| [0004](0004-integration-testing-with-testcontainers.md) | 통합테스트 인프라로 Testcontainers를 사용한다 | Accepted | 단위/통합 2계층 분리. 통합은 프로덕션 동일 DB 컨테이너 |
| [0005](0005-schema-migration-tool.md) | 스키마 마이그레이션 도구로 Flyway를 사용한다 | Accepted | SQL-first, forward-only. seed는 프로파일 분리 |

## 상태값

`Proposed` → `Accepted` → (`Superseded by ADR-XXXX` | `Deprecated`)

## 새 ADR 추가

1. 다음 번호로 `NNNN-kebab-case-title.md` 생성 ([0001](0001-record-architecture-decisions.md)의 골격 사용)
2. 위 목록 표에 한 줄 추가
3. 관련 문서에서 상호 링크
