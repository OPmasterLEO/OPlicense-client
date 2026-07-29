package net.opmasterleo.license;

import net.opmasterleo.license.api.Ed25519ResponseVerifier;
import net.opmasterleo.license.api.ValidationRequest;
import net.opmasterleo.license.internal.core.ClientConfig;
import net.opmasterleo.license.internal.core.RequestContext;
import net.opmasterleo.license.internal.core.ValidationEngine;
import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseResult;

/** OPLicense validation client. */
public final class LicenseClient {

    /** SDK version. */
    public static final String VERSION = "2.0.0";

    private final ClientConfig config;
    private final RequestContext context;
    private final ValidationEngine engine;

    /** Creates a client with an existing Ed25519 verifier. */
    public LicenseClient(String apiUrl, String licenseKey, String product, Ed25519ResponseVerifier verifier) {
        this.config = ClientConfig.create(apiUrl, licenseKey, product, verifier);
        this.context = new RequestContext();
        this.engine = new ValidationEngine();
    }

    /** Creates a client from an Ed25519 SPKI public key (Base64). */
    public static LicenseClient withEd25519(
            String apiUrl,
            String licenseKey,
            String product,
            String ed25519PublicKeySpkiBase64
    ) {
        return new LicenseClient(
                apiUrl,
                licenseKey,
                product,
                Ed25519ResponseVerifier.createEd25519(ed25519PublicKeySpkiBase64)
        );
    }

    /** Sets an explicit HWID. */
    public LicenseClient setHwid(String hwid) {
        context.setHwid(hwid);
        return this;
    }

    /** Regenerates the automatic HWID. */
    public LicenseClient useAutoHwid() {
        context.useAutoHwid();
        return this;
    }

    /** Sets the MAC address sent with validation. */
    public LicenseClient setMacAddress(String macAddress) {
        context.setMacAddress(macAddress);
        return this;
    }

    /** Sets the plugin/product version. */
    public LicenseClient setProductVersion(String productVersion) {
        context.setProductVersion(productVersion);
        return this;
    }

    /** Sets server software name and version. */
    public LicenseClient setServerSoftware(String serverSoftware, String serverSoftwareVersion) {
        context.setServerSoftware(serverSoftware, serverSoftwareVersion);
        return this;
    }

    /** Sets a container label (for example {@code pterodactyl}). */
    public LicenseClient setContainer(String container) {
        context.setContainer(container);
        return this;
    }

    /** Overrides {@code user.dir}. */
    public LicenseClient setUserDir(String userDir) {
        context.setUserDir(userDir);
        return this;
    }

    /** Overrides {@code user.home}. */
    public LicenseClient setUserHome(String userHome) {
        context.setUserHome(userHome);
        return this;
    }

    /** Overrides the reported username. */
    public LicenseClient setUserName(String userName) {
        context.setUserName(userName);
        return this;
    }

    /** Sets the Pterodactyl node name. */
    public LicenseClient setPterodactylNode(String pterodactylNode) {
        context.setPterodactylNode(pterodactylNode);
        return this;
    }

    /**
     * Attaches BuiltByBit anti-piracy placeholders for validation logging.
     * Pass the literal {@code %%__*__%%} tokens so BBB can inject values at download time.
     */
    public LicenseClient setBuiltByBit(
            String userId,
            String username,
            String resourceId,
            String resourceTitle,
            String version,
            String versionNumber,
            String downloadTimestamp,
            String nonce
    ) {
        context.setBuiltByBit(
                userId,
                username,
                resourceId,
                resourceTitle,
                version,
                versionNumber,
                downloadTimestamp,
                nonce
        );
        return this;
    }

    /** Attaches BuiltByBit Steam ID placeholders. */
    public LicenseClient setBuiltByBitSteam(String steam64, String steam32) {
        context.setBuiltByBitSteam(steam64, steam32);
        return this;
    }

    /** Attaches a BuiltByBit download nonce placeholder. */
    public LicenseClient setBuiltByBitNonce(String nonce) {
        context.setBuiltByBitNonce(nonce);
        return this;
    }

    /** Returns the captured environment snapshot. */
    public LicenseEnvironment environment() {
        return context.environment();
    }

    /** Starts a validation request. */
    public ValidationRequest validate() {
        return new ValidationRequest(this);
    }

    /** Runs validation synchronously and returns the result. */
    public LicenseResult execute() {
        return engine.validate(config, context);
    }
}
