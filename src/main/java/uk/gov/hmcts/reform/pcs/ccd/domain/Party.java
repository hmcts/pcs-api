package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.util.UUID;

@Builder
@Data
public class Party {

    @CCD(ignore = true)
    @JsonIgnore
    private final UUID id;

    @CCD(label = "Forename")
    private String forename;

    @CCD(label = "Surname")
    private String surname;

    @CCD(ignore = true)
    @JsonIgnore
    private final boolean active;

    public Party(UUID id, String forename, String surname, boolean active) {
        this.id = id;
        this.forename = forename;
        this.surname = surname;
        this.active = active;
    }

}
