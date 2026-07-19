# OPLicense Client

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/112c390e9e794b7bacaa6237ab126458)](https://app.codacy.com/gh/OPmasterLEO/OPlicense-client/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![](https://jitpack.io/v/OPmasterLEO/OPlicense-client.svg)](https://jitpack.io/#OPmasterLEO/OPlicense-client)
[![](https://jitci.com/gh/OPmasterLEO/OPlicense-client/svg)](https://jitci.com/gh/OPmasterLEO/OPlicense-client)

Java license SDK for validating against a self-hosted
[OPLicense backend](../oplicense-backend). Zero third-party dependencies —
`HttpURLConnection` + JDK crypto only.

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.opmasterleo:OPlicense-client:2.0.0")
}
```

```xml
<dependency>
    <groupId>com.github.opmasterleo</groupId>
    <artifactId>OPlicense-client</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Design

- **Public API stable** — `LicenseClient.withEd25519(...)`, `validate().run(callbacks)`,
  `ValidationCallbacks`, `LicenseResult` / `LicenseOutcome` names unchanged from 1.x
- **Fail closed** — unsigned, replayed, or product-mismatched responses never count as valid
- **Ed25519 signed responses** — verify every body against your product public key
- **Anti-replay** — nonce echo + issuedAt clock window (±120s)
- **Obfuscation-friendly** — no lambdas / `invokedynamic` in the SDK; prefer named nested
  callback classes in your plugin

## Package layout

```
net.opmasterleo.license/
  LicenseClient.java

net.opmasterleo.license.api/
  ValidationRequest.java
  ValidationCallbacks.java
  Ed25519ResponseVerifier.java

net.opmasterleo.license.model/
  LicenseResult.java / LicenseOutcome / LicenseUpdate / LicenseEnvironment

net.opmasterleo.license.exception/
  LicenseException.java

net.opmasterleo.license.internal/
  core/       ClientConfig, RequestContext, ValidationEngine
  http/       LicenseHttp, OutcomeReporter
  security/   Ed25519, Nonce, ResponseGuard
  probe/      HardwareId, EnvironmentProbe
  json/       Json
  util/       Digests, Io, Numbers, Strings
  crypto/     Concealed (optional string helper)
```

## Secrets

Keep **API URL**, **product slug**, and **Ed25519 public key** as constants in plugin
source. Only the **license key** belongs in `config.yml`.

```java
private static final String API_URL = "https://your-api.example";
private static final String PRODUCT = "your-product-slug";
private static final String PUBLIC_KEY = "MCowBQYDK2VwAyEA...";

LicenseClient client = LicenseClient.withEd25519(
    API_URL,
    getConfig().getString("license-key"),
    PRODUCT,
    PUBLIC_KEY
);
```

Prefer **HTTPS** in production. HTTP is accepted for local / IP endpoints.

## Basic usage

```java
client.setProductVersion(getDescription().getVersion())
      .setServerSoftware(Bukkit.getName(), Bukkit.getVersion());

client.validate().run(new PluginValidationCallbacks(this));

private static final class PluginValidationCallbacks extends ValidationCallbacks {

    private final JavaPlugin plugin;

    PluginValidationCallbacks(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onValid(LicenseResult result) {
        // register listeners / load features here
    }

    @Override
    public void onExpired(LicenseResult result) {
        Bukkit.getPluginManager().disablePlugin(plugin);
    }

    @Override
    public void onSignatureInvalid(LicenseResult result) {
        Bukkit.getPluginManager().disablePlugin(plugin);
    }

    @Override
    public void onNetworkError(Exception exception) {
        Bukkit.getPluginManager().disablePlugin(plugin);
    }
}
```

Call once at the top of `onEnable`. There is no offline cache or last-known-good fallback.

## Outcomes

| Callback | When |
| --- | --- |
| `onValid` | License accepted |
| `onExpired` / `onRevoked` / `onDeactivated` / `onDeleted` | License state |
| `onIpNotWhitelisted` | IP not on whitelist |
| `onHwidRequired` / `onMaxHwidExceeded` | HWID policy |
| `onBlacklistedIp` / `onBlacklistedHwid` | Blacklist |
| `onProductMismatch` / `onProductArchived` / `onLicenseNotFound` | Product / key |
| `onTimestampDesync` / `onRateLimited` | Request rejected |
| `onSignatureInvalid` | Signature / replay / nonce failure (local) |
| `onNetworkError` | Unreachable API / HTTP 5xx |

## Environment & HWID

Optional overrides before `validate()`:

```java
client.setProductVersion(getDescription().getVersion())
      .setServerSoftware(Bukkit.getName(), Bukkit.getVersion())
      .setContainer("pterodactyl")
      .setHwid("your-stable-server-id");
```

Default HWID order: explicit override → container env → `/etc/machine-id` → MAC → legacy.
Values are hashed (`HWID-...`) before leave the client.

`result.update()` exposes plugin version update hints when you set `setProductVersion`.

## Security notes (2.0)

- Response body capped at 1 MiB
- Redirects disabled on validate requests
- Signature required; algorithm must be `ed25519`
- Nonce compared in constant time
- SDK `User-Agent`: `OPLicense-Client/2.0.0`
- Local signature / replay failures reported to `/client-outcome`
