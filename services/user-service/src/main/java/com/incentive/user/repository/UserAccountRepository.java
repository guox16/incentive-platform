package com.incentive.user.repository;

import com.incentive.user.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  /** 检查用户名是否已被注册。 */
  boolean existsByUsername(String username);
  /** 检查手机号是否已被注册。 */
  boolean existsByPhone(String phone);
  /** 根据用户名查找用户账户。 */
  Optional<UserAccount> findByUsername(String username);
  /** 根据用户名或手机号查找用户账户。 */
  Optional<UserAccount> findByUsernameOrPhone(String username, String phone);
}
