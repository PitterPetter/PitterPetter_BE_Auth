AuthController.java (운영용 컨트롤러) 📡

설명: 실제 서비스에서 사용되는 핵심 인증 및 사용자 관련 API를 제공합니다. /api/auth 경로의 모든 엔드포인트는 SecurityConfig에 의해 보호됩니다.

주요 API 엔드포인트

POST /api/auth/refresh: 만료된 Access Token을 갱신하기 위해 Refresh Token을 받아 새로운 토큰들을 발급합니다.

GET /api/auth/me: 현재 로그인된 사용자의 상세 정보를 조회합니다. Authorization 헤더의 토큰을 기반으로 사용자를 식별합니다.

POST /api/auth/logout: 사용자의 로그아웃을 처리합니다. 클라이언트는 이 API를 호출한 후 자체적으로 저장된 토큰을 폐기해야 합니다.

GET /api/auth/status: 특정 이메일이 이미 가입되어 있는지, 가입된 경우 어떤 소셜 로그인 방식인지 상태를 확인합니다.

POST /api/auth/login/{provider}: OAuth2 로그인을 처리합니다. 제공된 provider (google 또는 kakao)를 사용하여 사용자를 인증하고, 기존 사용자면 정보를 조회하거나 신규 사용자면 생성합니다. 성공 시 JWT 토큰(access token, refresh token)과 사용자 정보를 반환합니다.

POST /api/auth/signup: 신규 회원을 가입시킵니다. 이메일과 providerId 중복을 확인하고, 문제가 없으면 새 사용자를 생성하고 저장합니다.

UserController.java (운영용 컨트롤러) 🤝

설명: 사용자 계정 관리와 관련된 API를 담당합니다.

주요 API 엔드포인트

DELETE /api/users/{userId}: 특정 userId의 회원을 삭제합니다. 삭제 요청을 한 사용자가 본인 계정인지 확인하며, 회원을 데이터베이스에서 소프트 삭제(DEACTIVATED 상태로 변경) 처리합니다.

OnboardingController.java (운영용 컨트롤러) 🚀

설명: 회원가입 후 추가 정보를 입력하는 온보딩 절차를 처리합니다.

주요 API 엔드포인트

POST /api/onboarding/me: 로그인된 사용자의 온보딩 정보를 업데이트합니다. 성공 시 사용자 ID와 온보딩 완료 상태를 반환합니다.

CoupleController.java (운영용 컨트롤러) 💖

설명: 커플 관련 기능을 관리하는 API를 제공합니다.

주요 API 엔드포인트

POST /api/home/coupleroom/rooms: 새로운 커플룸을 생성합니다. 커플홈 이름과 데이트 시작일을 받아 커플룸을 생성하고, 응답으로 초대 코드를 반환합니다.

POST /api/home/coupleroom/match: 커플 매칭을 처리합니다. 요청자의 userId를 JWT에서 추출하여 사용합니다.

DELETE /api/home/coupleroom/{coupleId}: 특정 coupleId에 해당하는 커플 매칭을 취소합니다.

POST /api/home/coupleroom/{coupleId}/onboarding: [DEPRECATED] 커플 온보딩 정보를 생성하거나 수정합니다. 이 API는 더 이상 사용되지 않으며, 대신 POST /api/home/coupleroom/rooms API를 사용하세요.

PATCH /api/home/coupleroom/{coupleId}: 커플 정보를 수정합니다.