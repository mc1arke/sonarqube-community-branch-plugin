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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.math.BigDecimal;

public interface DataValue extends Serializable {

    class Link implements DataValue {
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

    class CloudLink implements DataValue {
        private final String text;
        private final String href;

        @JsonCreator
        public CloudLink(@JsonProperty("text") String text, @JsonProperty("href") String href) {
            this.text = text;
            this.href = href;
        }

        public String getText() {
            return text;
        }

        public String getHref() {
            return href;
        }
    }

    class Text implements DataValue {
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

    class Percentage implements DataValue {

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
