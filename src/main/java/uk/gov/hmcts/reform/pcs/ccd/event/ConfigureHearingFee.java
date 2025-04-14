package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.HearingFee;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import java.math.BigDecimal;

@Component
@AllArgsConstructor
public class ConfigureHearingFee implements CCDConfig<PCSCase, State, UserRole> {

    private final PCSCaseRepository pcsCaseRepository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("configureHearingFee", this::submit)
            .forState(State.Open)
            .name("Configure the hearing fee")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Hearing Fee")
                .optional(PCSCase::getHearingFee)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> payload) {
        var caseEntity = pcsCaseRepository.getReferenceById(payload.caseReference());
        var caseData = payload.caseData();

        HearingFee hearingFee = caseData.getHearingFee();
        if (hearingFee != null) {
            caseEntity.setFeeAmount(convertToPounds(hearingFee));
            caseEntity.setFeeDueDate(hearingFee.getDueDate());
            caseEntity.setFeePaid(toBoolean(hearingFee.getPaid()));
        } else {
            caseEntity.setFeeAmount(null);
            caseEntity.setFeeDueDate(null);
            caseEntity.setFeePaid(null);
        }

        pcsCaseRepository.save(caseEntity);
    }

    private static BigDecimal convertToPounds(HearingFee hearingFee) {
        return hearingFee.getAmount() != null ? new BigDecimal(hearingFee.getAmount()).movePointLeft(2) : null;
    }

    private static Boolean toBoolean(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }

}
