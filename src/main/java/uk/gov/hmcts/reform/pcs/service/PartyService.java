package uk.gov.hmcts.reform.pcs.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;

@Service
public class PartyService {

    private final PCSCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;
    private final ModelMapper modelMapper;

    public PartyService(PCSCaseRepository pcsCaseRepository,
                        PartyRepository partyRepository,
                        ModelMapper modelMapper) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepository = partyRepository;
        this.modelMapper = modelMapper;
    }

    public void addParties(long caseReference, List<Party> parties) {
        PcsCase pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException("Case not found for " + caseReference));

        List<uk.gov.hmcts.reform.pcs.entity.Party> partyEntities = parties.stream()
            .map(this::convertToPartyEntity)
            .peek(partyEntity -> partyEntity.setPcsCase(pcsCaseEntity))
            .toList();

        partyRepository.saveAll(partyEntities);
    }

    private uk.gov.hmcts.reform.pcs.entity.Party convertToPartyEntity(Party party) {
        uk.gov.hmcts.reform.pcs.entity.Party partyEntity
            = modelMapper.map(party, uk.gov.hmcts.reform.pcs.entity.Party.class);

        partyEntity.setActive(true);
        return partyEntity;
    }

}
