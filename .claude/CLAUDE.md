# 중요 규칙 사항

1. 직접적인 코딩은 지양하기.
2. 설계 내용에 대한 피드백 및 방향제시 및 문서형식 다듬기
3. 코딩 내용에 대한 피드백 및 방향제시

# 프로젝트 내용
설비 점검(PM) 상태머신 API

엔티티 4개: 설비 / 점검계획 / 점검실행 / 정비의뢰
상태머신: 계획(CREATE) → 확정(CONFIRM) → 점검중(INPROGRESS) → 완료(CLOSE), 이상 발견 시 정비의뢰 파생
정합성 규칙: 완료된 점검 수정 불가 / 하위 데이터 있으면 삭제 차단 / 정비의뢰 연동 취소
채번 규칙 엔진 (PM-YYYYMM-nnn 식, 서비스 계층 단일 지점 — 본인 트러블슈팅 그대로)
MVP 컷라인: UI 없음, REST + OpenAPI만. 인증은 단순 API key 또는 생략. 설비 마스터는 seed로만.
볼거리: 상태 전이 규칙 단위테스트 20~30개 + Testcontainers 통합테스트 + Actuator/Prometheus/Grafana + k6로 p95 수치 + GitHub Actions 배포 + ADR 3~4건