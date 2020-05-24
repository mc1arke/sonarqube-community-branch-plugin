/*
 * Copyright (C) 2020 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultLinkHeaderReaderTest {

    @Test
    public void findNextLinkEmptyForNoHeader() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink(null)).isEmpty();
    }

    @Test
    public void findNextLinkEmptyForEmptyHeader() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("")).isEmpty();
    }

    @Test
    public void findNextLinkEmptyForMissingRelContent() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://url>; rel")).isEmpty();
    }

    @Test
    public void findNextLinkEmptyForInvalidRelContent() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://url>; abc=\"xyz\"")).isEmpty();
    }

    @Test
    public void findNextLinkEmptyForIncorrectHeader() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("dummy")).isEmpty();
    }

    @Test
    public void findNextLinkEmptyForMissingUrlPrefix() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("http://other>; rel=\"next\"")).isEmpty();
    }

    @Test
    public void findNextLinkEmptyForMissingUrlPostfix() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://other; rel=\"next\"")).isEmpty();
    }

    @Test
    public void findNextLinkEmptyForMissingUrlWrapper() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("http://other; rel=\"next\"")).isEmpty();
    }

    @Test
    public void findNextLinkReturnsCorrectUrlOnMatch() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://other>; rel=\"next\"")).hasValue("http://other");
    }

    @Test
    public void findNextLinkReturnsCorrectUrlOnMatchNoSpeechMarksAroundRel() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://other>; rel=next")).hasValue("http://other");
    }

    @Test
    public void findNextLinkReturnsCorrectUrlOnMatchWithOtherRelEntries() {
        DefaultLinkHeaderReader underTest = new DefaultLinkHeaderReader();
        assertThat(underTest.findNextLink("<http://other>; rel=\"last\", <http://other2>; rel=\"next\"")).hasValue("http://other2");
    }
}