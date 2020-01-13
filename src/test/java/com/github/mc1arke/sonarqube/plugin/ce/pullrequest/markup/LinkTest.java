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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LinkTest {

    @Test
    public void correctParametersReturned() {
        Link image = new Link("url", new Text("Text"));
        assertThat(image).extracting(Link::getUrl).isEqualTo("url");
    }

    @Test
    public void testIsValidChildInvalidChild() {
        assertFalse(new Link("url", new Text("Text")).isValidChild(new Paragraph()));
    }

    @Test
    public void testIsValidChildValidChildText() {
        assertTrue(new Link("url", new Text("Text")).isValidChild(new Text("")));
    }

}