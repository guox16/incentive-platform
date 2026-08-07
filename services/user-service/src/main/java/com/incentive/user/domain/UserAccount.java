package com.incentive.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** 用户服务拥有的账户聚合根；密码只保存 BCrypt 哈希值。 */
@Entity
@Table(name = "users")
public class UserAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32, updatable = false)
  private String username;

  @Column(name = "mobile", nullable = false, unique = true, length = 11)
  private String phone;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  /** 供 JPA 创建用户账户实体。 */
  protected UserAccount() {}

  /** 使用用户名、密码哈希和昵称创建用户账户。 */
  public UserAccount(String username, String phone, String passwordHash, String nickname) {
    this.username = username;
    this.phone = phone;
    this.passwordHash = passwordHash;
    this.nickname = nickname;
  }

  /** 在新增持久化前初始化创建和更新时间。 */
  @PrePersist void beforeInsert() { createdAt = Instant.now(); updatedAt = createdAt; }
  /** 在更新持久化前刷新更新时间。 */
  @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
  /** 修改用户昵称。 */
  public void changeNickname(String nickname) { this.nickname = nickname; }
  public void changePhone(String phone) { this.phone = phone; }
  /** 获取用户 ID。 */
  public Long getId() { return id; }
  /** 获取用户名。 */
  public String getUsername() { return username; }
  /** 获取用户手机号。 */
  public String getPhone() { return phone; }
  /** 获取密码哈希值。 */
  public String getPasswordHash() { return passwordHash; }
  /** 获取用户昵称。 */
  public String getNickname() { return nickname; }
  /** 获取账户创建时间。 */
  public Instant getCreatedAt() { return createdAt; }
  /** 获取账户最后更新时间。 */
  public Instant getUpdatedAt() { return updatedAt; }
}
