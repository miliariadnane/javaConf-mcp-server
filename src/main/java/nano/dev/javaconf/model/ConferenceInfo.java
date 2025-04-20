package nano.dev.javaconf.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class ConferenceInfo {
    private int year;
    private String conferenceName;
    private String location;
    private Boolean isHybrid;
    private String cfpStatus;
    private String cfpLink;
    private String link;
    private String country;
}
