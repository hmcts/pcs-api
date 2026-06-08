package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CaseListView {

    private static final String PERSON_UNKNOWN = "persons unknown";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setCaseFields(PCSCase pcsCase) {
        setClaimantNames(pcsCase);
        setDefendantNames(pcsCase);
        setDateIssuedString(pcsCase);
        setPostCode(pcsCase);
    }

    private void setClaimantNames(PCSCase pcsCase) {
        List<ListValue<Party>> claimants = pcsCase.getAllClaimants();

        if (!CollectionUtils.isEmpty(claimants)) {
            pcsCase.setClaimantNames(claimants.getFirst().getValue().getOrgName());
        }
    }

    private void setDefendantNames(PCSCase pcsCase) {
        List<ListValue<Party>> defendants = pcsCase.getAllDefendants();
        if (CollectionUtils.isEmpty(defendants)) {
            return;
        }

        String names = getDefendantName(defendants.getFirst().getValue());

        if (defendants.size() > 1) {
            names = names + ", " + getDefendantName(defendants.get(1).getValue());
        }

        if (defendants.size() > 2) {
            names = names + " and Others";
        }

        pcsCase.setDefendantNames(names);
    }

    private String getDefendantName(Party defendant) {
        if (defendant.getNameKnown() == VerticalYesNo.YES) {
            return defendant.getLastName();
        }

        return PERSON_UNKNOWN;
    }

    private void setDateIssuedString(PCSCase pcsCase) {
        LocalDateTime dateIssued = pcsCase.getDateIssued();
        if (dateIssued == null) {
            return;
        }

        pcsCase.setDateIssuedString(dateIssued.format(DATE_FORMATTER));
    }

    private void setPostCode(PCSCase pcsCase) {
        AddressUK address = pcsCase.getPropertyAddress();
        if (address == null) {
            return;
        }

        pcsCase.setPostCode(address.getPostCode());
    }
}
