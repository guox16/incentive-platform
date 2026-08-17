package com.incentive.user.repository;

import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.RolePermission;
import com.incentive.user.domain.RolePermissionId;
import com.incentive.user.domain.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
  @Query("select rp.id.permission from RolePermission rp where rp.id.role = :role order by rp.id.permission")
  List<PermissionCode> findPermissionCodesByRole(@Param("role") UserRole role);
}
