/*
 * Copyright (C) 2019-2022 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownFormatterFactoryTest {

    @Test
    void testDocumentFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("# Heading 1" + System.lineSeparator() +
                "Text" + System.lineSeparator()  + System.lineSeparator() +
                "- List Item 1"  + System.lineSeparator() +
                "- [Link](url)"  + System.lineSeparator() + System.lineSeparator() +
                "![alt](url)", testCase.documentFormatter().format(new Document(new Heading(1, new Text("Heading 1")), new Paragraph(new Text("Text")), new List(List.Style.BULLET, new ListItem(new Text("List Item 1")), new ListItem(new Link("url", new Text("Link")))), new Image("alt", "url"))));
    }

    @Test
    void testHeadingFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("## Text" + System.lineSeparator(),
                     testCase.headingFormatter().format(new Heading(2, new Text("Text"))));
    }

    @Test
    void testImageFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("![alt](source)", testCase.imageFormatter().format(new Image("alt", "source")));
    }

    @Test
    void testLinkFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("[Text](http://url)", testCase.linkFormatter().format(new Link("http://url", new Text("Text"))));
        assertEquals("[http://url](http://url)", testCase.linkFormatter().format(new Link("http://url")));
    }

    @Test
    void testListFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("- List Item 1" + System.lineSeparator() + System.lineSeparator(), testCase.listFormatter()
                .format(new List(List.Style.BULLET, new ListItem(new Text("List Item 1")))));
    }

    @Test
    void testListFormatterInvalidType() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        List list = new List(null, new ListItem(new Text("List Item 1")));
        Formatter<List> listFormatter = testCase.listFormatter();
        assertThatThrownBy(
                () -> listFormatter.format(list))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown list type: null");
    }

    @Test
    void testListItemFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("Text", testCase.listItemFormatter().format(new ListItem(new Text("Text"))));
    }

    @Test
    void testParagraphFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("Text" + System.lineSeparator() + System.lineSeparator(),
                     testCase.paragraphFormatter().format(new Paragraph(new Text("Text"))));
    }

    @Test
    void testTextFormatter() {
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("Text", testCase.textFormatter().format(new Text("Text")));
    }

    @Test
    void testContentTextFormatterEscapedHtml(){
        MarkdownFormatterFactory testCase = new MarkdownFormatterFactory();
        assertEquals("&lt;p&gt; no html allowed  ", testCase.textFormatter().format(new Text("<p> no html allowed  ")));
        assertEquals("no html &lt;p&gt; allowed", testCase.textFormatter().format(new Text("no html <p> allowed")));
        assertEquals("&lt;/i&gt;no html &lt;p&gt; allowed&lt;i&gt;", testCase.textFormatter().format(new Text("</i>no html <p> allowed<i>")));
    }
}
