/*
 * Copyright (C) 2021-2024 Michael Clarke
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultLinkHeaderReaderTest {

    @ParameterizedTest(name = "{arguments}")
    @CsvSource({"Missing Header,",
            "Empty Header,''",
            "Missing rel Content,<http://url>; rel",
            "Invalid rel Content,<http://url>; abc=\"xyz\"",
            "Incorrect Header,dummy",
            "Missing URL Prefix,http://other>; rel=\"next\"",
            "Missing URL Postfix,<http://other; rel=\"next\"",
            "Missing URL Wrapper,http://other; rel=\"next\""})
    void findNextLinkEmpty(String name, String input) {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink(input)).isEmpty();
    }

    @Test
    void findNextLinkReturnsCorrectUrlOnMatch() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://other>; rel=\"next\"")).hasValue("http://other");
    }

    @Test
    void findNextLinkReturnsCorrectUrlOnMatchNoSpeechMarksAroundRel() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://other>; rel=next")).hasValue("http://other");
    }

    @Test
    void findNextLinkReturnsCorrectUrlOnMatchWithOtherRelEntries() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://other>; rel=\"last\", <http://other2>; rel=\"next\"")).hasValue("http://other2");
    }
}
