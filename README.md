# 用户激励平台

面向面试展示的后端优先微服务项目。当前已完成工程骨架、基础用户账户以及积分余额、加减、流水和幂等能力；JWT、活动与发奖业务仍待实现。

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

1. 安装并启动 Docker Desktop，复制 `.env.example` 为 `.env` 并替换所有示例密码。
2. 执行 `docker compose up -d mysql redis rabbitmq nacos` 启动开发依赖。
3. 按 Docker 操作指南在本机启动用户服务和积分服务；RabbitMQ 管理台为 `http://localhost:15672`，Nacos 为 `http://localhost:8848/nacos`。

后端使用 Java 21、Spring Boot 3.2.4、Spring Cloud 2023.0.1 与 Spring Cloud Alibaba 2023.0.1.0（Nacos）；该组合按 Alibaba 的官方兼容表锁定。 本地 Maven 构建命令为 `mvn verify`。前端在 `web` 目录中执行 `npm install`、`npm run dev`。

## 工程约定

首次初始化仓库、检查敏感文件并推送远程的完整步骤见 [Git 首次上传指南](docs/git-operation-guide.md)。

- 对外 API 固定使用 `/api/v1` 前缀；命令接口必须支持幂等键。
- 每个服务只拥有自己的数据库，禁止跨库查询和外键。
- 所有 HTTP 响应透传或生成 `X-Trace-Id`；错误格式统一为 `code`、`message`、`traceId`、`timestamp`。
- 业务实现放在各服务内；`common` 仅保留真正跨服务且稳定的技术契约，避免形成共享业务模型。
- 提交遵循 Conventional Commits，例如 `feat(points): add balance ledger`、`fix(gateway): remove spoofed identity header`。

## 下一阶段

补齐 JWT、Refresh Token、`USER`/`ADMIN` 角色和 Vue 登录守卫，然后进入兑换活动闭环。
