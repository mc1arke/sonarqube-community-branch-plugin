/*
 * Copyright (C) 2021-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.gitlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.CommitNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequestNote;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitlabReviewPublisherIntegrationTest {
    @RegisterExtension
    static WireMockExtension server = WireMockExtension.extensionOptions().failOnUnmatchedRequests(true).build();

    private GitlabClient client() {
        return new GitlabRestClient(server.baseUrl() + "/api/v4", "token", new DefaultLinkHeaderReader(),
                new ObjectMapper(), HttpClients::createDefault);
    }

    @Test
    void publishesPositionedFindingAndSummaryThroughDraftApiOnly() throws Exception {
        String path = "/api/v4/projects/1/merge_requests/2/draft_notes";
        server.stubFor(get(urlEqualTo(path)).inScenario("review").whenScenarioStateIs("Started")
                .willReturn(okJson("[]")).willSetStateTo("preflight"));
        server.stubFor(post(urlEqualTo(path)).withFormParam("note", equalTo("finding"))
                .withFormParam("position[new_line]", equalTo("7"))
                .withFormParam("position[new_path]", equalTo("file.java"))
                .willReturn(created().withBody("{\"id\":11}")));
        server.stubFor(post(urlEqualTo(path)).withFormParam("note", equalTo("summary"))
                .willReturn(created().withBody("{\"id\":12}")));
        server.stubFor(get(urlEqualTo(path)).inScenario("review").whenScenarioStateIs("preflight")
                .willReturn(okJson("[{\"id\":11},{\"id\":12}]")));
        server.stubFor(post(urlEqualTo(path + "/bulk_publish")).willReturn(noContent()));
        new GitlabReviewPublisher().publish(client(), 1, 2, List.of(
                new CommitNote("finding", "base", "start", "head", "file.java", "file.java", 7),
                new MergeRequestNote("summary")));
        server.verify(1, postRequestedFor(urlEqualTo(path + "/bulk_publish")).withHeader("PRIVATE-TOKEN", equalTo("token")));
        server.verify(0, postRequestedFor(urlEqualTo("/api/v4/projects/1/merge_requests/2/discussions")));
    }

    @Test
    void refusesDraftsFoundOnLaterPages() {
        String path = "/api/v4/projects/1/merge_requests/2/draft_notes";
        server.stubFor(get(urlEqualTo(path)).willReturn(okJson("[]")
                .withHeader("Link", "<" + server.baseUrl() + path + "?page=2>; rel=\"next\"")));
        server.stubFor(get(urlEqualTo(path + "?page=2")).willReturn(okJson("[{\"id\":99}]")));
        assertThatThrownBy(() -> new GitlabReviewPublisher().publish(client(), 1, 2, List.of(new MergeRequestNote("summary"))))
                .hasMessageContaining("pending draft notes");
        server.verify(0, postRequestedFor(urlEqualTo(path + "/bulk_publish")));
        server.verify(0, postRequestedFor(urlEqualTo(path)));
    }

    @Test
    void deletesCreatedDraftWhenNextDraftIsRejected() {
        String path = "/api/v4/projects/1/merge_requests/2/draft_notes";
        server.stubFor(get(urlEqualTo(path)).willReturn(okJson("[]")));
        server.stubFor(post(urlEqualTo(path)).withFormParam("note", equalTo("finding"))
                .willReturn(created().withBody("{\"id\":11}")));
        server.stubFor(post(urlEqualTo(path)).withFormParam("note", equalTo("summary"))
                .willReturn(aResponse().withStatus(422)));
        server.stubFor(delete(urlEqualTo(path + "/11")).willReturn(noContent()));
        assertThatThrownBy(() -> new GitlabReviewPublisher().publish(client(), 1, 2,
                List.of(new MergeRequestNote("finding"), new MergeRequestNote("summary"))))
                .hasMessageContaining("Got: 422");
        server.verify(1, deleteRequestedFor(urlEqualTo(path + "/11")));
        server.verify(0, postRequestedFor(urlEqualTo(path + "/bulk_publish")));
    }
}
