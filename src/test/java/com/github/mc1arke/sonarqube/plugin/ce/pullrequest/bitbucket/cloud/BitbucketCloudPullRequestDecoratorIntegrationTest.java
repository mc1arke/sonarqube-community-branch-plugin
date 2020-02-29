package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.HttpUtils;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto.CommentPageDTO;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.response.activity.ActivityPage;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class BitbucketCloudPullRequestDecoratorIntegrationTest {

    private static final String CREDENTIALS = "base64(username:password)";
    private static final String PR_ENDPOINT = "http://localhost:8089/pullrequests/comments";
    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089));
    @Mock
    private ConfigurationRepository repository;
    @InjectMocks
    private BitbucketCloudPullRequestDecorator classUnderTest;
    private Map<String, String> headers;

    @Before
    public void setUp() {
        classUnderTest = new BitbucketCloudPullRequestDecorator(repository);

        headers = new HashMap<>();
        headers.put("Authorization", String.format("Basic %s", CREDENTIALS));
        headers.put("Accept", "application/json");

        classUnderTest.setHeaders(headers);
        classUnderTest.setCommentApiEndpoint("http://localhost:8089//pullrequests/comments");
    }

    @Test
    public void testCommentsSerialization() throws Exception {
        stubFor(
                get(urlEqualTo("/pullrequests/comments"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("")
                        )
        );
        assertThat(HttpUtils.getPage(PR_ENDPOINT, headers, CommentPageDTO.class), nullValue());

        stubFor(
                get(urlEqualTo("/pullrequests/comments"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/cloud/comments.json")))
                        )
        );

        CommentPageDTO activityPage = HttpUtils.getPage(PR_ENDPOINT, headers, CommentPageDTO.class);
        assertThat(activityPage, notNullValue());
        assertThat(activityPage.getComments().size(), is(10));
        assertThat(activityPage.getSize(), is(14));
        assertThat(activityPage.getNext(), notNullValue());
    }

    @Test
    public void testGetPaginatedComments() throws Exception {
        stubFor(
                get(urlEqualTo("/pullrequests/comments"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/cloud/comments-page0.json")))
                        )
        );

        stubFor(
                get(urlEqualTo("/pullrequests/comments?page=1"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/cloud/comments-page1.json")))
                        )
        );

        assertThat(classUnderTest.getComments().size(), is(20));
    }

}
