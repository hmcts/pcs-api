package uk.gov.hmcts.reform.pcs.controllers;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class CaseCreationHelper {

    static final String ACCESS_CODE = "ABC123XYZ789";

    private PcsCaseRepository pcsCaseRepository;
    private PartyAccessCodeRepository partyAccessCodeRepository;

    PcsCaseEntity createTestCaseWithParty(long caseReference, UUID idamUserId, PartyRole partyRole) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        ClaimEntity claimEntity = ClaimEntity.builder()
            .claimCosts(VerticalYesNo.NO)
            .build();

        caseEntity.addClaim(claimEntity);

        PartyEntity defendant = new PartyEntity();
        defendant.setIdamId(idamUserId);
        defendant.setFirstName("John");
        defendant.setLastName("Doe");

        caseEntity.addParty(defendant);
        claimEntity.addParty(defendant, partyRole);

        return pcsCaseRepository.save(caseEntity);
    }

    PcsCaseEntity createTestCaseWithMultipleDefendants(
        long caseReference, UUID firstIdamUserId, UUID secondIdamUserId) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        ClaimEntity claimEntity = ClaimEntity.builder()
            .claimCosts(VerticalYesNo.NO)
            .build();

        caseEntity.addClaim(claimEntity);

        PartyEntity defendant1 = new PartyEntity();
        defendant1.setIdamId(firstIdamUserId);
        defendant1.setFirstName("John");
        defendant1.setLastName("Doe");

        PartyEntity defendant2 = new PartyEntity();
        defendant2.setIdamId(secondIdamUserId);
        defendant2.setFirstName("Jane");
        defendant2.setLastName("Smith");

        caseEntity.addParty(defendant1);
        caseEntity.addParty(defendant2);

        claimEntity.addParty(defendant1, PartyRole.DEFENDANT);
        claimEntity.addParty(defendant2, PartyRole.DEFENDANT);

        return pcsCaseRepository.save(caseEntity);
    }

    String createPartyAccessCode(PcsCaseEntity caseEntity, UUID partyId) {
        PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
            .partyId(partyId)
            .pcsCase(caseEntity)
            .code(ACCESS_CODE)
            .role(PartyRole.DEFENDANT)
            .build();

        partyAccessCodeRepository.save(pac);
        return ACCESS_CODE;
    }

    List<PartyEntity> getDefendants(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims()
            .getFirst()
            .getClaimParties()
            .stream()
            .filter(claimPartyEntity -> claimPartyEntity.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();
    }

}
