package org.acme;



import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.CredentialService;
import org.keycloak.authorization.client.AuthzClient;

/**
 * A CredentialService implementation that provides OAuth2 access tokens
 * using Keycloak. This service is used by the A2A client transport
 * authentication interceptors.
 */
public final class KeycloakOauthCredentialService implements CredentialService {

    /** OAuth2 scheme name. */
    private static final String OAUTH2_SCHEME_NAME = "oauth2";

    /** Keycloak authz client. */
    private final AuthzClient authzClient;

    /**
     * Creates a new KeycloakOAuth2CredentialService using the
     * default keycloak.json file.
     *
     * @throws IllegalArgumentException if keycloak.json cannot be found/loaded
     */
    public KeycloakOauthCredentialService() {
        this.authzClient = KeycloakUtil.createAuthzClient();
    }

    @Override
    public String getCredential(final String securitySchemeName,
                                final ClientCallContext clientCallContext) {
        if (!OAUTH2_SCHEME_NAME.equals(securitySchemeName)) {
            throw new IllegalArgumentException("Unsupported security scheme: "
                + securitySchemeName);
        }

        try {
            System.out.println("xxxx");
            return KeycloakUtil.getAccessToken(authzClient);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to obtain OAuth2 access token for scheme: "
                    + securitySchemeName, e);
        }
    }
}
