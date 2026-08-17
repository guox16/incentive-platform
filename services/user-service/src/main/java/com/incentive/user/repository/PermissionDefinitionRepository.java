package com.incentive.user.repository;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.PermissionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionDefinitionRepository
    extends JpaRepository<PermissionDefinition, PermissionCode> {}
