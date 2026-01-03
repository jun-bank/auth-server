package com.jun_bank.auth_server.global.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IpRateLimitCacheRepository {

  private final RedisTemplate<String, String> stringRedisTemplate;

  private final ObjectMapper objectMapper = JsonMapper.builder()
          .findAndAddModules()
          .build();
}
