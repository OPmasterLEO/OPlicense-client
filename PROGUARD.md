# OPlicense-client Obfuscation Rules

`ClassFormatError: Illegal exception table range` means an obfuscator corrupted
bytecode while rewriting try/catch blocks. OPlicense-client 1.1.2+ centralizes
guarded I/O in `PlatformSupport` and lazy-initializes `LicenseRuntime` to reduce
that risk, but **flow/exception obfuscation must still be disabled for the SDK
package** when shading it into your plugin.

## Quick fix (recommended)

Exclude the whole SDK from flow, exception, and try/catch obfuscation. You can
still rename your own plugin classes normally.

### ProGuard / R8 (Gradle)

```kotlin
tasks.withType<ProGuardTask>().configureEach {
    configuration("proguard-rules.pro")
}

// or for R8 in build.gradle.kts:
// implementation('...') + consumerProguardFiles from dependency if published
```

`proguard-rules.pro`:

```proguard
-include META-INF/oplicense/consumer-rules.pro
```

The rules file ships inside the OPlicense-client JAR at
`META-INF/oplicense/consumer-rules.pro`.

### Skidfuscator

In your Skidfuscator config, exempt the SDK from transformers that rewrite
control flow or exception tables:

```yaml
exempt:
  - class: net.opmasterleo.license.**
```

Minimum transformers to exempt for the SDK package:

- flow (control-flow obfuscation)
- exception / try-catch obfuscation
- switch obfuscation on SDK classes

You may still obfuscate `com.yourplugin.**` aggressively.

### Zelix / other JVM obfuscators

Mark `net.opmasterleo.license.**` as excluded from:

- Exception obfuscation
- Aggressive flow obfuscation
- Try/catch range modification

Renaming (`LicenseClient` → `df.a`) is fine; corrupting exception tables is not.

## Manual keep rules (if you cannot use the bundled file)

```proguard
-keep class net.opmasterleo.license.** { *; }

-keep interface net.opmasterleo.license.api.ResponseVerifier {
    public boolean verify(java.lang.String, java.lang.String, java.lang.String);
}
-keep class net.opmasterleo.license.api.ResponseVerifier$* {
    public boolean verify(java.lang.String, java.lang.String, java.lang.String);
}

-keep class net.opmasterleo.license.internal.platform.PlatformSupport { *; }
-keep class net.opmasterleo.license.internal.crypto.Ed25519 { *; }
```

## What changed in 1.1.2

- `LicenseRuntime` no longer calls HWID/environment resolvers in field initializers
  (empty constructor; resolves on first validate).
- `PlatformSupport` owns guarded I/O instead of many per-method try/catch blocks.
- `HwidResolver` / `EnvironmentResolver` simplified for obfuscator-safe bytecode.

## Runtime mode

Use `LicenseClient.withEd25519(...)` — plugins embed only the public key, never the
signing secret.

## If it still fails

1. Confirm the shaded JAR contains `net/opmasterleo/license/internal/platform/PlatformSupport.class`.
2. Exempt `net.opmasterleo.license.**` from **all** bytecode transformers, not just renaming.
3. Update to OPlicense-client **1.1.2+**.
4. Rebuild the fat JAR after changing obfuscator settings (not just re-run on an old output).
