/*
 * Copyright (C) 2019-2022 Michael Clarke
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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.issue.IssueVisitor;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbIssues;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PostAnalysisIssueVisitor extends IssueVisitor {

    private final List<ComponentIssue> collectedIssues = new ArrayList<>();

    @Override
    public void onIssue(Component component, DefaultIssue defaultIssue) {
        collectedIssues.add(new ComponentIssue(component, new LightIssue(defaultIssue)));
    }

    public List<ComponentIssue> getIssues() {
        return Collections.unmodifiableList(collectedIssues);
    }

    public static class ComponentIssue {

        private final Component component;
        private final LightIssue issue;

        ComponentIssue(Component component, LightIssue issue) {
            super();
            this.component = component;
            this.issue = issue;
        }

        public Component getComponent() {
            return component;
        }

        public LightIssue getIssue() {
            return issue;
        }

        public Optional<String> getScmPath() {
            if (Component.Type.FILE == component.getType()) {
                return component.getReportAttributes().getScmPath();
            }
            return Optional.empty();
        }
    }

    /**
     * A simple bean for holding the useful bits of a #{@link DefaultIssue}.
     * <br>
     * It presents a subset of the #{@link DefaultIssue} interface, hence the inconsistent getters names,
     * and CheckForNull annotations.
     */
    public static class LightIssue {

        private final Long effortInMinutes;
        private final String key;
        private final Integer line;
        private final String message;
        private final String resolution;
        private final String severity;
        private final String status;
        private final RuleType type;
        private final DbIssues.Locations locations;
        private final RuleKey ruleKey;

        LightIssue(DefaultIssue issue) {
            this.effortInMinutes = issue.effortInMinutes();
            this.key = issue.key();
            this.line = issue.getLine();
            this.message = issue.getMessage();

            this.resolution = issue.resolution();
            this.severity = issue.severity();
            this.status = issue.status();
            this.type = issue.type();
            this.locations = issue.getLocations();
            this.ruleKey = issue.getRuleKey();
        }

        @CheckForNull
        public Long effortInMinutes() {
            return effortInMinutes;
        }

        public String key() {
            return key;
        }

        @CheckForNull
        public Integer getLine() {
            return line;
        }

        @CheckForNull
        public String getMessage() {
            return message;
        }

        @CheckForNull
        public String resolution() {
            return resolution;
        }

        public String severity() {
            return severity;
        }

        public String getStatus() {
            return status;
        }

        public String status() {
            return status;
        }

        public RuleType type() {
            return type;
        }

        public DbIssues.Locations getLocations() {
            return locations;
        }

        public RuleKey getRuleKey() {
            return ruleKey; 
        }

        @Override
        public int hashCode() {
            return Objects.hash(effortInMinutes, key, line, message, resolution, severity, status, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LightIssue other = (LightIssue) obj;
            return Objects.equals(effortInMinutes, other.effortInMinutes)
                    && Objects.equals(key, other.key)
                    && Objects.equals(line, other.line)
                    && Objects.equals(message, other.message)
                    && Objects.equals(resolution, other.resolution)
                    && Objects.equals(severity, other.severity)
                    && Objects.equals(status, other.status)
                    && type == other.type;
        }

    }
}
