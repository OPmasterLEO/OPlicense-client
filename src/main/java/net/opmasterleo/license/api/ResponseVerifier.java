package net.opmasterleo.license.api;

public interface ResponseVerifier {

    boolean verify(String payload, String signature, String algorithmHeader);
}
