package net.opmasterleo.example;

import net.opmasterleo.license.LicenseClient;
import net.opmasterleo.license.api.ValidationCallbacks;
import net.opmasterleo.license.model.LicenseResult;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String key = getConfig().getString("license-key");

        LicenseClient client = LicenseClient.withEd25519(
                "http://YOUR-VPS-IP:3000",
                key,
                "example-product",
                "YOUR-PRODUCT-ED25519-PUBLIC-KEY-SPKI-BASE64"
        );

        client.setProductVersion(getDescription().getVersion())
                .setServerSoftware(Bukkit.getName(), Bukkit.getVersion());

        client.validate().run(new ExampleValidationCallbacks());
    }

    private void loadPlugin() {
        getLogger().info("License valid, loading plugin.");
    }

    private void disableForFailure(LicenseResult result) {
        getLogger().severe(result.summary());
        Bukkit.getPluginManager().disablePlugin(this);
    }

    private final class ExampleValidationCallbacks extends ValidationCallbacks {

        @Override
        public void onValid(LicenseResult result) {
            getLogger().info(result.summary());
            if (result.update().updateAvailable()) {
                getLogger().warning(result.update().message());
            }
            loadPlugin();
        }

        @Override
        public void onExpired(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onRevoked(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onDeactivated(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onDeleted(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onIpNotWhitelisted(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onHwidRequired(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onMaxHwidExceeded(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onBlacklistedIp(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onBlacklistedHwid(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onProductMismatch(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onProductArchived(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onLicenseNotFound(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onTimestampDesync(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onRateLimited(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onSignatureInvalid(LicenseResult result) {
            disableForFailure(result);
        }

        @Override
        public void onNetworkError(Exception exception) {
            String message = exception != null ? exception.getMessage() : null;
            if (message == null || message.isBlank()) {
                message = "Could not reach the OPLicense API. The license server may be offline or unreachable.";
            }
            getLogger().severe("Network error validating license: " + message);
            Bukkit.getPluginManager().disablePlugin(ExamplePlugin.this);
        }
    }
}
