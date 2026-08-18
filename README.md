# 用户激励平台

面向面试展示的后端优先微服务项目。当前已完成工程骨架、基础用户账户、JWT/Refresh Token 登录、五表 RBAC，以及积分余额、加减、流水和幂等能力。

## 结构

- `services/gateway`：统一 API 入口与路由。
- `services/user-service`：身份与会员权益边界。
- `services/points-service`：积分资产边界。
- `services/incentive-service`：兑换与抽奖边界。
- `services/award-service`：异步发奖边界。
- `services/common`：错误响应与 Trace ID 基础约定。
- `web`：Vue 3 单前端工程。

## 本地启动

完整的命令、接口验证、数据卷管理和故障排查见 [Docker 操作指南](docs/docker-operation-guide.md)。

1. 安装并启动 Docker Desktop，复制 `.env.example` 为 `.env`，替换所有示例密码，并将 `JWT_SECRET`、`INTERNAL_JWT_SECRET` 设置为两个不同的、至少 32 个字符的随机密钥，同时设置随机的 `XXL_JOB_ACCESS_TOKEN`。
2. 执行 `docker compose up -d mysql redis rabbitmq nacos xxl-job-admin` 启动开发依赖。
3. 按 Docker 操作指南在本机启动用户服务和积分服务；RabbitMQ 管理台为 `http://localhost:15672`，Nacos 为 `http://localhost:8848/nacos`，XXL-JOB 管理台为 `http://localhost:8088/xxl-job-admin`。

XXL-JOB 初始化账号为 `admin / 123456`，仅用于本地开发，首次登录后应立即修改。过期积分预占补偿任务已初始化为停止状态，积分服务执行器注册成功后在管理台启用该任务。

如果本机已经存在 `mysql-data` 数据卷，MySQL 不会再次自动执行新增初始化脚本。可执行以下命令补建 XXL-JOB 数据库和表，无需删除现有数据卷：

```bash
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /docker-entrypoint-initdb.d/01-databases.sql && mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /docker-entrypoint-initdb.d/06-xxl-job-schema.sql'
```

后端使用 Java 21、Spring Boot 3.2.4、Spring Cloud 2023.0.1 与 Spring Cloud Alibaba 2023.0.1.0（Nacos）；该组合按 Alibaba 的官方兼容表锁定。 本地 Maven 构建命令为 `mvn verify`。前端在 `web` 目录中执行 `npm install`、`npm run dev`。

生产环境通过 HTTPS 部署时，应将 `REFRESH_TOKEN_COOKIE_SECURE` 设置为 `true`。

## 工程约定

首次初始化仓库、检查敏感文件并推送远程的完整步骤见 [Git 首次上传指南](docs/git-operation-guide.md)。

用户服务的代码阅读顺序、业务调用链、调试方法和练习题见 [用户模块学习指南](docs/user-module-learning-guide.md)。

- 对外 API 固定使用 `/api/v1` 前缀；命令接口必须支持幂等键。
- 登录成功后使用 `Authorization: Bearer <accessToken>` 请求受保护接口；用户自身资源统一使用 `/me` 路径，不从 URL 接收用户 ID。
- Access Token 过期后前端通过 HttpOnly Cookie 自动轮换 Refresh Token；退出登录会在服务端撤销当前令牌。
- RBAC 使用 `users`、`roles`、`permissions`、`user_roles`、`role_permissions` 五表；网关按 JWT 权限编码执行授权，不配置角色继承。
- 用户、积分、激励和奖品服务会再次验证用户 JWT 的签名、有效期、签发方、受众和权限；业务用户 ID 只读取已验证的 `sub`，不信任身份请求头。
- Access Token 使用 Redis 黑名单即时撤销：退出登录按 `jti` 撤销当前令牌，角色变更按用户签发截止点撤销全部旧令牌；网关和所有下游服务均会校验，记录随 Token 最长有效期自动过期。
- 激励服务调用内部积分命令时签发独立的一分钟服务 JWT，普通用户 JWT 无法直接执行积分增减。
- 每个服务只拥有自己的数据库，禁止跨库查询和外键。
- 所有 HTTP 响应透传或生成 `X-Trace-Id`；错误格式统一为 `code`、`message`、`traceId`、`timestamp`。
- 业务实现放在各服务内；`common` 仅保留真正跨服务且稳定的技术契约，避免形成共享业务模型。
- 提交遵循 Conventional Commits，例如 `feat(points): add balance ledger`、`fix(gateway): remove spoofed identity header`。

## 下一阶段

角色权限管理页面、JWT 非对称密钥轮换和过期 Refresh Token 清理任务。
