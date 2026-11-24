package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.DemotionOfTenancyType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.SuspensionOfRightToBuyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.AlternativesRules;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Maps the optional suspension of right to buy and demotion of tenancy requests to the alternatives to
 * possession sections.
 */
@Component
public class RentalAlternativesMapper {

    private final AlternativesRules alternativesRules;

    public RentalAlternativesMapper(AlternativesRules alternativesRules) {
        this.alternativesRules = alternativesRules;
    }

    public void apply(SuspensionOfRightToBuyType suspension,
                      DemotionOfTenancyType demotion,
                      PCSCase target,
                      String path,
                      BulkErrorCollector errors) {

        Set<AlternativesToPossession> alternatives = EnumSet.noneOf(AlternativesToPossession.class);
        SuspensionOfRightToBuyHousingAct suspensionAct = null;
        DemotionOfTenancyHousingAct demotionAct = null;

        if (suspension != null) {
            alternatives.add(AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY);
            String actPath = path + "/SuspensionOfRightToBuy";
            suspensionAct = housingAct(SuspensionOfRightToBuyHousingAct.class,
                                       suspension.getHousingActSection().value(),
                                       actPath + "/HousingActSection", errors);
            errors.requiredWithMaxLength(actPath + "/Reason", suspension.getReason(), BulkLimits.SHORT_TEXT,
                                         SuspensionOfRightToBuy.SUSPENSION_OF_RIGHT_TO_BUY_REASON_LABEL);
        }
        if (demotion != null) {
            alternatives.add(AlternativesToPossession.DEMOTION_OF_TENANCY);
            String actPath = path + "/DemotionOfTenancy";
            demotionAct = housingAct(DemotionOfTenancyHousingAct.class, demotion.getHousingActSection().value(),
                                     actPath + "/HousingActSection", errors);
            errors.requiredWithMaxLength(actPath + "/Reason", demotion.getReason(), BulkLimits.SHORT_TEXT,
                                         DemotionOfTenancy.DEMOTION_OF_TENANCY_REASON_LABEL);
            if (demotion.isStatementOfExpressTermsServed()) {
                errors.requiredWithMaxLength(actPath + "/StatementDetails", demotion.getStatementDetails(),
                                             BulkLimits.SHORT_TEXT,
                                             DemotionOfTenancy.STATEMENT_OF_EXPRESS_TERMS_DETAILS_LABEL);
            }
        }

        if (alternatives.isEmpty()) {
            return;
        }

        target.setAlternativesToPossession(alternatives);
        if (suspension != null) {
            target.setSuspensionOfRightToBuy(SuspensionOfRightToBuy.builder()
                .housingAct(suspensionAct)
                .reason(suspension.getReason())
                .build());
        }
        if (demotion != null) {
            boolean served = demotion.isStatementOfExpressTermsServed();
            target.setDemotionOfTenancy(DemotionOfTenancy.builder()
                .housingAct(demotionAct)
                .statementOfExpressTermsServed(VerticalYesNo.from(served))
                .statementOfExpressTermsDetails(served ? StringUtils.trimToNull(demotion.getStatementDetails()) : null)
                .reason(demotion.getReason())
                .build());
        }
        if (suspension != null && demotion != null) {
            target.setSuspensionOfRightToBuyDemotionOfTenancy(SuspensionOfRightToBuyDemotionOfTenancy.builder()
                .suspensionOfRightToBuyActs(suspensionAct)
                .suspensionOrderReason(suspension.getReason())
                .demotionOfTenancyActs(demotionAct)
                .demotionOrderReason(demotion.getReason())
                .build());
        }
        alternativesRules.applyPageFlags(target);
    }

    private static <E extends Enum<E>> E housingAct(Class<E> type, String value, String path,
                                                    BulkErrorCollector errors) {
        if (StringUtils.isBlank(value)) {
            errors.add(path, BulkErrorCollector.REQUIRED, "Housing Act section is required");
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            errors.add(path, BulkErrorCollector.INVALID_VALUE,
                       "Housing Act section must be one of " + Arrays.toString(type.getEnumConstants()));
            return null;
        }
    }
}
