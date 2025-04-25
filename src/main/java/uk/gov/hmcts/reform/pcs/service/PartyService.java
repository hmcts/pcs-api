package uk.gov.hmcts.reform.pcs.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;

@Service
public class PartyService {

    private final PcsCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;
    private final ModelMapper modelMapper;

    public PartyService(PcsCaseRepository pcsCaseRepository,
                        PartyRepository partyRepository,
                        ModelMapper modelMapper) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepository = partyRepository;
        this.modelMapper = modelMapper;
    }

    public void addParties(long caseReference, List<Party> parties) {
        PcsCase pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        List<uk.gov.hmcts.reform.pcs.entity.Party> partyEntities = parties.stream()
            .map(this::convertToPartyEntity)
            .peek(partyEntity -> partyEntity.setPcsCase(pcsCaseEntity))
            .toList();

        partyRepository.saveAll(partyEntities);
    }

    public List<DynamicListElement> getAllPartiesAsOptionsList(long caseReference) {
        return partyRepository.findAllDtoByCaseReference(caseReference, true)
            .stream()
            .map(party -> {
                String partyName = party.getForename() + " " + party.getSurname();
                return DynamicListElement.builder().code(party.getId()).label(partyName).build();
            })
            .toList();
    }

    private uk.gov.hmcts.reform.pcs.entity.Party convertToPartyEntity(Party party) {
        return modelMapper.map(party, uk.gov.hmcts.reform.pcs.entity.Party.class);
    }

}
