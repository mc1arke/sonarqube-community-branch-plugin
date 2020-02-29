package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto.CommentDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.config.Configuration;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

import java.util.List;
import java.util.Optional;

import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.BitbucketCloudPullRequestDecorator.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketCloudPullRequestDecoratorTestUnitTest {

    @Mock
    private ConfigurationRepository repository;

    @InjectMocks
    private FakeBitbucketCloudPullRequestDecorator classUnderTest;

    private static boolean deleteCommentsEnabled = false;
    private static boolean postSummaryEnabled = false;
    private static boolean fileBasedCommentEnabled = false;

    @Before
    public void before() {
        deleteCommentsEnabled = false;
        postSummaryEnabled = false;
        fileBasedCommentEnabled = false;
    }

    @Test
    public void testGetCommentApiReturnsCorrectUrl() {
        // given
        String pullRequestId = "3";
        Configuration configuration = mock(Configuration.class);
        when(repository.getConfiguration()).thenReturn(configuration);
        when(configuration.get(PULL_REQUEST_BITBUCKET_WORKSPACE)).thenReturn(Optional.of("marvinwichmann"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG)).thenReturn(Optional.of("sonartest"));

        // when
        String url = classUnderTest.getCommentApiUrl(pullRequestId);

        // then
        assertThat(url).isEqualTo("https://api.bitbucket.org/2.0/repositories/marvinwichmann/sonartest/pullrequests/3/comments");
    }

    @Test
    public void testGetName() {
        // given
        String expectedName = "BitbucketCloud";

        // when
        String result = classUnderTest.name();

        // then
        assertThat(result).isEqualTo(expectedName);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetMandatoryProperty() {
        // given
        Configuration configuration = mock(Configuration.class);
        when(configuration.get(PULL_REQUEST_BITBUCKET_WORKSPACE)).thenReturn(Optional.empty());

        // when
        String result = getMandatoryProperty(PULL_REQUEST_BITBUCKET_WORKSPACE, configuration);

        // then
        assertThat(false).isEqualTo(true);
    }

    @Test
    public void testDecorationWithAllFeaturesEnabled() throws Exception {
        // given
        postSummaryEnabled = true;
        fileBasedCommentEnabled = true;
        deleteCommentsEnabled = true;
        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        UnifyConfiguration unifyConfiguration = mock(UnifyConfiguration.class);

        Configuration configuration = mock(Configuration.class);
        when(repository.getConfiguration()).thenReturn(configuration);
        when(configuration.get(PULL_REQUEST_COMMENT_SUMMARY_ENABLED)).thenReturn(Optional.of(String.valueOf(postSummaryEnabled)));
        when(configuration.get(PULL_REQUEST_FILE_COMMENT_ENABLED)).thenReturn(Optional.of(String.valueOf(fileBasedCommentEnabled)));
        when(configuration.get(PULL_REQUEST_DELETE_COMMENTS_ENABLED)).thenReturn(Optional.of(String.valueOf(deleteCommentsEnabled)));
        when(configuration.get(PULL_REQUEST_BITBUCKET_APP_PASSWORD)).thenReturn(Optional.of("notimportant"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_APP_USERNAME)).thenReturn(Optional.of("notimportant"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_USER_UUID)).thenReturn(Optional.of("notimportant"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_WORKSPACE)).thenReturn(Optional.of("notimportant"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG)).thenReturn(Optional.of("notimportant"));

        // when
        classUnderTest.decorateQualityGateStatus(analysisDetails, unifyConfiguration);

        // then
        // do nothing - would throw exception if false.
    }

    @Test
    public void testDecorationWithAllFeaturesDisabled() throws Exception {
        // given
        postSummaryEnabled = false;
        fileBasedCommentEnabled = false;
        deleteCommentsEnabled = false;
        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        UnifyConfiguration unifyConfiguration = mock(UnifyConfiguration.class);

        Configuration configuration = mock(Configuration.class);
        when(repository.getConfiguration()).thenReturn(configuration);
        when(configuration.get(PULL_REQUEST_COMMENT_SUMMARY_ENABLED)).thenReturn(Optional.of(String.valueOf(postSummaryEnabled)));
        when(configuration.get(PULL_REQUEST_FILE_COMMENT_ENABLED)).thenReturn(Optional.of(String.valueOf(fileBasedCommentEnabled)));
        when(configuration.get(PULL_REQUEST_DELETE_COMMENTS_ENABLED)).thenReturn(Optional.of(String.valueOf(deleteCommentsEnabled)));
        when(configuration.get(PULL_REQUEST_BITBUCKET_APP_PASSWORD)).thenReturn(Optional.of("notimportant"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_APP_USERNAME)).thenReturn(Optional.of("notimportant"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_WORKSPACE)).thenReturn(Optional.of("workspace"));
        when(configuration.get(PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG)).thenReturn(Optional.of("slug"));

        // when
        classUnderTest.decorateQualityGateStatus(analysisDetails, unifyConfiguration);

        // then
        // do nothing - would throw exception if false.
    }

    private static class FakeBitbucketCloudPullRequestDecorator extends BitbucketCloudPullRequestDecorator {
        public FakeBitbucketCloudPullRequestDecorator(ConfigurationRepository configurationRepository) {
            super(configurationRepository);
        }

        @Override
        protected void deleteComments(List<CommentDTO> comments, String serviceUserUuid) {
            // do nothing
            if (!deleteCommentsEnabled) {
                throw new IllegalStateException("Test failed.");
            }
        }

        @Override
        protected void postSummaryComment(AnalysisDetails analysisDetails) {
            // do nothing
            if (!postSummaryEnabled) {
                throw new IllegalStateException("Test failed.");
            }
        }

        @Override
        protected void postFileBasedComments(AnalysisDetails analysisDetails) {
            // do nothing
            if (!fileBasedCommentEnabled) {
                throw new IllegalStateException("Test failed.");
            }
        }

        @Override
        protected List<CommentDTO> getComments() {
            return null;
        }
    }
}
