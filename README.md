# Gaia C端互动平台

基于 Spring Boot 3 + Dubbo 3（Triple）+ Kubernetes Service Discovery 的多模块互动平台后端，
配套移动端 H5 与活动管理后台。

## 技术栈

| 类别         | 选型                                          |
| ---------- | ------------------------------------------- |
| JDK / 框架   | JDK 17 / Spring Boot 3.2.x                  |
| RPC        | Apache Dubbo 3.3.x（Triple / HTTP/2）         |
| 注册中心       | Zookeeper 3.x（`zookeeper://`）                |
| 缓存 / 锁     | Redis（Jedis：BitMap + SETNX）                 |
| 数据库        | MySQL 8.0 + MyBatis-Plus 3.5.x              |
| 监控         | Micrometer + Prometheus                     |
| 全链路追踪      | OpenTelemetry 1.31 + OTLP（grafana/otel-lgtm / Jaeger 等 OTLP 后端，OTLP/HTTP 4318 / OTLP/gRPC 4317） |
| 移动端前端      | Vite 5 + React 18（Hash 路由）                |
| 后台管理前端     | Vite 5 + React 18 + Ant Design 5             |

## 工程结构

```
gaia/
├── pom.xml                           # 父 POM
├── gaia-common/                      # Result / DateUtils / RedisKeys
├── gaia-api/                         # RPC 接口契约：lottery / activity
├── gaia-domain/                      # DDD 领域层（Entity / Repository 接口）
├── gaia-app/                         # 应用层：@DubboService / @Service / Mapper / Entity
├── gaia-infra/                       # DDD 基础设施层（Mapper / Repository 实现）
├── gaia-bootstrap/                   # 启动模块：聚合 app + infra，承载 Spring Boot 入口（HTTP 8091 + Dubbo Triple 20881）
├── gaia-gateway/                     # C 端 HTTP 入口（@RestController，端口 8090）
├── gaia-backend/                     # 后台管理 HTTP 入口（端口 8092）
├── frontend-mobile/                  # 移动端 H5（会员中心 + 活动子页）
└── frontend-backend/                 # 活动管理后台（React + AntD）
```

> 每个启动模块只保留一份 `src/main/resources/application.yml`，不再按 `application-{dev,test}.yml` 切 profile。

## 关键约束

- gaia-app：**仅** `@DubboService(group="…", version="1.0.0")` 与 `@Service` 等业务注解，**不**含 HTTP 业务注解，**不**含启动类。
- gaia-bootstrap：唯一启动入口（`com.gaia.bootstrap.GaiaBootstrapApplication`），负责把 gaia-app + gaia-infra 拉到一起。
- gaia-gateway：**仅** `@RestController`，**不**含业务逻辑。
- gaia-backend：后台管理进程，端口 8092，鉴权本期不实现。
- DDD 依赖方向：
  - `gaia-app` → `gaia-domain`、`gaia-api`、`gaia-common`
  - `gaia-bootstrap` → `gaia-app`、`gaia-infra`
  - `gaia-backend` → `gaia-domain`、`gaia-common`
  - `gaia-infra` → `gaia-domain`（实现领域层 Repository 接口）
- 注册中心使用 Zookeeper（`zookeeper:latest`，端口 2181）。
- 每个启动模块（gaia-bootstrap / gaia-gateway / gaia-backend）只保留一份 `application.yml`，无 profile 切换；连接参数通过环境变量覆盖。

## 本地启动

```bash
# 1. 启动 MySQL / Redis / Zookeeper（示例）
docker run -d --name gaia-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0
docker run -d --name gaia-redis -p 6379:6379 redis:7
docker run -d --name gaia-zookeeper -p 2181:2181 zookeeper:latest

# 2. 初始化表（Spring 启动时也会幂等执行；MySQL 容器 root 密码以实际启动参数为准）
mysql -h127.0.0.1 -uroot -p12345 < gaia-bootstrap/src/main/resources/db/schema.sql

# 3. 构建后端
mvn -DskipTests clean package

# 4. 启动三个进程（注意 Mac 多网卡环境下必须显式指定 DUBBO_IP_TO_BIND/REGISTRY，见下方"踩坑"）
DUBBO_IP_TO_BIND=192.168.31.48 DUBBO_IP_TO_REGISTRY=192.168.31.48 \
  java -jar gaia-bootstrap/target/gaia-bootstrap.jar   # 应用进程（Dubbo Provider + HTTP 8091）
DUBBO_IP_TO_BIND=192.168.31.48 DUBBO_IP_TO_REGISTRY=192.168.31.48 \
  java -jar gaia-gateway/target/gaia-gateway.jar       # C 端 HTTP，端口 8090
DUBBO_IP_TO_BIND=192.168.31.48 DUBBO_IP_TO_REGISTRY=192.168.31.48 \
  MYSQL_PASSWORD=12345 \
  java -jar gaia-backend/target/gaia-backend.jar       # 管理端 HTTP，端口 8092

# 或者开发时用 Maven 直接跑（推荐，dev profile 默认连 localhost:2181 上的 ZK）：
mvn spring-boot:run -pl gaia-bootstrap -am -P dev-bootstrap

# 5. 启动前端（另开终端）
cd frontend-mobile && pnpm install && pnpm dev     # http://localhost:5180
cd frontend-backend && pnpm install && pnpm dev    # http://localhost:5174
```

### 本地启动踩坑（Mac / VPN 多网卡环境必看）

1. **Dubbo IP 解析**：macOS 上有 utun / bridge / anpi 等虚拟网卡，Java `NetworkInterface.getNetworkInterfaces()` 会枚举到这些接口，
   Dubbo 默认从中挑到的可能是 `28.0.0.1` 之类的无效 IP，导致 Provider 把自身发布到无效地址，Consumer 通过 ZK 拿到该地址后
   连接超时（`ConnectTimeoutException` / `error code: 6-16`）。启动时必须显式指定本机实际 LAN IP：
   ```bash
   DUBBO_IP_TO_BIND=<本机 IP>      # Provider 实际 bind 的地址
   DUBBO_IP_TO_REGISTRY=<本机 IP>  # 注册到 ZK 时对外公布的地址
   ```
   两端保持一致即可。不能用 `127.0.0.1` —— Dubbo 会校验该 IP 必须属于本机某张网卡，loopback 被拒。

2. **MySQL / Redis 密码与端口**：
   - `gaia-bootstrap` / `gaia-backend` 默认 `MYSQL_PASSWORD=12345`，与本仓库 docker 起的 MySQL 一致，**无需**额外参数。
   - `gaia-backend` / `gaia-bootstrap` 默认 `REDIS_PORT=6380`（匹配宿主 6380 → 容器 6379 的端口映射）；`gaia-gateway` 默认 `REDIS_PORT=6379`。

3. **Docker 启动示例（端口映射按本机实际为准）**：
   ```bash
   docker run -d --name gaia-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=12345 mysql:8.0
   docker run -d --name gaia-redis -p 6380:6379 redis:7        # 宿主 6380 → 容器 6379
   docker run -d --name gaia-zookeeper -p 2181:2181 zookeeper:latest
   ```

## 验证接口

```bash
# 玩法统一入口（新版，gaia-gateway HTTP 端口 = 8090）
curl -XPOST 'http://localhost:8090/api/interact' -H 'Content-Type: application/json' \
  -d '{"bizCode":"treasure","userId":1001,"models":[{"assertType":"treasure_task","subType":"list"}]}'

# Prometheus 指标（gaia-gateway）
curl 'http://localhost:8090/actuator/prometheus'

# 管理端 - 活动列表（需 gaia-backend 已启动，端口 8092）
curl 'http://localhost:8092/api/admin/activity/list'
```

## 后续路线

- gaia-module-lottery 二期实现：限流、Redis 库存、概率算法、防刷。
- 引入 Spring Authorization Server 统一鉴权。
- 当前已用 OpenTelemetry + OTLP 替换 Zipkin：本地默认对接 grafana/otel-lgtm，端点 `http://127.0.0.1:4318/v1/traces`（Jaeger native OTLP/HTTP、K8s `http://jaeger:4318/v1/traces` 等兼容后端亦可）。日志 MDC key 为 `trace_id` / `span_id`（区别于旧的 Brave `traceId` / `spanId`）。
- Dubbo 3.2 没有官方 OTLP starter，DUBBO RPC 链路依靠全局 W3C propagator 传播 trace context；后续评估接入 `opentelemetry-spring-boot-starter` 或 Dubbo OTel 社区模块。