package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketConfigurationUnitTest {

    @Test
    public void testIsCloudTrue() {
        // given
        BitbucketConfiguration configuration = new BitbucketConfiguration("https://api.bitbucket.org", "token", "repository", "project");

        // when
        boolean result = configuration.isCloud();

        // then
        assertTrue(result);
        assertEquals("token", configuration.getToken());
        assertEquals("repository", configuration.getRepository());
        assertEquals("https://api.bitbucket.org", configuration.getUrl());
        assertEquals("project", configuration.getProject());
    }

    @Test
    public void testIsCloudTrueForOtherCasing() {
        // given
        BitbucketConfiguration configuration = new BitbucketConfiguration("https://API.BITBUCKET.org", "token", "repository", "project");

        // when
        boolean result = configuration.isCloud();

        // then
        assertTrue(result);
    }

    @Test
    public void testIsCloudReturnsFalseForServerVersion() {
        // given
        BitbucketConfiguration configuration = new BitbucketConfiguration("https://API.server.org", "token", "repository", "project");

        // when
        boolean result = configuration.isCloud();

        // then
        assertFalse(result);
    }
}
