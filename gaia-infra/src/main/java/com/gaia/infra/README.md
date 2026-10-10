# gaia-infra（基础设施层）

DDD 中的基础设施层。负责实现 `gaia-domain` 定义的 Repository 接口、承载持久化技术细节（MyBatis-Plus Mapper、DataSource、SQL）。

## 依赖方向（"反向依赖"）

```
gaia-infra   ──→  gaia-domain    实现 Repository 接口（依赖倒置）
gaia-infra   ──→  gaia-server    复用其共用基础设施（数据源、监控布线）
```

## 装配方式

gaia-infra 是 **jar library**，不是独立部署进程。通过 Spring Boot 3 的
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
由 `gaia-server` / `gaia-backend` 在启动时自动加载。

## 包结构

```
com.gaia.infra
├── config/                # Spring 自动配置入口
├── persistence/
│   ├── mapper/            # MyBatis-Plus Mapper
│   └── repository/        # 领域 Repository 实现
└── datasource/            # DataSource / HikariCP 等
```
