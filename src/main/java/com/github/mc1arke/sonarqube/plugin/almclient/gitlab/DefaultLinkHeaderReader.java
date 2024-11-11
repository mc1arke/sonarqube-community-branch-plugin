/*
 * Copyright (C) 2020-2024 Michael Clarke
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

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import java.util.Arrays;
import java.util.Optional;

@ComputeEngineSide
@ServerSide
public class DefaultLinkHeaderReader implements LinkHeaderReader {

    @Override
    public Optional<String> findNextLink(String linkHeader) {
        return Optional.ofNullable(linkHeader)
                .flatMap(l -> Arrays.stream(l.split(","))
                .map(i -> i.split(";"))
                .filter(i -> i.length > 1)
                .filter(i -> {
                    String[] relParts = i[1].trim().split("=");

                    if (relParts.length < 2) {
                        return false;
                    }

                    if (!"rel".equals(relParts[0])) {
                        return false;
                    }

                    return "next".equals(relParts[1]) || "\"next\"".equals(relParts[1]);
                })
                .map(i -> i[0])
                .map(String::trim)
                .filter(i -> i.startsWith("<") && i.endsWith(">"))
                .map(i -> i.substring(1, i.length() - 1))
                .findFirst());
    }
}
