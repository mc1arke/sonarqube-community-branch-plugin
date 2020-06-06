package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.BitbucketServerPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.IAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.Annotation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.ce.posttask.QualityGate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketClientFacadeUnitTest {

    @Mock
    private BitbucketCloudClient bitbucketCloudClient;

    @Mock
    private BitbucketServerClient bitbucketServerClient;

    @Mock
    private UnifyConfiguration unifyConfiguration;

    @InjectMocks
    private BitbucketClientFacade underTest;

    @Before
    public void before() {
        this.underTest.withConfiguration(unifyConfiguration);

        verify(bitbucketCloudClient).setConfiguration(unifyConfiguration);
        verify(bitbucketServerClient).setConfiguration(unifyConfiguration);
    }

    @Test
    public void testCreateAnnotationForCloud() {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.bitbucket.org");

        // when
        IAnnotation annotation = underTest.createAnnotation("issueKey", 12, "http://localhost:9000/dashboard", "Failed", "/path/to/file", "MAJOR", "BUG");

        // then
        assertTrue(annotation instanceof CloudAnnotation);
        assertEquals("issueKey", ((CloudAnnotation)annotation).getExternalId());
        assertEquals(12, ((CloudAnnotation)annotation).getLine());
        assertEquals("http://localhost:9000/dashboard", ((CloudAnnotation)annotation).getLink());
        assertEquals("/path/to/file", ((CloudAnnotation)annotation).getPath());
        assertEquals("MAJOR", ((CloudAnnotation)annotation).getSeverity());
        assertEquals("BUG", ((CloudAnnotation)annotation).getAnnotationType());
    }

    @Test
    public void testCreateAnnotationForServer() {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://my-server.bitbucket.org");

        // when
        IAnnotation annotation = underTest.createAnnotation("issueKey", 12, "http://localhost:9000/dashboard", "Failed", "/path/to/file", "MAJOR", "BUG");

        // then
        assertTrue(annotation instanceof Annotation);
        assertEquals("issueKey", ((Annotation)annotation).getExternalId());
        assertEquals(12, ((Annotation)annotation).getLine());
        assertEquals("http://localhost:9000/dashboard", ((Annotation)annotation).getLink());
        assertEquals("/path/to/file", ((Annotation)annotation).getPath());
        assertEquals("MAJOR", ((Annotation)annotation).getSeverity());
        assertEquals("BUG", ((Annotation)annotation).getType());
    }

    @Test
    public void testDeleteAnnotationsForCloudVersion() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.bitbucket.org");

        // when
        underTest.deleteAnnotations("any", "any", "any");

        // then
        verify(bitbucketServerClient, times(0)).deleteAnnotations("any", "any", "any");
    }

    @Test
    public void testDeleteAnnotationsForServerVersion() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://my-git.org");

        // when
        underTest.deleteAnnotations("any", "any", "any");

        // then
        verify(bitbucketServerClient, times(1)).deleteAnnotations("any", "any", "any");
    }

    @Test
    public void testCreateDataLinkForCloud() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.bitbucket.org");

        // when
        DataValue data = underTest.createLinkDataValue("https://localhost:9000/any/project");

        // then
        assertTrue(data instanceof DataValue.CloudLink);
        assertEquals("https://localhost:9000/any/project", ((DataValue.CloudLink) data).getHref());
    }

    @Test
    public void testCreateDataLinkForServer() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://my-git.org");

        // when
        DataValue data = underTest.createLinkDataValue("https://localhost:9000/any/project");

        // then
        assertTrue(data instanceof DataValue.Link);
        assertEquals("https://localhost:9000/any/project", ((DataValue.Link) data).getHref());
    }

    @Test
    public void testCloudAlwaysSupportsCodeInsights() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.bitbucket.org");

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertTrue(result);
        verify(bitbucketServerClient, times(0)).supportsCodeInsights();
    }

    @Test
    public void testBitbucketServerUnsupportedCodeInsights() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.my-git.org");
        when(bitbucketServerClient.supportsCodeInsights()).thenReturn(false);

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertFalse(result);
        verify(bitbucketServerClient, times(1)).supportsCodeInsights();
    }

    @Test
    public void testBitbucketSupportsCodeInsights() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.my-git.org");
        when(bitbucketServerClient.supportsCodeInsights()).thenReturn(true);

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertTrue(result);
        verify(bitbucketServerClient, times(1)).supportsCodeInsights();
    }

    @Test
    public void testCreateCloudReport() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.bitbucket.org");

        // when
        underTest.createReport("project", "repo", "commitSha", new ArrayList<>(), "reportDescription", Instant.now(), "dashboardUrl", "logoUrl", QualityGate.Status.ERROR);

        // then
        verify(bitbucketServerClient, times(0)).createReport(eq("project"), eq("repo"), eq("commitSha"), any());
        verify(bitbucketCloudClient, times(1)).deleteReport("project", "repo", "commitSha");
        verify(bitbucketCloudClient, times(1)).createReport(eq("project"), eq("repo"), eq("commitSha"), any());
    }

    @Test
    public void testCreateServerReport() throws IOException {
        // given
        when(unifyConfiguration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)).thenReturn("https://api.my-git.org");

        // when
        underTest.createReport("project", "repo", "commitSha", new ArrayList<>(), "reportDescription", Instant.now(), "dashboardUrl", "logoUrl", QualityGate.Status.ERROR);

        // then
        verify(bitbucketServerClient, times(1)).createReport(eq("project"), eq("repo"), eq("commitSha"), any());
        verify(bitbucketCloudClient, times(0)).deleteReport("project", "repo", "commitSha");
        verify(bitbucketCloudClient, times(0)).createReport(eq("project"), eq("repo"), eq("commitSha"), any());
    }
}
