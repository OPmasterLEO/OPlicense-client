package net.opmasterleo.example;

import net.opmasterleo.license.LicenseClient;
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

        client.validate()
                .onValid(result -> {
                    getLogger().info(result.summary());
                    if (result.update().updateAvailable()) {
                        getLogger().warning(result.update().message());
                    }
                    loadPlugin();
                })
                .onExpired(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onRevoked(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onDeactivated(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onDeleted(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onIpNotWhitelisted(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onHwidRequired(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onMaxHwidExceeded(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onBlacklistedIp(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onBlacklistedHwid(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onProductMismatch(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onProductArchived(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onLicenseNotFound(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onTimestampDesync(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onRateLimited(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onSignatureInvalid(result -> {
                    getLogger().severe(result.summary());
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onNetworkError(exception -> {
                    String message = exception != null ? exception.getMessage() : null;
                    if (message == null || message.isBlank()) {
                        message = "Could not reach the OPLicense API. The license server may be offline or unreachable.";
                    }
                    getLogger().severe("Network error validating license: " + message);
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .run();
    }

    private void loadPlugin() {
        getLogger().info("License valid, loading plugin.");
        // config, GUI managers, listeners, everything else goes here,
        // never before this point
    }
}
