# Gaia C端互动平台后端

基于 Spring Boot 3 + Dubbo 3（Triple）+ Kubernetes Service Discovery 的多模块互动平台后端。

## 技术栈

| 类别         | 选型                                          |
| ---------- | ------------------------------------------- |
| JDK / 框架   | JDK 17 / Spring Boot 3.2.x                  |
| RPC        | Apache Dubbo 3.3.x（Triple / HTTP/2）         |
| 注册中心       | Kubernetes API（`kubernetes://`）              |
| 缓存 / 锁     | Redis（Jedis：BitMap + SETNX）                 |
| 数据库        | MySQL 8.0 + MyBatis-Plus 3.5.x              |
| 监控         | Micrometer + Prometheus                     |
| 全链路追踪      | OpenTelemetry 1.31 + Jaeger（OTLP/HTTP 4318） |

## 工程结构

```
gaia/
├── pom.xml                           # 父 POM
├── gaia-common/                      # Result / DateUtils / RedisKeys
├── gaia-api/                         # RPC 接口契约：sign / lottery
├── gaia-domain/                      # DDD 领域层（Entity / Repository 接口）
├── gaia-server/                      # 应用层：Dubbo Provider 承载方
├── gaia-infra/                       # DDD 基础设施层（Mapper / Repository 实现）
├── gaia-gateway/                     # 唯一 C 端 HTTP 入口（@RestController）
└── gaia-backend/                     # 后台管理 HTTP 入口（端口 8088）
```

## 关键约束

- gaia-server：**仅** `@DubboService(group="…", version="1.0.0")`，**不**含 HTTP 业务注解。
- gaia-gateway：**仅** `@RestController`，**不**含业务逻辑。
- gaia-backend：后台管理进程，端口 8088，鉴权本期不实现。
- DDD 依赖方向：
  - `gaia-server` → `gaia-domain`、`gaia-api`、`gaia-common`
  - `gaia-backend` → `gaia-domain`、`gaia-common`
  - `gaia-infra` → `gaia-domain`、`gaia-server`（实现领域层 Repository 接口）
- 注册中心使用 Kubernetes API，Pod 内通过 ServiceAccount 读取 Endpoints（详见 `deploy/k8s/`）。
- 所有 HTTPS / TLS / 域名 均**未启用**，Ingress 仅做 `pathType: Prefix`。

## 本地启动

```bash
# 1. 启动 MySQL / Redis（示例）
docker run -d --name gaia-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0
docker run -d --name gaia-redis -p 6379:6379 redis:7

# 2. 初始化表
mysql -h127.0.0.1 -uroot -proot < gaia-server/src/main/resources/db/schema.sql

# 3. 构建
mvn -DskipTests clean package

# 4. 启动两个进程（依赖 Kubernetes API；本地需配置 ~/.kube/config 或使用 KUBECONFIG）
java -jar gaia-gateway/target/gaia-gateway.jar
java -jar gaia-server/target/gaia-server.jar
```

## 验证接口

```bash
# 签到
curl -XPOST 'http://localhost:8080/api/sign/do?userId=1001'

# 当月日历
curl 'http://localhost:8080/api/sign/calendar?userId=1001&yearMonth=202608'

# 连续天数
curl 'http://localhost:8080/api/sign/streak?userId=1001'

# Prometheus 指标
curl 'http://localhost:8080/actuator/prometheus'
```

## K8s 部署

```bash
kubectl create namespace gaia
kubectl apply -f deploy/k8s/gaia-gateway.yaml      # 含 SA / ClusterRoleBinding
kubectl apply -f deploy/k8s/gaia-server.yaml
kubectl apply -f deploy/k8s/gaia-backend.yaml
kubectl apply -f deploy/k8s/gateway-ingress.yaml
```

## 后续路线

- gaia-module-lottery 二期实现：限流、Redis 库存、概率算法、防刷。
- 引入 Spring Authorization Server 统一鉴权。
- 当前已用 OpenTelemetry + Jaeger 替换 Zipkin：端点 `http://jaeger:4318/v1/traces`（本地开发 `http://127.0.0.1:4318/v1/traces`），日志 MDC key 为 `trace_id` / `span_id`（区别于旧的 Brave `traceId` / `spanId`）。
- Dubbo 3.2 没有官方 OTLP starter，DUBBO RPC 链路依靠全局 W3C propagator 传播 trace context；后续评估接入 `opentelemetry-spring-boot-starter` 或 Dubbo OTel 社区模块。