package com.incentive.user.application;

import com.incentive.user.domain.UserAccount;
import com.incentive.user.dto.LoginRequest;
import com.incentive.user.dto.LoginResponse;
import com.incentive.user.dto.RegisterRequest;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.dto.UserResponse;
import com.incentive.user.repository.UserAccountRepository;
import com.incentive.user.security.JwtTokenService;
import com.incentive.user.security.RefreshTokenService;
import com.incentive.user.security.IssuedSession;
import com.incentive.user.support.UserBusinessException;
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
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenService refreshTokenService;

  /** 创建用户账户应用服务。 */
  public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService, RefreshTokenService refreshTokenService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenService = refreshTokenService;
  }

  @Transactional
  /** 注册新用户并安全保存密码哈希。 */
  public UserResponse register(RegisterRequest request) {
    String username = request.username().trim();
    String phone = request.phone().trim();
    if (repository.existsByUsername(username)) {
      throw usernameTaken();
    }
    if (repository.existsByPhone(phone)) {
      throw phoneTaken();
    }
    // BCrypt 每次产生不同盐值；数据库中永远不保存请求携带的明文密码。
    UserAccount account = new UserAccount(username, phone, passwordEncoder.encode(request.password()), request.nickname().trim());
    try {
      return toResponse(repository.saveAndFlush(account));
    } catch (DataIntegrityViolationException ex) {
      // exists 检查无法消除并发注册竞争，唯一约束才是最终保障。
      throw usernameTaken();
    }
  }

  /** 校验用户登录凭证并返回用户资料。 */
  @Transactional
  public IssuedSession login(LoginRequest request) {
    String identifier = request.identifier().trim();
    UserAccount account = repository.findByUsernameOrPhone(identifier, identifier)
        .orElseThrow(this::invalidCredentials);
    if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
      throw invalidCredentials();
    }
    return issueSession(account, refreshTokenService.issue(account.getId()));
  }

  /** 轮换 Refresh Token 并签发新的短期访问令牌。 */
  @Transactional(noRollbackFor = UserBusinessException.class)
  public IssuedSession refresh(String refreshToken) {
    var rotated = refreshTokenService.rotate(refreshToken);
    UserAccount account = find(rotated.userId());
    var accessToken = jwtTokenService.issue(account.getId());
    return new IssuedSession(
        new LoginResponse(accessToken.value(), "Bearer", accessToken.expiresInSeconds(), toResponse(account)),
        rotated.value(), rotated.expiresInSeconds());
  }

  /** 撤销当前浏览器持有的 Refresh Token；重复调用保持幂等。 */
  @Transactional
  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  /** 查询指定用户的资料。 */
  public UserResponse getProfile(Long id) {
    return toResponse(find(id));
  }

  @Transactional
  /** 修改指定用户的昵称。 */
  public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
    UserAccount account = find(id);
    String phone = request.phone().trim();
    if (!phone.equals(account.getPhone()) && repository.existsByPhone(phone)) {
      throw phoneTaken();
    }
    account.changeNickname(request.nickname().trim());
    account.changePhone(phone);
    return toResponse(account);
  }

  /** 查找用户；不存在时抛出业务异常。 */
  private UserAccount find(Long id) {
    return repository.findById(id).orElseThrow(() -> new UserBusinessException(
        "USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND));
  }

  /** 创建用户名已被占用的异常。 */
  private UserBusinessException usernameTaken() {
    return new UserBusinessException("USERNAME_ALREADY_EXISTS", "用户名已被使用", HttpStatus.CONFLICT);
  }

  private UserBusinessException phoneTaken() {
    return new UserBusinessException("PHONE_ALREADY_EXISTS", "手机号已被使用", HttpStatus.CONFLICT);
  }

  /** 创建不暴露账户是否存在的登录失败异常。 */
  private UserBusinessException invalidCredentials() {
    // 用户不存在与密码错误使用同一错误，避免泄露用户名是否已注册。
    return new UserBusinessException("INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
  }

  /** 将用户账户实体转换为接口响应。 */
  private UserResponse toResponse(UserAccount account) {
    return new UserResponse(account.getId(), account.getUsername(), account.getPhone(), account.getNickname(), account.getCreatedAt(), account.getUpdatedAt());
  }

  private IssuedSession issueSession(com.incentive.user.domain.UserAccount account,
      com.incentive.user.security.IssuedRefreshToken refreshToken) {
    var accessToken = jwtTokenService.issue(account.getId());
    return new IssuedSession(
        new LoginResponse(accessToken.value(), "Bearer", accessToken.expiresInSeconds(), toResponse(account)),
        refreshToken.value(), refreshToken.expiresInSeconds());
  }
}
