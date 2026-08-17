package com.incentive.user.repository;

import com.incentive.user.domain.RoleDefinition;
import com.incentive.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDefinitionRepository extends JpaRepository<RoleDefinition, UserRole> {}
