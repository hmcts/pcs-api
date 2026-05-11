package uk.gov.hmcts.reform.pcs.testingsupport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;
import uk.gov.hmcts.reform.pcs.testingsupport.endpoint.RegenerateAccessCodesResponse;
import uk.gov.hmcts.reform.pcs.testingsupport.endpoint.RegenerateAccessCodesResponse.DefendantPin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class TestingSupportAccessCodeService {

    static final String SUFFIX_ALPHABET = "ABCDEFGHJKLMNPRSTVWXYZ";
    static final int SUFFIX_LENGTH = 3;
    static final int MAX_DEFENDANTS = (int) Math.pow(SUFFIX_ALPHABET.length(), SUFFIX_LENGTH);
    private static final String PIN_PREFIX = "DEFENDANT";

    private final PcsCaseRepository pcsCaseRepository;
    private final PartyAccessCodeRepository partyAccessCodeRepository;
    private final PartyAccessCodeHashingService hashingService;

    @Transactional
    public Optional<RegenerateAccessCodesResponse> regenerateAccessCodes(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .map(pcsCase -> regenerateForCase(pcsCase, caseReference));
    }

    private RegenerateAccessCodesResponse regenerateForCase(PcsCaseEntity pcsCase, long caseReference) {
        List<PartyAccessCodeEntity> rows = partyAccessCodeRepository
            .findAllByPcsCase_Id(pcsCase.getId())
            .stream()
            .sorted(byPartyId())
            .toList();

        if (rows.size() > MAX_DEFENDANTS) {
            throw new IllegalStateException(
                "Cannot regenerate access codes for case " + caseReference
                    + ": found " + rows.size() + " defendants, max supported is " + MAX_DEFENDANTS);
        }

        List<DefendantPin> assigned = new ArrayList<>(rows.size());
        int slot = 1;
        for (PartyAccessCodeEntity row : rows) {
            String pin = pinForSlot(slot++);
            row.setCode(hashingService.encodeForStorage(pin));
            assigned.add(new DefendantPin(row.getPartyId(), pin));
        }

        partyAccessCodeRepository.saveAll(rows);
        log.info("Regenerated {} access codes for case {}", assigned.size(), caseReference);

        return new RegenerateAccessCodesResponse(assigned);
    }

    private static String pinForSlot(int slot) {
        int n = slot - 1;
        int base = SUFFIX_ALPHABET.length();
        char[] suffix = new char[SUFFIX_LENGTH];
        for (int i = SUFFIX_LENGTH - 1; i >= 0; i--) {
            suffix[i] = SUFFIX_ALPHABET.charAt(n % base);
            n /= base;
        }
        return PIN_PREFIX + new String(suffix);
    }

    private static Comparator<PartyAccessCodeEntity> byPartyId() {
        return Comparator.comparing(e -> e.getPartyId().toString());
    }
}
