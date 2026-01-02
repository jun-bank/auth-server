--[[
    RefreshToken 저장 Lua Script

    원자적으로 토큰을 저장하며 다음을 처리:
    1. 같은 디바이스(deviceId)면 기존 토큰 교체
    2. 다른 디바이스면 추가
    3. 최대 세션 수 초과 시 가장 오래된 토큰 삭제

    KEYS:
      [1] user_tokens_key  - 사용자별 토큰 목록 (ZSET: score=생성시간)
      [2] token_data_key   - 토큰 데이터 해시 키 prefix

    ARGV:
      [1] userId           - 사용자 ID
      [2] token            - JWT 토큰 값
      [3] deviceId         - 디바이스 식별자 (User-Agent 해시 등)
      [4] tokenData        - 토큰 데이터 JSON
      [5] createdAt        - 생성 시간 (Unix timestamp)
      [6] ttlSeconds       - TTL (초)
      [7] maxSessions      - 최대 허용 세션 수

    RETURN:
      JSON: { "status": "CREATED|REPLACED|OVERFLOW", "removedToken": "..." }
--]]

local userTokensKey = KEYS[1]
local tokenDataPrefix = KEYS[2]

local userId = ARGV[1]
local newToken = ARGV[2]
local deviceId = ARGV[3]
local tokenData = ARGV[4]
local createdAt = tonumber(ARGV[5])
local ttlSeconds = tonumber(ARGV[6])
local maxSessions = tonumber(ARGV[7])

local result = { status = "CREATED", removedToken = nil }

-- 1. 같은 디바이스의 기존 토큰 찾기
local existingTokens = redis.call('ZRANGE', userTokensKey, 0, -1)
local existingTokenForDevice = nil

for _, token in ipairs(existingTokens) do
    local tokenKey = tokenDataPrefix .. token
    local existingDeviceId = redis.call('HGET', tokenKey, 'deviceId')

    if existingDeviceId == deviceId then
        existingTokenForDevice = token
        break
    end
end

-- 2. 같은 디바이스의 기존 토큰이 있으면 삭제 (교체)
if existingTokenForDevice then
    -- 기존 토큰 데이터 삭제
    redis.call('DEL', tokenDataPrefix .. existingTokenForDevice)
    -- ZSET에서 제거
    redis.call('ZREM', userTokensKey, existingTokenForDevice)
    result.status = "REPLACED"
    result.removedToken = existingTokenForDevice
end

-- 3. 현재 세션 수 확인 및 최대 세션 초과 처리
local currentSessionCount = redis.call('ZCARD', userTokensKey)

if currentSessionCount >= maxSessions then
    -- 가장 오래된 토큰 (score가 가장 낮은) 삭제
    local oldestTokens = redis.call('ZRANGE', userTokensKey, 0, 0)

    if #oldestTokens > 0 then
        local oldestToken = oldestTokens[1]
        -- 토큰 데이터 삭제
        redis.call('DEL', tokenDataPrefix .. oldestToken)
        -- ZSET에서 제거
        redis.call('ZREM', userTokensKey, oldestToken)

        if result.status ~= "REPLACED" then
            result.status = "OVERFLOW"
            result.removedToken = oldestToken
        end
    end
end

-- 4. 새 토큰 저장
-- ZSET에 추가 (score = 생성시간)
redis.call('ZADD', userTokensKey, createdAt, newToken)

-- 토큰 데이터 HASH에 저장
local tokenKey = tokenDataPrefix .. newToken
redis.call('HSET', tokenKey,
    'userId', userId,
    'token', newToken,
    'deviceId', deviceId,
    'data', tokenData,
    'createdAt', createdAt
)

-- TTL 설정
redis.call('EXPIRE', tokenKey, ttlSeconds)
redis.call('EXPIRE', userTokensKey, ttlSeconds)

-- 5. 결과 반환
return cjson.encode(result)