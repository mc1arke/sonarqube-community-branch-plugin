package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.sonar.db.alm.setting.ALM;
import org.sonarqube.ws.AlmSettings;

public class AlmTypeMapperTest {

    @Test
    public void testToAlmWs() {
        assertThat(AlmTypeMapper.toAlmWs(ALM.AZURE_DEVOPS)).isEqualTo(AlmSettings.Alm.azure);
        assertThat(AlmTypeMapper.toAlmWs(ALM.GITLAB)).isEqualTo(AlmSettings.Alm.gitlab);
        assertThat(AlmTypeMapper.toAlmWs(ALM.GITHUB)).isEqualTo(AlmSettings.Alm.github);
        assertThat(AlmTypeMapper.toAlmWs(ALM.BITBUCKET)).isEqualTo(AlmSettings.Alm.bitbucket);
    }
}