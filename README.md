# OPlicense-client

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
    implementation("com.github.opmasterleo:OPlicense-client:1.0.5")
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
    <version>1.0.5</version>
</dependency>
```

The actual Java code lives under `net.opmasterleo.license` — that's what
you import and reference in your own plugin regardless of the JitPack
coordinates above.

## What must never go in a user-editable config file

The API URL and signing material are **not** meant to be configurable by
whoever runs the server. Both belong in your plugin's private source (ideally
concealed — see below). Only the license key itself belongs in `config.yml`.

### Recommended: Ed25519 (public key only)

With `RESPONSE_SIGNING_MODE=ed25519` on the backend, the plugin embeds a
**public key** from `/product info`. Even if someone decompiles your JAR they
**cannot forge** valid license responses — unlike HMAC, where the extracted
secret lets them run a fake license server.

```java
LicenseClient client = LicenseClient.withEd25519(
    "https://your-api.example",          // use HTTPS in production
    getConfig().getString("license-key"),
    "your-product-slug",
    "MCowBQYDK2VwAyEA..."                // SPKI base64 from /product info DM
);
```



### Legacy: HMAC (symmetric secret — extractable)

HMAC mode still works but is weaker: anyone who extracts the secret from your
JAR can sign fake `valid: true` responses. Prefer Ed25519 for new products.

```java
LicenseClient client = new LicenseClient(
    "https://your-api.example",
    getConfig().getString("license-key"),
    "your-product-slug",
    "your-products-hmac-secret"
);
```



## Hiding strings from casual decompilation

ProGuard and similar tools **rename classes** but usually leave string literals
readable (`LICENSE_HMAC = "673a..."` stays visible). To raise the bar:

1. Switch to **Ed25519** so extracted material is not a forging key.
2. Use `Concealed.decode(int[], seed)` instead of `static final String`.
3. Run `examples/ConcealSecrets.java` at build time to generate the `int[]` arrays.
4. For stronger protection, use a commercial obfuscator with **string encryption**
  (Zelix, Stringer, Allatori) or a small **native (JNI)** verifier.
5. Use **HTTPS** — plain `http://` lets anyone on the network MITM your API.

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