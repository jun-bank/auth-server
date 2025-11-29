# Auth Server

> 인증/인가 서비스 - JWT 발급, 토큰 검증, 리프레시 토큰 관리

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8086 |
| 데이터베이스 | auth_db (PostgreSQL) |
| 주요 역할 | 인증/인가, JWT 토큰 관리 |

## 🎯 학습 포인트

### 1. JWT (JSON Web Token) 구현
- **Access Token**: 짧은 만료 시간 (30분), 요청마다 검증
- **Refresh Token**: 긴 만료 시간 (7일), Access Token 갱신용
- **토큰 구조**: Header.Payload.Signature

```
┌─────────────────────────────────────────────────────────────┐
│                      JWT 토큰 흐름                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Client                Gateway              Auth Server    │
│     │                     │                      │          │
│     │  1. 로그인 요청      │                      │          │
│     │ ─────────────────────────────────────────> │          │
│     │                     │                      │          │
│     │  2. Access + Refresh Token 발급            │          │
│     │ <───────────────────────────────────────── │          │
│     │                     │                      │          │
│     │  3. API 요청 (Bearer Token)                │          │
│     │ ──────────────────> │                      │          │
│     │                     │  4. 토큰 검증         │          │
│     │                     │ ──────────────────> │          │
│     │                     │  5. 검증 결과         │          │
│     │                     │ <────────────────── │          │
│     │                     │                      │          │
│     │                     │  6. X-User-Id 헤더 주입          │
│     │                     │ ─────────> 서비스들              │
│     │                     │                      │          │
└─────────────────────────────────────────────────────────────┘
```

### 2. Spring Security 기초
- SecurityFilterChain 설정
- 인증 제외 경로 설정
- PasswordEncoder (BCrypt)

### 3. Refresh Token Rotation
- 리프레시 토큰 사용 시 새 토큰 발급
- 이전 토큰 무효화 (토큰 탈취 방지)

---

## 🗄️ 도메인 모델

### RefreshToken Entity (리프레시 토큰 저장용)

```
┌─────────────────────────────────────────────┐
│               RefreshToken                   │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ userId: Long (FK → User)                    │
│ token: String (Unique)                      │
│ expiresAt: LocalDateTime                    │
│ isRevoked: Boolean (토큰 무효화 여부)        │
│ createdAt: LocalDateTime                    │
│ deviceInfo: String (디바이스 정보)           │
└─────────────────────────────────────────────┘
```

### LoginHistory Entity (로그인 이력)

```
┌─────────────────────────────────────────────┐
│               LoginHistory                   │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ userId: Long                                │
│ email: String                               │
│ loginAt: LocalDateTime                      │
│ ipAddress: String                           │
│ userAgent: String                           │
│ success: Boolean                            │
│ failReason: String (실패 시)                 │
└─────────────────────────────────────────────┘
```

---

## 📡 API 명세

### 1. 로그인
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER"
  }
}
```

**이벤트 발행**: `auth.login.success`

**실패 시 (401 Unauthorized)**
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
}
```

**이벤트 발행**: `auth.login.failed`

---

### 2. 토큰 갱신 (Refresh)
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...(new)",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...(new)",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

**이벤트 발행**: `auth.token.refreshed`

---

### 3. 토큰 검증 (Gateway 내부 호출용)
```http
POST /api/v1/auth/validate
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200 OK)**
```json
{
  "valid": true,
  "userId": 1,
  "email": "user@example.com",
  "role": "USER",
  "expiresAt": "2024-01-15T11:00:00"
}
```

**유효하지 않은 토큰 (401 Unauthorized)**
```json
{
  "valid": false,
  "error": "TOKEN_EXPIRED",
  "message": "토큰이 만료되었습니다."
}
```

---

### 4. 로그아웃
```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200 OK)**
```json
{
  "message": "로그아웃 되었습니다."
}
```

**이벤트 발행**: `auth.logout`

---

### 5. 전체 로그아웃 (모든 기기)
```http
POST /api/v1/auth/logout-all
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (200 OK)**
```json
{
  "message": "모든 기기에서 로그아웃 되었습니다.",
  "revokedTokenCount": 3
}
```

---

### 6. 로그인 이력 조회
```http
GET /api/v1/auth/login-history?page=0&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (200 OK)**
```json
{
  "content": [
    {
      "loginAt": "2024-01-15T10:30:00",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0...",
      "success": true
    },
    {
      "loginAt": "2024-01-14T09:00:00",
      "ipAddress": "192.168.1.2",
      "userAgent": "Mozilla/5.0...",
      "success": false,
      "failReason": "INVALID_PASSWORD"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25
}
```

---

## 🔐 JWT 토큰 구조

### Access Token Payload
```json
{
  "sub": "1",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1705302600,
  "exp": 1705304400,
  "iss": "jun-bank-auth-server"
}
```

### Refresh Token Payload
```json
{
  "sub": "1",
  "type": "refresh",
  "iat": 1705302600,
  "exp": 1705907400,
  "iss": "jun-bank-auth-server"
}
```

---

## 📂 패키지 구조

```
com.junbank.auth
├── AuthServerApplication.java
├── domain
│   ├── entity
│   │   ├── RefreshToken.java
│   │   └── LoginHistory.java
│   └── repository
│       ├── RefreshTokenRepository.java
│       └── LoginHistoryRepository.java
├── application
│   ├── service
│   │   ├── AuthService.java
│   │   └── JwtService.java
│   └── dto
│       ├── request
│       │   ├── LoginRequest.java
│       │   ├── RefreshRequest.java
│       │   └── ValidateRequest.java
│       └── response
│           ├── TokenResponse.java
│           ├── ValidateResponse.java
│           └── LoginHistoryResponse.java
├── infrastructure
│   ├── jwt
│   │   ├── JwtTokenProvider.java
│   │   └── JwtProperties.java
│   ├── kafka
│   │   ├── AuthEventProducer.java
│   │   └── UserEventConsumer.java
│   ├── feign
│   │   └── UserServiceClient.java
│   └── config
│       ├── SecurityConfig.java
│       ├── JwtConfig.java
│       └── KafkaConfig.java
└── presentation
    ├── controller
    │   └── AuthController.java
    └── advice
        └── AuthExceptionHandler.java
```

---

## 🔗 서비스 간 통신

### 발행 이벤트 (Kafka Producer)
| 이벤트 | 토픽 | 수신 서비스 | 설명 |
|--------|------|-------------|------|
| LOGIN_SUCCESS | auth.login.success | Ledger | 로그인 감사 로그 |
| LOGIN_FAILED | auth.login.failed | Ledger | 로그인 실패 기록 |
| LOGOUT | auth.logout | - | 로그아웃 알림 |
| TOKEN_REFRESHED | auth.token.refreshed | - | 토큰 갱신 기록 |

### 수신 이벤트 (Kafka Consumer)
| 이벤트 | 토픽 | 발신 서비스 | 설명 |
|--------|------|-------------|------|
| USER_CREATED | user.created | User Service | 신규 사용자 인지 |
| USER_DELETED | user.deleted | User Service | 토큰 전체 무효화 |

### Feign Client 호출
| 대상 서비스 | 용도 | 비고 |
|-------------|------|------|
| User Service | 사용자 정보 조회 | 로그인 시 |

---

## ⚙️ 설정

### JWT 설정 (config-repo)
```yaml
jwt:
  secret: ${JWT_SECRET:...}
  access-token:
    expiration: 1800000  # 30분
  refresh-token:
    expiration: 604800000  # 7일
  issuer: jun-bank-auth-server
```

### Security 설정
```yaml
security:
  permit-all-paths:
    - /api/v1/auth/login
    - /api/v1/auth/refresh
    - /actuator/**
    - /swagger-ui/**
```

---

## 🧪 테스트 시나리오

### 1. 로그인 테스트
```bash
# 정상 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234!"}'

# 잘못된 비밀번호 (401 예상)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"wrong"}'

# 5회 연속 실패 후 계정 잠금 확인
```

### 2. 토큰 갱신 테스트
```bash
# 유효한 Refresh Token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJ..."}'

# 만료된 Refresh Token (401 예상)
```

### 3. 토큰 검증 테스트
```bash
# Gateway에서 내부 호출
curl -X POST http://localhost:8086/api/v1/auth/validate \
  -H "Content-Type: application/json" \
  -d '{"accessToken":"eyJ..."}'
```

---

## 📝 구현 체크리스트

- [ ] Entity, Repository 생성
- [ ] JwtTokenProvider 구현
- [ ] AuthService 구현
- [ ] Controller 구현
- [ ] SecurityConfig 설정
- [ ] Kafka Producer 구현
- [ ] Kafka Consumer 구현 (user.created, user.deleted)
- [ ] Feign Client 구현 (User Service)
- [ ] Refresh Token Rotation 구현
- [ ] 로그인 실패 횟수 제한
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] API 문서화 (Swagger)