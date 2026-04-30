package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HelpWithFeesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

import java.time.Clock;
import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter.toYesOrNo;

@Service
public class GenAppService {

    private final GenAppRepository genAppRepository;
    private final Clock utcClock;

    public GenAppService(GenAppRepository genAppRepository,
                         @Qualifier("utcClock") Clock utcClock) {
        this.genAppRepository = genAppRepository;
        this.utcClock = utcClock;
    }

    public GenAppEntity createGenAppEntity(CitizenGenAppRequest citizenCreateGenApp,
                                           PcsCaseEntity pcsCaseEntity,
                                           PartyEntity applicantParty) {

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(citizenCreateGenApp.getApplicationType())
            .party(applicantParty)
            .state(GenAppState.SUBMITTED)
            .clientReference(citizenCreateGenApp.getClientReference())
            .within14Days(citizenCreateGenApp.getWithin14Days())
            .needHwf(citizenCreateGenApp.getNeedHwf())
            .appliedForHwf(citizenCreateGenApp.getAppliedForHwf())
            .build();

        if (citizenCreateGenApp.getAppliedForHwf() == VerticalYesNo.YES
                && citizenCreateGenApp.getHwfReference() != null) {
            HelpWithFeesEntity helpWithFeesEntity = new HelpWithFeesEntity();
            helpWithFeesEntity.setHwfReference(citizenCreateGenApp.getHwfReference());
            genAppEntity.setHelpWithFeesEntity(helpWithFeesEntity);
        }

        genAppEntity.setOtherPartiesAgreed(citizenCreateGenApp.getOtherPartiesAgreed());
        if (citizenCreateGenApp.getOtherPartiesAgreed() == VerticalYesNo.NO) {
            genAppEntity.setWithoutNotice(citizenCreateGenApp.getWithoutNotice());
            if (citizenCreateGenApp.getWithoutNotice() == VerticalYesNo.YES) {
                genAppEntity.setWithoutNoticeReason(citizenCreateGenApp.getWithoutNoticeReason());
            }
        }

        genAppEntity.setLanguageUsed(citizenCreateGenApp.getLanguageUsed());
        genAppEntity.setApplicationSubmittedDate(LocalDateTime.now(utcClock));

        if (citizenCreateGenApp.getSotAccepted() != null) {
            StatementOfTruthEntity statementOfTruthEntity = StatementOfTruthEntity.builder()
                .accepted(toYesOrNo(citizenCreateGenApp.getSotAccepted()))
                .fullName(citizenCreateGenApp.getSotFullName())
                .completedDate(LocalDateTime.now(utcClock))
                .build();
            genAppEntity.setStatementOfTruth(statementOfTruthEntity);
        }

        pcsCaseEntity.addGenApp(genAppEntity);

        return genAppRepository.save(genAppEntity);
    }

}
