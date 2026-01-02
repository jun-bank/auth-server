--[[
    IP Rate Limiting Lua Script
    
    IP 기반으로 요청을 제한하고 의심스러운 IP를 차단합니다.
    
    KEYS:
      [1] ip_counter_key   - IP별 요청 카운터
      [2] ip_block_key     - IP 차단 키
    
    ARGV:
      [1] action           - "CHECK_AND_INCREMENT" | "BLOCK" | "UNBLOCK" | "IS_BLOCKED"
      [2] maxRequests      - 윈도우 내 최대 요청 수
      [3] windowSeconds    - 시간 윈도우 (초)
      [4] blockSeconds     - 차단 시간 (초)
      [5] blockReason      - 차단 사유 (BLOCK 시)
    
    RETURN:
      JSON: { 
        "allowed": true|false, 
        "currentCount": 현재요청수, 
        "blocked": 차단여부,
        "remainingSeconds": 남은차단시간,
        "reason": 차단사유
      }
--]]

local ipCounterKey = KEYS[1]
local ipBlockKey = KEYS[2]

local action = ARGV[1]
local maxRequests = tonumber(ARGV[2])
local windowSeconds = tonumber(ARGV[3])
local blockSeconds = tonumber(ARGV[4])
local blockReason = ARGV[5]

local result = { 
    allowed = true, 
    currentCount = 0, 
    blocked = false, 
    remainingSeconds = 0,
    reason = nil
}

-- ACTION: IS_BLOCKED - 차단 여부만 확인
if action == "IS_BLOCKED" then
    local blockTtl = redis.call('TTL', ipBlockKey)
    if blockTtl > 0 then
        local reason = redis.call('GET', ipBlockKey)
        result.allowed = false
        result.blocked = true
        result.remainingSeconds = blockTtl
        result.reason = reason
    end
    return cjson.encode(result)
end

-- ACTION: BLOCK - IP 차단
if action == "BLOCK" then
    redis.call('SET', ipBlockKey, blockReason, 'EX', blockSeconds)
    redis.call('DEL', ipCounterKey)  -- 카운터 초기화
    result.allowed = false
    result.blocked = true
    result.remainingSeconds = blockSeconds
    result.reason = blockReason
    return cjson.encode(result)
end

-- ACTION: UNBLOCK - IP 차단 해제
if action == "UNBLOCK" then
    redis.call('DEL', ipBlockKey)
    redis.call('DEL', ipCounterKey)
    result.allowed = true
    result.blocked = false
    return cjson.encode(result)
end

-- ACTION: CHECK_AND_INCREMENT - 요청 확인 및 카운트 증가
if action == "CHECK_AND_INCREMENT" then
    -- 먼저 차단 여부 확인
    local blockTtl = redis.call('TTL', ipBlockKey)
    if blockTtl > 0 then
        local reason = redis.call('GET', ipBlockKey)
        result.allowed = false
        result.blocked = true
        result.remainingSeconds = blockTtl
        result.reason = reason
        return cjson.encode(result)
    end
    
    -- 카운터 증가
    local count = redis.call('INCR', ipCounterKey)
    
    -- 첫 요청이면 TTL 설정
    if count == 1 then
        redis.call('EXPIRE', ipCounterKey, windowSeconds)
    end
    
    result.currentCount = count
    
    -- 최대 요청 수 초과 시 자동 차단
    if count > maxRequests then
        local autoBlockReason = "Rate limit exceeded: " .. count .. " requests in " .. windowSeconds .. "s"
        redis.call('SET', ipBlockKey, autoBlockReason, 'EX', blockSeconds)
        result.allowed = false
        result.blocked = true
        result.remainingSeconds = blockSeconds
        result.reason = autoBlockReason
    end
    
    return cjson.encode(result)
end

-- 알 수 없는 action
result.allowed = false
result.reason = "UNKNOWN_ACTION"
return cjson.encode(result)