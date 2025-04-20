package nano.dev.javaconf.config;

import lombok.extern.slf4j.Slf4j;
import nano.dev.javaconf.service.ConferenceToolService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class McpToolConfiguration {

    /**
     * Defines the ConferenceToolService as a ToolCallback bean for the MCP server.
     * Spring AI MCP Server automatically discovers beans of type ToolCallback.
     */
    @Bean
    public List<ToolCallback> javaConferenceTool(ConferenceToolService conferenceToolService) {
        log.debug("Registering ConferenceToolService as ToolCallback bean via McpToolConfiguration.");
        return List.of(ToolCallbacks.from(conferenceToolService));
    }
}
