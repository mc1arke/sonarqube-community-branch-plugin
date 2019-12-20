/*
 * Copyright (C) 2019 Michael Clarke
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

public class HeadingTest {

    @Test
    public void correctParametersReturned() {
        Heading heading = new Heading(2);
        assertThat(heading.getLevel()).isEqualTo(2);
    }

    @Test
    public void checkInvalidAddChild() {
        assertFalse(new Heading(2).isValidChild(new Heading(2)));
    }

    @Test
    public void checkValidAddChildText() {
        assertTrue(new Heading(2).isValidChild(new Text("")));
    }

    @Test
    public void checkValidAddChildImage() {
        assertTrue(new Heading(2).isValidChild(new Image("alt", "src")));
    }

}