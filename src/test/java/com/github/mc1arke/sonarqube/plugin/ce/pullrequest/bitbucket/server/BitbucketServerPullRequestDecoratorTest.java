/*
 * Copyright (C) 2019 Oliver Jedinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.SummaryComment;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class BitbucketServerPullRequestDecoratorTest {

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089));

    @InjectMocks
    private BitbucketServerPullRequestDecorator bitbucketServerPullRequestDecorator;

    private Map<String, String> headers;

    /**
     * configure these settings if you want to trigger your server instead of the test
     * APITOKEN: use a real api token
     */
    private static final String APITOKEN = "APITOKEN";

    private static final String DIFFURL = "http://localhost:8089/diff";

    private static final String COMMENTURL = "http://localhost:8089/comments";

    @Before
    public void setUp() {
        bitbucketServerPullRequestDecorator = new BitbucketServerPullRequestDecorator();

        headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", APITOKEN));
        headers.put("Accept", "application/json");
    }

    @Test
    public void getPageDiffClass() throws Exception {
        stubFor(get(urlEqualTo("/diff")).withHeader("Accept", equalTo("application/json")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/diff.json")))));
        DiffPage page = bitbucketServerPullRequestDecorator.getPage(DIFFURL, headers, DiffPage.class);
        assertThat(page, notNullValue());
        assertThat(page.getDiffs().size(), is(1));
    }

    @Test
    public void getIssueType() throws Exception{
        stubFor(get(urlEqualTo("/diff")).withHeader("Accept", equalTo("application/json")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FileUtils.readFileToByteArray(new File("src/test/resources/bitbucket/diff.json")))));
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

        stubFor(post(urlEqualTo("/comments")).withHeader("Accept", equalTo("application/json")).willReturn(
                aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody("{}")));
        assertThat(bitbucketServerPullRequestDecorator.postComment(COMMENTURL, headers, summaryComment), is(false));

        stubFor(post(urlEqualTo("/comments")).withHeader("Accept", equalTo("application/json")).willReturn(
                aResponse().withStatus(201).withHeader("Content-Type", "application/json").withBody("{}")));
        assertThat(bitbucketServerPullRequestDecorator.postComment(COMMENTURL, headers, summaryComment), is(true));
    }
}
