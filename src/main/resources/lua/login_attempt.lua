--[[
    로그인 시도 카운팅 Lua Script
    
    원자적으로 로그인 시도를 카운팅하고 잠금 여부를 판단합니다.
    
    KEYS:
      [1] attempt_key      - 로그인 시도 카운터 키
      [2] lock_key         - 계정 잠금 키
    
    ARGV:
      [1] action           - "INCREMENT" | "RESET" | "CHECK"
      [2] maxAttempts      - 최대 허용 시도 횟수
      [3] lockSeconds      - 잠금 시간 (초)
      [4] attemptTtl       - 시도 카운터 TTL (초)
    
    RETURN:
      JSON: { 
        "status": "OK|LOCKED|ALREADY_LOCKED", 
        "attempts": 현재시도횟수, 
        "remainingSeconds": 남은잠금시간 
      }
--]]

local attemptKey = KEYS[1]
local lockKey = KEYS[2]

local action = ARGV[1]
local maxAttempts = tonumber(ARGV[2])
local lockSeconds = tonumber(ARGV[3])
local attemptTtl = tonumber(ARGV[4])

local result = { status = "OK", attempts = 0, remainingSeconds = 0 }

-- 이미 잠금 상태인지 확인
local lockTtl = redis.call('TTL', lockKey)
if lockTtl > 0 then
    result.status = "ALREADY_LOCKED"
    result.remainingSeconds = lockTtl
    
    -- 현재 시도 횟수도 반환
    local currentAttempts = redis.call('GET', attemptKey)
    result.attempts = currentAttempts and tonumber(currentAttempts) or 0
    
    return cjson.encode(result)
end

-- ACTION: CHECK - 현재 상태만 확인
if action == "CHECK" then
    local currentAttempts = redis.call('GET', attemptKey)
    result.attempts = currentAttempts and tonumber(currentAttempts) or 0
    return cjson.encode(result)
end

-- ACTION: RESET - 성공 시 카운터 초기화
if action == "RESET" then
    redis.call('DEL', attemptKey)
    redis.call('DEL', lockKey)
    result.status = "OK"
    result.attempts = 0
    return cjson.encode(result)
end

-- ACTION: INCREMENT - 실패 시 카운터 증가
if action == "INCREMENT" then
    -- 카운터 증가
    local newCount = redis.call('INCR', attemptKey)
    
    -- 첫 시도면 TTL 설정
    if newCount == 1 then
        redis.call('EXPIRE', attemptKey, attemptTtl)
    end
    
    result.attempts = newCount
    
    -- 최대 시도 횟수 초과 시 잠금
    if newCount >= maxAttempts then
        redis.call('SET', lockKey, 'LOCKED', 'EX', lockSeconds)
        result.status = "LOCKED"
        result.remainingSeconds = lockSeconds
    end
    
    return cjson.encode(result)
end

-- 알 수 없는 action
result.status = "UNKNOWN_ACTION"
return cjson.encode(result)