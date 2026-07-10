# OPLicense Client

Java client SDK for validating licenses against a self-hosted  
[OPLicense backend](../oplicense-backend) instance. No external  
dependencies — uses `HttpURLConnection` and `javax.crypto` from the  
standard library only, so there's nothing to shade or relocate.

Gradle (`build.gradle.kts`):

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.opmasterleo:OPlicense-client:1.1.3")
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
    <version>1.1.3</version>
</dependency>
```

## Package layout

```
net.opmasterleo.license/
  LicenseClient.java              # entry point — the only class most plugins import directly

net.opmasterleo.license.api/
  ValidationRequest.java          # run(ValidationCallbacks) — single dispatch entry
  ValidationCallbacks.java        # abstract class; override outcomes you care about
  Ed25519ResponseVerifier.java    # concrete signature verifier (no interface)
  ResponseVerifiers.java          # createEd25519(spkiBase64) factory

net.opmasterleo.license.model/
  LicenseResult.java              # validation outcome + metadata
  LicenseOutcome.java             # outcome enum
  LicenseUpdate.java              # plugin version updater info
  LicenseEnvironment.java         # captured server environment snapshot

net.opmasterleo.license.exception/
  LicenseException.java

net.opmasterleo.license.internal/
  transport/                      # HTTP, connection, validator, response parser
  runtime/                        # mutable request metadata
  environment/                    # user dir, CPU, Pterodactyl detection
  hardware/                       # stable HWID resolution
  crypto/                         # Ed25519 + Concealed string helper
  json/                           # minimal JSON encode/decode
```

Typical imports:

```java
import net.opmasterleo.license.LicenseClient;
import net.opmasterleo.license.model.LicenseResult;
import net.opmasterleo.license.model.LicenseEnvironment;
```

`ValidationRequest` is returned from `client.validate()` — you usually don't need to import it unless you store it in a variable.

## What must never go in a user-editable config file

The API URL and signing material are **not** meant to be configurable by
whoever runs the server. Both belong in your plugin's private source (ideally
concealed — see below). Only the license key itself belongs in `config.yml`.

### Ed25519 (public key only)

Embed the **public key** from `/product info`. Even if someone decompiles your JAR they
**cannot forge** valid license responses.

```java
LicenseClient client = LicenseClient.withEd25519(
    "https://your-api.example",          // use HTTPS in production
    getConfig().getString("license-key"),
    "your-product-slug",
    "MCowBQYDK2VwAyEA..."                // SPKI base64 from /product info DM
);
```

## Basic usage

Validate once at the top of `onEnable` with OPLicense before anything else runs:

```java
client.validate().run(new ValidationCallbacks() {
    @Override
    public void onValid(LicenseResult result) {
        // load config, register listeners, everything else goes here
    }

    @Override
    public void onExpired(LicenseResult result) {
        Bukkit.getPluginManager().disablePlugin(this);
    }

    @Override
    public void onNetworkError(Exception exception) {
        Bukkit.getPluginManager().disablePlugin(this);
    }
});
```

Subclass `ValidationCallbacks` and override only the outcomes you need.
The SDK dispatches with `invokevirtual` on your concrete class — no
functional interfaces, no fluent chains, no `invokedynamic` in the SDK itself.

Call this once, at the very top of `onEnable`, before anything else runs.
There's no polling loop — this is a single check per server boot. If the
backend is unreachable, `onNetworkError` fires and nothing after it
should ever execute; this SDK does not cache or fall back to a
last-known-good result.

## Full outcome list


| Callback                     | Fires when                                                                                        |
| ---------------------------- | ------------------------------------------------------------------------------------------------- |
| `onValid(result)`            | License is good                                                                                   |
| `onExpired(result)`          | Past its expiry date                                                                              |
| `onRevoked(result)`          | Explicitly revoked by an admin                                                                    |
| `onIpNotWhitelisted(result)` | This server's IP isn't on the license's whitelist                                                 |
| `onProductMismatch(result)`  | Key doesn't belong to this product                                                                |
| `onProductArchived(result)`  | Product has been discontinued                                                                     |
| `onLicenseNotFound(result)`  | Key doesn't exist                                                                                 |
| `onTimestampDesync(result)`  | Server clock drift outside the allowed window                                                     |
| `onRateLimited(result)`      | Too many recent validate attempts for this key/IP                                                 |
| `onSignatureInvalid(result)` | Response didn't verify against the public key — treat as a possible spoofed server                |
| `onNetworkError(exception)`  | Couldn't reach the OPLicense API (use `exception.getMessage()` or `result.networkErrorMessage()`) |


Any callback you don't set is simply skipped — nothing runs. There's no
forced console output or banner; build whatever presentation fits your
plugin's own style using the `LicenseResult` data (`result.status()`,
`result.expiresAt()`, `result.owner()`, `result.ownerDiscordId()`, `result.serverId()`,
`result.whitelistedIps()`, `result.rawBody()`).

## Optional environment fields

These are gathered automatically with sensible defaults but can be
overridden before calling `validate()`:

```java
client.setProductVersion(getDescription().getVersion())
      .setServerSoftware(Bukkit.getName(), Bukkit.getVersion())
      .setContainer("pterodactyl");
```

### Plugin updater message

Set your plugin version before validate. The backend tracks every reported version per product and returns the highest observed version as latest:

```java
client.setProductVersion(getDescription().getVersion());

client.validate().run(new ValidationCallbacks() {
    @Override
    public void onValid(LicenseResult result) {
        if (result.update().updateAvailable()) {
            getLogger().warning(result.update().message());
        }
    }
});
```

`result.update()` exposes:

- `currentVersion()` — version you sent
- `latestVersion()` — highest version seen across licensed servers
- `updateAvailable()` — `true` when current is behind latest
- `message()` — ready-to-print updater text

Defaults are also collected automatically:

- `userDir` from JVM `user.dir` (e.g. `C:\...\lifesteal` on Windows, `/home/container` on Pterodactyl)
- `userHome` from JVM `user.home`
- `userName` from `USER` / `USERNAME` / JVM `user.name` (`?` on Pterodactyl when unavailable)
- `cpuModel` from `/proc/cpuinfo` on Linux or WMI on Windows (override with `OPLICENSE_CPU_MODEL`)
- `cpuCores` from container cgroup CPU limit when available (falls back to JVM processors)
- `threadCount` as logical CPU count visible to the JVM (`availableProcessors`, cgroup-aware)
- `pterodactylServerId` / `pterodactylServerUuid` from `P_SERVER_ID` / `P_SERVER_UUID`
- `pterodactylNode` from `PTERODACTYL_NODE` / `P_NODE_NAME` (set in egg startup if Wings does not inject it)

Read them in your plugin before or after validate:

```java
LicenseEnvironment env = client.environment();
// or after validate: result.environment()

String cpu = env.cpuModel();
double allocatedCores = env.cpuCores();
int threads = env.threadCount();
String node = env.pterodactylNode();
```

Override with env vars (`OPLICENSE_USER_DIR`, `OPLICENSE_USER_HOME`, `OPLICENSE_USER_NAME`) or:

```java
client.setUserDir("C:\\path\\to\\server")
      .setUserHome("C:\\Users\\YourName")
      .setUserName("YourName")
      .setPterodactylNode("node-01");
```

For Pterodactyl node name, add to your egg startup env if not present:
`PTERODACTYL_NODE={{node.name}}` or `OPLICENSE_PTERODACTYL_NODE={{node.name}}`.

They're sent along with every request purely for your own visibility —
the backend logs them to your Discord log channel — and are never used
to gate access on their own.

### Stable server ID (HWID) for Pterodactyl/containers

The SDK now derives a more stable default HWID for containerized hosts:

1. explicit overrides (`-Doplicense.hwid=...` or `OPLICENSE_HWID`)
2. Pterodactyl/container env identifiers (`P_SERVER_UUID`, etc.)
3. machine-id files (`/etc/machine-id`)
4. MAC/legacy fallback

All candidates are hashed before use (sent as `HWID-...`), so raw panel IDs
are not sent directly.

If you want full control, still call:

```java
client.setHwid("your-stable-server-id");
```

