package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Builder
@Data
public class Party {

    @CCD(label = "Party ID", typeOverride = FieldType.Text, showCondition = NEVER_SHOW,
        access = DefaultAccess.class)
    private final UUID id;

    @CCD(label = "Forename",
        access = DefaultAccess.class)
    private String forename;

    @CCD(label = "Surname",
        access = DefaultAccess.class)
    private String surname;

    @CCD(label = "Is party active?",
        access = DefaultAccess.class)
    private final YesOrNo active;

    @JsonCreator
    public Party(UUID id, String forename, String surname, YesOrNo active) {
        this.id = id;
        this.forename = forename;
        this.surname = surname;
        this.active = active;
    }

    public Party(UUID uuid, String forename, String surname, boolean active) {
        this(uuid, forename, surname, YesOrNo.from(active));
    }

}
