package net.opmasterleo.license.internal.hardware;

import java.net.NetworkInterface;
import java.util.Enumeration;

public final class MacAddressResolver {

    private MacAddressResolver() {
    }

    public static String resolvePrimary() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] macArray = network.getHardwareAddress();
                if (macArray == null) {
                    continue;
                }

                StringBuilder mac = new StringBuilder();
                for (int i = 0; i < macArray.length; i++) {
                    mac.append(String.format("%02X", macArray[i]));
                    if (i + 1 < macArray.length) {
                        mac.append(':');
                    }
                }

                if (mac.length() > 0) {
                    return mac.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
