# OPLicense Client

Java client SDK for validating licenses against a self-hosted
[OPLicense backend](../oplicense-backend) instance. No external
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
    implementation("com.github.opmasterleo:OPlicense-client:1.1.1")
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
    <version>1.1.1</version>
</dependency>
```



## Package layout

```
net.opmasterleo.license/
  LicenseClient.java              # entry point — the only class most plugins import directly

net.opmasterleo.license.api/
  ValidationRequest.java          # fluent callback builder returned by validate()
  ResponseVerifier.java           # Ed25519 verifier interface

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



## Hiding strings from casual decompilation

ProGuard and similar tools **rename classes** but usually leave string literals
readable (`ED25519_PUBLIC_KEY = "MCow..."` stays visible). To raise the bar:

1. Use `Concealed.decode(int[], seed)` instead of `static final String`.
2. Run `examples/ConcealSecrets.java` at build time to generate the `int[]` arrays.
3. For stronger protection, use a commercial obfuscator with **string encryption**
  (Zelix, Stringer, Allatori) or a small **native (JNI)** verifier.
4. Use **HTTPS** — plain `http://` lets anyone on the network MITM your API.

```java
private static final int SEED = 0x1A2B3C4D;

LicenseClient.withEd25519(
    Concealed.decode(new int[] { /* ... */ }, SEED),
    getConfig().getString("license-key"),
    Concealed.decode(new int[] { /* ... */ }, SEED),
    Concealed.decode(new int[] { /* ... */ }, SEED)
);
```

**Reality check:** no client-side check is unbreakable. A determined attacker can
patch `verify()` to always return true. Ed25519 + obfuscation stops casual piracy
and fake license servers; it does not stop dedicated crackers.

## Obfuscation Compatibility

`OPLicense Client` is designed to survive heavy plugin obfuscation, including
ProGuard/R8 and Skidfuscator. Verifier internals now use explicit concrete
classes (not synthetic lambda implementations) to reduce runtime linkage issues
like `AbstractMethodError` / `NoSuchMethodError` at verifier call sites.

For consumer keep rules, see `[PROGUARD.md](./PROGUARD.md)`. Keep exemptions
minimal and focused on signature-critical classes only.

## Basic usage

Validate once at the top of `onEnable` with OPLicense before anything else runs:

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


| Callback                     | Fires when                                                                         |
| ---------------------------- | ---------------------------------------------------------------------------------- |
| `onValid()`                  | License is good                                                                    |
| `onExpired(result)`          | Past its expiry date                                                               |
| `onRevoked(result)`          | Explicitly revoked by an admin                                                     |
| `onIpNotWhitelisted(result)` | This server's IP isn't on the license's whitelist                                  |
| `onProductMismatch(result)`  | Key doesn't belong to this product                                                 |
| `onProductArchived(result)`  | Product has been discontinued                                                      |
| `onLicenseNotFound(result)`  | Key doesn't exist                                                                  |
| `onTimestampDesync(result)`  | Server clock drift outside the allowed window                                      |
| `onRateLimited(result)`      | Too many recent validate attempts for this key/IP                                  |
| `onSignatureInvalid(result)` | Response didn't verify against the public key — treat as a possible spoofed server |
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
client.setProductVersion(getDescription().getVersion())
      .validate()
      .onValid(result -> {
          if (result.update().updateAvailable()) {
              getLogger().warning(result.update().message());
          }
      })
      .run();
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



## Advanced: why signed responses matter

Every response from the OPLicense backend is Ed25519-signed, and `LicenseClient` verifies it
before trusting the payload. A forged host cannot produce a valid signature
without the product's private key (which never ships in plugins).

Plugins embed only a **public key**. Even if the jar is decompiled, attackers cannot mint valid signatures.

See `examples/ExamplePlugin.java` for a complete, wired-up example.