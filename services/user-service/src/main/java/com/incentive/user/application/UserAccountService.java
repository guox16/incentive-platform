package com.incentive.user.application;

import com.incentive.user.domain.UserAccount;
import com.incentive.user.dto.LoginRequest;
import com.incentive.user.dto.RegisterRequest;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.dto.UserResponse;
import com.incentive.user.repository.UserAccountRepository;
import com.incentive.user.support.UserBusinessException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 注册、登录校验和资料维护的应用服务。 */
@Service
@Transactional(readOnly = true)
public class UserAccountService {
  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String username = request.username().trim();
    if (repository.existsByUsername(username)) {
      throw usernameTaken();
    }
    // BCrypt 每次产生不同盐值；数据库中永远不保存请求携带的明文密码。
    UserAccount account = new UserAccount(username, passwordEncoder.encode(request.password()), request.nickname().trim());
    try {
      return toResponse(repository.saveAndFlush(account));
    } catch (DataIntegrityViolationException ex) {
      // exists 检查无法消除并发注册竞争，唯一约束才是最终保障。
      throw usernameTaken();
    }
  }

  public UserResponse login(LoginRequest request) {
    UserAccount account = repository.findByUsername(request.username().trim())
        .orElseThrow(this::invalidCredentials);
    if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
      throw invalidCredentials();
    }
    // 本轮仅做凭证校验，不创建 Session，也不签发 JWT。
    return toResponse(account);
  }

  public UserResponse getProfile(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
    UserAccount account = find(id);
    account.changeNickname(request.nickname().trim());
    return toResponse(account);
  }

  private UserAccount find(UUID id) {
    return repository.findById(id).orElseThrow(() -> new UserBusinessException(
        "USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND));
  }

  private UserBusinessException usernameTaken() {
    return new UserBusinessException("USERNAME_ALREADY_EXISTS", "用户名已被使用", HttpStatus.CONFLICT);
  }

  private UserBusinessException invalidCredentials() {
    // 用户不存在与密码错误使用同一错误，避免泄露用户名是否已注册。
    return new UserBusinessException("INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
  }

  private UserResponse toResponse(UserAccount account) {
    return new UserResponse(account.getId(), account.getUsername(), account.getNickname(), account.getCreatedAt(), account.getUpdatedAt());
  }
}

