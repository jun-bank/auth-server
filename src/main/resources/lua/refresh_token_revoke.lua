--[[
    RefreshToken 단일 폐기 Lua Script

    특정 토큰을 원자적으로 폐기합니다.

    KEYS:
      [1] user_tokens_key  - 사용자별 토큰 목록 (ZSET)
      [2] token_data_key   - 토큰 데이터 해시 키 prefix

    ARGV:
      [1] token            - 폐기할 토큰 값

    RETURN:
      1: 성공적으로 폐기
      0: 토큰이 존재하지 않음
--]]

local userTokensKey = KEYS[1]
local tokenDataPrefix = KEYS[2]

local token = ARGV[1]

-- 토큰 데이터 키
local tokenKey = tokenDataPrefix .. token

-- 토큰 존재 여부 확인
local exists = redis.call('EXISTS', tokenKey)

if exists == 0 then
    return 0
end

-- ZSET에서 제거
redis.call('ZREM', userTokensKey, token)

-- 토큰 데이터 삭제
redis.call('DEL', tokenKey)

return 1