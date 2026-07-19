package net.opmasterleo.license.model;

/** Validation outcome. */
public enum LicenseOutcome {
    /** License is valid. */
    VALID,
    /** License is expired. */
    EXPIRED,
    /** License is revoked. */
    REVOKED,
    /** License is deactivated. */
    DEACTIVATED,
    /** License is deleted. */
    DELETED,
    /** Server IP is not whitelisted. */
    IP_NOT_WHITELISTED,
    /** HWID is required. */
    HWID_REQUIRED,
    /** Maximum HWID count reached. */
    MAX_HWIDS_REACHED,
    /** Server IP is blacklisted. */
    BLACKLISTED_IP,
    /** HWID is blacklisted. */
    BLACKLISTED_HWID,
    /** License does not match the product. */
    PRODUCT_MISMATCH,
    /** Product is archived. */
    PRODUCT_ARCHIVED,
    /** License key was not found. */
    LICENSE_NOT_FOUND,
    /** Request timestamp is out of sync. */
    TIMESTAMP_DESYNC,
    /** Too many validation attempts. */
    RATE_LIMITED,
    /** Request nonce was rejected. */
    NONCE_INVALID,
    /** Response failed anti-replay checks. */
    RESPONSE_INVALID,
    /** Response signature is invalid. */
    SIGNATURE_INVALID,
    /** API was unreachable or returned a server error. */
    NETWORK_ERROR
}
