# Auth Server

> 인증/인가 서비스 - JWT 발급, 토큰 검증, 리프레시 토큰 관리

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8086 |
| 데이터베이스 | auth_db (PostgreSQL) |
| 주요 역할 | 인증 정보 관리, JWT 토큰 발급/검증 |

## 🏗️ 아키텍처 결정사항

### User Service와의 역할 분리

**Auth Server가 인증 정보를 직접 보유** (User Service에서 Feign 동기 호출)

```
┌─────────────────────────────────────────────────────────────────┐
│                        인증 아키텍처                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  User Service (프로필)          Auth Server (인증)               │
│  ┌─────────────────┐           ┌─────────────────┐             │
│  │ - name          │           │ - email         │             │
│  │ - phoneNumber   │   userId  │ - password      │             │
│  │ - birthDate     │ ◀────────▶│ - role          │             │
│  │ - status        │   (참조)   │ - status        │             │
│  └─────────────────┘           │ - loginAttempts │             │
│          │                     │ - lockedUntil   │             │
│          │                     └─────────────────┘             │
│          │                            │                        │
│          ▼                            ▼                        │
│       user_db                      auth_db                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**분리 이유:**
1. **인증 독립성**: User Service 장애 시에도 로그인 가능
2. **빠른 응답**: 로그인 시 Auth Server만 조회
3. **보안**: 비밀번호는 Auth Server에서만 관리
4. **확장성**: 인증 방식 변경이 User Service에 영향 없음

---

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

### 2. 계정 잠금 정책
- **최대 시도 횟수**: 5회
- **잠금 시간**: 30분
- **자동 해제**: 잠금 시간 경과 시

### 3. Refresh Token Rotation
- 리프레시 토큰 사용 시 새 토큰 발급
- 이전 토큰 무효화 (토큰 탈취 방지)

---

## 🗄️ 도메인 모델

### 도메인 구조 (3개 Bounded Context)
```
domain/
├── auth/                              # 인증 사용자 도메인 (8개)
│   └── domain/
│       ├── exception/
│       │   ├── AuthErrorCode.java     # 인증 에러 코드 (AUTH_xxx)
│       │   └── AuthException.java     # 인증 예외
│       └── model/
│           ├── AuthUser.java          # Aggregate Root (잠금 정책)
│           ├── AuthUserStatus.java    # ACTIVE/LOCKED/DISABLED
│           ├── UserRole.java          # USER/ADMIN
│           └── vo/
│               ├── AuthUserId.java    # AUT-xxxxxxxx
│               ├── Email.java         # 이메일 (검증, 마스킹)
│               └── Password.java      # 비밀번호 (정책 검증)
│
├── token/                             # 토큰 도메인 (4개)
│   └── domain/
│       ├── exception/
│       │   ├── TokenErrorCode.java    # 토큰 에러 코드 (TKN_xxx)
│       │   └── TokenException.java    # 토큰 예외
│       └── model/
│           ├── RefreshToken.java      # 리프레시 토큰 관리
│           └── vo/
│               └── RefreshTokenId.java # RTK-xxxxxxxx
│
└── history/                           # 로그인 이력 도메인 (4개)
    └── domain/
        ├── exception/
        │   ├── HistoryErrorCode.java  # 이력 에러 코드 (LGH_xxx)
        │   └── HistoryException.java  # 이력 예외
        └── model/
            ├── LoginHistory.java      # Append-only 이력
            └── vo/
                └── LoginHistoryId.java # LGH-xxxxxxxx
```

### 도메인 분리 이유
| 도메인 | 책임 | 특성 |
|--------|------|------|
| **auth** | 인증 사용자 관리, 잠금 정책 | 상태 변경 가능, Soft Delete |
| **token** | 리프레시 토큰 생명주기 | revoke만 가능, Stateless Access Token은 제외 |
| **history** | 로그인 시도 기록 | Append-only, 감사/보안 목적 |

### AuthUser 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                        AuthUser                              │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ authUserId: AuthUserId (PK, AUT-xxxxxxxx)                   │
│ userId: String (User Service의 USR-xxx 참조)                │
│ email: Email (로그인 ID)                                    │
│ password: Password (BCrypt 암호화)                          │
│ role: UserRole (USER/ADMIN)                                 │
│ status: AuthUserStatus (ACTIVE/LOCKED/DISABLED)             │
│ failedLoginAttempts: int (로그인 실패 횟수)                  │
│ lockedUntil: LocalDateTime (잠금 해제 시간)                  │
│ lastLoginAt: LocalDateTime                                  │
├─────────────────────────────────────────────────────────────┤
│ 【감사 필드 - BaseEntity】                                    │
│ createdAt, updatedAt, createdBy, updatedBy                  │
│ deletedAt, deletedBy, isDeleted (Soft Delete)               │
├─────────────────────────────────────────────────────────────┤
│ 【비즈니스 메서드】                                           │
│ + canLogin(): boolean         // 로그인 가능 여부 (잠금 시간 포함)
│ + isLocked(): boolean         // 실제 잠금 상태 (시간 경과 확인)
│ + getRemainingLockMinutes(): long                           │
│ + changePassword(Password): void                            │
│ + recordLoginSuccess(): void  // 실패 횟수 초기화, 잠금 해제  │
│ + recordLoginFailure(): void  // 실패 횟수 증가, 잠금 처리    │
│ + recordLoginFailure(maxAttempts, lockMinutes): void        │
│ + unlock(): void              // 수동 잠금 해제              │
│ + disable(): void             // 계정 비활성화               │
│ + enable(): void              // 계정 활성화                 │
│ + changeRole(UserRole): void                                │
├─────────────────────────────────────────────────────────────┤
│ 【상수】                                                     │
│ DEFAULT_MAX_ATTEMPTS = 5      // 기본 최대 시도 횟수          │
│ DEFAULT_LOCK_MINUTES = 30     // 기본 잠금 시간 (분)          │
└─────────────────────────────────────────────────────────────┘
```

### RefreshToken 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                      RefreshToken                            │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ refreshTokenId: RefreshTokenId (RTK-xxxxxxxx)               │
│ userId: String (토큰 소유자)                                 │
│ token: String (JWT 토큰 값)                                  │
│ expiresAt: LocalDateTime                                    │
│ isRevoked: boolean (폐기 여부)                               │
│ deviceInfo: String (User-Agent)                             │
│ ipAddress: String (접속 IP)                                  │
│ createdAt: LocalDateTime                                    │
├─────────────────────────────────────────────────────────────┤
│ 【비즈니스 메서드】                                           │
│ + isExpired(): boolean                                      │
│ + isValid(): boolean          // !revoked && !expired       │
│ + validateForUse(): void      // 사용 전 검증, 예외 발생     │
│ + revoke(): void              // 토큰 폐기                   │
│ + matchesContext(deviceInfo, ip): boolean                   │
└─────────────────────────────────────────────────────────────┘
```

### LoginHistory 도메인 모델 (Append-only)
```
┌─────────────────────────────────────────────────────────────┐
│                      LoginHistory                            │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ loginHistoryId: LoginHistoryId (LGH-xxxxxxxx)               │
│ userId: String (없을 수 있음)                                │
│ email: String (로그인 시도 이메일)                           │
│ loginAt: LocalDateTime                                      │
│ ipAddress: String                                           │
│ userAgent: String                                           │
│ success: boolean                                            │
│ failReason: String (실패 시: INVALID_PASSWORD 등)           │
├─────────────────────────────────────────────────────────────┤
│ 【팩토리 메서드】                                             │
│ + success(userId, email, ip, userAgent): LoginHistory       │
│ + failure(userId, email, ip, userAgent, reason): LoginHistory│
├─────────────────────────────────────────────────────────────┤
│ ※ Append-only: INSERT만 허용, UPDATE/DELETE 금지           │
└─────────────────────────────────────────────────────────────┘
```

### Enum (정책 메서드 포함)

#### UserRole
```java
public enum UserRole {
    USER("일반 사용자", level=1),
    ADMIN("관리자", level=100);
    
    public boolean isAdmin();
    public boolean hasAuthority(UserRole role);  // ADMIN은 USER 권한 포함
    public Set<UserRole> getIncludedRoles();
    public String toAuthority();  // "ROLE_USER", "ROLE_ADMIN"
}
```

#### AuthUserStatus
```java
public enum AuthUserStatus {
    ACTIVE("정상", canAuthenticate=true),
    LOCKED("잠금", canAuthenticate=false),   // 일시적 (시간 경과 시 해제)
    DISABLED("비활성화", canAuthenticate=false);  // 영구적 (관리자만 해제)
    
    public boolean canTransitionTo(AuthUserStatus target);
    public Set<AuthUserStatus> getAllowedTransitions();
}
```

**상태 전이 규칙:**
```
ACTIVE → LOCKED (로그인 실패), DISABLED (관리자)
LOCKED → ACTIVE (시간 경과/수동 해제), DISABLED (관리자)
DISABLED → ACTIVE (관리자)
```

### Value Objects

#### Password (비밀번호 정책)
```java
public record Password(String encodedValue) {
    // 평문 비밀번호 정책 검증 (Application Layer에서 호출)
    public static void validateRawPassword(String raw);
    public static boolean isValidRawPassword(String raw);
    
    // 정책: 8자 이상, 영문자, 숫자, 특수문자(@$!%*?&) 포함
    
    @Override
    public String toString() { return "[PROTECTED]"; }  // 보안
}
```

### Exception 체계

#### AuthErrorCode
```java
public enum AuthErrorCode implements ErrorCode {
    // 인증 실패 (401)
    INVALID_CREDENTIALS("AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다", 401),
    PASSWORD_MISMATCH("AUTH_002", "비밀번호가 일치하지 않습니다", 401),
    
    // 토큰 (401)
    INVALID_TOKEN("AUTH_010", "유효하지 않은 토큰입니다", 401),
    TOKEN_EXPIRED("AUTH_011", "토큰이 만료되었습니다", 401),
    TOKEN_REVOKED("AUTH_012", "폐기된 토큰입니다", 401),
    
    // 계정 상태 (403)
    ACCOUNT_LOCKED("AUTH_020", "계정이 잠겨있습니다", 403),
    ACCOUNT_DISABLED("AUTH_021", "비활성화된 계정입니다", 403),
    
    // 조회 (404)
    AUTH_USER_NOT_FOUND("AUTH_025", "인증 사용자를 찾을 수 없습니다", 404),
    
    // 유효성 (400)
    INVALID_PASSWORD_FORMAT("AUTH_031", "비밀번호 정책 위반", 400);
}
```

#### AuthException (팩토리 메서드 패턴)
```java
public class AuthException extends BusinessException {
    public static AuthException invalidCredentials();
    public static AuthException accountLocked(long remainingMinutes);
    public static AuthException tokenExpired();
    public static AuthException tokenRevoked();
    public static AuthException authUserNotFound(String identifier);
    // ...
}
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

**처리 흐름:**
1. AuthUser 조회 (email)
2. `authUser.canLogin()` 확인 (잠금/비활성화 체크)
3. 비밀번호 검증
4. 성공 시: `authUser.recordLoginSuccess()`, 토큰 발급
5. 실패 시: `authUser.recordLoginFailure()`, LoginHistory 기록

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "userId": "USR-a1b2c3d4",
    "email": "user@example.com",
    "role": "USER"
  }
}
```

**실패 응답:**
- 401: `INVALID_CREDENTIALS` (이메일/비밀번호 오류)
- 403: `ACCOUNT_LOCKED` (계정 잠금, 남은 시간 포함)
- 403: `ACCOUNT_DISABLED` (계정 비활성화)

### 2. 토큰 갱신 (Refresh)
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**처리 흐름:**
1. RefreshToken 조회
2. `refreshToken.validateForUse()` (만료/폐기 검증)
3. 기존 토큰 폐기: `refreshToken.revoke()`
4. 새 Access + Refresh Token 발급

### 3. 토큰 검증 (Gateway 호출)
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
  "userId": "USR-a1b2c3d4",
  "email": "user@example.com",
  "role": "USER",
  "expiresAt": "2024-01-15T11:00:00"
}
```

### 4. 로그아웃
```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**처리:** `refreshToken.revoke()` 호출

### 5. 회원가입 (User Service에서 Feign 호출)
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "userId": "USR-a1b2c3d4",
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**처리 흐름:**
1. `Password.validateRawPassword(password)` 정책 검증
2. 비밀번호 암호화 (BCrypt)
3. AuthUser 생성 및 저장

---

## 📂 패키지 구조

```
com.jun_bank.auth_server
├── AuthServerApplication.java
├── global/                              # 전역 설정 레이어
│   ├── config/
│   │   ├── JpaConfig.java
│   │   ├── QueryDslConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   ├── KafkaConsumerConfig.java
│   │   ├── SecurityConfig.java          # PasswordEncoder, 인증 제외 경로
│   │   ├── FeignConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── AsyncConfig.java
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java
│   ├── security/
│   │   └── SecurityContextUtil.java
│   ├── feign/
│   │   ├── FeignErrorDecoder.java
│   │   └── FeignRequestInterceptor.java
│   └── aop/
│       └── LoggingAspect.java
└── domain/
    ├── auth/                            # 인증 사용자 Bounded Context ★
    │   ├── domain/                      # 순수 도메인 ✅
    │   │   ├── exception/
    │   │   │   ├── AuthErrorCode.java   # AUTH_xxx 에러 코드
    │   │   │   └── AuthException.java
    │   │   └── model/
    │   │       ├── AuthUser.java        # Aggregate Root
    │   │       ├── UserRole.java
    │   │       ├── AuthUserStatus.java
    │   │       └── vo/
    │   │           ├── AuthUserId.java
    │   │           ├── Email.java
    │   │           └── Password.java
    │   ├── application/                 # 유스케이스 (TODO)
    │   ├── infrastructure/              # Adapter Out (TODO)
    │   └── presentation/                # Adapter In (TODO)
    │
    ├── token/                           # 토큰 Bounded Context ★
    │   ├── domain/                      # 순수 도메인 ✅
    │   │   ├── exception/
    │   │   │   ├── TokenErrorCode.java  # TKN_xxx 에러 코드
    │   │   │   └── TokenException.java
    │   │   └── model/
    │   │       ├── RefreshToken.java    # Aggregate Root
    │   │       └── vo/
    │   │           └── RefreshTokenId.java
    │   ├── application/                 # (TODO)
    │   ├── infrastructure/
    │   │   └── jwt/                     # JWT Provider
    │   │       ├── JwtTokenProvider.java
    │   │       └── JwtProperties.java
    │   └── presentation/                # (TODO)
    │
    └── history/                         # 로그인 이력 Bounded Context ★
        ├── domain/                      # 순수 도메인 ✅
        │   ├── exception/
        │   │   ├── HistoryErrorCode.java # LGH_xxx 에러 코드
        │   │   └── HistoryException.java
        │   └── model/
        │       ├── LoginHistory.java    # Aggregate Root (Append-only)
        │       └── vo/
        │           └── LoginHistoryId.java
        ├── application/                 # (TODO)
        ├── infrastructure/              # (TODO)
        └── presentation/                # (TODO)
```

---

## 🔐 JWT 토큰 구조

### Access Token Payload
```json
{
  "sub": "USR-a1b2c3d4",
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
  "sub": "USR-a1b2c3d4",
  "type": "refresh",
  "jti": "RTK-x1y2z3w4",
  "iat": 1705302600,
  "exp": 1705907400,
  "iss": "jun-bank-auth-server"
}
```

---

## 🔗 서비스 간 통신

### Feign Client 수신 (User Service → Auth Server)
| 엔드포인트 | 용도 |
|-----------|------|
| POST /api/v1/auth/register | 회원가입 시 인증 정보 생성 |

### Kafka (비동기 이벤트)
| 이벤트 | 토픽 | 수신 서비스 |
|--------|------|-------------|
| LOGIN_SUCCESS | auth.login.success | Ledger |
| LOGIN_FAILED | auth.login.failed | Ledger |
| USER_DELETED | user.deleted | Auth Server (토큰 무효화) |

---

## 📝 구현 체크리스트

### Domain Layer ✅ (16개 파일, 3개 도메인)

#### auth 도메인 (인증 사용자) - 8개
- [x] AuthErrorCode (AUTH_xxx 에러 코드)
- [x] AuthException (팩토리 메서드 패턴)
- [x] UserRole (권한 정책)
- [x] AuthUserStatus (상태 정책)
- [x] AuthUserId VO
- [x] Email VO
- [x] Password VO (정책 검증)
- [x] AuthUser (잠금 정책 포함)

#### token 도메인 (리프레시 토큰) - 4개
- [x] TokenErrorCode (TKN_xxx 에러 코드)
- [x] TokenException (팩토리 메서드 패턴)
- [x] RefreshTokenId VO
- [x] RefreshToken

#### history 도메인 (로그인 이력) - 4개
- [x] HistoryErrorCode (LGH_xxx 에러 코드)
- [x] HistoryException (팩토리 메서드 패턴)
- [x] LoginHistoryId VO
- [x] LoginHistory (Append-only)

### Application Layer
- [ ] LoginUseCase
- [ ] RefreshTokenUseCase
- [ ] ValidateTokenUseCase
- [ ] LogoutUseCase
- [ ] RegisterUseCase
- [ ] AuthUserPort
- [ ] RefreshTokenPort
- [ ] LoginHistoryPort
- [ ] DTO 정의

### Infrastructure Layer
- [ ] AuthUserEntity
- [ ] RefreshTokenEntity
- [ ] LoginHistoryEntity
- [ ] JPA Repository
- [ ] Repository Adapter
- [ ] JwtTokenProvider
- [ ] JwtProperties
- [ ] Kafka Producer/Consumer

### Presentation Layer
- [ ] AuthController
- [ ] Request/Response DTO
- [ ] Swagger 문서화

### 테스트
- [ ] 도메인 단위 테스트 (잠금 정책 등)
- [ ] Application 단위 테스트
- [ ] JWT 통합 테스트
- [ ] API 통합 테스트