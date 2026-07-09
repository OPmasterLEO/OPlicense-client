# OPlicense-client Obfuscation Rules

Use these minimal rules in consumer plugins when obfuscating with ProGuard/R8.
They preserve signature-critical call paths used by `LicenseClient` while still
allowing broad shrinking/obfuscation elsewhere.

```proguard
# Keep OPlicense public API used by plugin code and callback wiring.
-keep class net.opmasterleo.license.LicenseClient { public *; }
-keep class net.opmasterleo.license.ValidationRequest { public *; }
-keep class net.opmasterleo.license.LicenseResult { public *; }
-keep class net.opmasterleo.license.LicenseOutcome { public *; }
-keep class net.opmasterleo.license.LicenseException { public *; }

# Signature-critical verifier contract.
-keep interface net.opmasterleo.license.ResponseVerifier {
    public boolean verify(java.lang.String, java.lang.String, java.lang.String);
}
-keep class net.opmasterleo.license.ResponseVerifier$* {
    public boolean verify(java.lang.String, java.lang.String, java.lang.String);
}

# Keep cryptography internals referenced by verifier implementations.
-keep class net.opmasterleo.license.internal.Ed25519 { public *; }
```

## Skidfuscator Guidance

Skidfuscator can aggressively transform control flow and interface call sites.
If you encounter runtime linkage errors, minimally exempt:

- `net.opmasterleo.license.ResponseVerifier`
- `net.opmasterleo.license.ResponseVerifier$*`
- `net.opmasterleo.license.ValidationRequest`

Do **not** disable obfuscation for your entire plugin; keep the exemption scope
tight to these signature-critical classes.

## Recommended Runtime Mode

Use `LicenseClient.withEd25519(...)` whenever possible. Ed25519 ships only a
public key in the plugin, so decompilation does not leak a signing secret.
