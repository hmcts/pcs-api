package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DraftCaseJsonMergerTest {

    private ObjectMapper objectMapper;

    private DraftCaseJsonMerger underTest;

    @BeforeEach
    void setUp() {
        // Use the real object mapper, (without the Mixin), to ensure that it is also configured correctly
        objectMapper = new JacksonConfiguration().draftCaseDataObjectMapper();
        objectMapper.addMixIn(PCSCase.class, null);

        underTest = new DraftCaseJsonMerger(objectMapper);
    }

    @Test
    void shouldKeepExistingFieldsWhenMerging() throws JsonProcessingException {
        // Given
        PCSCase existingCaseData = Instancio.create(PCSCase.class);
        existingCaseData.setApplicationWithClaim(VerticalYesNo.NO);
        String baseJson = objectMapper.writeValueAsString(existingCaseData);

        DynamicStringList claimantTypeList = createClaimantTypeList();

        IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOtherGroundsForPossession =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .otherGroundDescription("some other ground description")
                .build();

        PCSCase patchCaseData = PCSCase.builder()
            .introductoryDemotedOrOtherGroundsForPossession(introductoryDemotedOtherGroundsForPossession)
            .applicationWithClaim(VerticalYesNo.YES)
            .claimantType(claimantTypeList)
            .noRentArrearsReasonForGrounds(NoRentArrearsReasonForGrounds.builder()
                                                .holidayLetTextArea("some holiday let details")
                                                .build())
            .build();

        String patchJson = objectMapper.writeValueAsString(patchCaseData);

        // When
        String mergedJson = underTest.mergeJson(baseJson, patchJson);

        // Then
        PCSCase mergedCaseData = objectMapper.readValue(mergedJson, PCSCase.class);

        assertThat(mergedCaseData)
            .usingRecursiveComparison()
            .ignoringFields("introductoryDemotedOrOtherGroundsForPossession.otherGroundDescription",
                            "applicationWithClaim",
                            "claimantType",
                            "noRentArrearsReasonForGrounds.holidayLetTextArea",
                            "waysToPay",
                            "enforcementOrder.showChangeNameAddressPage",
                            "enforcementOrder.showPeopleWhoWillBeEvictedPage",
                            "enforcementOrder.showPeopleYouWantToEvictPage",
                            "enforcementOrder.warrantDetails.statementOfTruth.claimantDetails",
                            "enforcementOrder.warrantDetails.statementOfTruth.claimantDetails.agreementClaimant",
                            "enforcementOrder.warrantDetails.statementOfTruth.claimantDetails.fullNameClaimant",
                            "enforcementOrder.warrantDetails.statementOfTruth.claimantDetails.positionClaimant",
                            "enforcementOrder.warrantDetails.statementOfTruth.legalRepDetails",
                            "enforcementOrder.warrantDetails.statementOfTruth.legalRepDetails.agreementLegalRep",
                            "enforcementOrder.warrantDetails.statementOfTruth.legalRepDetails.fullNameLegalRep",
                            "enforcementOrder.warrantDetails.statementOfTruth.legalRepDetails.firmNameLegalRep",
                            "enforcementOrder.warrantDetails.statementOfTruth.legalRepDetails.positionLegalRep",
                            "enforcementOrder.warrantDetails.selectedDefendants")
            .isEqualTo(existingCaseData);

        assertThat(mergedCaseData.getIntroductoryDemotedOrOtherGroundsForPossession()
                        .getOtherGroundDescription()).isEqualTo("some other ground description");
        assertThat(mergedCaseData.getApplicationWithClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(mergedCaseData.getClaimantType()).isEqualTo(claimantTypeList);
        assertThat(mergedCaseData.getNoRentArrearsReasonForGrounds().getHolidayLetTextArea())
            .isEqualTo("some holiday let details");

    }

    private DynamicStringList createClaimantTypeList() {
        DynamicStringListElement privateLandlordElement = createListElement(ClaimantType.PRIVATE_LANDLORD);
        DynamicStringListElement communityLandlordElement = createListElement(ClaimantType.COMMUNITY_LANDLORD);

        List<DynamicStringListElement> listItems = List.of(privateLandlordElement, communityLandlordElement);

        return DynamicStringList.builder()
            .value(privateLandlordElement)
            .listItems(listItems)
            .build();
    }

    private DynamicStringListElement createListElement(ClaimantType value) {
        return DynamicStringListElement.builder().code(value.name()).label(value.getLabel()).build();
    }

}
