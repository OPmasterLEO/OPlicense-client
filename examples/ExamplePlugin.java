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
                .onValid(this::loadPlugin)
                .onExpired(result -> {
                    getLogger().severe("License expired.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onRevoked(result -> {
                    getLogger().severe("License revoked.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onIpNotWhitelisted(result -> {
                    getLogger().severe("This server's IP is not whitelisted for this license.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onProductMismatch(result -> {
                    getLogger().severe("License is not valid for this product.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onProductArchived(result -> {
                    getLogger().severe("This product is no longer supported.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onLicenseNotFound(result -> {
                    getLogger().severe("License key not recognized.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onTimestampDesync(result -> {
                    getLogger().severe("Server clock is out of sync, cannot validate license.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onRateLimited(result -> {
                    getLogger().severe("Too many validation attempts, try again later.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onSignatureInvalid(result -> {
                    getLogger().severe("License response could not be verified.");
                    Bukkit.getPluginManager().disablePlugin(this);
                })
                .onNetworkError(exception -> {
                    getLogger().severe("Could not reach the license server: " + exception.getMessage());
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
