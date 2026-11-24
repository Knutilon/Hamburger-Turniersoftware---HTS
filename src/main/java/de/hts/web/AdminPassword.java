/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 1.7.0 (#141): Admin-Passwort für den Reiter „Webserver“. Das Passwort selbst steht nirgends im Quellcode (das
 * Repository ist öffentlich) – nur ein gesalzener Prüfwert (PBKDF2WithHmacSHA256, 310 000 Iterationen). Nach
 * Fehlversuchen wächst die Wartezeit.
 */
public final class AdminPassword {

    public static final int LENGTH = 40;
    private static final byte[] SALT = Base64.getDecoder().decode("5JmOyoZF68LcCHc+AkXMfw==");
    private static final byte[] HASH = Base64.getDecoder().decode("3OtUkAagHuJ0D58v5sOQfP5S4wj78l6FFD54U0yCrsg=");
    private static final int ITERATIONS = 310_000;

    private int failures;
    private long lockedUntil;
    private final java.util.function.LongSupplier clock;

    public AdminPassword() {
        this(System::currentTimeMillis);
    }

    AdminPassword(java.util.function.LongSupplier clock) {
        this.clock = clock;
    }

    public enum Result { OK, WRONG, LOCKED }

    /** Sekunden bis zum nächsten erlaubten Versuch (0 = sofort). */
    public long secondsLocked() {
        long ms = lockedUntil - clock.getAsLong();
        return ms <= 0 ? 0 : (ms + 999) / 1000;
    }

    public Result check(char[] password) {
        try {
            if (secondsLocked() > 0) return Result.LOCKED;
            boolean ok = matches(password);
            if (ok) {
                failures = 0;
                return Result.OK;
            }
            failures++;
            if (failures >= 3) lockedUntil = clock.getAsLong() + Math.min(300, 15L << Math.min(4, failures - 3)) * 1000;
            return Result.WRONG;
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
    }

    static boolean matches(char[] password) {
        if (password == null || password.length != LENGTH) {
            // gleiche Rechenzeit auch bei falscher Länge
            derive(new char[LENGTH]);
            return false;
        }
        return MessageDigest.isEqual(derive(password), HASH);
    }

    static byte[] derive(char[] password) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, SALT, ITERATIONS, HASH.length * 8);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
