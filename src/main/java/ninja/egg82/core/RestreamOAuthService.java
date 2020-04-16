package ninja.egg82.core;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.revoke.TokenTypeHint;
import java.io.OutputStream;

public class RestreamOAuthService extends OAuth20Service {
    public RestreamOAuthService(RestreamApi20 api, String apiKey, String apiSecret, String callback, String defaultScope,
                                String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig,
                                HttpClient httpClient) {
        super(api, apiKey, apiSecret, callback, defaultScope, responseType, debugStream, userAgent, httpClientConfig,
                httpClient);
    }

    @Override
    protected OAuthRequest createAccessTokenRequest(AccessTokenRequestParams params) {
        final DefaultApi20 api = getApi();
        final OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());
        request.addBodyParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.AUTHORIZATION_CODE);
        request.addBodyParameter(OAuthConstants.REDIRECT_URI, getCallback());
        request.addParameter(OAuthConstants.CODE, params.getCode());

        final String pkceCodeVerifier = params.getPkceCodeVerifier();
        if (pkceCodeVerifier != null) {
            request.addParameter(PKCE.PKCE_CODE_VERIFIER_PARAM, pkceCodeVerifier);
        }
        if (isDebug()) {
            log("created access token request with body params [%s], query string params [%s]",
                    request.getBodyParams().asFormUrlEncodedString(),
                    request.getQueryStringParams().asFormUrlEncodedString());
        }
        return request;
    }

    @Override
    protected OAuthRequest createRefreshTokenRequest(String refreshToken, String scope) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("The refreshToken cannot be null or empty");
        }

        final DefaultApi20 api = getApi();
        final OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getRefreshTokenEndpoint());

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());
        request.addBodyParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.REFRESH_TOKEN);
        request.addBodyParameter(OAuthConstants.REFRESH_TOKEN, refreshToken);

        if (isDebug()) {
            log("created refresh token request with body params [%s], query string params [%s]",
                    request.getBodyParams().asFormUrlEncodedString(),
                    request.getQueryStringParams().asFormUrlEncodedString());
        }
        return request;
    }

    @Override
    protected OAuthRequest createRevokeTokenRequest(String tokenToRevoke, TokenTypeHint tokenTypeHint) {
        final DefaultApi20 api = getApi();
        final OAuthRequest request = new OAuthRequest(Verb.POST, api.getRevokeTokenEndpoint());

        request.addParameter("token", tokenToRevoke);
        request.addParameter("token_type_hint", tokenTypeHint.getValue());

        if (isDebug()) {
            log("created revoke token request with body params [%s], query string params [%s]",
                    request.getBodyParams().asFormUrlEncodedString(),
                    request.getQueryStringParams().asFormUrlEncodedString());
        }

        return request;
    }

    @Override
    public void signRequest(String accessToken, OAuthRequest request) {
        request.addHeader(OAuthConstants.HEADER,
                accessToken == null ? "Client-ID " + getApiKey() : "Bearer " + accessToken);
    }
}
