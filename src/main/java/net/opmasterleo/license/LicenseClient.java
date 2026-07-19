package net.opmasterleo.license;

import net.opmasterleo.license.api.Ed25519ResponseVerifier;
import net.opmasterleo.license.api.ValidationRequest;
import net.opmasterleo.license.internal.core.ClientConfig;
import net.opmasterleo.license.internal.core.RequestContext;
import net.opmasterleo.license.internal.core.ValidationEngine;
import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseResult;

public final class LicenseClient {

    public static final String VERSION = "2.0.0";

    private final ClientConfig config;
    private final RequestContext context;
    private final ValidationEngine engine;

    public LicenseClient(String apiUrl, String licenseKey, String product, Ed25519ResponseVerifier verifier) {
        this.config = ClientConfig.create(apiUrl, licenseKey, product, verifier);
        this.context = new RequestContext();
        this.engine = new ValidationEngine();
    }

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

    public LicenseClient setHwid(String hwid) {
        context.setHwid(hwid);
        return this;
    }

    public LicenseClient useAutoHwid() {
        context.useAutoHwid();
        return this;
    }

    public LicenseClient setMacAddress(String macAddress) {
        context.setMacAddress(macAddress);
        return this;
    }

    public LicenseClient setProductVersion(String productVersion) {
        context.setProductVersion(productVersion);
        return this;
    }

    public LicenseClient setServerSoftware(String serverSoftware, String serverSoftwareVersion) {
        context.setServerSoftware(serverSoftware, serverSoftwareVersion);
        return this;
    }

    public LicenseClient setContainer(String container) {
        context.setContainer(container);
        return this;
    }

    public LicenseClient setUserDir(String userDir) {
        context.setUserDir(userDir);
        return this;
    }

    public LicenseClient setUserHome(String userHome) {
        context.setUserHome(userHome);
        return this;
    }

    public LicenseClient setUserName(String userName) {
        context.setUserName(userName);
        return this;
    }

    public LicenseClient setPterodactylNode(String pterodactylNode) {
        context.setPterodactylNode(pterodactylNode);
        return this;
    }

    public LicenseEnvironment environment() {
        return context.environment();
    }

    public ValidationRequest validate() {
        return new ValidationRequest(this);
    }

    public LicenseResult execute() {
        return engine.validate(config, context);
    }
}
