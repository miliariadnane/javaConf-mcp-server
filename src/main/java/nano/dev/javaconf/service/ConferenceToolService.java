package nano.dev.javaconf.service;

import lombok.extern.slf4j.Slf4j;
import nano.dev.javaconf.model.ConferenceInfo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ConferenceToolService {

    private final GitHubService gitHubService;
    private final MarkdownParsingService markdownParsingService;

    public ConferenceToolService(GitHubService gitHubService, MarkdownParsingService markdownParsingService) {
        this.gitHubService = gitHubService;
        this.markdownParsingService = markdownParsingService;
    }


    public record ToolRequest(String year) { }

    @Tool(name = "getJavaConferences",
          description = "Get information about Java conferences for a specific year (if specified and found in the source) or the current year by default. Parses data for all years found under H3 headings.")
    public List<ConferenceInfo> getJavaConferences(ToolRequest request) {
        final int finalTargetYear = determineTargetYear(request);

        try {
            String markdownContent = gitHubService.fetchMarkdownContent();

            if (!StringUtils.hasText(markdownContent)) {
                log.warn("Markdown content was null or empty after fetch, returning empty list.");
                return Collections.emptyList();
            }

            List<ConferenceInfo> allConferences = markdownParsingService.parse(markdownContent);
            log.debug("Parser returned {} conferences in total (before filtering).", allConferences.size());

            List<ConferenceInfo> filteredConferences = allConferences.stream()
                    .filter(conf -> conf.getYear() == finalTargetYear)
                    .toList();

            log.info("Returning {} conferences for year {}", filteredConferences.size(), finalTargetYear);
            return filteredConferences;

        } catch (Exception e) {
            log.error("Error processing conference info request for year {}", finalTargetYear, e);
            return Collections.emptyList();
        }
    }

    private int determineTargetYear(ToolRequest request) {
        int currentYear = Year.now().getValue();
        int targetYear;
        String requestedYear = request.year();

        if (StringUtils.hasText(requestedYear)) {
            try {
                targetYear = Integer.parseInt(requestedYear);
                log.info("Tool called: getJavaConferences attempting requested year: {}", targetYear);
            } catch (NumberFormatException e) {
                log.warn("Invalid year format '{}' requested, falling back to current year {}.", requestedYear, currentYear);
                targetYear = currentYear;
                log.info("Tool called: getJavaConferences falling back to current year: {}", targetYear);
            }
        } else {
            targetYear = currentYear;
            log.info("Tool called: getJavaConferences using current year: {} (no year specified)", targetYear);
        }
        return targetYear;
    }

}
