# gaia-domain（领域层）

DDD 中的领域层。**只承载领域模型、值对象、Repository 接口与领域服务**，不依赖任何基础设施（Spring / MyBatis / Dubbo）。

## 依赖方向

```
gaia-app       ──→  gaia-domain
gaia-bootstrap ──→  gaia-app + gaia-infra
gaia-infra     ──→  gaia-domain   （实现 domain 定义的 Repository 接口）
gaia-backend   ──→  gaia-domain
```

## 包结构

```
com.gaia.domain
├── entity/      # 领域实体（POJO / 充血模型）
├── valueobject/ # 值对象
├── repository/  # Repository 接口（由 gaia-infra 实现）
├── service/     # 领域服务
└── gateway/     # 网关接口（外部依赖抽象）
```

## 约束

- **不**引入 `org.springframework.*` / `com.baomidou.mybatisplus.*`。
- **不**持有 SQL / Mapper / EntityManager。
- Entity 是业务模型，不是数据库表（持久化形态由 `gaia-infra` 决定）。
