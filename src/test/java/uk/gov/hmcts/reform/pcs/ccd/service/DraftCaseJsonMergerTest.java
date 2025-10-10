package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnsubmittedCaseDataMixIn;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;

class DraftCaseJsonMergerTest {

    private ObjectMapper objectMapper;

    private DraftCaseJsonMerger underTest;

    @BeforeEach
    void setUp() {
        objectMapper = createObjectMapper();
        underTest = new DraftCaseJsonMerger(objectMapper);
    }

    @Test
    void shouldMergeJson() throws JsonProcessingException {
        // Given
        DynamicStringListElement privateLandlordElement = createListElement(ClaimantType.PRIVATE_LANDLORD);
        DynamicStringListElement communityLandlordElement = createListElement(ClaimantType.COMMUNITY_LANDLORD);

        List<DynamicStringListElement> listItems = List.of(privateLandlordElement, communityLandlordElement);

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .value(privateLandlordElement)
            .listItems(listItems)
            .build();

        PCSCase basePcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .arrearsJudgmentWanted(YesOrNo.YES)
            .claimantType(claimantTypeList)
            .build();

        String baseJson = objectMapper.writeValueAsString(basePcsCase);

        claimantTypeList = DynamicStringList.builder()
            .value(communityLandlordElement)
            .listItems(listItems)
            .build();

        PCSCase updatedPcsCase = PCSCase.builder()
            .arrearsJudgmentWanted(YesOrNo.NO)
            .otherGroundDescription("some other ground description")
            .welshUsed(VerticalYesNo.YES)
            .claimantType(claimantTypeList)
            .build();

        String patchJson = objectMapper.writeValueAsString(updatedPcsCase);


        // When
        String mergedJson = underTest.mergeJson(baseJson, patchJson);

        // Then
        String expectedJson = """
            {
               "legislativeCountry" : "England",
               "claimantType" : {
                 "value" : {
                   "code" : "COMMUNITY_LANDLORD",
                   "label" : "Registered community landlord"
                 },
                 "valueCode" : "COMMUNITY_LANDLORD",
                 "list_items" : [{
                   "code" : "PRIVATE_LANDLORD",
                   "label" : "Private landlord"
                 }, {
                   "code" : "COMMUNITY_LANDLORD",
                   "label" : "Registered community landlord"
                 } ]
               },
               "otherGroundDescription" : "some other ground description",
               "arrearsJudgmentWanted" : "No",
               "welshUsed" : "YES"
             }
            """;

        JSONAssert.assertEquals(expectedJson, mergedJson, JSONCompareMode.STRICT);
    }

    private DynamicStringListElement createListElement(ClaimantType value) {
        return DynamicStringListElement.builder().code(value.name()).label(value.getLabel()).build();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(PCSCase.class, UnsubmittedCaseDataMixIn.class);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModules(new ParameterNamesModule());
        return objectMapper;
    }

}
