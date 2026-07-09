package net.opmasterleo.license.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentResolverTest {

    @Test
    void resolvesJvmDefaults() {
        assertNotNull(EnvironmentResolver.resolveUserDir());
        assertNotNull(EnvironmentResolver.resolveUserHome());
        assertNotNull(EnvironmentResolver.resolveUserName());
    }

    @Test
    void respectsUserDirOverrideProperty() {
        String key = "oplicense.user.dir";
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, "C:\\Visual Studio Code\\lifesteal");
            assertEquals("C:\\Visual Studio Code\\lifesteal", EnvironmentResolver.resolveUserDir());
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test
    void resolvesThreadCount() {
        assertTrue(EnvironmentResolver.resolveThreadCount() > 0);
    }

    @Test
    void resolvesCpuCores() {
        assertTrue(EnvironmentResolver.resolveCpuCores() > 0);
    }
}
