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

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.issue.IssueVisitor;
import org.sonar.core.issue.DefaultIssue;

import java.util.*;

public class PostAnalysisIssueVisitor extends IssueVisitor {

    private static final Logger LOGGER = Loggers.get(PostAnalysisIssueVisitor.class);

    private final List<DefaultIssue> collectedIssues = new ArrayList<>();

    private final Map<DefaultIssue, String> collectedIssueMap = new HashMap<>();

    @Override
    public void onIssue(Component component, DefaultIssue defaultIssue) {
        if (Component.Type.FILE.equals(component.getType()))
        {
            LOGGER.info(component.toString());
            Optional<String> scmPath = component.getReportAttributes().getScmPath();
            scmPath.ifPresent(s -> collectedIssueMap.put(defaultIssue, s));
        }

        collectedIssues.add(defaultIssue);
    }

    public List<DefaultIssue> getIssues() {
        return Collections.unmodifiableList(collectedIssues);
    }

    public Map<DefaultIssue, String>  getIssueMap() {
        return Collections.unmodifiableMap(collectedIssueMap);
    }
}
