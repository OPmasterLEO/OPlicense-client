package net.opmasterleo.license;

import net.opmasterleo.license.api.Ed25519ResponseVerifier;
import net.opmasterleo.license.api.ValidationRequest;
import net.opmasterleo.license.internal.runtime.LicenseRuntime;
import net.opmasterleo.license.internal.transport.LicenseConnection;
import net.opmasterleo.license.internal.transport.LicenseValidator;
import net.opmasterleo.license.model.LicenseEnvironment;
import net.opmasterleo.license.model.LicenseResult;

public final class LicenseClient {

    private final LicenseConnection connection;
    private final LicenseRuntime runtime;
    private final LicenseValidator validator;

    public LicenseClient(String apiUrl, String licenseKey, String product, Ed25519ResponseVerifier verifier) {
        this.connection = new LicenseConnection(apiUrl, licenseKey, product, verifier);
        this.runtime = new LicenseRuntime();
        this.validator = new LicenseValidator();
    }

    public static LicenseClient withEd25519(
            String apiUrl,
            String licenseKey,
            String product,
            String ed25519PublicKeySpkiBase64
    ) {
        return new LicenseClient(apiUrl, licenseKey, product, Ed25519ResponseVerifier.createEd25519(ed25519PublicKeySpkiBase64));
    }

    public LicenseClient setHwid(String hwid) {
        runtime.setHwid(hwid);
        return this;
    }

    public LicenseClient useAutoHwid() {
        runtime.useAutoHwid();
        return this;
    }

    public LicenseClient setMacAddress(String macAddress) {
        runtime.setMacAddress(macAddress);
        return this;
    }

    public LicenseClient setProductVersion(String productVersion) {
        runtime.setProductVersion(productVersion);
        return this;
    }

    public LicenseClient setServerSoftware(String serverSoftware, String serverSoftwareVersion) {
        runtime.setServerSoftware(serverSoftware, serverSoftwareVersion);
        return this;
    }

    public LicenseClient setContainer(String container) {
        runtime.setContainer(container);
        return this;
    }

    public LicenseClient setUserDir(String userDir) {
        runtime.setUserDir(userDir);
        return this;
    }

    public LicenseClient setUserHome(String userHome) {
        runtime.setUserHome(userHome);
        return this;
    }

    public LicenseClient setUserName(String userName) {
        runtime.setUserName(userName);
        return this;
    }

    public LicenseClient setPterodactylNode(String pterodactylNode) {
        runtime.setPterodactylNode(pterodactylNode);
        return this;
    }

    public LicenseEnvironment environment() {
        return runtime.environment();
    }

    public ValidationRequest validate() {
        return new ValidationRequest(this);
    }

    public LicenseResult execute() {
        return validator.validate(connection, runtime);
    }
}
