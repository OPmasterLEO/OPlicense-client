package net.opmasterleo.example;

import net.opmasterleo.license.LicenseClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String key = getConfig().getString("license");

        LicenseClient client = new LicenseClient(
                "http://YOUR-VPS-IP:3000",
                key,
                "example-product",
                "YOUR-PRODUCT-HMAC-SECRET"
        );

        client.setProductVersion(getDescription().getVersion())
                .setServerSoftware(Bukkit.getName(), Bukkit.getVersion());

        client.validate()
                .onValid(result -> {
                    getLogger().info(result.summary());
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
                .onIpNotWhitelisted(result -> {
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
                    getLogger().severe("Network error validating license: " + exception.getMessage());
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
