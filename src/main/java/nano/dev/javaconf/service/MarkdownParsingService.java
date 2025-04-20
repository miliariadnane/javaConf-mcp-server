package nano.dev.javaconf.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nano.dev.javaconf.model.ConferenceInfo;
import nano.dev.javaconf.service.util.MarkdownParsingHelper;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MarkdownParsingService {

    private final Parser parser;

    private static final int CONF_NAME_INDEX = 0;
    private static final int LOCATION_INDEX = 1;
    private static final int HYBRID_INDEX = 2;
    private static final int CFP_LINK_INDEX = 4;
    private static final int EXPECTED_COLUMNS = 5;

    public MarkdownParsingService() {
        this.parser = Parser.builder()
                .extensions(Collections.singletonList(TablesExtension.create()))
                .build();
    }

    public List<ConferenceInfo> parse(String markdownContent) {
        if (!StringUtils.hasText(markdownContent)) {
            log.warn("Markdown content is empty or null. Skipping parsing.");
            return Collections.emptyList();
        }

        List<ConferenceInfo> allConferences = new ArrayList<>();
        try {
            Node document = parser.parse(markdownContent);
            Node node = document.getFirstChild();

            while (node != null) {
                if (node instanceof Heading heading && heading.getLevel() == 3) {
                    String headingText = MarkdownParsingHelper.extractText(heading).trim();
                    log.debug("Found H3 heading: '{}'", headingText);

                    // Check if the heading text looks like a 4-digit year
                    if (headingText.matches("^\\d{4}$")) {
                        processYearSection(heading, headingText).ifPresent(allConferences::addAll);
                    } else {
                         log.debug("Skipping H3 heading '{}' as it doesn't look like a year.", headingText);
                    }
                }
                node = node.getNext();
            }

            log.info("Parsed {} conferences in total from Markdown.", allConferences.size());
            return allConferences;

        } catch (Exception e) {
            log.error("Failed to parse markdown content due to an unexpected error", e);
            return Collections.emptyList();
        }
    }

    // Helper to process a section identified by a year heading
    private Optional<List<ConferenceInfo>> processYearSection(Heading yearHeading, String year) {
        log.info("Processing potential section for year: {}", year);
        Node nextNode = yearHeading.getNext();

        if (nextNode instanceof TableBlock tableBlock) {
            log.debug("Found TableBlock immediately after H3 heading for year {}", year);
            Node tableChild = tableBlock.getFirstChild();
            while (tableChild != null && !(tableChild instanceof TableBody)) {
                tableChild = tableChild.getNext();
            }

            if (tableChild instanceof TableBody tableBody) {
                return Optional.of(processTableRows(tableBody, year));
            } else {
                log.warn("Could not find TableBody within TableBlock for year {}", year);
                return Optional.empty();
            }
        } else {
            log.warn("No TableBlock found immediately after H3 heading for year {}", year);
            return Optional.empty();
        }
    }

    // Helper to process all rows within a TableBody using Streams
    private List<ConferenceInfo> processTableRows(TableBody tableBody, String currentYear) {
        return MarkdownParsingHelper.streamChildren(tableBody, TableRow.class)
                .map(row -> mapRowToConferenceInfo(row, currentYear))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    // Helper to map a single TableRow to ConferenceInfo, handling errors
    private Optional<ConferenceInfo> mapRowToConferenceInfo(TableRow row, String currentYear) {
        List<String> cellTexts = new ArrayList<>();
        List<Node> cellNodes = new ArrayList<>();
        Node cellNode = row.getFirstChild();
        while (cellNode instanceof TableCell) {
            cellTexts.add(MarkdownParsingHelper.extractText(cellNode).trim());
            cellNodes.add(cellNode);
            cellNode = cellNode.getNext();
        }
        log.trace("Processing row for year {} with {} cells: {}", currentYear, cellTexts.size(), cellTexts);

        if (cellTexts.size() < EXPECTED_COLUMNS) {
            log.warn("Skipping row in year {}: Insufficient cells (expected >= {}, found {}). Cell texts: {}",
                     currentYear, EXPECTED_COLUMNS, cellTexts.size(), cellTexts);
            return Optional.empty();
        }

        try {
            String confName = cellTexts.get(CONF_NAME_INDEX);
            String primaryLink = MarkdownParsingHelper.extractFirstLinkUrlFromNode(cellNodes.get(CONF_NAME_INDEX));
            String cfpStatusText = "-".equals(cellTexts.get(CFP_LINK_INDEX)) ? null : cellTexts.get(CFP_LINK_INDEX);
            String extractedCfpLink = MarkdownParsingHelper.extractFirstLinkUrlFromNode(cellNodes.get(CFP_LINK_INDEX));

            ConferenceInfo conference = ConferenceInfo.builder()
                    .year(Integer.parseInt(currentYear))
                    .conferenceName(confName)
                    .location(cellTexts.get(LOCATION_INDEX))
                    .isHybrid(MarkdownParsingHelper.parseHybrid(cellTexts.get(HYBRID_INDEX)))
                    .cfpStatus(cfpStatusText)
                    .cfpLink(extractedCfpLink)
                    .link(primaryLink)
                    .country(MarkdownParsingHelper.extractCountryFromLocation(cellTexts.get(LOCATION_INDEX)))
                    .build();

            log.debug("Mapped conference: {} from year {}", confName, currentYear);
            return Optional.of(conference);

        } catch (NumberFormatException nfe) {
             log.warn("Skipping row in year {}: Could not parse currentYear '{}' as integer.", currentYear, currentYear);
             return Optional.empty();
        } catch (IndexOutOfBoundsException iobe) {
            log.warn("Skipping row in year {}: Error accessing cell index. Columns expected >= {}, found {}. Error: {}. Row cell texts: {}",
                     currentYear, EXPECTED_COLUMNS, cellTexts.size(), iobe.getMessage(), cellTexts);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Skipping row in year {} due to generic parsing error: {}. Row cell texts: {}", currentYear, e.getMessage(), cellTexts, e);
            return Optional.empty();
        }
    }
}
