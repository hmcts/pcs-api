package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class ConfigureHearingDate implements CCDConfig<PCSCase, State, UserRole> {

    private final PCSCaseRepository pcsCaseRepository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("configureHearingDate", this::submit)
            .forState(State.Open)
            .name("Configure the hearing date")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Hearing Date")
                .optional(PCSCase::getHearingDate)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> payload) {
        var caseEntity = pcsCaseRepository.getReferenceById(payload.caseReference());
        var caseData = payload.caseData();

        LocalDateTime hearingDateTime = getHearingDateTime(caseData);
        caseEntity.setHearingDate(hearingDateTime);

        pcsCaseRepository.save(caseEntity);
    }

    private static LocalDateTime getHearingDateTime(PCSCase caseData) {
        return caseData.getHearingDate() != null ? caseData.getHearingDate().atTime(14, 30) : null;
    }

}
