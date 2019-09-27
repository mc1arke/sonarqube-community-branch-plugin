package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.SummaryComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Activity;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.ActivityPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.User;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffPage;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.FileUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
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
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089));

    @InjectMocks
    private BitbucketServerPullRequestDecorator bitbucketServerPullRequestDecorator;

    private Map<String, String> headers;

    /**
     * configure these settings if you want to trigger your server instead of the test
     * APITOKEN: use a real api token
     * ACTIVITYURL: use for your bitbucket url (http://localhost:7990/rest/api/1.0/users/repo.owner/repos/testrepo/pull-requests/1/activities)
     */
    private static final String APITOKEN = "APITOKEN";

    private static final String ACTIVITYURL = "http://localhost:8089/activities";

    private static final String DIFFURL = "http://localhost:8089/diff";

    private static final String COMMENTURL = "http://localhost:8089/comments";

    @Before
    public void setUp() throws Exception {
        bitbucketServerPullRequestDecorator = new BitbucketServerPullRequestDecorator(null, null, null, null, null, null);

        headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", APITOKEN));
        headers.put("Accept", "application/json");
    }

    @Test
    public void getPageActivityClass() throws Exception {
        stubFor(
                get(urlEqualTo("/activities"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("")
                        )
        );
        assertThat(bitbucketServerPullRequestDecorator.getPage(ACTIVITYURL, headers, ActivityPage.class), nullValue());

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
        assertThat(bitbucketServerPullRequestDecorator.getPage(ACTIVITYURL, headers, ActivityPage.class), nullValue());

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
        ActivityPage activityPage = bitbucketServerPullRequestDecorator.getPage(ACTIVITYURL, headers, ActivityPage.class);
        assertThat(activityPage, notNullValue());
        assertThat(activityPage.getSize(), is(3));
    }


    @Test
    public void getPageDiffClass() throws Exception {
        stubFor(
                get(urlEqualTo("/diff"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/diff.json")))
                        )
        );
        DiffPage page = bitbucketServerPullRequestDecorator.getPage(DIFFURL, headers, DiffPage.class);
        assertThat(page, notNullValue());
        assertThat(page.getDiffs().size(), is(1));
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
        assertThat(bitbucketServerPullRequestDecorator.deleteComments(ACTIVITYURL, COMMENTURL, "susi.sonar", headers, false), is(false));

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

        assertThat(bitbucketServerPullRequestDecorator.deleteComments(ACTIVITYURL, COMMENTURL, "susi.sonar", headers, true), is(false));

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
        assertThat(bitbucketServerPullRequestDecorator.deleteComments(ACTIVITYURL, COMMENTURL, "susi.sonar", headers, true), is(true));
    }

    @Test
    public void getIssueType() throws Exception{
        stubFor(
                get(urlEqualTo("/diff"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/diff.json")))
                        )
        );
        DiffPage diffPage = bitbucketServerPullRequestDecorator.getPage(DIFFURL, headers, DiffPage.class);

        // wrong file
        String issueType = bitbucketServerPullRequestDecorator.getIssueType(diffPage, "src/DoesNotExist.java", 15);
        assertThat(issueType, is("CONTEXT"));

        // line not within diff
        issueType = bitbucketServerPullRequestDecorator.getIssueType(diffPage, "src/com/sonar/sample/classes/ClassWithInvalidMethodName.java", 0);
        assertThat(issueType, is("CONTEXT"));

        issueType = bitbucketServerPullRequestDecorator.getIssueType(diffPage, "src/com/sonar/sample/classes/ClassWithInvalidMethodName.java", 15);
        assertThat(issueType, is("ADDED"));
    }

    @Test
    public void postComment() throws Exception{
        StringEntity summaryComment = new StringEntity(new ObjectMapper().writeValueAsString(new SummaryComment("summaryComment")), ContentType.APPLICATION_JSON);
        assertThat(bitbucketServerPullRequestDecorator.postComment(COMMENTURL, headers, summaryComment, false), is(false));

        stubFor(
                post(urlEqualTo("/comments"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(400)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{}")
                        )
        );
        assertThat(bitbucketServerPullRequestDecorator.postComment(COMMENTURL, headers, summaryComment, true), is(false));

        stubFor(
                post(urlEqualTo("/comments"))
                        .withHeader("Accept" , equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{}")
                        )
        );
        assertThat(bitbucketServerPullRequestDecorator.postComment(COMMENTURL, headers, summaryComment, true), is(true));
    }
}
