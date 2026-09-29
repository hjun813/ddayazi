# 따야지

개발자와 IT 취업 준비생이 직무에 맞는 자격증을 찾고, 검수된 시험 일정과 준비 상태를 한곳에서 관리하는 서비스입니다.

- 직무 기반 자격증 추천과 추천 근거
- 자격증 검색·필터·상세 정보
- 접수, 시험, 발표 일정을 모아보는 일정 화면
- 관심 자격증과 준비 상태 관리
- 운영 데이터 CSV 사전 검증 및 일괄 반영
- 시스템 설정을 따르는 라이트·다크 모드

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 3, Spring Data JPA, Spring Security, JWT, OpenAPI |
| Frontend | React 18, TypeScript, Vite, TanStack Query, React Router, Lucide |
| Database | PostgreSQL 16, H2(Test) |
| Runtime | Docker Compose, Nginx |

## 빠른 시작

Docker Desktop이 실행 중인 상태에서 프로젝트 루트에서 다음 명령을 실행합니다.

```bash
docker compose up --build -d
```

서비스 주소:

- 웹: <https://ddayazi.vercel.app>
- 백엔드 API: <https://ddayazi.onrender.com/api>
- Swagger UI: <https://ddayazi.onrender.com/swagger-ui.html>

상태와 로그 확인:

```bash
docker compose ps
docker compose logs -f
```

종료:

```bash
docker compose down
```

DB 볼륨까지 초기화하려면 다음 명령을 사용합니다. 저장된 운영 데이터도 삭제되므로 주의하세요.

```bash
docker compose down -v
```

### 환경 변수

`.env.example`을 `.env`로 복사한 후 필요한 값을 변경할 수 있습니다.

```bash
cp .env.example .env
```

주요 설정:

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `POSTGRES_DB` | `certpath` | PostgreSQL 데이터베이스명 |
| `POSTGRES_USER` | `certpath` | PostgreSQL 사용자 |
| `POSTGRES_PASSWORD` | `certpath` | 로컬 DB 비밀번호 |
| `JWT_SECRET` | Compose 개발 기본값 | JWT 서명 키 |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | 허용 프런트엔드 Origin |
| `FRONTEND_PORT` | `5173` | 웹 포트 |
| `BACKEND_PORT` | `8080` | API 포트 |
| `POSTGRES_PORT` | `5432` | DB 포트 |

운영 환경에서는 반드시 `POSTGRES_PASSWORD`와 `JWT_SECRET`을 안전한 값으로 교체해야 합니다. `.env`는 Git에 포함되지 않습니다.

## 관리자 기능

로컬 초기 관리자 계정:

```text
이메일: admin@certpath.local
비밀번호: Admin123!
```

> 이 계정은 로컬 개발 전용입니다. 운영 환경에서는 초기 계정을 제거하거나 비밀번호를 반드시 변경하세요.

로그인 후 <http://localhost:5173/admin>에서 자격증 기본 정보와 시험 일정을 각각 CSV로 관리할 수 있습니다.

1. `양식 받기`로 UTF-8 CSV 템플릿을 다운로드합니다.
2. 작성한 파일을 선택하고 `업로드 전 검증`을 실행합니다.
3. 행별 `CREATE`, `UPDATE`, `UNCHANGED`, `ERROR` 판정과 변경 필드·오류를 확인합니다.
4. 오류가 없으면 `검증 결과 반영`을 실행합니다.

검증 결과 토큰은 30분 동안 유효하며 한 번만 반영할 수 있습니다. 기존 데이터는 자격증명 또는 일정 중복 키로 찾아 새 행을 만들지 않고 변경 여부를 표시합니다.

### 자격증 CSV

필수 헤더:

```text
name,summary,category,type,organization,difficulty,preparationWeeks,officialSourceUrl,lastVerifiedAt,reviewStatus,published
```

허용 값:

| 필드 | 값 |
| --- | --- |
| `type` | `NATIONAL`, `PRIVATE`, `VENDOR` |
| `difficulty` | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `reviewStatus` | `DRAFT`, `VERIFIED`, `OUTDATED` |
| `published` | `true`, `false` |
| `preparationWeeks` | 1~260 사이 정수 |
| `lastVerifiedAt` | `YYYY-MM-DD` |

일반 사용자 화면에 공개하려면 반드시 다음 두 값을 사용해야 합니다.

```text
reviewStatus=VERIFIED
published=true
```

CSV로 등록한 데이터에는 자동으로 `sampleData=false`가 적용됩니다. 동일한 자격증명은 새로 생성하지 않고 기존 데이터를 갱신합니다.

### 시험 일정 CSV

필수 헤더:

```text
certificationName,examRound,title,scheduleType,startsAt,endsAt,officialSourceUrl,lastVerifiedAt,reviewStatus
```

`endsAt` 값만 비울 수 있으며 나머지는 필수입니다. 자격증 CSV를 먼저 반영해야 일정의 `certificationName`을 찾을 수 있습니다.

| 필드 | 값 |
| --- | --- |
| `scheduleType` | `REGISTRATION_OPEN`, `REGISTRATION_CLOSE`, `EXAM`, `RESULT`, `RENEWAL` |
| `reviewStatus` | `DRAFT`, `VERIFIED`, `OUTDATED` |
| `startsAt`, `endsAt` | UTC ISO-8601 (`2026-10-01T01:00:00Z`) |
| `lastVerifiedAt` | `YYYY-MM-DD` |

일정 중복 키:

```text
certificationName + examRound + scheduleType
```

접수 기간은 이벤트 단위로 `REGISTRATION_OPEN`과 `REGISTRATION_CLOSE`를 각각 등록합니다. 한국 기관이 공지한 KST 시각은 UTC로 변환해 입력합니다. 예를 들어 KST 오전 10시는 같은 날짜의 `01:00:00Z`입니다.

### 공개 데이터 정책

일반 사용자 자격증 목록과 상세에는 다음 조건을 모두 만족하는 데이터만 노출됩니다.

```text
sampleData=false
reviewStatus=VERIFIED
published=true
```

일정도 `sampleData=false`, `reviewStatus=VERIFIED`, `active=true`여야 하며, 연결된 자격증이 위 공개 조건을 만족해야 합니다.

CSV 반영 후 화면에 보이지 않는다면 자격증 CSV의 `reviewStatus`가 `VERIFIED`인지, `published`가 소문자 `true`인지 먼저 확인하세요. 일정만 `VERIFIED`여도 연결된 자격증이 `DRAFT` 또는 비공개이면 사용자 화면에는 나타나지 않습니다.

직무 맞춤 추천은 공개 조건 외에도 자격증과 직무 간 추천 관계가 필요합니다. 현재 CSV는 자격증·일정 운영 데이터 등록을 지원하며, 직무 추천 관계는 별도 관리자 데이터로 관리됩니다.

## 로컬 개발

요구 사항:

- JDK 17 이상
- Node.js 20 이상
- Docker Desktop 또는 별도 PostgreSQL

백엔드와 DB:

```bash
docker compose up -d postgres
./gradlew :backend:bootRun
```

다른 터미널에서 프런트엔드 실행:

```bash
cd frontend
npm install
npm run dev
```

## 테스트와 빌드

백엔드 테스트:

```bash
./gradlew :backend:test
```

프런트엔드 테스트와 프로덕션 빌드:

```bash
cd frontend
npm test
npm run build
```

전체 컨테이너 빌드 검증:

```bash
docker compose build
```

## 프로젝트 구조

```text
.
├─ backend/                 Spring Boot API
│  └─ src/
├─ frontend/                React 애플리케이션
│  ├─ src/
│  ├─ Dockerfile
│  └─ nginx.conf
├─ gradle/                  Gradle Wrapper
├─ docker-compose.yml       PostgreSQL/API/Web 통합 실행
├─ .env.example             환경 변수 예시
└─ README.md
```

## API와 인증

- 공개 API: 회원가입·로그인, 자격증 목록·상세, 직무, 일정, 추천
- 사용자 API: 프로필, 관심 자격증 등록, 준비 상태 변경·삭제
- 관리자 API: CSV 템플릿, 사전 검증, 반영 및 운영 데이터 관리

관리자 API는 `ADMIN` 역할이 있는 JWT 사용자만 접근할 수 있습니다. 공통 API 응답은 다음 형식을 사용합니다.

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

## 데이터 안내

초기 데이터와 실제 검증 데이터는 `sampleData` 값으로 구분합니다. 초기 샘플은 운영 데이터로 공개하지 않으며, 실제 정보는 공식 출처 URL·마지막 확인일·검수 상태를 함께 저장합니다. 검수된 정보도 변경될 수 있으므로 실제 접수 전 공식 기관 홈페이지를 다시 확인해야 합니다.
