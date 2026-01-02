--[[
    RefreshToken 전체 폐기 Lua Script

    사용자의 모든 토큰을 원자적으로 폐기합니다.
    (전체 로그아웃, 비밀번호 변경 시 사용)

    KEYS:
      [1] user_tokens_key  - 사용자별 토큰 목록 (ZSET)
      [2] token_data_key   - 토큰 데이터 해시 키 prefix

    ARGV:
      (없음)

    RETURN:
      폐기된 토큰 수
--]]

local userTokensKey = KEYS[1]
local tokenDataPrefix = KEYS[2]

-- 사용자의 모든 토큰 조회
local tokens = redis.call('ZRANGE', userTokensKey, 0, -1)

local deletedCount = 0

-- 각 토큰 데이터 삭제
for _, token in ipairs(tokens) do
    local tokenKey = tokenDataPrefix .. token
    redis.call('DEL', tokenKey)
    deletedCount = deletedCount + 1
end

-- ZSET 전체 삭제
redis.call('DEL', userTokensKey)

return deletedCount