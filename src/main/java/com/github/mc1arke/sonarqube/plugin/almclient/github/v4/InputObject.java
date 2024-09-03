/*
 * Copyright (c) 2018-2024 American Express Travel Related Services Company, Inc, Michael Clarke
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.mc1arke.sonarqube.plugin.almclient.github.v4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InputObject<T> {
    private final Map<String, T> map;

    InputObject(Builder<T> builder) {
        this.map = builder.map;
    }

    String getMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (stringBuilder.length() != 1) stringBuilder.append(",");
            stringBuilder.append(formatGraphQLParameter(entry.getKey(), entry.getValue()));
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return getMessage();
    }

    public Map<String, T> getMap() {
        return this.map;
    }

    public static class Builder<T> {

        private final Map<String, T> map = new HashMap<>();

        public Builder<T> put(String key, T value) {
            this.map.put(key, value);
            return this;
        }

        public InputObject<T> build() {
            return new InputObject<>(this);
        }
    }


    private static <T> String formatGraphQLParameter(String key, T value) {
        StringBuilder stringBuilder = new StringBuilder();
        Pattern pattern = Pattern.compile("^\\$");
        Matcher matcher = pattern.matcher("" + value);
        if (value instanceof String && ((String) value).contains("\n")) {
            stringBuilder.append(key).append(":\"\"\"").append(value).append("\"\"\"");
        } else if (value instanceof String && !matcher.find()) {
            stringBuilder.append(key).append(":\"").append(value).append("\"");
        } else if (value instanceof List) {
            stringBuilder.append(key).append(":").append(formatGraphQLArgumentList((List<?>)value));
        } else if (value instanceof com.github.mc1arke.sonarqube.plugin.almclient.github.v4.InputObject) {
            stringBuilder.append(key).append(":").append(((com.github.mc1arke.sonarqube.plugin.almclient.github.v4.InputObject<?>)value).getMessage());
        } else {
            stringBuilder.append(key).append(":").append(value);
        }

        return stringBuilder.toString();
    }

    private static String formatGraphQLArgumentList(List<?> values) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        Pattern p = Pattern.compile("^\\$");
        for (Object value: values) {
            if (stringBuilder.length() != 1) stringBuilder.append(",");
            Matcher m = p.matcher("" + value);
            if (value instanceof String && !m.find()) {
                stringBuilder.append("\"").append(value).append("\"");
            } else if (value instanceof com.github.mc1arke.sonarqube.plugin.almclient.github.v4.InputObject) {
                stringBuilder.append(((InputObject<?>) value).getMessage());
            } else {
                stringBuilder.append(value);
            }
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

}
