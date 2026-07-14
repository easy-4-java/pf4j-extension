# pf4j-extension

独立的 PF4J 扩展组件，统一承载原 `pf4j-extension` 与 `pf4j3-extension` 的能力，提供：

- PF4J/Spring 插件管理、扩展注入与生命周期管理；
- Spring MVC Controller 动态注册和注销；
- REST、Maven 插件更新仓库；
- 扩展元数据、扩展点查找、动态代理和通用加解密扩展点。

所有版本线统一使用 `org.pf4j:pf4j:3.15.0`。旧 `pf4j3-extension` 的公开包名暂时保留，已有应用可以只替换 Maven 坐标，无需立即修改 Java import。

## 版本线

| 分支 | Java | PF4J | 整合基线 | 组件版本 |
| --- | --- | --- | --- | --- |
| `feature/1.0.x` | 8 | 3.15.0 | Spring 5.3.x / pf4j-spring 0.9.x | `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | 17 | 3.15.0 | Spring 6.2.x / pf4j-spring 0.10.x | `2.0.x.20260630-SNAPSHOT` |
| `feature/3.0.x` | 21 | 3.15.0 | Spring 7.0.x / pf4j-spring 0.10.x | `3.0.x.20260630-SNAPSHOT` |

```xml
<dependency>
    <groupId>io.github.hiwepy</groupId>
    <artifactId>pf4j-extension</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

从旧坐标迁移：

```xml
<!-- 删除 io.github.hiwepy:pf4j3-extension -->
<!-- 添加 io.github.hiwepy:pf4j-extension，并选择与运行时 JDK 对应的版本线 -->
```
