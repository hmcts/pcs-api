package uk.gov.hmcts.reform.pcs.ccd.event;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

@Component
public class CreateClaim implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository repository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("getStarted", this::submit)
            .initialState(State.PreSubmission)
            .name("Get started")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("enterAddress", this::validateAddress)
            .label("whichAddress", """
                # What's the address of the property?
                """)
                .mandatory(PCSCase::getPropertyAddress)
            .page("chooseClaimType")
            .mandatory(PCSCase::getClaimTypes)
            .page("chooseClaimant")
            .mandatory(PCSCase::getClaimantType)
            .page("chooseEvictionType")
            .mandatory(PCSCase::getEvictionType);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> validateAddress(
        CaseDetails<PCSCase, State> d, CaseDetails<PCSCase, State> before) {
        var builder = AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(d.getData());
        List<DynamicListElement> claimantsOptionsList;

        if (d.getData().getPropertyAddress().getPostTown().toUpperCase().contains("LONDON")
        || d.getData().getPropertyAddress().getPostCode().toUpperCase().contains("BEDFORD")) {
            claimantsOptionsList = List.of(
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Possession claim")
                    .build(),
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Demotion of a tenancy")
                    .build(),
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Suspension of right to buy")
                    .build(),
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Relief from forfeiture")
                    .build()
                );
        } else {
            claimantsOptionsList = List.of(
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Accelerated possession claim")
                    .build(),
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Prohibited conduct standard contract order")
                    .build(),
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Relief from forfeiture")
                    .build()
            );
        }

        DynamicList dynamicList = DynamicList.builder()
            .listItems(claimantsOptionsList)
            .build();
        d.getData().setClaimTypes(dynamicList);









        List<DynamicListElement> evictionTypeOptionsList;

        if (d.getData().getPropertyAddress().getPostTown().toUpperCase().contains("LONDON")
            || d.getData().getPropertyAddress().getPostCode().toUpperCase().contains("BEDFORD")) {
            evictionTypeOptionsList = List.of(
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Evicting Tenants")
                    .build(),
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Evicting Trespassers")
                    .build()
            );
        } else {
            evictionTypeOptionsList = List.of(
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Evicting occupation contract holders")
                    .build(),
                DynamicListElement.builder()
                    .code(UUID.randomUUID())
                    .label("Evicting Trespassers")
                    .build()
            );
        }

        DynamicList dlist = DynamicList.builder()
            .listItems(evictionTypeOptionsList)
            .build();
        d.getData().setEvictionType(dlist);


        return builder.build();
    }

    public void submit(EventPayload<PCSCase, State> p) {
        var country = p.caseData().getPropertyAddress().getPostTown().toUpperCase().contains("LONDON")
            || p.caseData().getPropertyAddress().getPostCode().toUpperCase().contains("BEDFORD")
            ? "England"
            : "Wales";
        var c = PcsCase.builder()
            .reference(p.caseReference())
            .caseDescription("Possession claim for " + p.caseData().getPropertyAddress().getAddressLine1())
            .country(country)
            .build();
        repository.save(c);
    }
}
