package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

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
                                                .holidayLet("some holiday let details")
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
                            "noRentArrearsReasonForGrounds.holidayLet",
                            "waysToPay",
                            "caseLinks",
                            "claimGroundSummaries",
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
                            "enforcementOrder.rawWarrantDetails.selectedDefendants",
                            "enforcementOrder.rawWarrantDetails.vulnerablePeoplePresent",
                            "enforcementOrder.rawWarrantDetails.vulnerableAdultsChildren")
            .isEqualTo(existingCaseData);

        assertThat(mergedCaseData.getIntroductoryDemotedOrOtherGroundsForPossession()
                        .getOtherGroundDescription()).isEqualTo("some other ground description");
        assertThat(mergedCaseData.getApplicationWithClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(mergedCaseData.getClaimantType()).isEqualTo(claimantTypeList);
        assertThat(mergedCaseData.getNoRentArrearsReasonForGrounds().getHolidayLet())
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

    @Test
    void shouldClearAddressFieldsWhenPatchContainsAddress() throws Exception {
        //Given
        String baseJson = """
        {
          "party": {
            "address": {
              "AddressLine1": "Old Line 1",
              "AddressLine2": "Old Line 2",
              "AddressLine3": "Old Line 3",
              "PostTown": "Old Town",
              "County": "Old County",
              "PostCode": "OLD123",
              "Country": "Old Country"
            }
          }
        }
            """;

        String patchJson = """
            {
              "party": {
                "address": {
                  "AddressLine1": "New Line 1"
                }
              }
            }
            """;

        // When
        String mergedJson = underTest.mergeJson(baseJson, patchJson);
        JsonNode merged = objectMapper.readTree(mergedJson);
        JsonNode address = merged.at("/party/address");

        // Then: new field present
        assertThat(address.get("AddressLine1").asText()).isEqualTo("New Line 1");

        // All old fields removed
        assertThat(address.has("AddressLine2")).isFalse();
        assertThat(address.has("AddressLine3")).isFalse();
        assertThat(address.has("PostTown")).isFalse();
        assertThat(address.has("County")).isFalse();
        assertThat(address.has("PostCode")).isFalse();
        assertThat(address.has("Country")).isFalse();
    }

    @Test
    void shouldNotClearAddressIfPatchDoesNotContainAddress() throws Exception {
        //Given
        String baseJson = """
        {
          "party": {
            "address": {
              "AddressLine1": "Line 1",
              "PostCode": "ABC123"
            }
          }
        }
            """;

        String patchJson = """
            {
              "party": {
                "name": "John"
              }
            }
            """;

        // When
        String mergedJson = underTest.mergeJson(baseJson, patchJson);
        JsonNode merged = objectMapper.readTree(mergedJson);
        JsonNode address = merged.at("/party/address");

        // Then: address untouched
        assertThat(address.get("AddressLine1").asText()).isEqualTo("Line 1");
        assertThat(address.get("PostCode").asText()).isEqualTo("ABC123");
    }

}
