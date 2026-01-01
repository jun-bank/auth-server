package com.jun_bank.auth_server.domain.auth.infrastructure.persistence.mapper;

import com.jun_bank.auth_server.domain.auth.domain.model.AuthUser;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.AuthUserId;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Email;
import com.jun_bank.auth_server.domain.auth.domain.model.vo.Password;
import com.jun_bank.auth_server.domain.auth.infrastructure.persistence.entity.AuthUserEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthUserMapper {

  public AuthUserEntity toEntity(AuthUser domain) {
    String authUserId =domain.isNew()
        ? AuthUserId.generateId()
        : domain.getAuthUserId().value();
    return AuthUserEntity.of(
        authUserId,
        domain.getUserId(),
        domain.getEmail().value(),
        domain.getPassword().encodedValue(),
        domain.getRole(),
        domain.getStatus(),
        domain.getFailedLoginAttempts(),
        domain.getLockedUntil(),
        domain.getLastLoginAt()
    );
  }

  public AuthUser toDomain(AuthUserEntity entity) {
    return AuthUser.restoreBuilder()
        .authUserId(AuthUserId.of(entity.getAuthUserId()))
        .userId(entity.getUserId())
        .email(Email.of(entity.getEmail()))
        .password(Password.of(entity.getPassword()))
        .role(entity.getRole())
        .status(entity.getStatus())
        .failedLoginAttempts(entity.getFailedLoginAttempts())
        .lockedUntil(entity.getLockedUntil())
        .lastLoginAt(entity.getLastLoginAt())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedBy(entity.getUpdatedBy())
        .deletedAt(entity.getDeletedAt())
        .deletedBy(entity.getDeletedBy())
        .isDeleted(entity.getIsDeleted())
        .build();
  }

  public void updateEntity(AuthUserEntity entity, AuthUser domain) {
    entity.update(
        domain.getPassword().encodedValue(),
        domain.getRole(),
        domain.getStatus(),
        domain.getFailedLoginAttempts(),
        domain.getLockedUntil(),
        domain.getLastLoginAt()
    );

    // Soft Delete 처리
    if (domain.getIsDeleted() != null && domain.getIsDeleted() && !entity.getIsDeleted()) {
      String deletedBy = domain.getDeletedBy() != null
          ? domain.getDeletedBy()
          : domain.getUserId();
      entity.delete(deletedBy);
    }
  }
}
