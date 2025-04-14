package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

@Component
@AllArgsConstructor
public class ConfigureDocuments implements CCDConfig<PCSCase, State, UserRole> {

    private final PCSCaseRepository pcsCaseRepository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("configureDocuments", this::submit)
            .forState(State.Open)
            .name("Configure case documents")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Documents")
                .optional(PCSCase::getDocumentsProvided)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> payload) {
        var caseEntity = pcsCaseRepository.getReferenceById(payload.caseReference());
        var caseData = payload.caseData();

        caseEntity.setDocumentsProvided(toBoolean(caseData.getDocumentsProvided()));

        pcsCaseRepository.save(caseEntity);
    }

    private static Boolean toBoolean(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

}
