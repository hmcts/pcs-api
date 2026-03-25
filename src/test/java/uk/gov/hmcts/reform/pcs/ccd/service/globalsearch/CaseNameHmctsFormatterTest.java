package uk.gov.hmcts.reform.pcs.ccd.service.globalsearch;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.NO;
import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseNameHmctsFormatterTest {

    private static final String CLAIMANT_NAME = "Freeman";
    private static final String DEFENDANT_LAST_NAME = "Jackson";
    private static final String CLAIMANT_ORGANISATION_NAME = "Treetops Housing";

    @Mock
    private PCSCase pcsCase;
    private CaseNameHmctsFormatter underTest;

    @Mock
    private ListValue<Party> defendantPartyListValue;

    @Mock
    private ListValue<Party> claimantListValue;

    @Mock
    private Party defendantParty;

    @Mock
    private Party claimantParty;

    @BeforeEach
    void setUp() {
        underTest = new CaseNameHmctsFormatter();
    }


    @Test
    void shouldSetCaseNameWhenClaimantIsOrgAndDefendantIsKnown() {
        when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
        when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue));
        when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
        when(claimantListValue.getValue()).thenReturn(claimantParty);
        when(claimantParty.getOrgName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
        when(defendantParty.getNameKnown()).thenReturn(YES);
        when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Treetops Housing vs Jackson");
        verify(pcsCase).setCaseNameHmctsInternal("Treetops Housing vs Jackson");
        verify(pcsCase).setCaseNamePublic("Treetops Housing vs Jackson");
    }

    @Test
    void shouldSetCaseNameWhenClaimantIsCitizenAndDefendantIsKnown() {
        when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
        when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue));
        when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
        when(claimantListValue.getValue()).thenReturn(claimantParty);
        when(claimantParty.getLastName()).thenReturn(CLAIMANT_NAME);
        when(defendantParty.getNameKnown()).thenReturn(YES);
        when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Freeman vs Jackson");
        verify(pcsCase).setCaseNameHmctsInternal("Freeman vs Jackson");
        verify(pcsCase).setCaseNamePublic("Freeman vs Jackson");
    }

    @Test
    void shouldSetCaseNameWhenClaimantIsOrgAndMultipleDefendants() {
        when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
        when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue, defendantPartyListValue));
        when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
        when(claimantListValue.getValue()).thenReturn(claimantParty);
        when(claimantParty.getOrgName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
        when(defendantParty.getNameKnown()).thenReturn(YES);
        when(defendantParty.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Treetops Housing vs Jackson and Others");
        verify(pcsCase).setCaseNameHmctsInternal("Treetops Housing vs Jackson and Others");
        verify(pcsCase).setCaseNamePublic("Treetops Housing vs Jackson and Others");
    }

    @Test
    void shouldSetCaseNameWhenClaimantIsCitizenAndDefendantUnkown() {
        when(pcsCase.getAllClaimants()).thenReturn(List.of(claimantListValue));
        when(pcsCase.getAllDefendants()).thenReturn(List.of(defendantPartyListValue));
        when(claimantListValue.getValue()).thenReturn(claimantParty);
        when(defendantPartyListValue.getValue()).thenReturn(defendantParty);
        when(claimantParty.getLastName()).thenReturn(CLAIMANT_NAME);
        when(defendantParty.getNameKnown()).thenReturn(NO);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Freeman vs persons unknown");
        verify(pcsCase).setCaseNameHmctsInternal("Freeman vs persons unknown");
        verify(pcsCase).setCaseNamePublic("Freeman vs persons unknown");
    }
}
