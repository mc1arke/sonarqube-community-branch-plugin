package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpUtils {

    private static final Logger LOGGER = Loggers.get(HttpUtils.class);

    private HttpUtils() {
    }

    public static <T> T getPage(String url, Map<String, String> headers, Class<T> type) {
        T page = null;
        try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault()) {
            HttpClientContext clientContext = HttpClientContext.create();
            LOGGER.debug(String.format("Getting page %s", type));
            HttpGet httpGet = new HttpGet(url);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue());
            }
            HttpResponse httpResponse = closeableHttpClient.execute(httpGet, clientContext);
            if (null == httpResponse) {
                LOGGER.error(String.format("HttpResponse for getting page %s was null", type));
            } else if (httpResponse.getStatusLine().getStatusCode() != 200) {
                HttpEntity entity = httpResponse.getEntity();
                LOGGER.error("Error response from Bitbucket Server/Cloud: " + IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()));
                throw new IllegalStateException(String.format("Error response returned from Bitbucket Server/Cloud. Expected HTTP Status 200 but got %s", httpResponse.getStatusLine().getStatusCode()));
            } else {
                HttpEntity entity = httpResponse.getEntity();
                page = new ObjectMapper()
                        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()), type);
                LOGGER.debug(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(page));
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Could not get %s from Bitbucket Server/Cloud", type.getName()), ex);
        }
        return type.cast(page);
    }
}
