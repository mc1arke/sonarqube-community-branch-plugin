/*
 * Copyright (C) 2019 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;

import com.github.mc1arke.sonarqube.plugin.SonarqubeCompatibility;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;

public interface PostProjectAnalysisTaskCompatibility extends SonarqubeCompatibility {

    interface PostProjectAnalysisTaskCompatibilityMajor8
            extends PostProjectAnalysisTaskCompatibility, SonarqubeCompatibility.Major8 {

        interface PostProjectAnalysisTaskCompatibilityMinor0
                extends PostProjectAnalysisTaskCompatibilityMajor8, SonarqubeCompatibility.Major8.Minor0 {

            String getDescription();

            /**
             * Setup and perform any post analysis task (e.g. pull request decoration)
             *
             * @param projectAnalysis the details of the task that was performed (quality gate, branches etc)
             *                        <p>
             *                        See {@link PostProjectAnalysisTask#finished(PostProjectAnalysisTask.ProjectAnalysis)} for implementation
             *                        details
             * @deprecated We can't introduce the new finished(Context context) method until we compile against
             * SonarQube 8, since the Context class was only introduced at that point. Once our base is SonarQube 8 or
             * above, we'll replace the implementation of this method with the non-deprecated equivalent. Marking this
             * as Deprecated in the meantime so it's clear work will be needed here.
             */
            @Deprecated
            void finished(PostProjectAnalysisTask.ProjectAnalysis projectAnalysis);

        }

    }

}
