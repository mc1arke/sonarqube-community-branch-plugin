package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.Activity;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.ActivityPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.User;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.io.File;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.Is.is;


public class BitbucketServerPullRequestDecoratorTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089).httpsPort(8443));

    @InjectMocks
    private BitbucketServerPullRequestDecorator bitbucketServerPullRequestDecorator;

    private Map<String, String> headers;

    private static final String APITOKEN = "TESTTOKEN";

    @Before
    public void setUp() throws Exception {
        bitbucketServerPullRequestDecorator = new BitbucketServerPullRequestDecorator(null, null, null, null, null, null);

        headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", APITOKEN));
        headers.put("Accept", "application/json");
    }

    @Test
    public void testGetActivityPage() throws Exception {
        stubFor(
                get(urlEqualTo("/activities"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("")
                        )
        );
        assertThat(bitbucketServerPullRequestDecorator.getActivityPage("http://localhost:8089/activities", headers), nullValue());

        stubFor(
                get(urlEqualTo("/activities"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(400)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{}")
                        )
        );
        assertThat(bitbucketServerPullRequestDecorator.getActivityPage("http://localhost:8089/activities", headers), nullValue());

        stubFor(
                get(urlEqualTo("/activities"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/activity.json")))
                        )
        );
        ActivityPage activityPage = bitbucketServerPullRequestDecorator.getActivityPage("http://localhost:8089/activities", headers);
        assertThat(activityPage, notNullValue());
        assertThat(activityPage.getSize(), is(5));
    }

    @Test
    public void getCommentsToDelete() {
        final ActivityPage activityPage = new ActivityPage();
        activityPage.setValues(new Activity[0]);
        assertThat(bitbucketServerPullRequestDecorator.getCommentsToDelete("susi.sonar", activityPage).size() , is(0));

        Activity activityWithoutComment = new Activity();
        activityWithoutComment.setId(123);
        activityPage.setValues(new Activity[] {activityWithoutComment});
        assertThat(bitbucketServerPullRequestDecorator.getCommentsToDelete("susi.sonar", activityPage).size() , is(0));

        Activity activityWithCommentWrongSlug = new Activity();
        activityWithCommentWrongSlug.setId(123);
        Comment comment1 = new Comment();
        comment1.setId(1000);
        User authorJayUnit = new User();
        authorJayUnit.setName("Jay Unit");
        authorJayUnit.setSlug("jay.unit");
        comment1.setAuthor(authorJayUnit);
        activityWithCommentWrongSlug.setComment(comment1);

        activityPage.setValues(new Activity[] {activityWithoutComment, activityWithCommentWrongSlug});
        assertThat(bitbucketServerPullRequestDecorator.getCommentsToDelete("susi.sonar", activityPage).size() , is(0));

        Activity activityWithCommentCorrectSlug = new Activity();
        activityWithCommentCorrectSlug.setId(123);
        Comment comment2 = new Comment();
        comment2.setId(1000);
        User authorSusiSonar = new User();
        authorSusiSonar.setName("Susi Sonar");
        authorSusiSonar.setSlug("susi.sonar");
        comment2.setAuthor(authorSusiSonar);
        activityWithCommentCorrectSlug.setComment(comment2);
        activityPage.setValues(new Activity[] {activityWithoutComment, activityWithCommentWrongSlug, activityWithCommentCorrectSlug});
        assertThat(bitbucketServerPullRequestDecorator.getCommentsToDelete("susi.sonar", activityPage).size() , is(1));
    }

    @Test
    public void deleteComments() throws Exception {
        assertThat(bitbucketServerPullRequestDecorator.deleteComments("http://localhost:8089/activities", "http://localhost:8089/comments", "susi.sonar", headers, false), is(false));

        stubFor(
                get(urlEqualTo("/activities"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/activity.json")))
                        )
        );

        stubFor(
                delete(urlMatching("/comments/([0-9]*)\\?version=([0-9]*)"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(404)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{}")
                        )
        );

        assertThat(bitbucketServerPullRequestDecorator.deleteComments("http://localhost:8089/activities", "http://localhost:8089/comments", "susi.sonar", headers, true), is(false));

        stubFor(
                delete(urlMatching("/comments/([0-9]*)\\?version=([0-9]*)"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(204)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{}")
                        )
        );
        assertThat(bitbucketServerPullRequestDecorator.deleteComments("http://localhost:8089/activities", "http://localhost:8089/comments", "susi.sonar", headers, true), is(true));

    }
}
