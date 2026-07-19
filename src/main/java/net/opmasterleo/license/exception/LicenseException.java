package net.opmasterleo.license.exception;

/** Runtime exception for OPLicense client failures. */
public class LicenseException extends RuntimeException {

    /** Creates an exception with a message. */
    public LicenseException(String message) {
        super(message);
    }

    /** Creates an exception with a message and cause. */
    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
