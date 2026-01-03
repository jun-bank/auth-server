package com.jun_bank.auth_server.domain.auth.infrastructure.persistence;

import com.jun_bank.auth_server.domain.auth.application.port.out.AuthUserRepository;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.jpa.AuthUserJpaRepository;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.mapper.AuthUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthUserRepositoryAdapter implements AuthUserRepository {

  private final AuthUserJpaRepository authUserJpaRepository;
  private final AuthUserMapper authUserMapper;
  private final AuthUserCacheRepository

}
