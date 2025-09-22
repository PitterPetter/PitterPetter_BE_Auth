# AuthService: 인증 및 인가 (Security) 아키텍처 📜

이 문서는 PitterPatter 프로젝트의 `authService` 내 `security` 패키지의 인증 및 인가 아키텍처를 설명합니다. 우리 서비스의 보안은 **Spring Security**, **OAuth2 소셜 로그인**, 그리고 **JWT(JSON Web Token)**를 기반으로 구축되었습니다.

## 핵심 원칙

* **Stateless (무상태) 인증**: 서버는 세션을 통해 사용자의 로그인 상태를 저장하지 않습니다. 모든 API 요청은 `Authorization` 헤더에 포함된 JWT를 통해 자체적으로 인증되어야 합니다.
* **토큰 기반 인증**: 사용자는 OAuth2 소셜 로그인 성공 후 서버로부터 **Access Token**과 **Refresh Token**을 발급받습니다. 이후 모든 인증이 필요한 요청에는 Access Token을 사용합니다.

---

## 🚀 인증/인가 흐름 요약

우리 서비스의 보안 흐름은 크게 두 가지로 나뉩니다.

1.  **최초 소셜 로그인**: 사용자가 구글/카카오 로그인에 성공하여 우리 서비스의 **JWT를 발급받는 과정**입니다.
2.  **로그인 이후 API 요청**: 발급받은 JWT를 이용해 **API 접근 권한을 확인하는 과정**입니다.

### ① 최초 소셜 로그인 흐름

`Client` → `Spring Security` → `CustomOAuth2UserService` → `DB` → `OAuth2LoginSuccessHandler` → `JWTUtil` → `Client (with JWT)`

1.  사용자가 소셜 로그인을 시도하면 Spring Security의 OAuth2 처리 로직이 시작됩니다.
2.  `CustomOAuth2UserService`가 소셜 플랫폼으로부터 사용자 정보를 받아와 DB에 해당 유저가 있는지 확인합니다.
    * **신규 유저**: DB에 정보를 저장하여 **자동으로 회원가입** 처리합니다.
    * **기존 유저**: DB에서 정보를 조회하고 필요한 경우 업데이트합니다.
3.  로그인/회원가입이 성공하면 `OAuth2LoginSuccessHandler`가 호출됩니다.
4.  `SuccessHandler`는 `JWTUtil`을 사용하여 우리 서비스 전용 **Access Token**과 **Refresh Token**을 생성합니다.
5.  생성된 토큰들을 클라이언트에게 전달하며 로그인 과정이 종료됩니다.

### ② 로그인 이후 API 요청 흐름

`Client (with JWT)` → `JWTFilter` → `JWTUtil` → `DB` → `SecurityContext` → `Controller`

1.  클라이언트는 발급받은 Access Token을 HTTP 요청의 `Authorization: Bearer <token>` 헤더에 담아 전송합니다.
2.  요청은 가장 먼저 `JWTFilter`라는 '인증 검문소'를 통과합니다.
3.  `JWTFilter`는 헤더에서 토큰을 추출하여 `JWTUtil`을 통해 유효성을 검증합니다. (서명, 만료 시간 등)
4.  토큰이 유효하면 토큰에 담긴 사용자 ID를 이용해 DB에서 실제 사용자가 존재하는지, 계정이 활성 상태인지 다시 한번 확인합니다.
5.  모든 검증을 통과하면, 해당 요청에 대한 사용자 인증 정보를 `SecurityContextHolder`에 등록합니다.
6.  인증 정보가 등록된 후, 요청은 비로소 `Controller`의 로직에 도달하게 됩니다.

---

## 🛠️ 주요 컴포넌트 설명

### `SecurityConfig.java`

**보안 설정의 총책임자**입니다. 애플리케이션의 전반적인 보안 규칙을 정의합니다.

* 세션을 사용하지 않는 `STATELESS` 정책을 설정합니다.
* OAuth2 로그인 성공/실패 시 동작할 `Handler`와 `Service`를 지정합니다.
* 모든 요청의 길목에 `JWTFilter`를 배치하는 역할을 합니다.
* URL별 접근 권한(`permitAll`, `authenticated`)을 설정합니다.

### `JWTUtil.java`

**JWT 토큰 생성 및 검증 도구**입니다.

* `application.yml`에 정의된 비밀 키(`spring.jwt.secret`)를 사용하여 토큰을 암호화하고 서명합니다.
* 사용자 정보를 바탕으로 Access Token과 Refresh Token을 생성합니다.
* 요청으로 들어온 토큰의 유효성을 검사하고, 토큰 내부의 데이터를 추출합니다.

### `JWTFilter.java`

**인증 검문소 필터**입니다. 로그인 이후의 모든 요청을 가로채 인증을 처리합니다.

* 요청 헤더에서 JWT를 확인하고, `JWTUtil`을 통해 검증합니다.
* 검증이 완료되면 `SecurityContextHolder`에 인증 정보를 등록하여, 해당 요청이 처리되는 동안 사용자가 '인증된 상태'임을 알려줍니다.

### `handler` 및 `service` 패키지 (예정)

* **`CustomOAuth2UserService`**: OAuth2 로그인 시, 소셜 플랫폼에서 받아온 사용자 정보를 우리 서비스의 DB와 동기화(회원가입/로그인)하는 핵심 비즈니스 로직을 담당합니다.
* **`OAuth2LoginSuccessHandler`**: `CustomOAuth2UserService`의 작업이 성공적으로 끝난 직후, `JWTUtil`을 호출하여 클라이언트에게 전달할 최종 JWT를 생성하는 역할을 합니다.

---

## 📖 개발자 가이드

### Controller에서 인증된 사용자 정보 가져오기

`JWTFilter`가 인증을 완료하면 Controller에서 `@AuthenticationPrincipal` 어노테이션을 통해 사용자 정보를 쉽게 주입받을 수 있습니다.

```java
@GetMapping("/my-info")
public ResponseEntity<String> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
    // userDetails.getUsername()을 통해 JWT에 저장된 사용자 ID를 얻을 수 있습니다.
    String userId = userDetails.getUsername();
    // ... 비즈니스 로직 처리
    return ResponseEntity.ok("User ID: " + userId);
}