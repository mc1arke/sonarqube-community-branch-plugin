/*
 * Copyright (C) 2020 Marvin Wichmann
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.BitbucketServerPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.IAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudCreateReportRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.Annotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.CreateAnnotationsRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.CreateReportRequest;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.ce.posttask.QualityGate;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This facade provides the ability to delegate the pull request decoration
 * between the cloud and the server version of bitbucket.
 * <p>
 * The URL is used in order to identify the cloud version.
 */
@ComputeEngineSide
public class BitbucketClientFacade {

    private final BitbucketServerClient bitbucketServerClient;
    private final BitbucketCloudClient bitbucketCloudClient;
    private UnifyConfiguration configuration;

    public BitbucketClientFacade(BitbucketServerClient bitbucketServerClient, BitbucketCloudClient cloudClient) {
        this.bitbucketServerClient = bitbucketServerClient;
        this.bitbucketCloudClient = cloudClient;
    }

    public void withConfiguration(UnifyConfiguration configuration) {
        this.configuration = configuration;
        this.bitbucketServerClient.setConfiguration(configuration);
        this.bitbucketCloudClient.setConfiguration(configuration);
    }

    /**
     * <p>
     * Creates an annotation for the given parameters based on the fact if the cloud
     * or the on prem bitbucket solution is used.
     * </p>
     *
     * @return IAnnotation either from the server or the cloud version
     */
    public IAnnotation createAnnotation(String issueKey, int line, String issueUrl, String message, String path, String severity, String type) {
        if (isCloud()) {
            return new CloudAnnotation(issueKey,
                    line,
                    issueUrl,
                    message,
                    path,
                    severity,
                    type);
        }

        return new Annotation(issueKey,
                line,
                issueUrl,
                message,
                path,
                severity,
                type);
    }

    public void deleteAnnotations(String project, String repo, String commitSha) throws IOException {
        if (!isCloud()) {
            bitbucketServerClient.deleteAnnotations(project, repo, commitSha);
        }
        // Cloud version doesn't have that endpoint. Instead we delete the complete report before we continue.
    }

    public void createAnnotations(String project, String repo, String commitSha, Set<IAnnotation> annotations) throws IOException {
        if (isCloud()) {
            Set<CloudAnnotation> annotationSet = annotations.stream().map(annotation -> (CloudAnnotation) annotation).collect(Collectors.toSet());
            bitbucketCloudClient.createAnnotations(project, repo, commitSha, annotationSet);
        } else {
            Set<Annotation> annotationSet = annotations.stream().map(annotation -> (Annotation) annotation).collect(Collectors.toSet());
            bitbucketServerClient.createAnnotations(project, repo, commitSha, new CreateAnnotationsRequest(annotationSet));
        }
    }

    public DataValue createLinkDataValue(String dashboardUrl) {
        if (isCloud()) {
            return new DataValue.CloudLink("Go to SonarQube", dashboardUrl);
        }

        return new DataValue.Link("Go to SonarQube", dashboardUrl);
    }

    public void createReport(String project, String repo, String commitSha, List<ReportData> reportData,
                             String reportDescription, Instant creationDate, String dashboardUrl,
                             String logoUrl, QualityGate.Status status) throws IOException {
        if (isCloud()) {
            // delete report first for cloud here then create new one
            bitbucketCloudClient.deleteReport(project, repo, commitSha);

            CloudCreateReportRequest report = new CloudCreateReportRequest(
                    reportData,
                    reportDescription,
                    "SonarQube",
                    "SonarQube",
                    Date.from(creationDate),
                    dashboardUrl, // you need to change this to a real https URL for local debugging since localhost will get declined by the API
                    logoUrl,
                    "COVERAGE",
                    QualityGate.Status.ERROR.equals(status) ? "FAILED" : "PASSED"
            );

            bitbucketCloudClient.createReport(project, repo, commitSha, report);
        } else {
            CreateReportRequest report = new CreateReportRequest(
                    reportData,
                    reportDescription,
                    "SonarQube",
                    "SonarQube",
                    creationDate,
                    dashboardUrl,
                    logoUrl,
                    QualityGate.Status.ERROR.equals(status) ? "FAIL" : "PASS"
            );

            bitbucketServerClient.createReport(project, repo, commitSha, report);
        }
    }

    /**
     * <p>
     * Determines if the used bitbucket endpoint supports the code insights feature.
     * <p>
     * For the cloud version we simply return true and for the server version a version
     * check is implemented that tests if the given server version is higher than 5.15
     * </p>
     *
     * @return boolean
     */
    public boolean supportsCodeInsights() {
        return isCloud() || bitbucketServerClient.supportsCodeInsights();
    }

    private boolean isCloud() {
        return baseUrl().contains("https://api.bitbucket.org");
    }

    private String baseUrl() {
        return configuration.getRequiredProperty(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL);
    }
}
