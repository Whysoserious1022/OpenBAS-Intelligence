package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {
    Optional<AppUserEntity> findByUsernameAndActiveTrue(String username);
    Optional<AppUserEntity> findByEmailAndActiveTrue(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
