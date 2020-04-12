package ninja.egg82.core;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.ParameterList;
import java.io.OutputStream;
import java.util.Map;

public class RestreamApi20 extends DefaultApi20 {
    protected RestreamApi20() { }

    private static class InstanceHolder {
        private static final RestreamApi20 INSTANCE = new RestreamApi20();
    }

    public static RestreamApi20 instance() { return InstanceHolder.INSTANCE; }

    @Override
    public String getAccessTokenEndpoint() { return "https://api.restream.io/oauth/token"; }

    @Override
    public String getAuthorizationUrl(String responseType, String apiKey, String callback, String scope, String state,
                                      Map<String, String> additionalParams) {
        final ParameterList parameters = new ParameterList(additionalParams);
        parameters.add(OAuthConstants.RESPONSE_TYPE, responseType);
        parameters.add(OAuthConstants.CLIENT_ID, apiKey);
        parameters.add(OAuthConstants.REDIRECT_URI, callback);
        parameters.add(OAuthConstants.SCOPE, scope);

        if (state != null) {
            parameters.add(OAuthConstants.STATE, state);
        }

        return parameters.appendTo("https://api.restream.io/login");
    }

    @Override
    protected String getAuthorizationBaseUrl() { throw new UnsupportedOperationException("use getAuthorizationUrl instead"); }

    @Override
    public String getRevokeTokenEndpoint() { return "https://api.restream.io/oauth/revoke"; }

    @Override
    public RestreamOAuthService createService(String apiKey, String apiSecret, String callback, String defaultScope,
                                              String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig,
                                              HttpClient httpClient) {
        return new RestreamOAuthService(this, apiKey, apiSecret, callback, defaultScope, responseType, debugStream,
                userAgent, httpClientConfig, httpClient);
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() { return OAuth2AccessTokenJsonExtractor.instance(); }
}
