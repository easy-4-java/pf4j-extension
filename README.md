# pf4j-extension

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-8-orange)](https://github.com/easy-4-java/pf4j-extension) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

pf4j-extension is a production-oriented extension layer for PF4J (Plugin Framework for

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

**pf4j-extension** is a production-oriented extension layer for [PF4J](https://pf4j.org) (Plugin Framework for
Java), consolidating the capabilities of the former `pf4j-extension` and `pf4j3-extension` projects into one
multi-module project with three clearly separated modules:

| Module                    | Root package                | Capability                                                                 |
| :------------------------ | :-------------------------- | :------------------------------------------------------------------------- |
| `pf4j-extension-core`     | `org.pf4j.core.extension`   | Extension catalog, strict lifecycle, invocation chain, health checks, traffic draining and runtime diagnostics |
| `pf4j-extension-spring`   | `org.pf4j.spring.extension` | Dynamic Spring Bean/Controller registration, state events, container lifecycle |
| `pf4j-extension-update`   | `org.pf4j.update.extension` | REST/Maven repositories, artifact admission, signature verification, transactional update with automatic rollback |

The root project `io.github.easy4j:pf4j-extension` is an aggregator and dependency-management POM only — it
does not produce a runtime jar. Applications depend on the modules above as needed.

| Is                                                       | Is not                                             |
| :------------------------------------------------------- | :------------------------------------------------- |
| Lifecycle, invocation, health and update governance for PF4J | A security sandbox — PF4J class loaders are not sandboxes |
| Works with `org.pf4j:pf4j` 3.15.x                        | A fork or replacement of PF4J itself               |
| Spring integration via `pf4j-spring`                      | A Spring Boot starter                              |

Typical scenarios:

| Scenario                          | Description                                                |
| :-------------------------------- | :--------------------------------------------------------- |
| Strict plugin start/stop          | Batch start with rollback, safe shutdown with reverse stop order |
| Multi-extension dispatch          | Interceptor chain for logging/metrics/tracing/fault tolerance over extension calls |
| Zero-downtime plugin updates      | Backup → drain → update → restore dependents → health check → rollback |
| Plugin operations observability   | Plugin state events, diagnostics reports, health/readiness checks |

## 2. Features & Status

| Capability                                       | Status      | Main API                                                                 |
| :----------------------------------------------- | :---------- | :----------------------------------------------------------------------- |
| Lifecycle safety                                 | Implemented | `PluginLifecycleManager` — serial load/start/stop/unload, strict start-state checks, batch failure rollback (avoids the PF4J 3.15.0 concurrent-modification issue on batch stop) |
| Extension catalog                                | Implemented | `ExtensionCatalog` — immutable metadata snapshot without holding plugin instances; detects duplicate extension IDs and multiple `@Primary` |
| Invocation governance                            | Implemented | `ExtensionInvoker`, `ExtensionInterceptor` — responsibility-chain logging, metrics, tracing and fault-tolerant extensions |
| Health & traffic draining                        | Implemented | `PluginHealthService` — aggregate health, readiness, drain-before-update/stop with timeout |
| Runtime diagnostics                              | Implemented | `PluginDiagnostics` — dependents, extension classes, failure causes, class origins, duplicated host API bundling |
| Spring synchronization                           | Implemented | `PluginBeanRegistry`, `SpringPluginLifecycleSynchronizer` — register beans/controllers on start, unregister precisely by ownership on stop/failure/unload |
| Artifact admission                               | Implemented | `PluginArtifactVerifier`, `SecureFileDownloader` — protocol, size, SHA-512, archive scale, path traversal, forbidden classes, optional detached signature checks |
| Transactional update                             | Implemented | `TransactionalPluginUpdateManager` — backup, drain, update, dependent restore, health check, automatic rollback and result listeners |
| Spring state events                              | Implemented | `SpringPluginStateChangedEvent` — dependency-free full state snapshot, safe for async listeners/audit |
| Unit tests                                       | Implemented | JUnit 5 tests in all three modules (`src/test`)                          |

## 3. Requirements & Compatibility

| Requirement | Version                                    |
| :---------- | :----------------------------------------- |
| JDK         | 8+ (this branch)                           |
| Maven       | 3.0+ (wrapper included)                    |
| PF4J        | 3.15.0                                     |
| pf4j-spring | 0.9.0                                      |
| pf4j-update | 2.3.0                                      |
| Spring      | 5.3.39 (`spring-core`/`beans`/`context`/`web`/`webmvc`) |

Version lines of the easy4j project:

| Branch        | JDK  | Spring / pf4j-spring | Version pattern | Notes                       |
| :------------ | :--- | :------------------- | :-------------- | :-------------------------- |
| `feature/1.0.x` | 8    | 5.3.39 / 0.9.0       | `1.0.x.*`       | This README, current branch |
| `feature/2.0.x` | 17   | 6.2.19 / 0.10.0      | `2.0.x.*`       | JDK 17 line                 |
| `feature/3.0.x` | 21   | 7.0.8 / 0.10.0       | `3.0.x.*`       | JDK 21 line                 |

All three lines share the same public API and module structure; only the JDK, Maven compiler settings and
third-party baselines change. Do not use a higher line's jar on an older JDK. Detailed constraints are in
[COMPATIBILITY.md](COMPATIBILITY.md).

## 4. Architecture & Modules

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

Module relationships:

```text
pf4j-extension-spring
└── pf4j-extension-core
    └── org.pf4j:pf4j

pf4j-extension-update
├── pf4j-extension-core
└── org.pf4j:pf4j-update
```

`core` is guarded by Maven Enforcer against Spring, `pf4j-spring` and `pf4j-update`, so it can be used in
plain Java/PF4J applications. `pf4j-extension-update` declares
`spring-cloud-deployer-resource-maven` as **optional** — when using `MavenUpdateRepository` /
`MavenFileDownloader`, applications must add a version compatible with their own Spring baseline.

## 5. Installation

Artifacts are published to the aliyun repository and GitHub Releases; they are **not** on Maven Central yet.
Only PF4J's general capability is needed:

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>pf4j-extension-core</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

With Spring integration and dynamic controllers:

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>pf4j-extension-spring</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

With the plugin update repository:

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

## 6. Quick Start

Strict startup and safe shutdown:

```java
PluginManager pluginManager = new DefaultPluginManager(pluginsRoot);
PluginLifecycleManager lifecycle = new PluginLifecycleManager(pluginManager);

lifecycle.addListener(result -> audit(result));
lifecycle.loadAllAndStartStrictly();

// On application shutdown: copies and stops the list in reverse order.
lifecycle.unloadAllSafely();
```

Invocation chain, health checks and diagnostics:

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

Plugins may implement the `PluginHealthCheck`, `PluginReadinessCheck` and `PluginDrainHook` extension points.
The transactional update commits a new version only after health and readiness report `UP`.

## 7. Configuration

There is no properties file; behavior is configured programmatically:

| Concern              | Configuration point                                        |
| :------------------- | :--------------------------------------------------------- |
| Download limits      | `DownloadPolicy` constructor / `DownloadPolicy.secureDefaults()` |
| Verification order   | `PluginArtifactVerifier(FileVerifier delegate, List<ArtifactVerificationPolicy> policies)` |
| Transaction timeout  | `TransactionalPluginUpdateManager(..., long drainTimeoutSeconds)` |
| Interceptor chain    | `ExtensionInvoker(List<ExtensionInterceptor>)`             |
| Spring wiring        | `ExtendedSpringPluginManager` constructors (plugins root, autowire, singleton, injectable) |

## 8. Core Usage / API

Secure download and transactional update:

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

`installPlugin` and `updatePlugin` (compatibility entries) are routed into the transactional flow as well.
Strong isolation still requires a separate process or container — PF4J class loaders are not a security sandbox.

Spring plugin state events:

`SpringPluginStateChangedEvent` is a complete state snapshot that does **not** hold the plugin runtime object.
It contains:

- event id, occurrence time and the state transition;
- the plugin descriptor, required/optional dependencies;
- plugin path, runtime mode, host version;
- plugin manager class name, class-loader type and extension implementation class names;
- outermost exception, root exception and the string form of the exception cause chain.

The event never keeps `PluginWrapper`, plugin instances, plugin classes or the actual class loader, so it is
safe for async listeners, audit persistence and admin UIs. Listener exceptions are isolated and do not
interrupt the PF4J lifecycle operation.

## 9. Testing & Build

```bash
./mvnw clean verify
```

- JUnit 5 (Jupiter) tests cover all three modules, e.g. `PluginLifecycleManagerTest`, `ExtensionInvokerTest`,
  `PluginHealthServiceTest`, `PluginDiagnosticsTest`, `ExtensionCatalogTest` (core),
  `ExtendedSpringPluginManagerTest`, `SpringPluginEventPublisherTest`, `PluginBeanRegistryTest`,
  `InjectorUtilsTest` (spring), `RestTemplateUpdateRepositoryTest`, `PluginArtifactSecurityTest`,
  `FileSystemPluginArtifactStoreTest`, `TransactionalPluginUpdateManagerTest` (update).
- JaCoCo is configured with a line-coverage rule of 90% (`haltOnFailure=false`).
- Use the JDK matching the branch: JDK 8 for `feature/1.0.x`, JDK 17 for `feature/2.0.x`, JDK 21 for
  `feature/3.0.x`.

## 10. Versioning & Branches

| Branch        | JDK  | Version pattern | Maintenance                                      |
| :------------ | :--- | :-------------- | :----------------------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`       | Current branch; Spring 5.3.x / pf4j-spring 0.9.x |
| `feature/2.0.x` | 17   | `2.0.x.*`       | Spring 6.2.x / pf4j-spring 0.10.x                |
| `feature/3.0.x` | 21   | `3.0.x.*`       | Spring 7.0.x / pf4j-spring 0.10.x                |

Artifacts are distributed via the aliyun Maven repository and GitHub Releases.

**Package migration from the consolidated projects** (duplicate old implementations were dropped):

| Old package                          | New package                        |
| :----------------------------------- | :--------------------------------- |
| `io.github.hiwepy.pf4j.*`            | `org.pf4j.core.extension.*`        |
| `org.pf4j.spring.boot.ext.*`         | `org.pf4j.spring.extension.*`      |
| `org.pf4j.spring.boot.ext.update.*`  | `org.pf4j.update.extension.*`      |

When migrating from `pf4j3-extension`, remove the old coordinates, pick `core` / `spring` / `update` by
capability and update the Java imports.

## 11. Contributing & License

Contributions are welcome — please open an issue first. In-depth PF4J practice guides live in the docs:

- [Practice 1: business extension points and multi-implementation plugins](docs/PF4J_3.15_PRACTICE_01_BUSINESS_EXTENSION.md)
- [Practice 2: production release, observability and rollback](docs/PF4J_3.15_PRACTICE_02_PRODUCTION_GOVERNANCE.md)

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
