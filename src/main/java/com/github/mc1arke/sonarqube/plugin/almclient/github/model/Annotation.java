/*
 * Copyright (C) 2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.model;

import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckAnnotationLevel;

public class Annotation {

    private final Integer line;
    private final String scmPath;
    private final CheckAnnotationLevel severity;
    private final String message;

    private Annotation(Builder builder) {
        line = builder.line;
        scmPath = builder.scmPath;
        severity = builder.severity;
        message = builder.message;
    }

    public Integer getLine() {
        return line;
    }

    public String getScmPath() {
        return scmPath;
    }

    public CheckAnnotationLevel getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer line;
        private String scmPath;
        private CheckAnnotationLevel severity;
        private String message;

        private Builder() {
            super();
        }

        public Builder withLine(Integer line) {
            this.line = line;
            return this;
        }

        public Builder withScmPath(String scmPath) {
            this.scmPath = scmPath;
            return this;
        }

        public Builder withSeverity(CheckAnnotationLevel severity) {
            this.severity = severity;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Annotation build() {
            return new Annotation(this);
        }
    }
}
