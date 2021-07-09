package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        when(responseBody.string()).thenReturn("{}");
        when(builder.build().newCall(any()).execute().body()).thenReturn(responseBody);

        // when
        BitbucketClient client = new DefaultBitbucketClientFactory(() -> builder).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertTrue(client instanceof BitbucketCloudClient);
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

        // when
        BitbucketClient client = new DefaultBitbucketClientFactory(() -> mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS)).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertTrue(client instanceof BitbucketServerClient);
    }


}
