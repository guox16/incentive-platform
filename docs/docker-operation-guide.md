# 用户激励平台 Docker 操作指南

本文以 Windows PowerShell 和项目根目录 `D:\Practise` 为例。当前最稳定的开发方式是：**基础设施运行在 Docker 中，Java/Vue 应用在本机运行**。

## 1. Docker 在本项目中的职责

| 服务 | Compose 名称 | 宿主机端口 | 用途 |
|---|---|---:|---|
| MySQL 8.4 | `mysql` | `3306` | `user_db`、`points_db`、`incentive_db`、`award_db` |
| Redis 7.4 | `redis` | `6379` | 会话、缓存、限流等后续能力 |
| RabbitMQ 4 | `rabbitmq` | `5672` | 异步发奖消息 |
| RabbitMQ 管理台 | `rabbitmq` | `15672` | 队列和消费者管理界面 |
| Nacos 2.5 | `nacos` | `8848` | 服务注册与配置管理 |
| API 网关 | `gateway` | `8080` | 对外 API 路由 |
| Vue/Nginx | `web` | `80` | Web 页面和 `/api` 反向代理 |

Compose 内的容器通过服务名互相访问，例如 `mysql:3306`、`nacos:8848`。在宿主机直接运行的 Java 程序必须改用 `localhost:3306`、`localhost:8848`。

## 2. 首次准备

先启动 Docker Desktop，等待界面显示 Docker Engine 已运行，然后在 PowerShell 中验证：

```powershell
docker version
docker compose version
docker info
```

如果 `docker info` 报找不到 `//./pipe/docker_engine`，说明 Docker CLI 已安装，但 Docker Desktop/Engine 尚未启动。

进入项目并创建本地环境文件：

```powershell
Set-Location D:\Practise
Copy-Item .env.example .env
notepad .env
```

将所有 `change-me` 替换为仅用于本机开发的密码。`.env` 已被 `.gitignore` 排除，不要将真实密码提交到仓库。

检查 Compose 能否正确解析：

```powershell
docker compose config --quiet
```

该命令只检查配置，不会创建容器。

## 3. 推荐开发方式：只启动基础设施

首次启动需要拉取镜像：

```powershell
docker compose pull mysql redis rabbitmq nacos
docker compose up -d mysql redis rabbitmq nacos
```

查看运行和健康状态：

```powershell
docker compose ps
docker compose logs --tail 100 mysql
docker compose logs --tail 100 nacos
```

持续跟踪某个容器的日志，按 `Ctrl+C` 仅退出日志跟踪，不会停止容器：

```powershell
docker compose logs -f rabbitmq
```

### 3.1 在本机启动用户服务

新开一个 PowerShell，环境变量值要与 `.env` 一致：

```powershell
Set-Location D:\Practise
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_USER = "incentive"
$env:MYSQL_PASSWORD = "你的 MYSQL_PASSWORD"
$env:NACOS_ADDR = "localhost:8848"
$env:SPRING_CLOUD_NACOS_DISCOVERY_ENABLED = "false"
mvn -B -pl services/user-service -am install -DskipTests
mvn -f services/user-service/pom.xml spring-boot:run
```

用户服务监听 `http://localhost:8081`，Swagger UI 位于：

```text
http://localhost:8081/swagger-ui/index.html
```

### 3.2 在本机启动积分服务

再开一个 PowerShell，并设置相同的数据库/Nacos环境变量：

```powershell
Set-Location D:\Practise
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_USER = "incentive"
$env:MYSQL_PASSWORD = "你的 MYSQL_PASSWORD"
$env:NACOS_ADDR = "localhost:8848"
$env:SPRING_CLOUD_NACOS_DISCOVERY_ENABLED = "false"
mvn -B -pl services/points-service -am install -DskipTests
mvn -f services/points-service/pom.xml spring-boot:run
```

积分服务监听 `http://localhost:8082`，Swagger UI 位于：

```text
http://localhost:8082/swagger-ui/index.html
```

当前 Compose 只发布了 Nacos 的 HTTP/UI 端口 `8848`，没有发布 Nacos 2.x 客户端使用的 gRPC 端口。因此上面的本地启动命令暂时关闭服务注册；应用的数据库功能不受影响。需要验证服务注册时，应先在 Compose 中补充 Nacos 的 `9848:9848` 端口映射，再移除 `SPRING_CLOUD_NACOS_DISCOVERY_ENABLED=false`。

## 4. 快速验证业务接口

### 4.1 注册用户

```powershell
$registerBody = @{
  username = "alice"
  password = "secret12"
  nickname = "Alice"
} | ConvertTo-Json

$user = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/api/v1/auth/register" `
  -ContentType "application/json" `
  -Body $registerBody

$user
```

### 4.2 增加积分并查询余额

```powershell
$businessId = [guid]::NewGuid().ToString()
$creditBody = @{
  businessId = $businessId
  userId = $user.id
  amount = 100
  source = "ADMIN"
  remark = "本地 Docker 联调"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8082/api/v1/internal/points/credit" `
  -ContentType "application/json" `
  -Body $creditBody

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8082/api/v1/points/users/$($user.id)/balance"
```

使用同一个 `$businessId` 重复提交完全相同的请求，只会入账一次，响应中的 `replayed` 会变为 `true`。

## 5. 数据库和中间件检查

进入 MySQL 客户端（随后按提示输入 `.env` 中的 `MYSQL_PASSWORD`）：

```powershell
docker compose exec mysql mysql -uincentive -p points_db
```

常用 SQL：

```sql
SHOW TABLES;
SELECT * FROM point_accounts;
SELECT * FROM point_transactions ORDER BY created_at DESC;
```

Redis 交互检查：

```powershell
docker compose exec redis redis-cli
```

进入后执行：

```text
AUTH 你的 REDIS_PASSWORD
PING
```

管理界面：

- RabbitMQ：`http://localhost:15672`，用户名和密码来自 `.env`。
- Nacos：`http://localhost:8848/nacos`。当前本地 Compose 关闭了 Nacos 鉴权。

## 6. 常用生命周期命令

```powershell
# 查看本项目容器
docker compose ps

# 查看全部服务最近日志
docker compose logs --tail 200

# 重启单个基础设施服务
docker compose restart redis

# 停止但保留容器和数据
docker compose stop

# 再次启动已有容器
docker compose start

# 删除容器和项目网络，但保留 MySQL 命名卷
docker compose down

# 查看项目使用的卷
docker volume ls --filter "name=incentive-platform"
```

以下命令会删除 MySQL 命名卷，四个逻辑数据库及其中数据将无法通过 Compose 恢复，必须确认不再需要数据后才能执行：

```powershell
docker compose down -v
```

仅清理未使用的构建缓存时可以运行：

```powershell
docker builder prune
```

执行前先查看 Docker 给出的回收范围，不要在不了解影响时使用 `docker system prune --volumes`。

## 7. 构建与测试

构建用户或积分服务镜像：

```powershell
docker compose build user-service
docker compose build points-service
```

代码或依赖变化后强制重新构建：

```powershell
docker compose build --no-cache points-service
```

积分服务包含 MySQL Testcontainers 测试。Docker Engine 运行时执行：

```powershell
mvn -B -pl services/points-service -am test
```

Docker Engine 未运行时，三个真实 MySQL 集成测试会自动跳过；普通单元测试和接口测试仍会执行。

## 8. 当前全容器启动限制

`docker compose up -d --build` 是项目最终目标，但当前仓库暂不建议把它作为主要开发入口，原因如下：

1. `gateway`、`incentive-service`、`award-service` 目前还是基础骨架，尚未统一绑定 Spring Boot `repackage`，对应镜像可能无法用 `java -jar` 启动。
2. Compose 尚未把 `.env` 中的 MySQL 密码显式注入业务服务；修改默认密码后，容器内服务可能仍使用配置文件默认值。
3. 业务服务缺少完整健康检查，`depends_on: service_started` 只代表进程已启动，不代表数据库和 API 已准备就绪。
4. 用户和积分服务没有映射宿主机端口；这符合“只通过网关访问”的最终设计，但当前内部积分命令又故意不经过网关，因此全容器模式下不便从宿主机调试该接口。

在这些基础设施项完善前，使用第 3 节的“中间件容器化、应用本地运行”模式最容易开发和排障。

## 9. 常见故障

### Docker Engine 未运行

症状：

```text
failed to connect to the docker API
open //./pipe/docker_engine: The system cannot find the file specified
```

处理：启动 Docker Desktop，等待 Engine 就绪，再运行 `docker info`。

### 端口被占用

```powershell
Get-NetTCPConnection -LocalPort 3306,6379,8848,15672 -ErrorAction SilentlyContinue
```

停止冲突程序，或在 `docker-compose.yml` 中调整冒号左侧的宿主机端口。

### 修改 `.env` 密码后 MySQL 登录失败

MySQL 初始化变量只在数据卷首次创建时生效。已有 `mysql-data` 卷时，修改 `.env` 不会自动修改数据库内部账户密码。应继续使用原密码，或先备份数据，再明确执行 `docker compose down -v` 重新初始化。

### 容器内能访问、宿主机不能访问

检查服务是否在 Compose 中配置了 `ports`。`expose` 或容器内部端口只供 Compose 网络使用，不会自动开放给 Windows 宿主机。

### Docker 配置文件访问警告

如果持续出现 `C:\Users\yanf\.docker\config.json: Access is denied`，先确认 Docker Desktop 使用当前 Windows 用户运行，并检查该文件的读取权限。不要直接删除配置文件；它可能包含 Docker 登录和上下文配置。
