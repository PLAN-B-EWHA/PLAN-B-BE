# MyExpressionFriend Backend

아동 감정/행동 훈련 지원을 위한 Spring Boot 백엔드입니다.  
권한 기반 아동 관리, 미션/노트, JWT 인증, SSE 알림, AI 리포트 발행 기능을 제공합니다.

## 기술 스택
- Java 17
- Spring Boot 3.5
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Swagger(OpenAPI)

## 핵심 기능

### 1) 인증/권한
- 회원가입 시 기본 `PENDING` 권한 부여
- 관리자(`ADMIN`)가 `PARENT/TEACHER/THERAPIST`로 승급
- Access/Refresh 토큰 기반 인증
- Refresh 토큰 서버 저장/회전/로그아웃 무효화

### 2) 아동/권한 관리
- 아동 생성/조회/수정/삭제
- 아동 PIN 설정/검증/제거
- 주보호자 변경
- 아동별 사용자 권한 부여/수정/취소

### 3) 미션/노트
- 미션 템플릿 관리
- 미션 하달/진행/완료/검증/취소
- 미션 사진 업로드
- 노트/댓글/첨부파일 관리

### 4) 알림(SSE + 목록)
- 실시간 SSE 스트림: `GET /api/notifications/stream`
- 알림 목록 조회/읽음 처리
- 이벤트 기반 알림:
  - 미션 완료
  - 미션 사진 업로드
  - 리포트 생성 완료

### 5) AI 리포트
- 리포트 설정(주기/시간/타임존/모델/프롬프트 등)
- 수동 생성 테스트 API: `POST /api/reports/test-generate`
- 자동 발행 스케줄러:
  - `enabled=true && nextIssueAt<=now` 대상 처리
  - 실패 시 백오프(다음 발행 시각 지연)
- Gemini 429(쿼터 초과) 사용자 친화 예외 응답

## 실행 방법

## 1. 환경 변수
아래 값은 코드에 하드코딩하지 않고 OS 환경 변수로 주입하는 것을 권장합니다.

```bash
DB_URL=jdbc:postgresql://localhost:5432/myexpressionfriend
DB_USERNAME=postgres
DB_PASSWORD=...
JWT_SECRET=...

LLM_GEMINI_ENABLED=true
GEMINI_API_KEY=...
GEMINI_MODEL=gemini-2.0-flash

REPORT_SCHEDULER_ENABLED=true
REPORT_SCHEDULER_DELAY_MS=60000
```

## 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

## 3. API 문서
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## 주요 API

### 인증
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### 사용자
- `GET /api/users/me`
- `PUT /api/users/me`
- `PATCH /api/users/{userId}/role` (ADMIN)

### 아동/권한
- `POST /api/children`
- `GET /api/children/my`
- `GET /api/children/{childId}`
- `POST /api/children/{childId}/authorizations`
- `PUT /api/children/{childId}/authorizations/{targetUserId}`

### 미션
- `POST /api/children/{childId}/missions`
- `PATCH /api/missions/{missionId}/status`
- `POST /api/missions/{missionId}/photos`

### 알림
- `GET /api/notifications/stream`
- `GET /api/notifications`
- `PATCH /api/notifications/{notificationId}/read`

### 리포트
- `GET /api/reports/preferences/me`
- `PUT /api/reports/preferences/me`
- `POST /api/reports/test-generate`
- `GET /api/reports/me`
- `GET /api/reports/{reportId}`
