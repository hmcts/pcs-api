package uk.gov.hmcts.reform.pcs.ccd.service.globalsearch;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
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

    private static String CLAIMANT_NAME = "Freeman";
    private static String DEFENDANT_LAST_NAME = "Jackson";
    private static String CLAIMANT_ORGANISATION_NAME = "Treetops Housing";

    @Mock
    private PCSCase pcsCase;
    private CaseNameHmctsFormatter underTest;

    @Mock
    private ClaimantInformation claimantInformation;

    @Mock
    private ListValue<Party> partyListValue;

    @Mock
    private Party party;

    @BeforeEach
    void setUp() {
        underTest = new CaseNameHmctsFormatter();
    }


    @Test
    void shouldSetCaseNameWhenClaimantIsOrgAndDefandantIsKnown() {
        when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
        when(claimantInformation.getOrgNameFound()).thenReturn(YesOrNo.YES);
        when(pcsCase.getAllDefendants()).thenReturn(List.of(partyListValue));
        when(claimantInformation.getClaimantName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
        when(partyListValue.getValue()).thenReturn(party);
        when(party.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Treetops Housing vs Jackson");
        verify(pcsCase).setCaseNameHmctsInternal("Treetops Housing vs Jackson");
        verify(pcsCase).setCaseNamePublic("Treetops Housing vs Jackson");
    }

    @Test
    void shouldSetCaseNameWhenClaimantIsCitizenAndDefandantIsKnown() {
        when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
        when(claimantInformation.getOrgNameFound()).thenReturn(YesOrNo.NO);
        when(pcsCase.getAllDefendants()).thenReturn(List.of(partyListValue));
        when(claimantInformation.getFallbackClaimantName()).thenReturn(CLAIMANT_NAME);
        when(partyListValue.getValue()).thenReturn(party);
        when(party.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Freeman vs Jackson");
        verify(pcsCase).setCaseNameHmctsInternal("Freeman vs Jackson");
        verify(pcsCase).setCaseNamePublic("Freeman vs Jackson");
    }

    @Test
    void shouldSetCaseNameWhenClaimantIsOrgAndMultipleDefandants() {
        when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
        when(pcsCase.getAllDefendants()).thenReturn(List.of(partyListValue, partyListValue));
        when(claimantInformation.getClaimantName()).thenReturn(CLAIMANT_ORGANISATION_NAME);
        when(partyListValue.getValue()).thenReturn(party);
        when(party.getLastName()).thenReturn(DEFENDANT_LAST_NAME);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Treetops Housing vs Jackson and Others");
        verify(pcsCase).setCaseNameHmctsInternal("Treetops Housing vs Jackson and Others");
        verify(pcsCase).setCaseNamePublic("Treetops Housing vs Jackson and Others");
    }

    @Test
    void shouldSetCaseNameWhenClaimantIsCitizenAndDefandantsUnkown() {
        when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
        when(pcsCase.getAllDefendants()).thenReturn(null);
        when(claimantInformation.getClaimantName()).thenReturn(CLAIMANT_NAME);

        underTest.setCaseNameHmctsField(pcsCase);

        // Then
        verify(pcsCase).setCaseNameHmctsRestricted("Freeman vs persons unknown");
        verify(pcsCase).setCaseNameHmctsInternal("Freeman vs persons unknown");
        verify(pcsCase).setCaseNamePublic("Freeman vs persons unknown");
    }
}
