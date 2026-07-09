package net.opmasterleo.license.internal.environment;

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
    void resolvesThreadCountAsAvailableProcessors() {
        assertEquals(Runtime.getRuntime().availableProcessors(), EnvironmentResolver.resolveThreadCount());
    }

    @Test
    void respectsCpuModelOverrideProperty() {
        String key = "oplicense.cpu.model";
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, "AMD Ryzen 7 7800X3D 8-Core Processor");
            assertEquals("AMD Ryzen 7 7800X3D 8-Core Processor", EnvironmentResolver.resolveCpuModel());
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test
    void resolvesCpuCores() {
        assertTrue(EnvironmentResolver.resolveCpuCores() > 0);
    }
}
