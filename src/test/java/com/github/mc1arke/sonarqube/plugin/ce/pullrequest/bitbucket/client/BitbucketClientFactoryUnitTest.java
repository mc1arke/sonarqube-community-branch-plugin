package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

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

public class BitbucketClientFactoryUnitTest {

    @Test
    public void testCreateClientIsCloudIfCloudConfig() throws IOException {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD);
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto();
        OkHttpClient.Builder builder = mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS);
        when(builder.addInterceptor(any())).thenReturn(builder);

        ResponseBody responseBody = mock(ResponseBody.class);
        when(responseBody.string()).thenReturn("{}");
        when(builder.build().newCall(any()).execute().body()).thenReturn(responseBody);

        // when
        BitbucketClient client = new BitbucketClientFactory(() -> builder).createClient(almSettingDto, projectAlmSettingDto);

        // then
        assertTrue(client instanceof BitbucketCloudClient);
    }

    @Test
    public void testCreateClientIfNotCloudConfig() {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET);
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto();

        // when
        BitbucketClient client = new BitbucketClientFactory(() -> mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS)).createClient(almSettingDto, projectAlmSettingDto);

        // then
        assertTrue(client instanceof BitbucketServerClient);
    }


}
