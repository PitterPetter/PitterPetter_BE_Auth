# AuthService: API 컨트롤러 (Controller) 📡

이 문서는 `authService` 내 `controller` 패키지의 역할과 API 엔드포인트들을 설명합니다. 이 패키지는 클라이언트의 HTTP 요청을 가장 먼저 받아 처리하는 **애플리케이션의 진입점**입니다.

컨트롤러는 크게 **실제 서비스용 컨트롤러**와 **개발/테스트용 컨트롤러**로 나뉩니다.

---

## `AuthController.java` (운영용 컨트롤러)

실제 서비스에서 사용되는 핵심 인증 및 사용자 관련 API를 제공합니다. `/api/auth` 경로의 모든 엔드포인트는 `SecurityConfig`에 의해 인증(`JWTFilter`를 통과해야 함)이 필요하도록 보호됩니다. (일부 예외 존재)

###  주요 API 엔드포인트

* **`POST /api/auth/refresh`**
    * **설명**: 만료된 Access Token을 갱신하기 위해 Refresh Token을 받아 새로운 토큰들을 발급합니다.
    * **요청**: `refreshToken` (쿼리 파라미터)
    * **응답**: 성공 시 새로운 토큰 정보 (`AuthResponse`)

* **`GET /api/auth/me`**
    * **설명**: 현재 로그인된 사용자의 상세 정보를 조회합니다. `Authorization` 헤더의 토큰을 기반으로 사용자를 식별합니다.
    * **요청**: `Authorization: Bearer <access_token>` (HTTP 헤더)
    * **응답**: 성공 시 사용자 정보 (`UserInfo`)

* **`POST /api/auth/logout`**
    * **설명**: 사용자의 로그아웃을 처리합니다. 클라이언트는 이 API를 호출한 후 자체적으로 저장된 토큰을 폐기해야 합니다.
    * **요청**: `Authorization: Bearer <access_token>` (HTTP 헤더)
    * **응답**: 성공 메시지

* **`GET /api/auth/status`**
    * **설명**: 특정 이메일이 이미 가입되어 있는지, 가입된 경우 어떤 소셜 로그인 방식인지 상태를 확인합니다. (이 API는 인증이 필요 없음)
    * **요청**: `email` (쿼리 파라미터)
    * **응답**: 계정 존재 여부, 가입 유형(`providerType`), 상태(`status`)

---

## `TestController.java` (개발용 컨트롤러) 🧪

**⚠️ 중요: 이 컨트롤러는 개발 및 테스트 단계의 편의를 위해서만 사용되며, 절대로 운영(Production) 환경에 배포되어서는 안 됩니다.**

매번 소셜 로그인을 거치지 않고도 사용자를 생성하거나 JWT를 발급받는 등 백엔드 기능을 독립적으로 테스트할 수 있는 API를 제공합니다.

### 주요 API 엔드포인트

* **`GET /api/test/jwt/create`**
    * **설명**: 특정 `providerId`를 가진 임시 JWT 토큰을 즉시 생성합니다.
    * **요청**: `providerId` (쿼리 파라미터)

* **`GET /api/test/jwt/validate`**
    * **설명**: 주어진 토큰의 유효성을 검증하고, 토큰에 담긴 사용자 정보를 반환합니다.
    * **요청**: `token` (쿼리 파라미터)

* **`GET /api/test/users/create`**
    * **설명**: 소셜 로그인 절차 없이 테스트용 사용자를 DB에 강제로 생성합니다.
    * **요청**: `providerId`, `email`, `name` (쿼리 파라미터)

### 보안 가이드

운영 환경 배포 시, `@Profile("!prod")` 어노테이션을 `TestController` 클래스에 추가하여 운영 프로필에서는 이 컨트롤러가 로드되지 않도록 반드시 설정해야 합니다.