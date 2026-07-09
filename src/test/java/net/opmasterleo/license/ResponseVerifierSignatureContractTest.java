package net.opmasterleo.license;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseVerifierSignatureContractTest {

    @Test
    void verifyMethodHasStableThreeStringSignature() throws Exception {
        Method verify = ResponseVerifier.class.getMethod(
                "verify",
                String.class,
                String.class,
                String.class
        );

        assertEquals(boolean.class, verify.getReturnType());
        assertTrue(Modifier.isAbstract(verify.getModifiers()));
    }

    @Test
    void concreteVerifierImplementationExposesSameSignature() throws Exception {
        Method edVerify = ResponseVerifier.Ed25519ResponseVerifier.class.getMethod(
                "verify",
                String.class,
                String.class,
                String.class
        );

        assertEquals(boolean.class, edVerify.getReturnType());
    }
}
