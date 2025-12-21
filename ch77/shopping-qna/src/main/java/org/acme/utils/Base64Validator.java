package org.acme.utils;

import java.util.Base64;

public class Base64Validator {

    public static boolean isBase64(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false; // not valid Base64
        }
    }

}
