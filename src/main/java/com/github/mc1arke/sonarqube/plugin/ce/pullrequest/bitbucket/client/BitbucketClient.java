/*
 * Copyright (C) 2020-2021 Marvin Wichmann, Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.ReportData;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface BitbucketClient {

    /**
     * <p>
     * Creates an annotation for the given parameters based on the fact if the cloud
     * or the on prem bitbucket solution is used.
     * </p>
     *
     * @return The newly created {@link CodeInsightsAnnotation}
     */
    CodeInsightsAnnotation createCodeInsightsAnnotation(String issueKey, int line, String issueUrl, String message, String path, String severity, String type);

    /**
     * <p>
     * Creates a report for the given parameters based on the fact if the cloud
     * or the on prem bitbucket solution is used.
     * </p>
     *
     * @return The newly created {@link CodeInsightsReport}
     */
    CodeInsightsReport createCodeInsightsReport(List<ReportData> reportData,
                                                String reportDescription, Instant creationDate, String dashboardUrl,
                                                String logoUrl, QualityGate.Status status);

    /**
     * Deletes all code insights annotations for the given parameters.
     *
     * @throws IOException if the annotations cannot be deleted
     */
    void deleteAnnotations(String project, String repo, String commitSha) throws IOException;

    /**
     * Uploads CodeInsights Annotations for the given commit.
     *
     * @throws IOException if the annotations cannot be uploaded
     */
    void uploadAnnotations(String project, String repo, String commitSha, Set<CodeInsightsAnnotation> annotations) throws IOException;

    /**
     * Creates a DataValue of type DataValue.Link or DataValue.CloudLink depending on the implementation
     */
    DataValue createLinkDataValue(String dashboardUrl);

    /**
     * Uploads the code insights report for the given commit
     */
    void uploadReport(String project, String repo, String commitSha, CodeInsightsReport codeInsightReport) throws IOException;

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
    boolean supportsCodeInsights();

    /**
     * <p>
     *     Returns the annotation upload limit consisting of two different objects:
     *     1. the batch size for each incremental annotation upload
     *     2. the total allowed annotations for the given provider
     * </p>
     *
     * @return the configured limit
     */
    AnnotationUploadLimit getAnnotationUploadLimit();

    /**
     * Extract the name of the project from the relevant configuration. The project is
     * the value that should be used in the calls that take a `project` parameter.
     *
     * @param almSettingDto the global `AlmSettingDto` containing the global configuration for this ALM
     * @param projectAlmSettingDto the `ProjectAlmSettingDto` assigned to the current project
     * @return the resolved project name.
     */
    String resolveProject(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto);

    /**
     * Extract the name of the repository from the relevant configuration. The project is
     * the value that should be used in the calls that take a `repository` parameter.
     *
     * @param almSettingDto the global `AlmSettingDto` containing the global configuration for this ALM
     * @param projectAlmSettingDto the `ProjectAlmSettingDto` assigned to the current project
     * @return the resolved repository name.
     */
    String resolveRepository(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto);

}
