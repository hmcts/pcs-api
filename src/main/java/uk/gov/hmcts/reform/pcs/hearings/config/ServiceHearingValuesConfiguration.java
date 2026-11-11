package uk.gov.hmcts.reform.pcs.hearings.config;

import lombok.Data;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.reform.pcs.hearings.model.HearingLocation;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.PanelRequirements;
import uk.gov.hmcts.reform.pcs.hearings.model.PartyDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;

@Data
@Component
@ConfigurationProperties("hmc-service-hearing-values")
public class ServiceHearingValuesConfiguration {
    private int numberOfDaysOffset;
    private String hmctsServiceId;
    private boolean caseAdditionalSecurityFlag;
    private boolean caseRestrictedFlag;
    private boolean autoListFlag;
    private boolean hearingInWelshFlag;
    private boolean privateHearingRequiredFlag;
    private boolean hearingIsLinkedFlag;
    private String locationType;
    private List<String> roleTypes;
    private String preferredHearingChannel;

    public ServiceHearingValues getDefaultValues(long caseReference) {
        List<PartyDetails> parties = List.of(
            PartyDetails.builder()
                .individualDetails(IndividualDetails.builder()
                    .preferredHearingChannel(preferredHearingChannel)
                    .build())
                .build()
        );

        return ServiceHearingValues.builder()
            .caseId(String.valueOf(caseReference))
            .hmctsServiceID(hmctsServiceId)
            .caseAdditionalSecurityFlag(caseAdditionalSecurityFlag)
            .caseRestrictedFlag(caseRestrictedFlag)
            .autoListFlag(autoListFlag)
            .hearingInWelshFlag(hearingInWelshFlag)
            .privateHearingRequiredFlag(privateHearingRequiredFlag)
            .hearingIsLinkedFlag(hearingIsLinkedFlag)
            .hearingLocations(List.of(
                HearingLocation.builder()
                    .locationType(locationType)
                    .build()
            ))
            .panelRequirements(PanelRequirements.builder()
                .roleType(roleTypes)
                .build())
            .parties(parties)
            .build();
    }
}
