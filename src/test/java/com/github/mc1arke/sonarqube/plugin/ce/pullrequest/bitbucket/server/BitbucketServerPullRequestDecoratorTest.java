package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.SummaryComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.ActivityPage;
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
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.Is.is;

public class BitbucketServerPullRequestDecoratorTest {

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089));

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
    public void setUp() {
        bitbucketServerPullRequestDecorator = new BitbucketServerPullRequestDecorator(null);

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
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/activity.json")))
                        )
        );
        ActivityPage activityPage = bitbucketServerPullRequestDecorator.getPage(ACTIVITYURL, headers, ActivityPage.class);
        assertThat(activityPage, notNullValue());
        assertThat(activityPage.getSize(), is(3));
    }

    @Test(expected = IllegalStateException.class)
    public void getPageActivityClassError() {

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
        bitbucketServerPullRequestDecorator.getPage(ACTIVITYURL, headers, ActivityPage.class);
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
    public void getCommentsToDelete() throws Exception {
        ActivityPage activityPage = new ObjectMapper().readValue(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/delete/activityPageCase1.json")), ActivityPage.class);
        assertThat(bitbucketServerPullRequestDecorator.getCommentsToDelete("susi.sonar", activityPage).size() , is(0));

        activityPage = new ObjectMapper().readValue(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/delete/activityPageCase2.json")), ActivityPage.class);
        assertThat(bitbucketServerPullRequestDecorator.getCommentsToDelete("susi.sonar", activityPage).size() , is(0));

        activityPage = new ObjectMapper().readValue(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/delete/activityPageCase3.json")), ActivityPage.class);
        assertThat(bitbucketServerPullRequestDecorator.getCommentsToDelete("susi.sonar", activityPage).size() , is(0));

        activityPage = new ObjectMapper().readValue(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/delete/activityPageCase4.json")), ActivityPage.class);
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
