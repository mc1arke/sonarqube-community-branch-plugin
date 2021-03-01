package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import org.junit.Test;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import static org.junit.Assert.assertTrue;

public class BitbucketClientFactoryUnitTest {

    @Test
    public void testCreateClientIsCloudIfCloudConfig() {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD);
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto();

        // when
        BitbucketClient client = BitbucketClientFactory.createClient(almSettingDto, projectAlmSettingDto);

        // then
        assertTrue(client instanceof BitbucketCloudClient);
    }

    @Test
    public void testCreateClientIfNotCLoudConfig() {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET);
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto();

        // when
        BitbucketClient client = BitbucketClientFactory.createClient(almSettingDto, projectAlmSettingDto);

        // then
        assertTrue(client instanceof BitbucketServerClient);
    }


}
