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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class MarkdownFormatterFactoryTest {

    @Test
    public void testDocumentFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("Text", testCase.documentFormatter().format(new Document(new Text("Text")), testCase));
    }

    @Test
    public void testHeadingFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("## Text" + System.lineSeparator(),
                     testCase.headingFormatter().format(new Heading(2, new Text("Text")), testCase));
    }

    @Test
    public void testImageFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("![alt](source)", testCase.imageFormatter().format(new Image("alt", "source"), testCase));
    }

    @Test
    public void testLinkFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("[Text](http://url)", testCase.linkFormatter().format(new Link("http://url", new Text("Text")), testCase));
        assertEquals("[http://url](http://url)", testCase.linkFormatter().format(new Link("http://url"), testCase));
    }

    @Test
    public void testListFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("- List Item 1" + System.lineSeparator() + System.lineSeparator(), testCase.listFormatter()
                .format(new List(List.Style.BULLET, new ListItem(new Text("List Item 1"))), testCase));
    }

    @Test
    public void testListFormatterInvalidType() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertThatThrownBy(
                () -> testCase.listFormatter().format(new List(null, new ListItem(new Text("List Item 1"))), testCase))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Unknown list type: null");
    }

    @Test
    public void testListItemFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("Text", testCase.listItemFormatter().format(new ListItem(new Text("Text")), testCase));
    }

    @Test
    public void testParagraphFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("Text" + System.lineSeparator() + System.lineSeparator(),
                     testCase.paragraphFormatter().format(new Paragraph(new Text("Text")), testCase));
    }

    @Test
    public void testTextFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("Text", testCase.textFormatter().format(new Text("Text"), testCase));
    }
}