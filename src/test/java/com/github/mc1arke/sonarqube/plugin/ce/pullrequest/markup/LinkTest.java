/*
 * Copyright (C) 2020 Markus Heberling
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LinkTest {

    private final String testUrl = "url";
    private final Link link = new Link(testUrl, new Text("Text"));

    @Nested
    class Constructor {
        @Test
        void shouldCorrectlyAssignParameters() {
            // given link under test

            // when
            String url = link.getUrl();

            // then
            assertThat(url).isEqualTo(testUrl);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class IsValid {

        @MethodSource("childNodes")
        @ParameterizedTest(name = "child type: {0} => {1}")
        void shouldReturnTrueForSupportedChildren(Node child, boolean expectedResult) {
            // given link under test and parameters

            // when
            boolean validChild = link.isValidChild(child);

            // then
            assertThat(validChild).isEqualTo(expectedResult);
        }

        private Stream<Arguments> childNodes() {
            return Stream.of(
                    arguments(named(Text.class.getSimpleName(), new Text("")), true),
                    arguments(named(Image.class.getSimpleName(), new Image("alt", "source")), true),
                    arguments(named(Paragraph.class.getSimpleName(), new Paragraph()), false),
                    arguments(named("null", null), false)
            );
        }
    }
}
