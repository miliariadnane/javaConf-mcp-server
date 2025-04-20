package nano.dev.javaconf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate;
    private final String markdownUrl;

    public GitHubService(RestTemplateBuilder builder,
                         @Value("${github.markdown.url}") String markdownUrl
    ) {
        this.restTemplate = builder.build();
        this.markdownUrl = markdownUrl;
        log.info("GitHubService initialized to fetch from URL: {}", this.markdownUrl);
    }

    public String fetchMarkdownContent() {
        log.info("Fetching Java Conference Markdown from: {} using RestTemplate", markdownUrl);
        try {
            log.debug("Attempting to fetch Markdown content from {}", markdownUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(markdownUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String content = response.getBody();
                log.info("Successfully fetched Markdown content ({} bytes)", content != null ? content.length() : 0);
                return content;
            } else {
                log.error("Failed to fetch Markdown content. Status code: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error fetching Markdown content from {}: {}", markdownUrl, e.getMessage());
            return null;
        }
    }
}
