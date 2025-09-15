package uk.gov.hmcts.reform.pcs.ccd.event;

import de.cronn.reflection.util.TypedPropertyGetter;
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
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantsTableService;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;

@Component
@Slf4j
@AllArgsConstructor
public class AddDefendantsimplements implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final DefendantsTableService defendantsTableService;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        var eventBuilder = configBuilder
            .decentralisedEvent("addDefendants", this::submit)
            .forStates(CASE_ISSUED)
            .name("Add defendants")
            .description("Add up to " + DefendantConstants.MAX_NUMBER_OF_DEFENDANTS + " additional defendants")
            .grant(Permission.CRU, PCS_CASE_WORKER)
            .showSummary()
            .fields();
        addDefendantPages(eventBuilder);
        eventBuilder.done();
    }

    private void addDefendantPages(FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder
            <PCSCase, UserRole, State>> event) {
        for (int i = 1; i <= DefendantConstants.MAX_NUMBER_OF_DEFENDANTS; i++) {

            //Defendant details page
            var defendantPage = event.page("AddDefendant" + i);
            if (i > 1) {
                defendantPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
            }
            defendantPage.pageLabel("Defendant ")
                .mandatory(getTempDefField(i));

            //Add Another / Summary page
            var addAnotherPage = event.page("AddAnotherDefendant" + i);
            if (i > 1) {
                addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
            }
                   addAnotherPage.pageLabel("Defendant List")
                           .label("defTable" + i, buildDefendantsSummaryTable(i));
            if (i != DefendantConstants.MAX_NUMBER_OF_DEFENDANTS) {
                addAnotherPage.mandatory(getAddAnotherField(i));
            }

        }
    }

    private TypedPropertyGetter<PCSCase, DefendantDetails> getTempDefField(int i) {
        switch (i) {
            case 1:  return PCSCase::getDefendant1;
            case 2:  return PCSCase::getDefendant2;
            case 3:  return PCSCase::getDefendant3;
            case 4:  return PCSCase::getDefendant4;
            case 5:  return PCSCase::getDefendant5;
            case 6:  return PCSCase::getDefendant6;
            case 7:  return PCSCase::getDefendant7;
            case 8:  return PCSCase::getDefendant8;
            case 9:  return PCSCase::getDefendant9;
            case 10: return PCSCase::getDefendant10;
            case 11: return PCSCase::getDefendant11;
            case 12: return PCSCase::getDefendant12;
            case 13: return PCSCase::getDefendant13;
            case 14: return PCSCase::getDefendant14;
            case 15: return PCSCase::getDefendant15;
            case 16: return PCSCase::getDefendant16;
            case 17: return PCSCase::getDefendant17;
            case 18: return PCSCase::getDefendant18;
            case 19: return PCSCase::getDefendant19;
            case 20: return PCSCase::getDefendant20;
            case 21: return PCSCase::getDefendant21;
            case 22: return PCSCase::getDefendant22;
            case 23: return PCSCase::getDefendant23;
            case 24: return PCSCase::getDefendant24;
            case 25: return PCSCase::getDefendant25;
            default: throw new IllegalArgumentException("Invalid defendant index: " + i);
        }
    }

    private TypedPropertyGetter<PCSCase, ?> getAddAnotherField(int i) {
        switch (i) {
            case 1:  return PCSCase::getAddAnotherDefendant1;
            case 2:  return PCSCase::getAddAnotherDefendant2;
            case 3:  return PCSCase::getAddAnotherDefendant3;
            case 4:  return PCSCase::getAddAnotherDefendant4;
            case 5:  return PCSCase::getAddAnotherDefendant5;
            case 6:  return PCSCase::getAddAnotherDefendant6;
            case 7:  return PCSCase::getAddAnotherDefendant7;
            case 8:  return PCSCase::getAddAnotherDefendant8;
            case 9:  return PCSCase::getAddAnotherDefendant9;
            case 10: return PCSCase::getAddAnotherDefendant10;
            case 11: return PCSCase::getAddAnotherDefendant11;
            case 12: return PCSCase::getAddAnotherDefendant12;
            case 13: return PCSCase::getAddAnotherDefendant13;
            case 14: return PCSCase::getAddAnotherDefendant14;
            case 15: return PCSCase::getAddAnotherDefendant15;
            case 16: return PCSCase::getAddAnotherDefendant16;
            case 17: return PCSCase::getAddAnotherDefendant17;
            case 18: return PCSCase::getAddAnotherDefendant18;
            case 19: return PCSCase::getAddAnotherDefendant19;
            case 20: return PCSCase::getAddAnotherDefendant20;
            case 21: return PCSCase::getAddAnotherDefendant21;
            case 22: return PCSCase::getAddAnotherDefendant22;
            case 23: return PCSCase::getAddAnotherDefendant23;
            case 24: return PCSCase::getAddAnotherDefendant24;
            case 25: return PCSCase::getAddAnotherDefendant25;
            default: throw new IllegalArgumentException("Invalid add-another index: " + i);
        }
    }

           private String buildDefendantsSummaryTable(int upToDefendant) {
               StringBuilder htmlTable = new StringBuilder();
               htmlTable.append("""
                   <h2> Defendants </h2><br>
                   <table>
                     <thead>
                       <tr>
                         <th>Defendant</th>
                         <th>Defendant <br> name</th>
                         <th>Defendant <br> correspondence address</th>
                         <th>Defendant <br> email address</th>
                       </tr>
                     </thead>
                     <tbody>
                       """);

               for (int i = 1; i <= upToDefendant; i++) {
                   htmlTable.append("<tr><td>Defendant ")
                       .append(i)
                       .append("</td><td>${defendant")
                       .append(i)
                       .append(".firstName} ${defendant")
                       .append(i)
                       .append(".lastName}</td><td>")
                       .append("Address details will be displayed here")
                       .append("</td><td>${defendant")
                       .append(i)
                       .append(".email}</td></tr>");
               }

               htmlTable.append("""
                     </tbody>
                   </table>
                       """);

               return htmlTable.toString();
           }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        List<ListValue<DefendantDetails>> finalDefendants = new ArrayList<>();
        
        // Add all defendants that have data
        for (int i = 1; i <= DefendantConstants.MAX_NUMBER_OF_DEFENDANTS; i++) {
            DefendantDetails defendant = getDefendantByIndex(pcsCase, i);
            if (defendant != null) {
                finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), defendant));
            }
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

    /**
     * Mid-event callback to populate the defendants table HTML dynamically
     */
    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        // Build the table with current defendant data
        defendantsTableService.populateDefendantsTableHtml(caseData);
        
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }


}

