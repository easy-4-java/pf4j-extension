# PF4J 3.15.0 实践（一）：把稳定业务接口做成可插拔能力

## 1. 本文解决什么问题

当一个系统需要不断接入新的计价规则、支付渠道、消息通道、数据解析器或客户定制逻辑时，最容易出现的结果是：宿主工程持续膨胀，每次增加一种能力都要重新编译、测试和发布整个应用。

PF4J 适合解决的是“同一 JVM 内、接口边界清晰、实现可独立交付”的扩展问题。本文以订单优惠策略为例，给出一套从扩展 API、插件实现、描述符、构建、加载到关闭的完整做法。

## 2. 从源码看，PF4J 到底提供了什么

PF4J 的核心不是一个依赖注入容器，也不是安全沙箱，而是一套轻量的 JVM 插件运行时：

1. `PluginRepository` 从插件根目录发现 JAR、ZIP 或展开目录。
2. `PluginDescriptorFinder` 从 `plugin.properties` 或 `MANIFEST.MF` 读取插件元数据。
3. `DependencyResolver` 建立插件依赖图，检查循环依赖、缺失依赖和版本约束。
4. 每个插件由独立的 `PluginClassLoader` 加载。
5. `ExtensionAnnotationProcessor` 在编译期生成 `META-INF/extensions.idx`。
6. `IndexedExtensionFinder` 在运行期读取索引并实例化扩展。
7. `Plugin` 提供可选的 `start()`、`stop()`、`delete()` 生命周期回调。

完整调用链可以概括为：

```mermaid
flowchart LR
    A["plugins 目录"] --> B["PluginRepository 发现制品"]
    B --> C["DescriptorFinder 读取元数据"]
    C --> D["DependencyResolver 解析依赖图"]
    D --> E["PluginClassLoader 加载插件"]
    E --> F["Plugin.start 启动生命周期"]
    F --> G["IndexedExtensionFinder 读取 extensions.idx"]
    G --> H["宿主按 ExtensionPoint 获取扩展"]
```

### 2.1 适合的使用场景

| 场景 | 典型扩展点 | PF4J 的价值 |
| --- | --- | --- |
| 业务策略 | 计价、路由、风控、审核规则 | 新策略独立构建和交付 |
| 外部系统连接器 | 支付、短信、对象存储、设备协议 | 隔离厂商 SDK 和实现 |
| 数据处理流水线 | Parser、Transformer、Exporter | 按部署环境组装能力 |
| 产品可选功能 | 报表、导入导出、客户定制 | 宿主保持稳定，功能按需安装 |
| 桌面或 CLI 扩展 | 命令、菜单、文件格式 | 第三方可以基于公开 API 开发插件 |

### 2.2 不应交给 PF4J 单独解决的问题

- 不可信代码隔离。插件就是宿主 JVM 内的普通代码，可以访问文件、网络和进程权限。
- 进程级故障隔离。插件死循环、内存泄漏或调用 `System.exit` 都可能影响宿主。
- 分布式服务编排。需要独立扩缩容、资源配额或强隔离时，应使用独立进程、容器或远程服务。
- 无状态热替换。PF4J 能卸载类加载器，但不会替你排空请求、停止线程或释放插件持有的外部资源。

## 3. 场景设计：订单优惠策略插件

建议把工程拆成三个边界：

```text
pricing-host
├── pricing-api                 # 宿主和插件共同依赖的稳定 SPI
├── pricing-application         # 宿主应用，持有 PluginManager
└── pricing-plugin-vip          # 可独立交付的 VIP 优惠插件
```

最重要的规则是：`pricing-api` 必须由宿主类加载器加载，插件包中不要再打入一份 API 类。PF4J 3.15.0 的默认加载顺序是 `PDA`：

```text
Plugin -> Dependencies -> Application
```

如果插件 JAR 内又包含一份 `PricingRule`，插件类加载器会优先加载自己的副本。即使两个类的全限定名完全相同，它们也属于不同的类型，最终会导致 `isAssignableFrom` 返回 `false`，宿主发现不到扩展。

## 4. 定义稳定的扩展 API

```java
package com.acme.pricing.api;

import org.pf4j.ExtensionPoint;

import java.math.BigDecimal;

public interface PricingRule extends ExtensionPoint {

    boolean supports(PricingContext context);

    BigDecimal calculate(PricingContext context);
}
```

```java
package com.acme.pricing.api;

import java.math.BigDecimal;
import java.util.Objects;

public final class PricingContext {

    private final String customerLevel;
    private final BigDecimal originalAmount;

    public PricingContext(String customerLevel, BigDecimal originalAmount) {
        this.customerLevel = Objects.requireNonNull(customerLevel, "customerLevel must not be null");
        this.originalAmount = Objects.requireNonNull(originalAmount, "originalAmount must not be null");
    }

    public String getCustomerLevel() {
        return customerLevel;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }
}
```

设计扩展 API 时应遵循以下约束：

- 只暴露稳定 DTO、接口和受控的宿主服务，不把数据库连接、Spring 容器或 `PluginManager` 直接交给插件。
- API 变更遵循语义化版本；删除方法、改变参数类型属于不兼容变更。
- DTO 尽量是不可变对象，避免插件修改宿主共享状态。
- 为超时、幂等、异常分类和返回值语义建立明确契约。
- API 模块只放契约，不放具体业务实现和厂商 SDK。

## 5. 实现插件扩展

```java
package com.acme.pricing.vip;

import com.acme.pricing.api.PricingContext;
import com.acme.pricing.api.PricingRule;
import org.pf4j.Extension;

import java.math.BigDecimal;
import java.util.Objects;

@Extension(ordinal = 100)
public final class VipPricingRule implements PricingRule {

    private static final BigDecimal VIP_RATE = new BigDecimal("0.90");

    @Override
    public boolean supports(PricingContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return "VIP".equals(context.getCustomerLevel());
    }

    @Override
    public BigDecimal calculate(PricingContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return context.getOriginalAmount().multiply(VIP_RATE);
    }
}
```

`ordinal` 会用于扩展排序，源码中的 `ExtensionWrapper.compareTo` 按数值升序排列。因此数值越小越先返回。它适合表达过滤器或责任链顺序，但不应该被当成业务优先级唯一来源；复杂路由仍应由宿主显式决策。

### 5.1 可选的插件生命周期类

只有插件需要启动线程、注册外部资源或执行初始化时，才需要自定义 `Plugin`：

```java
package com.acme.pricing.vip;

import org.pf4j.Plugin;

public final class VipPricingPlugin extends Plugin {

    @Override
    public void start() {
        log.info("VIP pricing plugin started");
    }

    @Override
    public void stop() {
        log.info("VIP pricing plugin stopped");
    }
}
```

PF4J 3.15.0 仍兼容接收 `PluginWrapper` 的构造器，但该构造方式及 `Plugin.wrapper` 已被源码标记为废弃。新插件优先使用无参构造器，并由应用定义受限的 `PluginContext`，通过自己的工厂完成上下文注入。

如果不关心生命周期，可以不声明 `plugin.class`。`DefaultPluginDescriptor` 默认使用空实现 `org.pf4j.Plugin`。

## 6. 编写插件描述符

在插件 JAR 根目录放置 `plugin.properties`：

```properties
plugin.id=pricing-vip
plugin.description=VIP customer pricing rule
plugin.class=com.acme.pricing.vip.VipPricingPlugin
plugin.version=1.2.0
plugin.provider=acme
plugin.dependencies=
plugin.requires=>=2.0.0
plugin.license=Apache-2.0
```

源码支持的全部属性是：

| properties 属性 | Manifest 属性 | 含义 |
| --- | --- | --- |
| `plugin.id` | `Plugin-Id` | 唯一插件 ID，必填 |
| `plugin.description` | `Plugin-Description` | 描述 |
| `plugin.class` | `Plugin-Class` | 生命周期类 |
| `plugin.version` | `Plugin-Version` | 插件版本，必填 |
| `plugin.provider` | `Plugin-Provider` | 提供者 |
| `plugin.dependencies` | `Plugin-Dependencies` | 插件依赖 |
| `plugin.requires` | `Plugin-Requires` | 宿主系统版本约束 |
| `plugin.license` | `Plugin-License` | 许可证 |

依赖表达式示例：

```properties
# 必选依赖
plugin.dependencies=customer-data@>=1.2.0

# ? 放在插件 ID 后、@ 之前，表示可选依赖
plugin.dependencies=customer-data@>=1.2.0,coupon?@>=2.0.0
```

需要特别区分两个版本字段：

- `plugin.dependencies` 约束的是其他插件的版本。
- `plugin.requires` 约束的是宿主调用 `setSystemVersion` 设置的系统版本。

宿主系统版本默认是 `0.0.0`，源码会在这个默认值下跳过 `requires` 校验。生产环境必须主动设置真实版本。另一个容易忽略的行为是：默认情况下，纯数字 `plugin.requires=2.0.0` 会被改写为 `>=2.0.0`，并不是精确匹配；只有调用 `setExactVersionAllowed(true)` 才按精确版本解释。

## 7. 确保编译期生成扩展索引

默认扩展发现器只读取 `META-INF/extensions.idx`。PF4J JAR 自带 `ExtensionAnnotationProcessor` 的服务注册，但如果 Maven 配置了显式 `annotationProcessorPaths`，就必须把 PF4J 也列进去：

```xml
<properties>
    <pf4j.version>3.15.0</pf4j.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.acme</groupId>
        <artifactId>pricing-api</artifactId>
        <version>${project.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.pf4j</groupId>
        <artifactId>pf4j</artifactId>
        <version>${pf4j.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.pf4j</groupId>
                        <artifactId>pf4j</artifactId>
                        <version>${pf4j.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

构建后应检查：

```bash
jar tf pricing-plugin-vip.jar | grep -E 'plugin.properties|META-INF/extensions.idx'
```

索引缺失时，插件本身仍可能进入 `STARTED`，但 `getExtensions(PricingRule.class)` 会返回空列表。这是最常见的“插件已经启动但找不到实现”原因之一。

## 8. 宿主加载与调用

```java
package com.acme.pricing.host;

import com.acme.pricing.api.PricingContext;
import com.acme.pricing.api.PricingRule;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PricingApplication {

    private PricingApplication() {
    }

    public static void main(String[] args) {
        Path pluginsRoot = Paths.get("plugins");
        PluginManager pluginManager = new DefaultPluginManager(pluginsRoot);
        pluginManager.setSystemVersion("2.3.0");
        pluginManager.addPluginStateListener(event ->
            System.out.printf("plugin=%s, %s -> %s%n",
                event.getPlugin().getPluginId(),
                event.getOldState(),
                event.getPluginState()));

        try {
            pluginManager.loadPlugins();
            startResolvedPluginsStrictly(pluginManager);

            PricingContext context = new PricingContext("VIP", new BigDecimal("100.00"));
            List<PricingRule> rules = pluginManager.getExtensions(PricingRule.class);

            BigDecimal result = rules.stream()
                .filter(rule -> rule.supports(context))
                .findFirst()
                .map(rule -> rule.calculate(context))
                .orElse(context.getOriginalAmount());

            System.out.println("final amount: " + result);
        } finally {
            try {
                stopStartedPluginsSafely(pluginManager);
            } finally {
                pluginManager.unloadPlugins();
            }
        }
    }

    private static void startResolvedPluginsStrictly(PluginManager pluginManager) {
        for (PluginWrapper plugin : pluginManager.getResolvedPlugins()) {
            if (plugin.getPluginState().isDisabled()) {
                continue;
            }

            PluginState state = pluginManager.startPlugin(plugin.getPluginId());
            if (state.isFailed()) {
                Throwable failure = plugin.getFailedException();
                throw new IllegalStateException("Cannot start plugin " + plugin.getPluginId(), failure);
            }
        }
    }

    private static void stopStartedPluginsSafely(PluginManager pluginManager) {
        List<PluginWrapper> startedPlugins = new ArrayList<>(pluginManager.getStartedPlugins());
        Collections.reverse(startedPlugins);
        for (PluginWrapper plugin : startedPlugins) {
            PluginState state = pluginManager.stopPlugin(plugin.getPluginId());
            if (state.isFailed()) {
                throw new IllegalStateException(
                    "Cannot stop plugin " + plugin.getPluginId(),
                    plugin.getFailedException());
            }
        }
    }
}
```

这里没有直接调用 `startPlugins()`，是一个有意的选择。PF4J 3.15.0 源码中：

- `startPlugin(pluginId)` 会递归启动依赖；必选依赖启动失败时，当前插件进入 `FAILED`，失败原因写入 `PluginWrapper.failedException`；可选依赖失败时允许降级启动。
- `startPlugins()` 按已解析顺序直接调用内部启动方法，没有复用上述依赖启动结果校验。
- `stopPlugins()` 在 3.15.0 源码中迭代 `startedPlugins` 的同时，内部停止方法又从同一列表删除元素；真实运行会触发 `ConcurrentModificationException`。示例因此复制列表、逆序逐个调用 `stopPlugin(id)`。

如果业务要求“依赖未成功启动，依赖方绝不能启动”，应像示例一样逐个调用 `startPlugin` 并检查返回状态。

## 9. 插件制品如何选择

### 9.1 单 JAR 插件

适合没有私有三方依赖，或已经通过受控方式构建 fat JAR 的插件。`DefaultPluginManager` 同时支持 JAR，且会先尝试 `plugin.properties`，再尝试 Manifest。

```text
plugins/
└── pricing-plugin-vip-1.2.0.jar
    ├── plugin.properties
    ├── META-INF/extensions.idx
    └── com/acme/pricing/vip/*.class
```

不要把宿主 API 打进 fat JAR。

### 9.2 ZIP 或展开目录插件

插件有多个私有依赖时，更推荐使用 PF4J 默认目录布局：

```text
plugins/pricing-vip/
├── plugin.properties
├── classes/
│   ├── META-INF/extensions.idx
│   └── com/acme/pricing/vip/*.class
└── lib/
    └── plugin-private-library.jar
```

`DefaultPluginLoader` 会加载 `classes/` 和递归加载 `lib/` 下的 JAR。ZIP 被发现后，会先解压到同名目录再加载。源码已经对 ZIP 路径穿越做了规范化校验，但生产入口仍应额外限制压缩包大小、文件数量、解压后总量和签名来源。

### 9.3 开发模式

设置：

```bash
-Dpf4j.mode=development
```

开发模式默认插件根目录是 `../plugins`，部署模式默认是 `plugins`。也可以使用逗号分隔的多目录：

```bash
-Dpf4j.pluginsDir=/opt/app/plugins,/opt/app/customer-plugins
```

开发加载器能识别 Maven、Gradle、Kotlin 和 IntelliJ 的常见输出目录，例如 `target/classes`、`build/classes/java/main` 和 `out/production/classes`。

## 10. 扩展实例和依赖的几个细节

### 10.1 扩展只对已启动插件可见

`AbstractExtensionFinder.find(type, pluginId)` 会检查插件状态。插件未进入 `STARTED` 时，即使索引存在，也不会返回其扩展。

### 10.2 默认扩展工厂要求公开无参构造器

`DefaultExtensionFactory` 使用反射创建扩展。需要构造器注入时，应在宿主中替换 `ExtensionFactory`，或者使用 PF4J Spring 集成，不要让插件直接查找全局静态容器。

### 10.3 “单例”不是默认跨查询保证

一次扩展查找中的 `ExtensionWrapper` 会缓存实例，但新的查询会创建新的 wrapper。若需要按插件类加载器缓存扩展实例，可覆盖 `createExtensionFactory()` 并使用 `SingletonExtensionFactory`。使用单例后，要特别确保插件停止时不再有宿主对象持有该实例。

### 10.4 可选插件依赖与扩展级依赖

插件依赖可通过 `?` 标为可选。扩展还可以声明：

```java
@Extension(plugins = {"coupon"})
public final class CouponAwarePricingRule implements PricingRule {
    // ...
}
```

扩展级检查依赖 ASM，且必须在 `AbstractExtensionFinder` 上启用。源码在发现某个已启动插件具有可选依赖时会自动打开检查，但生产代码最好显式配置并确保 ASM 在宿主类路径中，避免行为依赖插件集合的偶然状态。

## 11. 常见故障定位表

| 现象 | 优先检查 |
| --- | --- |
| 插件目录存在但未加载 | 根目录配置、运行模式、隐藏文件、JAR/ZIP 后缀 |
| `No PluginDescriptorFinder` | `plugin.properties` 位置或 Manifest 属性 |
| 插件已加载但未解析 | 循环依赖、缺失必选依赖、依赖版本不满足 |
| 插件 `FAILED` | `PluginWrapper.getFailedException()` |
| 插件已启动但扩展为空 | `META-INF/extensions.idx`、插件状态、API 是否重复打包 |
| 扩展类找不到依赖 | `lib/` 布局、插件依赖声明、类加载顺序 |
| 调用 `stopPlugins()` 抛并发修改异常 | PF4J 3.15.0 的批量停止实现；改为复制列表后逐个 `stopPlugin(id)` |
| 卸载后内存不下降 | 插件线程、线程上下文类加载器、静态缓存、监听器、JDBC Driver |

## 12. 落地检查清单

- [ ] 扩展 API 独立模块，并由宿主类加载器提供。
- [ ] 插件构建不重复打入扩展 API 和 PF4J 核心类。
- [ ] 插件 JAR 中存在 `META-INF/extensions.idx`。
- [ ] `plugin.id` 全局唯一，`plugin.version` 使用可比较版本。
- [ ] 宿主显式调用 `setSystemVersion`。
- [ ] 严格依赖场景逐个调用 `startPlugin` 并检查 `FAILED`。
- [ ] 插件 `stop()` 能关闭线程、连接、监听器和定时任务。
- [ ] 宿主关闭时复制已启动列表，逆序逐个停止，再卸载插件以关闭类加载器。
- [ ] 插件入口有来源校验、摘要校验和制品大小限制。
- [ ] 对每个扩展点建立契约测试，插件在发布前运行同一套测试。

## 13. 小结

业务插件化成功与否，主要不取决于能不能调用 `loadPlugins()`，而取决于扩展 API 是否稳定、类加载边界是否干净、插件启动失败是否被正确处理，以及卸载时资源是否真正释放。PF4J 负责发现、依赖、类加载和生命周期骨架；路由、限流、超时、数据权限和发布治理仍应由宿主系统明确实现。
