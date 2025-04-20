package nano.dev.javaconf.service.util;

import org.commonmark.node.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class containing static helper methods for parsing specific elements
 * from commonmark-java Nodes, primarily for the Java Conference MCP server.
 */
public final class MarkdownParsingHelper {

    private MarkdownParsingHelper() {}

    public static <T extends Node> Stream<T> streamChildren(Node parent, Class<T> type) {
        List<T> children = new ArrayList<>();
        Node child = parent.getFirstChild();
        while (child != null) {
            if (type.isInstance(child)) {
                 children.add(type.cast(child));
             }
            child = child.getNext();
        }
        return children.stream();
    }


    /**
     * Extracts the destination URL of the first Link node found within the children of parentNode.
     * Uses a dedicated visitor for efficiency.
     * @param parentNode The node potentially containing a Link node.
     * @return The URL string if a link is found, otherwise null.
     */
    public static String extractFirstLinkUrlFromNode(Node parentNode) {
        if (parentNode == null) return null;
        UrlExtractorVisitor visitor = new UrlExtractorVisitor();
        parentNode.accept(visitor);
        return visitor.getFirstUrl();
    }

    /**
     * Helper visitor (inner class) to find the first Link node and get its destination URL.
     */
    private static class UrlExtractorVisitor extends AbstractVisitor {
        private String firstUrl = null;

        public String getFirstUrl() {
            return this.firstUrl;
        }

        @Override
        public void visit(Link link) {
            if (this.firstUrl == null) {
                this.firstUrl = link.getDestination();
            }
        }
    }

    /**
     * Extracts all visible text content from a node and its children.
     * Handles common inline formatting nodes.
     * @param node The node to extract text from.
     * @return The extracted text as a single string.
     */
    public static String extractText(Node node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        TextExtractorVisitor textVisitor = new TextExtractorVisitor(sb);
        node.accept(textVisitor);
        return sb.toString();
    }

    /**
     * Helper visitor (inner class) to extract all text content from a node and its children.
     */
    private static class TextExtractorVisitor extends AbstractVisitor {
         private final StringBuilder buffer;

        TextExtractorVisitor(StringBuilder buffer) {
            this.buffer = buffer;
        }

        @Override public void visit(Text text) { buffer.append(text.getLiteral()); }
        @Override public void visit(Code code) { buffer.append(code.getLiteral()); }
        @Override public void visit(Emphasis emphasis) { visitChildren(emphasis); }
        @Override public void visit(StrongEmphasis strongEmphasis) { visitChildren(strongEmphasis); }
        @Override public void visit(Link link) { visitChildren(link); }
    }

    /**
     * Parses the hybrid status text ("yes" or "hybrid", case-insensitive).
     * @param text The text from the table cell.
     * @return true if the text indicates hybrid, false otherwise.
     */
    public static Boolean parseHybrid(String text) {
        if (text == null) return false;
        String lowerCaseText = text.trim().toLowerCase();
        return lowerCaseText.equals("yes") || lowerCaseText.equals("hybrid");
    }

    /**
     * Extracts the country name from a location string (assumes "City, Country" format).
     * @param location The location string.
     * @return The country name if found, otherwise null.
     */
    public static String extractCountryFromLocation(String location) {
        if (location != null && location.contains(",")) {
            String[] parts = location.split(",");
            if (parts.length > 1) {
                return parts[parts.length - 1].trim();
            }
        }
        return null;
    }
}
