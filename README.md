# 나의표정친구 - 치료 노트 & 미션 시스템

## 📋 구현 완료 기능

### 치료 노트 관리
- 노트 CRUD (권한 기반 접근 제어)
- 노트 타입: PARENT_NOTE, THERAPIST_NOTE, SYSTEM
- 검색/필터링 (키워드, 타입, 작성자, 날짜)
- 페이징 지원

### 파일 첨부
- 이미지/비디오/문서 업로드
- 파일 크기 제한: 이미지 5MB, 비디오/문서 10MB
- 파일 다운로드
- 스토리지 사용량 조회

### 댓글 시스템
- 댓글/대댓글 (1depth)
- 계층 구조 지원
- 댓글 수정/삭제

### 홈 트레이닝 미션 시스템 ✨ NEW
- 미션 템플릿 관리 (카테고리별/난이도별)
- 미션 할당 및 상태 관리 (ASSIGNED → IN_PROGRESS → COMPLETED → VERIFIED)
- 미션 완료 증빙 사진 업로드
- 마감일 관리 및 알림
- 자동 시스템 노트 생성 (미션 할당/완료/검증 시)
- 부모-치료사 피드백 시스템

## 🚀 실행 방법

### 환경 변수 설정
```bash
export DB_URL=jdbc:postgresql://localhost:5432/mydb
export DB_USERNAME=postgres
export DB_PASSWORD=password
export JWT_SECRET=your-secret-key
```

### 서버 실행
```bash
./gradlew bootRun
```

### API 문서
- Swagger UI: http://localhost:8080/swagger-ui.html

## 📁 프로젝트 구조
```
common/
├── domain/
│   ├── note/
│   │   ├── ChildNote.java
│   │   ├── NoteAsset.java
│   │   ├── NoteComment.java
│   │   ├── NoteType.java
│   │   └── AssetType.java
│   └── mission/              ✨ NEW
│       ├── MissionTemplate.java
│       ├── AssignedMission.java
│       ├── MissionPhoto.java
│       ├── MissionCategory.java
│       ├── MissionDifficulty.java
│       └── MissionStatus.java
├── repository/
│   ├── ChildNoteRepository.java
│   ├── NoteAssetRepository.java
│   ├── NoteCommentRepository.java
│   ├── MissionTemplateRepository.java     ✨ NEW
│   └── AssignedMissionRepository.java     ✨ NEW
├── service/
│   ├── ChildNoteService.java
│   ├── NoteAssetService.java
│   ├── NoteCommentService.java
│   ├── MissionTemplateService.java        ✨ NEW
│   └── AssignedMissionService.java        ✨ NEW
├── dto/
│   ├── note/
│   │   ├── ChildNoteDTO.java
│   │   ├── NoteAssetDTO.java
│   │   └── NoteCommentDTO.java
│   └── mission/                            ✨ NEW
│       ├── MissionTemplateDTO.java
│       ├── AssignedMissionDTO.java
│       ├── MissionStatusUpdateDTO.java
│       └── MissionSearchDTO.java
└── controller/
    ├── ChildNoteController.java
    ├── NoteAssetController.java
    ├── NoteCommentController.java
    ├── MissionTemplateController.java      ✨ NEW
    └── AssignedMissionController.java      ✨ NEW
```

## 🧪 테스트
```bash
./gradlew test
```

## 🔑 주요 권한

### 노트 시스템
- `VIEW_REPORT`: 노트 조회, 댓글 작성
- `WRITE_NOTE`: 노트 작성
- 노트 수정: 작성자 본인만
- 노트 삭제: 작성자 본인 또는 주보호자

### 미션 시스템 ✨ NEW
- 템플릿 생성/수정/삭제: THERAPIST만
- 미션 할당/검증/취소: THERAPIST만 (할당한 치료사)
- 미션 시작/완료: PARENT만 (WRITE_NOTE 권한)
- 미션 조회: VIEW_REPORT 권한 필요

## 📝 API 엔드포인트

### 노트
- `POST /api/children/{childId}/notes` - 노트 생성
- `GET /api/notes/{noteId}` - 노트 상세 조회
- `GET /api/children/{childId}/notes` - 노트 목록
- `GET /api/children/{childId}/notes/search` - 노트 검색
- `PUT /api/notes/{noteId}` - 노트 수정
- `DELETE /api/notes/{noteId}` - 노트 삭제

### 첨부파일
- `POST /api/notes/{noteId}/assets` - 파일 업로드
- `GET /api/notes/{noteId}/assets` - 첨부파일 목록
- `GET /api/assets/{assetId}/download` - 파일 다운로드
- `DELETE /api/assets/{assetId}` - 파일 삭제

### 댓글
- `POST /api/notes/{noteId}/comments` - 댓글 작성
- `GET /api/notes/{noteId}/comments` - 댓글 목록
- `PUT /api/comments/{commentId}` - 댓글 수정
- `DELETE /api/comments/{commentId}` - 댓글 삭제

### 미션 템플릿 ✨ NEW
- `POST /api/mission-templates` - 템플릿 생성
- `GET /api/mission-templates` - 템플릿 목록
- `GET /api/mission-templates/{templateId}` - 템플릿 상세
- `GET /api/mission-templates/category/{category}` - 카테고리별 조회
- `GET /api/mission-templates/difficulty/{difficulty}` - 난이도별 조회
- `GET /api/mission-templates/search` - 템플릿 검색
- `PUT /api/mission-templates/{templateId}` - 템플릿 수정
- `PATCH /api/mission-templates/{templateId}/activate` - 템플릿 활성화
- `DELETE /api/mission-templates/{templateId}` - 템플릿 삭제

### 할당된 미션 ✨ NEW
- `POST /api/children/{childId}/missions` - 미션 할당
- `GET /api/missions/{missionId}` - 미션 상세
- `GET /api/children/{childId}/missions` - 미션 목록
- `GET /api/children/{childId}/missions/search` - 미션 검색
- `GET /api/children/{childId}/missions/overdue` - 마감일 지난 미션
- `GET /api/children/{childId}/missions/pending-verification` - 완료 대기 미션
- `PATCH /api/missions/{missionId}/start` - 미션 시작
- `PATCH /api/missions/{missionId}/complete` - 미션 완료
- `PATCH /api/missions/{missionId}/verify` - 미션 검증
- `PATCH /api/missions/{missionId}/cancel` - 미션 취소
- `DELETE /api/missions/{missionId}` - 미션 삭제

## 🎯 미션 상태 전이
```
ASSIGNED (할당됨)
    ↓ 부모가 시작
IN_PROGRESS (진행중)
    ↓ 부모가 완료 + 사진 업로드
COMPLETED (완료)
    ↓ 치료사가 검증
VERIFIED (검증완료)

※ 어느 단계에서든 CANCELLED (취소) 가능 (치료사만)
```

## 🔔 자동 알림 기능

미션 관련 이벤트 발생 시 자동으로 시스템 노트 생성:
- 미션 할당 시
- 미션 완료 시
- 미션 검증 완료 시