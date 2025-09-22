# 💌 Loventure Auth Service

`Loventure` 프로젝트의 **인증(Auth) 마이크로서비스**입니다.
사용자 회원가입, 로그인, JWT 발급 및 검증, 커플 페어링 로직을 담당합니다.

---

## 📌 주요 기능

* 소셜 로그인 (Google, Kakao)
* JWT 기반 인증/인가
* Refresh Token 관리 
* 커플 페어링 로직
* 마이페이지 관리

  * 사용자가 **페어링 코드**를 생성 → 상대방이 입력하여 연결
  * 성공 시 `커플 ID` 부여 및 양쪽 계정에 매핑
  * 페어링 상태 관리 (대기 / 연결 완료)

---

## 🔧 Tech Stack

* **Language**: Java 17
* **Framework**: Spring Boot 3.5.5
* **Security**: Spring Security, JWT
* **Database**: postgreSQL
* **API Docs**: Spring REST Docs / Swagger

---

## 🏗️ 아키텍처 개요

   * A[사용자] -->|회원가입/로그인| B(Auth API);
   * B -->|JWT 발급| A;
   * A -->|페어링 코드 생성| B;
   * A2[상대 사용자] -->|페어링 코드 입력| B;
   * B -->|커플 ID 생성 & 저장| DB[(postgreSQL)];
```

---

## ⚙️ 환경 변수

| Key                  | 설명                  |
| -------------------- | ------------------- |
| `JWT_SECRET`         | JWT 서명용 비밀키         |
| `JWT_EXPIRATION`     | Access Token 만료 시간  |
| `REFRESH_EXPIRATION` | Refresh Token 만료 시간 |
| `DB_URL`             | postgreSQL 연결 URL        |
| `DB_USERNAME`        | postgreSQL 계정            |
| `DB_PASSWORD`        | postgreSQL 비밀번호          |

---

## ▶️ 실행 방법

```bash
# 빌드
./gradlew clean build

# 실행
java -jar build/libs/auth-service-0.0.1-SNAPSHOT.jar
```

---

## 📑 API 문서

* Swagger UI: `/swagger-ui.html`
* REST Docs: `build/generated-snippets`

---

## 🌿 브랜치 전략

* `main`: 안정화된 운영 배포용
* `develop`: 개발 통합 브랜치
* `feature/PID-이슈번호`: 기능 단위 개발 브랜치

---

## 📝 커밋 컨벤션

* `feat`: 새로운 기능 추가
* `fix`: 버그 수정
* `docs`: 문서 수정
* `refactor`: 코드 리팩토링
* `test`: 테스트 코드 작성/수정
