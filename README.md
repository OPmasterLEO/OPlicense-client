# OPlicense-client

[![](https://jitci.com/gh/OPmasterLEO/OPlicense-client/svg)](https://jitci.com/gh/OPmasterLEO/OPlicense-client)

[![](https://jitpack.io/v/OPmasterLEO/OPlicense-client.svg)](https://jitpack.io/#OPmasterLEO/OPlicense-client)

Java client SDK for validating licenses against a self-hosted
[oplicense-backend](../oplicense-backend) instance. No external
dependencies — uses `java.net.http.HttpClient` (Java 11+) and
`javax.crypto` from the standard library only, so there's nothing to
shade or relocate.

## Adding the dependency

Gradle (`build.gradle.kts`):

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.opmasterleo:OPlicense-client:1.0.1")
}
```

Maven:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.opmasterleo</groupId>
    <artifactId>OPlicense-client</artifactId>
    <version>1.0.1</version>
</dependency>
```

The actual Java code lives under `net.opmasterleo.license` — that's what
you import and reference in your own plugin regardless of the JitPack
coordinates above.

## What must never go in a user-editable config file

The API URL and the HMAC secret are **not** meant to be configurable by
whoever runs the server the plugin is installed on. Both belong hardcoded
directly in your plugin's own private source, right where you construct
`LicenseClient`. Only the license key itself belongs in something like
`config.yml`, since that's meant to differ per install.

The **HMAC secret** (4th constructor argument) is **not** `GLOBAL_HMAC_SECRET`
from your backend `.env` by default. Each product has its own secret — get it
from `/product-info` in Discord (sent via DM), or set `HMAC_SIGNING_MODE=global`
on the backend and then use `GLOBAL_HMAC_SECRET` everywhere.

```yaml
license-key: "XXXX-XXXX-XXXX-XXXX"
```

```java
LicenseClient client = new LicenseClient(
    "http://your-vps-ip:3000",   // hardcoded, not from config
    getConfig().getString("license-key"),
    "your-product-slug",
    "your-products-hmac-secret"   // from /product-info (NOT GLOBAL_HMAC_SECRET unless HMAC_SIGNING_MODE=global)
);
```

If any of the four constructor arguments are missing, the SDK throws
immediately rather than silently skipping verification — there's no way
to accidentally ship a plugin with signature checking turned off.

## Basic usage

```java
client.validate()
    .onValid(() -> {
        // load config, register listeners, everything else goes here
    })
    .onExpired(result -> Bukkit.getPluginManager().disablePlugin(this))
    .onRevoked(result -> Bukkit.getPluginManager().disablePlugin(this))
    .onIpNotWhitelisted(result -> Bukkit.getPluginManager().disablePlugin(this))
    .onNetworkError(exception -> Bukkit.getPluginManager().disablePlugin(this))
    .run();
```

Call this once, at the very top of `onEnable`, before anything else runs.
There's no polling loop — this is a single check per server boot. If the
backend is unreachable, `onNetworkError` fires and nothing after it
should ever execute; this SDK does not cache or fall back to a
last-known-good result.

## Full outcome list


| Callback                     | Fires when                                                                     |
| ---------------------------- | ------------------------------------------------------------------------------ |
| `onValid()`                  | License is good                                                                |
| `onExpired(result)`          | Past its expiry date                                                           |
| `onRevoked(result)`          | Explicitly revoked by an admin                                                 |
| `onIpNotWhitelisted(result)` | This server's IP isn't on the license's whitelist                              |
| `onProductMismatch(result)`  | Key doesn't belong to this product                                             |
| `onProductArchived(result)`  | Product has been discontinued                                                  |
| `onLicenseNotFound(result)`  | Key doesn't exist                                                              |
| `onTimestampDesync(result)`  | Server clock drift outside the allowed window                                  |
| `onRateLimited(result)`      | Too many recent validate attempts for this key/IP                              |
| `onSignatureInvalid(result)` | Response didn't verify against the secret — treat as a possible spoofed server |
| `onNetworkError(exception)`  | Couldn't reach the backend at all                                              |


Any callback you don't set is simply skipped — nothing runs. There's no
forced console output or banner; build whatever presentation fits your
plugin's own style using the `LicenseResult` data (`result.status()`,
`result.expiresAt()`, `result.rawBody()`).

## Optional environment fields

These are gathered automatically with sensible defaults but can be
overridden before calling `validate()`:

```java
client.setProductVersion(getDescription().getVersion())
      .setServerSoftware(Bukkit.getName(), Bukkit.getVersion())
      .setContainer("pterodactyl");
```

They're sent along with every request purely for your own visibility —
the backend logs them to your Discord log channel — and are never used
to gate access on their own.

## Advanced: why the secret matters

Every response from the backend is HMAC-signed with that product's own
secret. `LicenseClient` recomputes the signature on every response and
rejects anything that doesn't match — via `onSignatureInvalid` — even if
the response body itself says `valid: true`. This is what stops someone
from pointing your plugin at a fake server that always claims success:
without the real secret, a forged server can't produce a signature that
verifies.

See `examples/ExamplePlugin.java` for a complete, wired-up example.