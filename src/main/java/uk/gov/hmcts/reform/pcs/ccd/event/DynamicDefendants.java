package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.DefendantConstants;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantsTableService;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.DynamicDefendantsPages;
import uk.gov.hmcts.reform.pcs.ccd.util.DefendantUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_FURTHER_CLAIM_DETAILS;

@Component
@Slf4j
@AllArgsConstructor
public class DynamicDefendants implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final DefendantsTableService defendantsTableService;
    private final DynamicDefendantsPages dynamicDefendantsPages;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        var eventBuilder = configBuilder
            .decentralisedEvent("dynamicDefendants", this::submit)
            .forStates(AWAITING_FURTHER_CLAIM_DETAILS)
            .name("Add defendants")
            .description("Add up to 25 defendants")
            .grant(Permission.CRU, PCS_CASE_WORKER)
            .showSummary()
            .fields();
        addDefendantPages(eventBuilder);
        eventBuilder.done();
    }

    private void addDefendantPages(FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder
            <PCSCase, UserRole, State>> event) {
        for (int i = 1; i <= DefendantConstants.MAX_NUMBER_OF_DEFENDANTS; i++) {

            // Defendant details page
            var defendantPage = event.page("DefendantDetails" + i);
            if (i > 1) {
                defendantPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"Yes\"");
            }
            defendantPage.pageLabel("Defendant " + i + " details")
                .mandatory(DefendantUtils.getTempDefField(i));

            // Add Another / Summary page
            var addAnotherPage = event.page("DefendantList" + i);
            if (i > 1) {
                addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"Yes\"");
            }
                   addAnotherPage.pageLabel("Defendant List")
                           .label("defTable" + i, dynamicDefendantsPages.buildDefendantsSummaryTable(i));
            if (i != DefendantConstants.MAX_NUMBER_OF_DEFENDANTS) {
                addAnotherPage.mandatory(DefendantUtils.getAddAnotherField(i));
            }

        }
    }



    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        List<ListValue<DefendantDetails>> finalDefendants = new ArrayList<>();
        
        // Set correspondence address to property address if addressSameAsPossession is YES
        setCorrespondenceAddressIfSameAsPossession(pcsCase);
        
        // Add all individual defendants to the final list
        if (pcsCase.getDefendant1() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant1()));
        }
        if (pcsCase.getDefendant2() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant2()));
        }
        if (pcsCase.getDefendant3() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant3()));
        }
        if (pcsCase.getDefendant4() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant4()));
        }
        if (pcsCase.getDefendant5() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant5()));
        }
        if (pcsCase.getDefendant6() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant6()));
        }
        if (pcsCase.getDefendant7() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant7()));
        }
        if (pcsCase.getDefendant8() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant8()));
        }
        if (pcsCase.getDefendant9() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant9()));
        }
        if (pcsCase.getDefendant10() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant10()));
        }
        if (pcsCase.getDefendant11() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant11()));
        }
        if (pcsCase.getDefendant12() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant12()));
        }
        if (pcsCase.getDefendant13() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant13()));
        }
        if (pcsCase.getDefendant14() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant14()));
        }
        if (pcsCase.getDefendant15() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant15()));
        }
        if (pcsCase.getDefendant16() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant16()));
        }
        if (pcsCase.getDefendant17() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant17()));
        }
        if (pcsCase.getDefendant18() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant18()));
        }
        if (pcsCase.getDefendant19() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant19()));
        }
        if (pcsCase.getDefendant20() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant20()));
        }
        if (pcsCase.getDefendant21() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant21()));
        }
        if (pcsCase.getDefendant22() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant22()));
        }
        if (pcsCase.getDefendant23() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant23()));
        }
        if (pcsCase.getDefendant24() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant24()));
        }
        if (pcsCase.getDefendant25() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant25()));
        }
        
        pcsCase.setDefendants(finalDefendants);
        
        // Populate the defendants table HTML with formatted data
        defendantsTableService.populateDefendantsTableHtml(pcsCase);
        
        long caseReference = eventPayload.caseReference();

        log.info("Caseworker updated case {} with {} defendants", caseReference, finalDefendants.size());

        pcsCaseService.patchCase(caseReference, pcsCase);
    }




    private DefendantDetails getDefendantByIndex(PCSCase pcsCase, int index) {
        return switch (index) {
            case 1 -> pcsCase.getDefendant1();
            case 2 -> pcsCase.getDefendant2();
            case 3 -> pcsCase.getDefendant3();
            case 4 -> pcsCase.getDefendant4();
            case 5 -> pcsCase.getDefendant5();
            case 6 -> pcsCase.getDefendant6();
            case 7 -> pcsCase.getDefendant7();
            case 8 -> pcsCase.getDefendant8();
            case 9 -> pcsCase.getDefendant9();
            case 10 -> pcsCase.getDefendant10();
            case 11 -> pcsCase.getDefendant11();
            case 12 -> pcsCase.getDefendant12();
            case 13 -> pcsCase.getDefendant13();
            case 14 -> pcsCase.getDefendant14();
            case 15 -> pcsCase.getDefendant15();
            case 16 -> pcsCase.getDefendant16();
            case 17 -> pcsCase.getDefendant17();
            case 18 -> pcsCase.getDefendant18();
            case 19 -> pcsCase.getDefendant19();
            case 20 -> pcsCase.getDefendant20();
            case 21 -> pcsCase.getDefendant21();
            case 22 -> pcsCase.getDefendant22();
            case 23 -> pcsCase.getDefendant23();
            case 24 -> pcsCase.getDefendant24();
            case 25 -> pcsCase.getDefendant25();
            default -> null;
        };
    }

    private void setCorrespondenceAddressIfSameAsPossession(PCSCase pcsCase) {
        for (int i = 1; i <= DefendantConstants.MAX_NUMBER_OF_DEFENDANTS; i++) {
            DefendantDetails defendant = getDefendantByIndex(pcsCase, i);
            if (defendant != null && defendant.getAddressSameAsPossession() == VerticalYesNo.YES) {
                defendant.setCorrespondenceAddress(pcsCase.getPropertyAddress());
            }
        }
    }
}
