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

import java.util.stream.IntStream;
import static com.google.common.html.HtmlEscapers.htmlEscaper;

public final class MarkdownFormatterFactory extends BaseFormatterFactory {

    @Override
    public Formatter<Document> documentFormatter() {
        return this::childContents;
    }

    @Override
    public Formatter<Heading> headingFormatter() {
        return node -> {
            StringBuilder output = new StringBuilder();
            IntStream.range(0, node.getLevel()).forEach(i -> output.append("#"));
            return output.append(" ").append(childContents(node)).append(System.lineSeparator())
                    .toString();
        };
    }

    @Override
    public Formatter<Image> imageFormatter() {
        return node -> String.format("![%s](%s)", node.getAltText(), node.getSource());
    }

    @Override
    public Formatter<Link> linkFormatter() {
        return node -> String.format("[%s](%s)", node.getChildren().isEmpty() ? node.getUrl() : childContents(node), node.getUrl());
    }

    @Override
    public Formatter<List> listFormatter() {
        return node -> {
            StringBuilder output = new StringBuilder();
            node.getChildren().forEach(i -> {
                if (node.getStyle() == List.Style.BULLET) {
                    output.append("- ").append(format(i));
                } else {
                    throw new IllegalArgumentException("Unknown list type: " + node.getStyle());
                }
                output.append(System.lineSeparator());
            });
            output.append(System.lineSeparator());
            return output.toString();
        };
    }

    @Override
    public Formatter<ListItem> listItemFormatter() {
        return this::childContents;
    }

    @Override
    public Formatter<Paragraph> paragraphFormatter() {
        return node -> childContents(node) + System.lineSeparator() + System.lineSeparator();
    }

    @Override
    public Formatter<Text> textFormatter() {
        return node -> htmlEscaper().escape(node.getContent());
    }
}
