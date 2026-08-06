# pf4j-extension

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-8-orange)](https://github.com/easy-4-java/pf4j-extension) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 功能与状态](#2-功能与状态)
- [3. 环境要求与兼容性](#3-环境要求与兼容性)
- [4. 架构与模块](#4-架构与模块)
- [5. 安装](#5-安装)
- [6. 快速开始](#6-快速开始)
- [7. 配置](#7-配置)
- [8. 核心用法 / API](#8-核心用法--api)
- [9. 测试与构建](#9-测试与构建)
- [10. 版本线与分支](#10-版本线与分支)
- [11. 贡献与许可](#11-贡献与许可)

## 1. 项目概述

**pf4j-extension** 是面向 [PF4J](https://pf4j.org)（Java 插件框架）的生产级扩展组件，统一承载原 `pf4j-extension` 与 `pf4j3-extension` 的能力，整合为一个多模块项目，只保留三个职责清晰的模块：

| 模块                      | 根包                        | 能力                                                              |
| :------------------------ | :-------------------------- | :---------------------------------------------------------------- |
| `pf4j-extension-core`     | `org.pf4j.core.extension`   | 扩展目录、严格生命周期、调用链、健康检查、流量摘除与运行诊断       |
| `pf4j-extension-spring`   | `org.pf4j.spring.extension` | Spring Bean/Controller 动态注册注销、状态事件与容器生命周期        |
| `pf4j-extension-update`   | `org.pf4j.update.extension` | REST/Maven 仓库、制品准入、签名校验、事务更新与自动回滚            |

根项目 `io.github.easy4j:pf4j-extension` 是聚合与依赖管理 POM，不再提供 Jar。应用应按需依赖上表中的模块。

| 是                                                       | 不是                                             |
| :------------------------------------------------------- | :----------------------------------------------- |
| PF4J 的生命周期、调用、健康与更新治理                      | 安全沙箱——PF4J 类加载器不是沙箱                   |
| 基于 `org.pf4j:pf4j` 3.15.x 构建                         | PF4J 本身的 fork 或替代品                        |
| 通过 `pf4j-spring` 集成 Spring                            | Spring Boot Starter                              |

典型场景：

| 场景                       | 说明                                                    |
| :------------------------- | :------------------------------------------------------ |
| 严格插件启动/停止          | 批量启动失败回滚，安全关闭时逆序停止                     |
| 多扩展分发                 | 面向扩展调用的日志/指标/追踪/容错拦截器责任链            |
| 插件零停机更新             | 备份 → 流量摘除 → 更新 → 依赖方恢复 → 健康检查 → 回滚    |
| 插件运行观测               | 插件状态事件、诊断报告、健康/就绪检查                    |

## 2. 功能与状态

| 能力                                       | 状态       | 主要 API                                                              |
| :----------------------------------------- | :--------- | :-------------------------------------------------------------------- |
| 生命周期安全                               | 已实现     | `PluginLifecycleManager`：串行加载/启动/停止/卸载，严格检查启动状态，批量失败逆序回滚（规避 PF4J 3.15.0 批量停止并发修改问题） |
| 扩展目录                                   | 已实现     | `ExtensionCatalog`：生成不持有插件实例的不可变元数据快照，校验重复扩展 ID 和多个 `@Primary` |
| 调用治理                                   | 已实现     | `ExtensionInvoker`、`ExtensionInterceptor`：责任链式日志、指标、追踪和容错扩展 |
| 健康与流量摘除                             | 已实现     | `PluginHealthService`：聚合健康、就绪扩展，更新或停止前执行流量摘除与超时等待 |
| 运行诊断                                   | 已实现     | `PluginDiagnostics`：输出依赖方、扩展类、失败原因、类来源，并检测插件重复打包宿主 API |
| Spring 同步                                | 已实现     | `PluginBeanRegistry`、`SpringPluginLifecycleSynchronizer`：插件启动时注册 Bean/Controller，停止、失败和卸载时按所有权精确注销 |
| 制品准入                                   | 已实现     | `PluginArtifactVerifier`、`SecureFileDownloader`：协议、大小、SHA-512、压缩规模、路径穿越、禁止类和可选离线签名校验 |
| 事务更新                                   | 已实现     | `TransactionalPluginUpdateManager`：备份、流量摘除、更新、依赖方恢复、健康检查、自动回滚和结果监听 |
| Spring 状态事件                            | 已实现     | `SpringPluginStateChangedEvent`：不持有插件运行对象的完整状态快照，适合异步监听/审计 |
| 单元测试                                   | 已实现     | 三个模块均含 JUnit 5 测试（`src/test`）                                |

## 3. 环境要求与兼容性

| 要求       | 版本                                       |
| :--------- | :----------------------------------------- |
| JDK        | 8+（本分支）                               |
| Maven      | 3.0+（已内置 wrapper）                      |
| PF4J       | 3.15.0                                     |
| pf4j-spring| 0.9.0                                      |
| pf4j-update| 2.3.0                                      |
| Spring     | 5.3.39（`spring-core`/`beans`/`context`/`web`/`webmvc`）|

easy4j 项目的版本线：

| 分支           | JDK  | Spring / pf4j-spring | 版本模式   | 说明                            |
| :------------- | :--- | :------------------- | :--------- | :------------------------------ |
| `feature/1.0.x` | 8    | 5.3.39 / 0.9.0       | `1.0.x.*`  | 本文档对应分支                   |
| `feature/2.0.x` | 17   | 6.2.19 / 0.10.0      | `2.0.x.*`  | JDK 17 版本线                   |
| `feature/3.0.x` | 21   | 7.0.8 / 0.10.0       | `3.0.x.*`  | JDK 21 版本线                   |

三个版本线使用相同的公开 API 和模块结构，仅调整 JDK、Maven 编译配置及第三方依赖基线。不要在高版本线产物上使用低版本 JDK。详细约束见 [COMPATIBILITY.md](COMPATIBILITY.md)。

## 4. 架构与模块

```text
         +--------- pf4j-extension ---------+
         |                                   |
         v                                   v
   pf4j-extension-core             pf4j-extension-update
   org.pf4j.core.extension        org.pf4j.update.extension
   (lifecycle/catalog/            (repositories/security/
    invocation/health/diag)        transactional update)
         |                                   |
         |       org.pf4j:pf4j 3.15.x        |
         |                                   |
         v
   pf4j-extension-spring
   org.pf4j.spring.extension
   (beans/controllers/events)
```

模块依赖关系：

```text
pf4j-extension-spring
└── pf4j-extension-core
    └── org.pf4j:pf4j

pf4j-extension-update
├── pf4j-extension-core
└── org.pf4j:pf4j-update
```

`core` 通过 Maven Enforcer 禁止引入 Spring、`pf4j-spring` 和 `pf4j-update`，保证其可以在纯 Java/PF4J 应用中独立使用。`pf4j-extension-update` 中 Maven 仓库能力依赖的 `spring-cloud-deployer-resource-maven` 是**可选依赖**；使用 `MavenUpdateRepository` / `MavenFileDownloader` 时，应用需要显式引入与自身 Spring 版本兼容的该组件。

## 5. 安装

制品发布在阿里云私服与 GitHub Releases，**尚未发布到 Maven Central**。只使用 PF4J 通用能力：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>pf4j-extension-core</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

集成 Spring 与动态 Controller：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>pf4j-extension-spring</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

接入插件更新仓库：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>pf4j-extension-update</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

```groovy
implementation 'io.github.easy4j:pf4j-extension-core:1.0.x.20260630-SNAPSHOT'
```

## 6. 快速开始

严格启动与安全关闭：

```java
PluginManager pluginManager = new DefaultPluginManager(pluginsRoot);
PluginLifecycleManager lifecycle = new PluginLifecycleManager(pluginManager);

lifecycle.addListener(result -> audit(result));
lifecycle.loadAllAndStartStrictly();

// 应用关闭时调用，内部复制并逆序停止列表。
lifecycle.unloadAllSafely();
```

调用链、健康检查与诊断：

```java
ExtensionInvoker invoker = new ExtensionInvoker(Arrays.asList(
        new LoggingExtensionInterceptor(),
        new TimingExtensionInterceptor(listener)));
PaymentExtension payment = invoker.createProxy(
        PaymentExtension.class, target, "payment-plugin", "wechat-pay");

PluginHealthService healthService = new PluginHealthService(pluginManager);
PluginHealth health = healthService.checkHealth("payment-plugin");

PluginDiagnostics diagnostics = new PluginDiagnostics(pluginManager);
PluginDiagnosticReport report = diagnostics.diagnose("payment-plugin");
```

插件可按需实现 `PluginHealthCheck`、`PluginReadinessCheck` 和 `PluginDrainHook` 扩展点。健康与就绪结果为 `UP` 后，事务更新才会提交新版本。

## 7. 配置

无属性文件，行为全部通过编程方式配置：

| 关注点             | 配置点                                                        |
| :----------------- | :------------------------------------------------------------ |
| 下载限制           | `DownloadPolicy` 构造方法 / `DownloadPolicy.secureDefaults()` |
| 校验顺序           | `PluginArtifactVerifier(FileVerifier delegate, List<ArtifactVerificationPolicy> policies)` |
| 事务超时           | `TransactionalPluginUpdateManager(..., long drainTimeoutSeconds)` |
| 拦截器链           | `ExtensionInvoker(List<ExtensionInterceptor>)`                |
| Spring 接线        | `ExtendedSpringPluginManager` 构造方法（插件根目录、autowire、singleton、injectable） |

## 8. 核心用法 / API

安全下载与事务更新：

```java
DownloadPolicy policy = DownloadPolicy.secureDefaults();
FileDownloader downloader = new SecureFileDownloader(delegateDownloader, policy);
FileVerifier verifier = new PluginArtifactVerifier(policy);

UpdateRepository repository = new RestTemplateUpdateRepository(
        "internal", metadataUrl, restTemplate, downloader, verifier);
PluginArtifactStore store = new FileSystemPluginArtifactStore(
        pluginManager.getPluginsRoot(), backupRoot);
TransactionalPluginUpdateManager updates = new TransactionalPluginUpdateManager(
        pluginManager, Collections.singletonList(repository), lifecycle, store, 30);

PluginUpdateResult result = updates.updateTransactional("payment-plugin", "2.1.0");
```

`installPlugin` 和 `updatePlugin` 兼容入口也会自动转入事务流程。强隔离仍需使用独立进程或容器；PF4J 类加载器不是安全沙箱。

Spring 插件状态事件：

`SpringPluginStateChangedEvent` 是不持有插件运行对象的完整状态快照，包含：

- 事件 ID、发生时间和状态转换；
- 插件标准描述符、必选/可选依赖；
- 插件路径、运行模式、宿主版本；
- 插件管理器类名、类加载器类型和扩展实现类名；
- 最外层异常、根异常以及字符串形式的异常链。

事件不保存 `PluginWrapper`、插件实例、插件类型或实际类加载器，适合异步监听、审计落库和管理界面展示。事件监听器异常会被隔离，不会反向中断 PF4J 生命周期操作。

## 9. 测试与构建

```bash
./mvnw clean verify
```

- 三个模块均使用 JUnit 5（Jupiter）测试，例如 core 模块的 `PluginLifecycleManagerTest`、`ExtensionInvokerTest`、`PluginHealthServiceTest`、`PluginDiagnosticsTest`、`ExtensionCatalogTest`；spring 模块的 `ExtendedSpringPluginManagerTest`、`SpringPluginEventPublisherTest`、`PluginBeanRegistryTest`、`InjectorUtilsTest`；update 模块的 `RestTemplateUpdateRepositoryTest`、`PluginArtifactSecurityTest`、`FileSystemPluginArtifactStoreTest`、`TransactionalPluginUpdateManagerTest`。
- 已配置 JaCoCo 行覆盖率 90% 门禁（`haltOnFailure=false`）。
- 使用与分支匹配的 JDK：`feature/1.0.x` 用 JDK 8，`feature/2.0.x` 用 JDK 17，`feature/3.0.x` 用 JDK 21。

## 10. 版本线与分支

| 分支           | JDK  | 版本模式   | 维护说明                                      |
| :------------- | :--- | :--------- | :-------------------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`  | 当前分支；Spring 5.3.x / pf4j-spring 0.9.x    |
| `feature/2.0.x` | 17   | `2.0.x.*`  | Spring 6.2.x / pf4j-spring 0.10.x             |
| `feature/3.0.x` | 21   | `3.0.x.*`  | Spring 7.0.x / pf4j-spring 0.10.x             |

制品通过阿里云 Maven 私服与 GitHub Releases 分发。

**整合后的包名迁移**（旧包的重复实现已删除）：

| 旧包                                 | 新包                                 |
| :----------------------------------- | :----------------------------------- |
| `io.github.hiwepy.pf4j.*`            | `org.pf4j.core.extension.*`          |
| `org.pf4j.spring.boot.ext.*`         | `org.pf4j.spring.extension.*`        |
| `org.pf4j.spring.boot.ext.update.*`  | `org.pf4j.update.extension.*`        |

从 `pf4j3-extension` 迁移时，应删除旧坐标，并根据实际能力选择 `core`、`spring` 或 `update` 模块，同时更新 Java import。

## 11. 贡献与许可

欢迎贡献——请先提交 issue 讨论。PF4J 深入实践文档见：

- [实践一：业务扩展点与多实现插件](docs/PF4J_3.15_PRACTICE_01_BUSINESS_EXTENSION.md)
- [实践二：生产发布、观测与回滚](docs/PF4J_3.15_PRACTICE_02_PRODUCTION_GOVERNANCE.md)

本项目基于 [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) 许可。
