package com.incentive.user.repository;

import com.incentive.user.domain.UserRole;
import com.incentive.user.domain.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select assignment from UserRoleAssignment assignment where assignment.role = :role")
  List<UserRoleAssignment> findAllByRoleForUpdate(@Param("role") UserRole role);
}
