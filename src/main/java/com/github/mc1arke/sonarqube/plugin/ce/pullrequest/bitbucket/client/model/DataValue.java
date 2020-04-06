/*
 * Copyright (C) 2020 Mathias Åhsberg
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.math.BigDecimal;

public abstract class DataValue implements Serializable {

    public static class Link extends DataValue {
        private final String linktext;
        private final String href;

        @JsonCreator
        public Link(@JsonProperty("linktext") String linktext, @JsonProperty("href") String href) {
            this.linktext = linktext;
            this.href = href;
        }

        public String getLinktext() {
            return linktext;
        }

        public String getHref() {
            return href;
        }
    }

    public static class Text extends DataValue {
        private final String value;

        @JsonCreator
        public Text(@JsonProperty("value") String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public static class Percentage extends DataValue {

        private final BigDecimal value;

        @JsonCreator
        public Percentage(@JsonProperty("value") BigDecimal value) {
            this.value = value;
        }

        @JsonValue
        public BigDecimal getValue() {
            return value;
        }
    }
}