package uk.gov.hmcts.reform.pcs.ccd.util;

import de.cronn.reflection.util.TypedPropertyGetter;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Utility class for defendant-related operations.
 * Centralizes common methods used across multiple defendant event and page classes.
 */
public class DefendantUtils {

    /**
     * Gets the appropriate defendant field getter based on the index.
     * 
     * @param i the defendant index (1-25)
     * @return the TypedPropertyGetter for the defendant field
     * @throws IllegalArgumentException if the index is invalid
     */
    public static TypedPropertyGetter<PCSCase, DefendantDetails> getTempDefField(int i) {
        switch (i) {
            case 1:  return PCSCase::getDefendant1;
            case 2:  return PCSCase::getDefendant2;
            case 3:  return PCSCase::getDefendant3;
            case 4:  return PCSCase::getDefendant4;
            case 5:  return PCSCase::getDefendant5;
            case 6:  return PCSCase::getDefendant6;
            case 7:  return PCSCase::getDefendant7;
            case 8:  return PCSCase::getDefendant8;
            case 9:  return PCSCase::getDefendant9;
            case 10: return PCSCase::getDefendant10;
            case 11: return PCSCase::getDefendant11;
            case 12: return PCSCase::getDefendant12;
            case 13: return PCSCase::getDefendant13;
            case 14: return PCSCase::getDefendant14;
            case 15: return PCSCase::getDefendant15;
            case 16: return PCSCase::getDefendant16;
            case 17: return PCSCase::getDefendant17;
            case 18: return PCSCase::getDefendant18;
            case 19: return PCSCase::getDefendant19;
            case 20: return PCSCase::getDefendant20;
            case 21: return PCSCase::getDefendant21;
            case 22: return PCSCase::getDefendant22;
            case 23: return PCSCase::getDefendant23;
            case 24: return PCSCase::getDefendant24;
            case 25: return PCSCase::getDefendant25;
            default: throw new IllegalArgumentException("Invalid defendant index: " + i);
        }
    }

    /**
     * Gets the appropriate add-another defendant field getter based on the index.
     * 
     * @param i the add-another index (1-25)
     * @return the TypedPropertyGetter for the add-another field
     * @throws IllegalArgumentException if the index is invalid
     */
    public static TypedPropertyGetter<PCSCase, ?> getAddAnotherField(int i) {
        switch (i) {
            case 1:  return PCSCase::getAddAnotherDefendant1;
            case 2:  return PCSCase::getAddAnotherDefendant2;
            case 3:  return PCSCase::getAddAnotherDefendant3;
            case 4:  return PCSCase::getAddAnotherDefendant4;
            case 5:  return PCSCase::getAddAnotherDefendant5;
            case 6:  return PCSCase::getAddAnotherDefendant6;
            case 7:  return PCSCase::getAddAnotherDefendant7;
            case 8:  return PCSCase::getAddAnotherDefendant8;
            case 9:  return PCSCase::getAddAnotherDefendant9;
            case 10: return PCSCase::getAddAnotherDefendant10;
            case 11: return PCSCase::getAddAnotherDefendant11;
            case 12: return PCSCase::getAddAnotherDefendant12;
            case 13: return PCSCase::getAddAnotherDefendant13;
            case 14: return PCSCase::getAddAnotherDefendant14;
            case 15: return PCSCase::getAddAnotherDefendant15;
            case 16: return PCSCase::getAddAnotherDefendant16;
            case 17: return PCSCase::getAddAnotherDefendant17;
            case 18: return PCSCase::getAddAnotherDefendant18;
            case 19: return PCSCase::getAddAnotherDefendant19;
            case 20: return PCSCase::getAddAnotherDefendant20;
            case 21: return PCSCase::getAddAnotherDefendant21;
            case 22: return PCSCase::getAddAnotherDefendant22;
            case 23: return PCSCase::getAddAnotherDefendant23;
            case 24: return PCSCase::getAddAnotherDefendant24;
            case 25: return PCSCase::getAddAnotherDefendant25;
            default: throw new IllegalArgumentException("Invalid add-another index: " + i);
        }
    }
}
