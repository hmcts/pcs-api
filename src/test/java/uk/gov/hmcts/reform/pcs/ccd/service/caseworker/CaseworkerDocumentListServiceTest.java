package uk.gov.hmcts.reform.pcs.ccd.service.caseworker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;

class CaseworkerDocumentListServiceTest {

    private static final UUID CLAIMANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEFENDANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String GEN_APP_ID = "33333333-3333-3333-3333-333333333333";

    private CaseworkerDocumentListService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseworkerDocumentListService(new PartyService(null, null));
    }

    @Test
    void shouldRetainSelectedRelatedSubmissionWhenListIsRebuilt() {
        DynamicStringList existingList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(GEN_APP_ID_PREFIX + ":" + GEN_APP_ID)
                .build())
            .build();

        DynamicStringList result = underTest.buildRelatedSubmissionsList(
            PcsCaseEntity.builder().build(),
            List.of(ListValue.<GeneralApplication>builder()
                .id(GEN_APP_ID)
                .value(GeneralApplication.builder()
                    .rank(1)
                    .state(GenAppState.GEN_APP_ISSUED)
                    .submittedOn(LocalDateTime.of(2026, 7, 1, 9, 30))
                    .build())
                .build()),
            List.of(),
            existingList
        );

        assertThat(result.getValue().getCode()).isEqualTo(GEN_APP_ID_PREFIX + ":" + GEN_APP_ID);
        assertThat(result.getValue().getLabel()).isEqualTo("Gen app GA1 - submitted 1 July 2026");
    }

    @Test
    void shouldRetainSelectedRelatedPartyWhenListIsRebuilt() {
        PcsCaseEntity pcsCaseEntity = caseWithParties();
        DynamicList existingList = DynamicList.builder()
            .value(DynamicListElement.builder()
                .code(DEFENDANT_ID)
                .build())
            .build();

        DynamicList result = underTest.buildRelatedPartyList(pcsCaseEntity, existingList);

        assertThat(result.getValue().getCode()).isEqualTo(DEFENDANT_ID);
        assertThat(result.getValue().getLabel()).isEqualTo("Daniel Defendant - Defendant 1");
    }

    @Test
    void shouldRetainSelectedDocumentTypeWhenListIsRebuilt() {
        DynamicStringList existingList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(CaseworkerDocumentType.WITNESS_STATEMENT.name())
                .build())
            .build();

        DynamicStringList result = underTest.buildDocumentTypeList(LegislativeCountry.ENGLAND, existingList);

        assertThat(result.getValue().getCode()).isEqualTo(CaseworkerDocumentType.WITNESS_STATEMENT.name());
        assertThat(result.getValue().getLabel()).isEqualTo(CaseworkerDocumentType.WITNESS_STATEMENT.getLabel());
    }

    private static PcsCaseEntity caseWithParties() {
        PartyEntity claimant = PartyEntity.builder()
            .id(CLAIMANT_ID)
            .firstName("Clara")
            .lastName("Claimant")
            .build();
        PartyEntity defendant = PartyEntity.builder()
            .id(DEFENDANT_ID)
            .firstName("Daniel")
            .lastName("Defendant")
            .build();
        ClaimEntity claim = ClaimEntity.builder().build();
        claim.addParty(claimant, PartyRole.CLAIMANT);
        claim.addParty(defendant, PartyRole.DEFENDANT);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        pcsCaseEntity.addClaim(claim);
        return pcsCaseEntity;
    }
}
