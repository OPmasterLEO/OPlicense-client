# OPlicense-client Obfuscation Rules

Use these minimal rules in consumer plugins when obfuscating with ProGuard/R8.
They preserve signature-critical call paths used by `LicenseClient` while still
allowing broad shrinking/obfuscation elsewhere.

```proguard
# Keep OPlicense public API used by plugin code and callback wiring.
-keep class net.opmasterleo.license.LicenseClient { public *; }
-keep class net.opmasterleo.license.api.ValidationRequest { public *; }
-keep class net.opmasterleo.license.model.LicenseResult { public *; }
-keep class net.opmasterleo.license.model.LicenseUpdate { public *; }
-keep class net.opmasterleo.license.model.LicenseEnvironment { public *; }
-keep class net.opmasterleo.license.model.LicenseOutcome { public *; }
-keep class net.opmasterleo.license.exception.LicenseException { public *; }

# Internal transport/runtime layers used by LicenseClient.
-keep class net.opmasterleo.license.internal.transport.LicenseValidator { *; }
-keep class net.opmasterleo.license.internal.transport.LicenseResponseParser { *; }
-keep class net.opmasterleo.license.internal.transport.LicenseHttpTransport { *; }
-keep class net.opmasterleo.license.internal.transport.LicenseConnection { *; }
-keep class net.opmasterleo.license.internal.runtime.LicenseRuntime { *; }

# Signature-critical verifier contract.
-keep interface net.opmasterleo.license.api.ResponseVerifier {
    public boolean verify(java.lang.String, java.lang.String, java.lang.String);
}
-keep class net.opmasterleo.license.api.ResponseVerifier$* {
    public boolean verify(java.lang.String, java.lang.String, java.lang.String);
}

# Keep cryptography internals referenced by verifier implementations.
-keep class net.opmasterleo.license.internal.crypto.Ed25519 { public *; }
```

## Skidfuscator Guidance

Skidfuscator can aggressively transform control flow and interface call sites.
If you encounter runtime linkage errors, minimally exempt:

- `net.opmasterleo.license.api.ResponseVerifier`
- `net.opmasterleo.license.api.ResponseVerifier$*`
- `net.opmasterleo.license.api.ValidationRequest`

Do **not** disable obfuscation for your entire plugin; keep the exemption scope
tight to these signature-critical classes.

## Recommended Runtime Mode

Use `LicenseClient.withEd25519(...)` whenever possible. Ed25519 ships only a
public key in the plugin, so decompilation does not leak a signing secret.
