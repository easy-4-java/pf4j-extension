# pf4j-extension

面向 PF4J 的独立扩展组件，统一承载原 `pf4j-extension` 与 `pf4j3-extension` 的能力。项目只保留三个职责清晰的模块：

| 模块 | 根包 | 能力 |
| --- | --- | --- |
| `pf4j-extension-core` | `org.pf4j.core.extension` | 扩展目录、严格生命周期、调用链、健康检查、摘流与运行诊断 |
| `pf4j-extension-spring` | `org.pf4j.spring.extension` | Spring Bean/Controller 动态注册注销、状态事件与容器生命周期 |
| `pf4j-extension-update` | `org.pf4j.update.extension` | REST/Maven 仓库、制品准入、签名校验、事务更新与自动回滚 |

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
├── pf4j-extension-core
└── org.pf4j:pf4j-update
```

`core` 通过 Maven Enforcer 禁止引入 Spring、`pf4j-spring` 和 `pf4j-update`，保证其可以在纯 Java/PF4J 应用中独立使用。

## 生产扩展能力

| 场景 | 主要 API | 行为 |
| --- | --- | --- |
| 生命周期安全 | `PluginLifecycleManager` | 串行加载/启动/停止/卸载，严格检查启动状态，批量失败逆序回滚，规避 PF4J 3.15.0 批量停止并发修改问题 |
| 扩展目录 | `ExtensionCatalog` | 生成不持有插件实例的不可变元数据快照，校验重复扩展 ID 和多个 `@Primary` |
| 调用治理 | `ExtensionInvoker`、`ExtensionInterceptor` | 责任链式日志、指标、追踪和容错扩展，统一异常边界并保留插件/扩展 ID |
| 健康与摘流 | `PluginHealthService` | 聚合健康、就绪扩展，更新或停止前执行摘流与超时等待 |
| 运行诊断 | `PluginDiagnostics` | 输出依赖方、扩展类、失败原因、类来源，并检测插件重复打包宿主 API |
| Spring 同步 | `PluginBeanRegistry`、`SpringPluginLifecycleSynchronizer` | 插件启动时注册 Bean/Controller，停止、失败和卸载时按所有权精确注销 |
| 制品准入 | `PluginArtifactVerifier`、`SecureFileDownloader` | 协议、大小、SHA-512、压缩规模、路径穿越、禁止类和可选离线签名校验 |
| 事务更新 | `TransactionalPluginUpdateManager` | 备份、摘流、更新、依赖方恢复、健康检查、自动回滚和结果监听 |

### 严格启动与安全关闭

```java
PluginManager pluginManager = new DefaultPluginManager(pluginsRoot);
PluginLifecycleManager lifecycle = new PluginLifecycleManager(pluginManager);

lifecycle.addListener(result -> audit(result));
lifecycle.loadAllAndStartStrictly();

// 应用关闭时调用，内部复制并逆序停止列表。
lifecycle.unloadAllSafely();
```

### 调用链、健康检查与诊断

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

### 安全下载与事务更新

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

`installPlugin` 和 `updatePlugin` 兼容入口在 `TransactionalPluginUpdateManager` 中也会自动转入事务流程。强隔离仍需使用独立进程或容器；PF4J 类加载器不是安全沙箱。

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

PF4J 源码行为与完整实践见：

- [实践一：业务扩展点与多实现插件](docs/PF4J_3.15_PRACTICE_01_BUSINESS_EXTENSION.md)
- [实践二：生产发布、观测与回滚](docs/PF4J_3.15_PRACTICE_02_PRODUCTION_GOVERNANCE.md)
