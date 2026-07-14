# pf4j-extension

面向 PF4J 的独立扩展组件，统一承载原 `pf4j-extension` 与 `pf4j3-extension` 的能力。项目只保留三个职责清晰的模块：

| 模块 | 根包 | 能力 |
| --- | --- | --- |
| `pf4j-extension-core` | `org.pf4j.core.extension` | 扩展元数据、类型安全解析、默认实现选择、插件生命周期与代理工具 |
| `pf4j-extension-spring` | `org.pf4j.spring.extension` | Spring 扩展注入、MVC Controller 动态注册、插件容器生命周期 |
| `pf4j-extension-update` | `org.pf4j.update.extension` | REST/Maven 更新仓库、下载器与线程安全元数据缓存 |

根项目 `io.github.hiwepy:pf4j-extension` 是聚合与依赖管理 POM，不再提供 Jar。应用应按需依赖上表中的模块。

## 版本线

| 分支 | Java | PF4J | Spring / pf4j-spring | 组件版本 |
| --- | --- | --- | --- | --- |
| `feature/1.0.x` | 8 | 3.15.0 | 5.3.39 / 0.9.0 | `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | 17 | 3.15.0 | 6.2.19 / 0.10.0 | `2.0.x.20260630-SNAPSHOT` |
| `feature/3.0.x` | 21 | 3.15.0 | 7.0.8 / 0.10.0 | `3.0.x.20260630-SNAPSHOT` |

三个版本线使用相同的公开 API 和模块结构，仅调整 JDK、Maven 编译配置及第三方依赖基线。

## Maven 依赖

只使用 PF4J 通用能力：

```xml
<dependency>
    <groupId>io.github.hiwepy</groupId>
    <artifactId>pf4j-extension-core</artifactId>
    <version>2.0.x.20260630-SNAPSHOT</version>
</dependency>
```

集成 Spring 与动态 Controller：

```xml
<dependency>
    <groupId>io.github.hiwepy</groupId>
    <artifactId>pf4j-extension-spring</artifactId>
    <version>2.0.x.20260630-SNAPSHOT</version>
</dependency>
```

接入插件更新仓库：

```xml
<dependency>
    <groupId>io.github.hiwepy</groupId>
    <artifactId>pf4j-extension-update</artifactId>
    <version>2.0.x.20260630-SNAPSHOT</version>
</dependency>
```

`pf4j-extension-update` 中 Maven 仓库能力依赖的 `spring-cloud-deployer-resource-maven` 是可选依赖；使用 `MavenUpdateRepository` 或 `MavenFileDownloader` 时，应用需要显式引入与自身 Spring 版本兼容的该组件。

## 模块关系

```text
pf4j-extension-spring
└── pf4j-extension-core
    └── org.pf4j:pf4j

pf4j-extension-update
└── org.pf4j:pf4j-update
```

`core` 通过 Maven Enforcer 禁止引入 Spring、`pf4j-spring` 和 `pf4j-update`，保证其可以在纯 Java/PF4J 应用中独立使用。

## 包名迁移

本次整合不保留旧包的重复实现，迁移关系如下：

| 旧包 | 新包 |
| --- | --- |
| `io.github.hiwepy.pf4j.*` | `org.pf4j.core.extension.*` |
| `org.pf4j.spring.boot.ext.*` | `org.pf4j.spring.extension.*` |
| `org.pf4j.spring.boot.ext.update.*` | `org.pf4j.update.extension.*` |
| `org.pf4j.spring.boot.ext.property.*` | `org.pf4j.update.extension.property.*` |

从 `pf4j3-extension` 迁移时，应删除旧坐标，并根据实际能力选择 `core`、`spring` 或 `update` 模块，同时更新 Java import。

## 构建

使用与分支对应的 JDK 执行：

```bash
mvn clean verify
```

详细版本约束见 [COMPATIBILITY.md](COMPATIBILITY.md)。
