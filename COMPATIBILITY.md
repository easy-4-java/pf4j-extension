# 兼容性说明

## 版本选择

| 运行时 | 分支 | Maven 版本 |
| --- | --- | --- |
| JDK 8 | `feature/1.0.x` | `1.0.x.20260630-SNAPSHOT` |
| JDK 17 | `feature/2.0.x` | `2.0.x.20260630-SNAPSHOT` |
| JDK 21 | `feature/3.0.x` | `3.0.x.20260630-SNAPSHOT` |

不要在较低 JDK 中使用较高版本线构建出的 Jar。三个版本线的 Java API 与模块名保持一致，便于应用仅修改版本号完成 JDK 升级。

## 依赖边界

- `pf4j-extension-core`：只依赖 PF4J、cglib、Commons Lang、SLF4J 和 Lombok，不依赖 Spring。
- `pf4j-extension-spring`：依赖 `core`、`pf4j-spring`、Spring Context 与 Spring Web MVC。
- `pf4j-extension-update`：独立依赖 `pf4j-update` 和 Spring Web；Maven 资源解析能力为可选依赖。

父 POM 对同一版本线中的 Spring Core、Beans、Context、Web 和 Web MVC 进行统一版本管理，防止 `pf4j-spring` 的传递依赖导致 Spring 组件版本错配。

## 兼容性承诺

- PF4J 基线统一为 3.15.0，以最新整合实现为基础。
- 1.0.x 使用可运行于 JDK 8 的依赖和 Java 语法。
- 2.0.x 使用 JDK 17，并保持 Spring 6.2.x 基线。
- 3.0.x 使用 JDK 21 与 Spring 7.0.x 基线。
- 根坐标 `io.github.hiwepy:pf4j-extension` 是 `pom`，实际运行时依赖必须选择子模块。
