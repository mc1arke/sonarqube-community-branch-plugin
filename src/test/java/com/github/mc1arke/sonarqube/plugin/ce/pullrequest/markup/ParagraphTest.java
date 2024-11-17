/*
 * Copyright (C) 2019-2024 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ParagraphTest {

    @Test
    void shouldNorAcceptParagraphAsValidChild() {
        assertThat(new Paragraph().isValidChild(new Paragraph())).isFalse();
    }

    @Test
    void shouldAcceptTextAsValidChild() {
        assertThat(new Paragraph().isValidChild(new Text(""))).isTrue();
    }

    @Test
    void shouldAcceptImageAsValidChild() {
        assertThat(new Paragraph().isValidChild(new Image("", ""))).isTrue();
    }

    @Test
    void shouldAcceptLinkAsValidChild() {
        assertThat(new Paragraph().isValidChild(new Link(""))).isTrue();
    }
}