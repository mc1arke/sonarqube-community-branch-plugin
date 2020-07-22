package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.BitbucketConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BitbucketClientFactoryUnitTest {

    @Test
    public void testCreateClientIsCloudIfUrlMatches() {
        // given
        BitbucketConfiguration configuration = new BitbucketConfiguration("https://api.bitbucket.org", "token", "repository", "project");

        // when
        BitbucketClient client = BitbucketClientFactory.createClient(configuration);

        // then
        assertTrue(client instanceof BitbucketCloudClient);
    }

    @Test
    public void testCreateClientIsServerIfNotApiUrl() {
        // given
        BitbucketConfiguration configuration = new BitbucketConfiguration("https://api.server.org", "token", "repository", "project");

        // when
        BitbucketClient client = BitbucketClientFactory.createClient(configuration);

        // then
        assertTrue(client instanceof BitbucketServerClient);
    }


}
