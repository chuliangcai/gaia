# Gaia C端互动平台

基于 Spring Boot 3 + Dubbo 3（Triple）+ Kubernetes Service Discovery 的多模块互动平台后端，
配套移动端 H5 与活动管理后台。

## 技术栈

| 类别         | 选型                                          |
| ---------- | ------------------------------------------- |
| JDK / 框架   | JDK 17 / Spring Boot 3.2.x                  |
| RPC        | Apache Dubbo 3.3.x（Triple / HTTP/2）         |
| 注册中心       | Kubernetes API（`kubernetes://`）              |
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
├── gaia-api/                         # RPC 接口契约：sign / lottery / activity
├── gaia-domain/                      # DDD 领域层（Entity / Repository 接口）
├── gaia-app/                         # 应用层：@DubboService / @Service / Mapper / Entity
├── gaia-infra/                       # DDD 基础设施层（Mapper / Repository 实现）
├── gaia-bootstrap/                   # 启动模块：聚合 app + infra，承载 Spring Boot 入口
├── gaia-gateway/                     # C 端 HTTP 入口（@RestController，端口 8080）
├── gaia-backend/                     # 后台管理 HTTP 入口（端口 8088）
├── frontend-mobile/                  # 移动端 H5（会员中心 + 活动子页）
└── frontend-backend/                 # 活动管理后台（React + AntD）
```

## 关键约束

- gaia-app：**仅** `@DubboService(group="…", version="1.0.0")` 与 `@Service` 等业务注解，**不**含 HTTP 业务注解，**不**含启动类。
- gaia-bootstrap：唯一启动入口（`com.gaia.bootstrap.GaiaBootstrapApplication`），负责把 gaia-app + gaia-infra 拉到一起。
- gaia-gateway：**仅** `@RestController`，**不**含业务逻辑。
- gaia-backend：后台管理进程，端口 8088，鉴权本期不实现。
- DDD 依赖方向：
  - `gaia-app` → `gaia-domain`、`gaia-api`、`gaia-common`
  - `gaia-bootstrap` → `gaia-app`、`gaia-infra`
  - `gaia-backend` → `gaia-domain`、`gaia-common`
  - `gaia-infra` → `gaia-domain`（实现领域层 Repository 接口）
- 注册中心当前使用 Zookeeper（docker-compose 启 `zookeeper:latest`，端口 2181），原 Kubernetes Service Discovery 已下线。
- 所有 HTTPS / TLS / 域名 均**未启用**，Ingress 仅做 `pathType: Prefix`。

## 本地启动

```bash
# 1. 启动 MySQL / Redis / Zookeeper（示例）
docker run -d --name gaia-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0
docker run -d --name gaia-redis -p 6379:6379 redis:7
docker run -d --name gaia-zookeeper -p 2181:2181 zookeeper:latest

# 2. 初始化表（Spring 启动时也会幂等执行）
mysql -h127.0.0.1 -uroot -proot < gaia-bootstrap/src/main/resources/db/schema.sql

# 3. 构建后端
mvn -DskipTests clean package

# 4. 启动三个进程
java -jar gaia-bootstrap/target/gaia-bootstrap.jar     # 应用进程（Dubbo Provider + HTTP 8081）
java -jar gaia-gateway/target/gaia-gateway.jar         # C 端 HTTP，端口 8080
java -jar gaia-backend/target/gaia-backend.jar         # 管理端 HTTP，端口 8088

# 或者开发时用 Maven 直接跑（推荐，dev profile 默认连 localhost:2181 上的 ZK）：
mvn spring-boot:run -pl gaia-bootstrap -am -P dev-bootstrap

# 5. 启动前端（另开终端）
cd frontend-mobile && pnpm install && pnpm dev     # http://localhost:5173
cd frontend-backend && pnpm install && pnpm dev    # http://localhost:5174
```

## 验证接口

```bash
# 玩法统一入口（新版）
curl -XPOST 'http://localhost:8080/api/interact' -H 'Content-Type: application/json' \
  -d '{"bizCode":"sign","userId":1001,"models":[{"assertType":"sign_do","subType":""}]}'

# Prometheus 指标
curl 'http://localhost:8080/actuator/prometheus'

# 管理端 - 活动列表（需 gaia-backend 已启动）
curl 'http://localhost:8088/api/admin/activity/list'
```

## K8s 部署

```bash
kubectl create namespace gaia
kubectl apply -f deploy/k8s/gaia-gateway.yaml      # 含 SA / ClusterRoleBinding
kubectl apply -f deploy/k8s/gaia-app.yaml
kubectl apply -f deploy/k8s/gaia-backend.yaml
kubectl apply -f deploy/k8s/gateway-ingress.yaml
```

## 后续路线

- gaia-module-lottery 二期实现：限流、Redis 库存、概率算法、防刷。
- 引入 Spring Authorization Server 统一鉴权。
- 当前已用 OpenTelemetry + OTLP 替换 Zipkin：本地默认对接 grafana/otel-lgtm，端点 `http://127.0.0.1:4318/v1/traces`（Jaeger native OTLP/HTTP、K8s `http://jaeger:4318/v1/traces` 等兼容后端亦可）。日志 MDC key 为 `trace_id` / `span_id`（区别于旧的 Brave `traceId` / `spanId`）。
- Dubbo 3.2 没有官方 OTLP starter，DUBBO RPC 链路依靠全局 W3C propagator 传播 trace context；后续评估接入 `opentelemetry-spring-boot-starter` 或 Dubbo OTel 社区模块。