package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultBitbucketClientFactoryUnitTest {

    @Test
    public void testCreateClientIsCloudIfCloudConfig() throws IOException {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD)
                .setClientId("clientId")
                .setAppId("appId")
                .setClientSecret("clientSecret");
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
                .setAlmRepo("almRepo");
        OkHttpClient.Builder builder = mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS);
        when(builder.addInterceptor(any())).thenReturn(builder);

        ResponseBody responseBody = mock(ResponseBody.class);
        when(responseBody.string()).thenReturn("{\"access_token\": \"dummy\"}");
        when(builder.build().newCall(any()).execute().body()).thenReturn(responseBody);

        Settings settings = mock(Settings.class);
        Encryption encryption = mock(Encryption.class);

        // when
        when(settings.getEncryption()).thenReturn(encryption);
        HttpClientBuilderFactory httpClientBuilderFactory = mock(HttpClientBuilderFactory.class);
        when(httpClientBuilderFactory.createClientBuilder()).then(i -> builder);
        BitbucketClient client = new DefaultBitbucketClientFactory(settings, httpClientBuilderFactory).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertTrue(client instanceof BitbucketCloudClient);

        ArgumentCaptor<Interceptor> interceptorArgumentCaptor = ArgumentCaptor.forClass(Interceptor.class);
        verify(builder, times(2)).addInterceptor(interceptorArgumentCaptor.capture());

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request request = mock(Request.class);
        when(chain.request()).thenReturn(request);
        Request.Builder requestBuilder = mock(Request.Builder.class);
        when(requestBuilder.addHeader(any(), any())).thenReturn(requestBuilder);
        when(request.newBuilder()).thenReturn(requestBuilder);

        Request request2 = mock(Request.class);
        when(requestBuilder.build()).thenReturn(request2);

        interceptorArgumentCaptor.getValue().intercept(chain);

        verify(requestBuilder).addHeader("Authorization", "Bearer dummy");
        verify(requestBuilder).addHeader("Accept", "application/json");
        verify(chain).proceed(request2);
    }

    @Test
    public void testCreateClientIfNotCloudConfig() {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET)
                .setUrl("url")
                .setPersonalAccessToken("personalAccessToken");
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
                .setAlmRepo("almRepo")
                .setAlmSlug("almSlug");

        Settings settings = mock(Settings.class);
        Encryption encryption = mock(Encryption.class);

        // when
        when(settings.getEncryption()).thenReturn(encryption);
        HttpClientBuilderFactory httpClientBuilderFactory = mock(HttpClientBuilderFactory.class);
        when(httpClientBuilderFactory.createClientBuilder()).then(i -> mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS));
        BitbucketClient client = new DefaultBitbucketClientFactory(settings, httpClientBuilderFactory).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertTrue(client instanceof BitbucketServerClient);
    }


}
