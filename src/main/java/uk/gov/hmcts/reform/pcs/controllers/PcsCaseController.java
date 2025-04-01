package uk.gov.hmcts.reform.pcs.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.entity.Claim;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.ListValueUtils.wrapListItems;

@RestController
@RequestMapping("/pcs-case")
public class PcsCaseController {

    private final PcsCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;

    public PcsCaseController(PcsCaseRepository pcsCaseRepository,
                             PartyRepository partyRepository) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepository = partyRepository;
    }

    @GetMapping("/reference/{reference}")
    public PcsCase getCaseByReference(@PathVariable long reference) {
        PcsCase pcsCase = pcsCaseRepository.findDtoByCaseReference(reference)
            .orElseThrow(() -> new CaseNotFoundException("Case not found for " + reference));


        Map<YesOrNo, List<Party>> partiesByActiveFlag = partyRepository.findAllDtoByCaseReference(reference)
            .stream()
            .collect(Collectors.groupingBy(Party::getActive));

        pcsCase.setActiveParties(wrapListItems(partiesByActiveFlag.get(YesOrNo.YES)));
        pcsCase.setInactiveParties(wrapListItems(partiesByActiveFlag.get(YesOrNo.NO)));

        return pcsCase;
    }

    @PostMapping("/reference/{reference}")
    public ResponseEntity<String> createSampleCase(@PathVariable long reference) {
        uk.gov.hmcts.reform.pcs.entity.PcsCase pcsCase = uk.gov.hmcts.reform.pcs.entity.PcsCase.builder()
            .caseReference(reference)
            .build();

        Claim claim = Claim.builder()
            .build();

        pcsCase.addClaim(claim);

        uk.gov.hmcts.reform.pcs.entity.Party claimantParty = createAndSaveParty("Charlotte", "Claimer");
        uk.gov.hmcts.reform.pcs.entity.Party defendantParty = createAndSaveParty("David", "Defender");
        uk.gov.hmcts.reform.pcs.entity.Party interestedParty = createAndSaveParty("Ivan", "Interested");

        pcsCase.addParty(claimantParty);
        pcsCase.addParty(defendantParty);
        pcsCase.addParty(interestedParty);

        claim.addParty(claimantParty, PartyRole.CLAIMANT);
        claim.addParty(defendantParty, PartyRole.DEFENDANT);
        claim.addParty(interestedParty, PartyRole.INTERESTED_PARTY);

        pcsCaseRepository.save(pcsCase);

        return ResponseEntity.ok("done");
    }

    private uk.gov.hmcts.reform.pcs.entity.Party createAndSaveParty(String forename, String surname) {
        uk.gov.hmcts.reform.pcs.entity.Party party = uk.gov.hmcts.reform.pcs.entity.Party.builder()
            .forename(forename)
            .surname(surname)
            .build();

        return partyRepository.save(party);
    }

}
