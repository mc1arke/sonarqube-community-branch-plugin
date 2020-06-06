package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.BitbucketServerPullRequestDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractBitbucketClientUnitTest {

    @Mock
    private UnifyConfiguration unifyConfiguration;

    private AbstractBitbucketClient client = new AbstractBitbucketClient() {
    };

    @Before
    public void before() {
        this.client.setConfiguration(unifyConfiguration);
    }

    @Test
    public void testURLConfiguration() {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.bitbucket.org");

        // when
        String result = client.baseUrl();

        // then
        assertEquals("https://api.bitbucket.org", result);
    }

    @Test
    public void testTokenConfiguration() {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_TOKEN)).thenReturn("iohoisdjfsdf==");

        // when
        String result = client.getToken();

        // then
        assertEquals("iohoisdjfsdf==", result);
    }

}
