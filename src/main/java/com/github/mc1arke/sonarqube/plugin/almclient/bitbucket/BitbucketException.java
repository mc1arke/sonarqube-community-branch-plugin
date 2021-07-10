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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket;

import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.ErrorResponse;

import java.util.Optional;
import java.util.stream.Collectors;

public class BitbucketException extends RuntimeException {
    public static final int PAYLOAD_TOO_LARGE = 413;

    private final int code;
    private final ErrorResponse errors;

    BitbucketException(int code, ErrorResponse errors) {
        this.code = code;
        this.errors = errors;
    }

    public boolean isError(int code) {
        return this.code == code;
    }

    @Override
    public String getMessage() {
        return Optional.ofNullable(errors)
                .map(ErrorResponse::getErrors)
                .map(e -> e.stream()
                        .map(ErrorResponse.Error::getMessage)
                        .collect(Collectors.joining(System.lineSeparator())))
                .orElse(String.format("Bitbucket responded with an error status (%d)", code));
    }
}
