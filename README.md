# 💌 Loventure Auth Service

## 📌 서비스 개요

PitterPetter Auth Service는 커플 데이팅 앱의 핵심 인증 마이크로서비스입니다. 도메인 주도 설계(DDD)를 적용하여 안정적이고 확장 가능한 마이크로서비스 아키텍처를 구현했습니다.

## 🚀 주요 기능

### 🔐 인증 및 인가

**소셜 로그인**: Google, Kakao OAuth2 연동
**JWT 기반 인증**: Access/Refresh Token 분리 관리
**자동 회원가입**: OAuth2 로그인 시 자동 사용자 생성
**토큰 갱신**: 자동 Refresh Token 갱신 시스템

### 👫 커플 매칭

**초대 코드 시스템**: 6자리 안전한 초대 코드 생성
**양방향 매칭**: 생성자-파트너 양방향 연결
**상태 관리**: PENDING → ACTIVE 상태 전환
**JWT 갱신**: 매칭 완료 시 CoupleId 포함 JWT 재발급

### 👤 사용자 관리

**프로필 관리**: 개인정보 및 선호도 설정
**마이페이지**: 사용자 정보 조회 및 수정
**계정 상태**: ACTIVE/INACTIVE 상태 관리
**데이터 동기화**: OAuth2 정보 실시간 동기화

### 🔗 서비스 연동

**마이크로서비스 인증**: Territory, AI, Content 서비스 연동
**JWT 검증 API**: 내부 서비스용 토큰 검증
**헤더 전달**: X-User-Id, X-Couple-Id 헤더 설정
**데이터 공유**: 사용자 정보 및 커플 정보 제공

## 🛠 기술 스택

### Backend
- **Java 17** + Spring Boot 3.5.5
- **Spring Security** + OAuth2 Login
- **Spring Data JPA** + PostgreSQL
- **SpringDoc OpenAPI 3** (Swagger)

### Build & Deploy
- **Gradle** 빌드 도구
- **Docker** 컨테이너화
- **Spring Cloud Config** 중앙 설정 관리
- **API Gateway** 연동
- **Kubernetes** 오케스트레이션

### 추가 라이브러리
- **JWT (jjwt)**: JWT 토큰 처리
- **Lombok**: 코드 간소화
- **Jackson**: JSON 처리
- **TSID**: 고유 ID 생성
- **Spring Cloud Config**: 중앙 설정 관리

## 🏗️ 아키텍처 설계

### 마이크로서비스 아키텍처
```
API Gateway → Auth Service (8081)
     ↓              ↓
Config Server    Database
     ↓              ↓
OAuth2 Provider  PostgreSQL
```

### 도메인 모델
```
User (1) ←→ (1) CoupleRoom (1) ←→ (1) Couple
  ↓
OAuth2 Provider (Google, Kakao)
```

### 핵심 엔티티
- **User**: 사용자 정보 (TSID 기반)
- **CoupleRoom**: 커플룸 정보 (초대 코드 기반)
- **Couple**: 커플 정보 (티켓 관리)
- **CoupleOnboarding**: 온보딩 정보

### 성능 최적화
- **인덱스 전략**: provider_id, couple_id, email 인덱스
- **JWT 메모리 검증**: DB 접근 없이 토큰 검증
- **LAZY 로딩**: 메모리 효율성
- **Redis 캐싱**: 사용자 정보 캐싱

## ⚙️ 환경 설정

### Config Server 연동
이 서비스는 Spring Cloud Config Server를 통해 중앙화된 설정 관리를 사용합니다.

```yaml
# application.yaml
spring:
  config:
    import: "optional:configserver:"
  cloud:
    config:
      uri: http://config-server.config-server.svc.cluster.local:80
      name: auth-service
      label: main
      fail-fast: false
  application:
    name: auth-service
  profiles:
    active: prod
```

### API Gateway 연동
API Gateway를 통해 외부 요청을 라우팅하며, 내부 서비스 포트는 8081을 사용합니다.

```yaml
# 내부 서비스 포트
SERVER_PORT=8081

# API Gateway를 통한 외부 접근
# Gateway URL: http://api-gateway:8080
# Service Path: /auth-service
```

## ▶️ 실행 방법

### 로컬 개발 환경
```bash
# 의존성 설치
./gradlew build

# 애플리케이션 실행 (Config Server 연동)
./gradlew bootRun
```

### Docker 실행 (마이크로서비스 환경)
```bash
# Docker 이미지 빌드
docker build -t pitterpetter-auth-service .

# 컨테이너 실행 (Config Server 연동)
docker run -p 8081:8081 \
  --network microservices-network \
  pitterpetter-auth-service
```

### Kubernetes 배포
```bash
# Config Server와 함께 배포
kubectl apply -f k8s/auth-service-deployment.yaml
kubectl apply -f k8s/auth-service-service.yaml
```

## 📡 API 엔드포인트

### 내부 서비스 엔드포인트 (포트 8081)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| GET | `/api/auth/status` | 인증 상태 확인 | ❌ |
| POST | `/api/auth/refresh` | 토큰 갱신 | ❌ |
| GET | `/api/users/profile` | 프로필 조회 | ✅ |
| PUT | `/api/users/profile` | 프로필 수정 | ✅ |
| DELETE | `/api/users/{userId}` | 회원 삭제 | ✅ |
| POST | `/api/couples/room` | 커플룸 생성 | ✅ |
| POST | `/api/couples/match` | 커플 매칭 | ✅ |
| GET | `/api/couples/info` | 커플 정보 조회 | ✅ |
| PUT | `/api/couples/info` | 커플 정보 수정 | ✅ |
| GET | `/internal/user/{providerId}` | 내부 사용자 조회 | ❌ |
| GET | `/internal/api/regions/verify` | JWT 검증 | ❌ |

### API Gateway를 통한 외부 접근

| Method | Gateway Endpoint | 설명 | 인증 |
|--------|------------------|------|------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| GET | `/api/auth/status` | 인증 상태 확인 | ❌ |
| POST | `/api/auth/refresh` | 토큰 갱신 | ❌ |
| GET | `/api/users/profile` | 프로필 조회 | ✅ |
| PUT | `/api/users/profile` | 프로필 수정 | ✅ |
| DELETE | `/api/users/{userId}` | 회원 삭제 | ✅ |
| POST | `/api/couples/room` | 커플룸 생성 | ✅ |
| POST | `/api/couples/match` | 커플 매칭 | ✅ |
| GET | `/api/couples/info` | 커플 정보 조회 | ✅ |
| PUT | `/api/couples/info` | 커플 정보 수정 | ✅ |

### API 문서
- **내부 Swagger UI**: http://localhost:8081/swagger-ui.html
- **Gateway를 통한 접근**: http://api-gateway:8080/auth-service/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs

## 🗄️ 데이터베이스 설계

### 주요 테이블
- **users**: 사용자 정보 (TSID 기반 ID)
- **couple_rooms**: 커플룸 정보 (초대 코드 기반)
- **couple_onboardings**: 온보딩 정보
- **couple**: 커플 정보 (티켓 관리)

### 인덱스 전략
- **users**: provider_id, email, user_id
- **couple_rooms**: invite_code, couple_id, creator_user_id, partner_user_id
- **couple_onboardings**: couple_id

## 🔧 개발 가이드

### 프로젝트 구조
```
src/main/java/PitterPatter/loventure/authService/
├── api/                    # API 레이어
│   ├── controller/        # REST 컨트롤러
│   └── dto/              # 요청/응답 DTO
├── config/               # 설정 클래스
├── domain/               # 도메인 모델
├── exception/            # 예외 처리
├── handler/              # OAuth2 핸들러
├── mapper/               # 매퍼 클래스
├── repository/           # 데이터 접근
├── security/             # 보안 설정
├── service/              # 비즈니스 로직
└── scheduler/            # 스케줄러
```

### 코드 스타일
- **도메인 주도 설계**: 비즈니스 로직을 도메인에 캡슐화
- **클린 아키텍처**: 계층 분리 및 의존성 역전
- **단일 책임 원칙**: 각 클래스의 명확한 역할 분담

## 🧪 테스트

### 단위 테스트
```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "AuthServiceTest"
```

### 통합 테스트
```bash
# 통합 테스트 실행
./gradlew integrationTest
```

## 📊 모니터링

### 성능 지표
- **응답 시간**: p95 100-150ms 목표
- **처리량**: 2000 req/s 목표
- **JWT 검증**: 10-20ms (메모리 기반)
- **DB 조회**: 30-50ms (인덱스 최적화)

### 로그 모니터링
- **OAuth2 로그인**: 처리 시간 측정
- **커플 매칭**: 매칭 성공률 추적
- **JWT 검증**: 토큰 검증 성공률
- **에러 로그**: 상세한 에러 정보

## 🚀 배포

### 마이크로서비스 환경 배포
```bash
# Docker 이미지 빌드
docker build -t pitterpetter-auth-service:latest .

# 마이크로서비스 네트워크에서 실행
docker run -d \
  --name auth-service \
  --network microservices-network \
  -p 8081:8081 \
  pitterpetter-auth-service:latest
```

### Kubernetes 배포 (Config Server 연동)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: microservices
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: pitterpetter-auth-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SERVER_PORT
          value: "8081"
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: microservices
spec:
  selector:
    app: auth-service
  ports:
  - port: 8081
    targetPort: 8081
  type: ClusterIP
```

## 🧩 브랜치 전략

- **main**: 배포용 안정 버전
- **develop**: 통합 개발 브랜치
- **feature/PIT-이슈번호**: 기능 단위 개발 브랜치
- **hotfix/PIT-이슈번호**: 긴급 수정 브랜치

## 📜 커밋 규칙

- **feat**: 새로운 기능 추가
- **fix**: 버그 수정
- **docs**: 문서 수정
- **refactor**: 코드 리팩토링
- **test**: 테스트 코드
- **perf**: 성능 개선
- **chore**: 빌드 과정 또는 보조 도구 변경
