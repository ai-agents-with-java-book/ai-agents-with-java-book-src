package org.acme;

import java.io.InputStream;
import java.util.concurrent.ConcurrentMap;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;

/** Utility class for common Keycloak operations and token caching. */
public final class KeycloakUtil {

    private KeycloakUtil() {
        // Utility class, prevent instantiation
    }

    /**
     * Creates a Keycloak AuthzClient from the default keycloak.json
     * configuration file.
     *
     * @return a configured AuthzClient
     * @throws IllegalArgumentException if keycloak.json cannot be found/loaded
     */
    public static AuthzClient createAuthzClient() {
        return createAuthzClient("keycloak.json");
    }

    private static AuthzClient createAuthzClient(final String configFileName) {
        try {
            InputStream configStream = null;

            // First try to load from current directory (for JBang)
            try {
                java.io.File configFile = new java.io.File(configFileName);
                if (configFile.exists()) {
                    configStream = new java.io.FileInputStream(configFile);
                }
            } catch (Exception ignored) {
                // Fall back to classpath
            }

            // If not found in current directory, try classpath
            if (configStream == null) {
                configStream = KeycloakUtil.class
                    .getClassLoader()
                    .getResourceAsStream(configFileName);
            }

            if (configStream == null) {
                throw new IllegalArgumentException("Config file not found: "
                    + configFileName);
            }
            return AuthzClient.create(configStream);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Failed to load Keycloak configuration from " + configFileName, e);
        }
    }

    /**
     * Gets a valid access token for the specified cache key, using the
     * provided cache and AuthzClient. Uses caching to avoid unnecessary
     * token requests.

     * @param authzClient the Keycloak AuthzClient to use for token requests
     * @return a valid access token
     * @throws RuntimeException if token acquisition fails
     */
    public static String getAccessToken(final AuthzClient authzClient) {

        try {
            // Obtain a new access token from Keycloak
            AccessTokenResponse tokenResponse = authzClient.obtainAccessToken();

            return tokenResponse.getToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain token from Keycloak", e);
        }
    }
}
