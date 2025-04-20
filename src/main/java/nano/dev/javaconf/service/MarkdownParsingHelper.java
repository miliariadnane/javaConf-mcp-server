package nano.dev.javaConf.service;

import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParsingHelper {

    private static final Logger log = LoggerFactory.getLogger(MarkdownParsingHelper.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("(\w+)\s+(\d{1,2})(-\d{1,2})?"); // e.g., "January 10-12" or "Feb 5"

    /**
     * Extracts the URL from a Link node or its text child.
     */
    public static String extractLinkUrl(Node cellNode) {
        if (cellNode instanceof Link) {
            return ((Link) cellNode).getDestination();
        } else if (cellNode.getFirstChild() instanceof Link) {
            // Sometimes the link is wrapped inside another node (e.g., StrongEmphasis)
            return ((Link) cellNode.getFirstChild()).getDestination();
        } else if (cellNode.getFirstChild() instanceof Text) {
            // Check if the text itself is a URL (less common but possible)
            String text = ((Text) cellNode.getFirstChild()).getLiteral().trim();
            if (text.startsWith("http://") || text.startsWith("https://")) {
                return text;
            }
        }
        // Look deeper for Link nodes if nested
        Node node = cellNode.getFirstChild();
        while (node != null) {
            if (node instanceof Link) {
                return ((Link) node).getDestination();
            }
            // Recursively check children if necessary, though CommonMark usually flattens
            if (node.getFirstChild() != null) {
                String nestedUrl = extractLinkUrl(node); // Simplified recursion for example
                if (nestedUrl != null) {
                    return nestedUrl;
                }
            }
            node = node.getNext();
        }
        log.trace("Could not extract URL from node: {}", cellNode);
        return null;
    }

    /**
     * Extracts and parses the conference date string.
     */
    public static String extractConferenceDate(Node cellNode, int year) {
        String dateString = cellNode.getFirstChild() instanceof Text ? ((Text) cellNode.getFirstChild()).getLiteral().trim() : "N/A";
        return parseDate(dateString, year);
    }

    /**
     * Parses the date string into a standard format (e.g., "YYYY-MM-DD").
     * Handles formats like "Month DD-DD" or "Month DD".
     */
    private static String parseDate(String dateString, int year) {
        Matcher matcher = DATE_PATTERN.matcher(dateString);
        if (matcher.find()) {
            try {
                String monthStr = matcher.group(1);
                String dayStr = matcher.group(2);
                // Simple month abbreviation handling
                if (monthStr.length() > 3 && !monthStr.equalsIgnoreCase("sept")) {
                    monthStr = monthStr.substring(0, 3);
                } else if (monthStr.equalsIgnoreCase("sept")) {
                    monthStr = "Sep"; // Handle September abbreviation specifically
                }
                // Use DateTimeFormatter for robust month parsing
                DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
                Month month = Month.from(monthFormatter.parse(monthStr));
                int day = Integer.parseInt(dayStr);
                LocalDate startDate = LocalDate.of(year, month, day);
                return startDate.format(DateTimeFormatter.ISO_DATE); // Format as YYYY-MM-DD
            } catch (DateTimeParseException | NumberFormatException e) {
                log.warn("Could not parse date string: '{}' for year {}. Error: {}", dateString, year, e.getMessage());
                return dateString; // Return original string if parsing fails
            }
        }
        // Handle TBD or other non-standard formats
        if (dateString.equalsIgnoreCase("TBD")) {
            return "TBD";
        }
        log.warn("Date string '{}' did not match expected pattern for year {}", dateString, year);
        return dateString; // Return original string if no match
    }


    /**
     * Parses the hybrid status string. Returns true for "yes" or "hybrid", false otherwise.
     */
    public static boolean parseHybrid(Node cellNode) {
        if (cellNode != null && cellNode.getFirstChild() instanceof Text) {
            String hybridStatus = ((Text) cellNode.getFirstChild()).getLiteral().trim().toLowerCase();
            // Consider "yes" or "hybrid" as true, explicitly treat "no" as false, otherwise false.
            return hybridStatus.equals("yes") || hybridStatus.equals("hybrid");
        }
        return false; // Default to false if node is null or not Text
    }

} 