package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefendantsTableService {


    /**
     * Populates the defendantsTableHtml field with formatted data
     */
    public void populateDefendantsTableHtml(PCSCase pcsCase) {
        try {
            // Count how many defendants have data
            int defendantCount = countDefendantsWithData(pcsCase);
            
            if (defendantCount > 0) {
                pcsCase.setDefendantsTableHtml("<p>Defendants table will be displayed here</p>");
                log.info("Found {} defendants, setting placeholder message", defendantCount);
            } else {
                pcsCase.setDefendantsTableHtml("<p>No defendants added yet.</p>");
                log.info("No defendants found, setting empty table message");
            }
        } catch (Exception e) {
            log.error("Error populating defendants table HTML", e);
            pcsCase.setDefendantsTableHtml("<p>Error loading defendant information.</p>");
        }
    }

    private int countDefendantsWithData(PCSCase pcsCase) {
        int count = 0;
        for (int i = 1; i <= 25; i++) {
            DefendantDetails defendant = getDefendantByIndex(pcsCase, i);
            if (defendant != null && hasDefendantData(defendant)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasDefendantData(DefendantDetails defendant) {
        return (defendant.getFirstName() != null && !defendant.getFirstName().trim().isEmpty()) ||
               (defendant.getLastName() != null && !defendant.getLastName().trim().isEmpty()) ||
               (defendant.getEmail() != null && !defendant.getEmail().trim().isEmpty()) ||
               (defendant.getCorrespondenceAddress() != null);
    }

    private DefendantDetails getDefendantByIndex(PCSCase pcsCase, int index) {
        return switch (index) {
            case 1 -> pcsCase.getDefendant1();
            case 2 -> pcsCase.getDefendant2();
            case 3 -> pcsCase.getDefendant3();
            case 4 -> pcsCase.getDefendant4();
            case 5 -> pcsCase.getDefendant5();
            case 6 -> pcsCase.getDefendant6();
            case 7 -> pcsCase.getDefendant7();
            case 8 -> pcsCase.getDefendant8();
            case 9 -> pcsCase.getDefendant9();
            case 10 -> pcsCase.getDefendant10();
            case 11 -> pcsCase.getDefendant11();
            case 12 -> pcsCase.getDefendant12();
            case 13 -> pcsCase.getDefendant13();
            case 14 -> pcsCase.getDefendant14();
            case 15 -> pcsCase.getDefendant15();
            case 16 -> pcsCase.getDefendant16();
            case 17 -> pcsCase.getDefendant17();
            case 18 -> pcsCase.getDefendant18();
            case 19 -> pcsCase.getDefendant19();
            case 20 -> pcsCase.getDefendant20();
            case 21 -> pcsCase.getDefendant21();
            case 22 -> pcsCase.getDefendant22();
            case 23 -> pcsCase.getDefendant23();
            case 24 -> pcsCase.getDefendant24();
            case 25 -> pcsCase.getDefendant25();
            default -> null;
        };
    }
}
