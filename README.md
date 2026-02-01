# 나의표정친구 - 노트 시스템

## 📋 구현 완료 기능

### 노트 관리
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
├── domain/note/
│   ├── ChildNote.java
│   ├── NoteAsset.java
│   ├── NoteComment.java
│   ├── NoteType.java
│   └── AssetType.java
├── repository/
│   ├── ChildNoteRepository.java
│   ├── NoteAssetRepository.java
│   └── NoteCommentRepository.java
├── service/note/
│   ├── ChildNoteService.java
│   ├── NoteAssetService.java
│   └── NoteCommentService.java
├── dto/note/
│   ├── ChildNoteDTO.java
│   ├── NoteAssetDTO.java
│   └── NoteCommentDTO.java
└── exception/
    └── GlobalExceptionHandler.java

web/controller/note/
├── ChildNoteController.java
├── NoteAssetController.java
└── NoteCommentController.java
```

## 🧪 테스트
```bash
./gradlew test
```

## 🔑 주요 권한

- `VIEW_REPORT`: 노트 조회, 댓글 작성
- `WRITE_NOTE`: 노트 작성
- 노트 수정: 작성자 본인만
- 노트 삭제: 작성자 본인 또는 주보호자

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