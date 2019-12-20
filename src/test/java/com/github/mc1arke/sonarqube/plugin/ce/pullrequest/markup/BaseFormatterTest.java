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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BaseFormatterTest {


    @Test
    public void checkChildContentsDocument() {
        verify(checkFormatInvocation(new Document())).documentFormatter();
    }

    @Test
    public void checkChildContentsHeading() {
        verify(checkFormatInvocation(new Heading(1))).headingFormatter();
    }

    @Test
    public void checkChildContentImage() {
        verify(checkFormatInvocation(new Image("", ""))).imageFormatter();
    }

    @Test
    public void checkChildContentList() {
        verify(checkFormatInvocation(new List(List.Style.BULLET))).listFormatter();
    }

    @Test
    public void checkChildContentListItem() {
        verify(checkFormatInvocation(new ListItem())).listItemFormatter();
    }

    @Test
    public void checkChildContentParagraph() {
        verify(checkFormatInvocation(new Paragraph())).paragraphFormatter();
    }

    @Test
    public void checkChildContentText() {
        verify(checkFormatInvocation(new Text(""))).textFormatter();
    }

    @Test
    public void checkChildContentUnkownType() {
        Node node = mock(Node.class);
        assertThatThrownBy(() -> checkFormatInvocation(mock(Node.class)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown node type: " + node.getClass().getName());
    }


    private static FormatterFactory checkFormatInvocation(Node node) {
        BaseFormatterImpl baseFormatter = new BaseFormatterImpl();
        Formatter formatter = mock(Formatter.class);
        FormatterFactory formatterFactory = mock(FormatterFactory.class, invocation -> formatter);

        Node wrapperNode = mock(Node.class);
        doReturn(Collections.singletonList(node)).when(wrapperNode).getChildren();
        doReturn("dummy").when(formatter).format(eq(node), eq(formatterFactory));

        assertEquals("dummy", baseFormatter.format(wrapperNode, formatterFactory));
        verify(formatter).format(node, formatterFactory);
        return formatterFactory;
    }


    private static class BaseFormatterImpl extends BaseFormatter<Node> {

        @Override
        public String format(Node node, FormatterFactory formatterFactory) {
            return childContents(node, formatterFactory);
        }
    }

}