package uk.gov.hmcts.reform.pcs.ccd.event;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.requestBreathingSpace;

@Component
@AllArgsConstructor
public class RequestBreathingSpace implements CCDConfig<PcsCase, State, UserRole> {

    private final CoreCaseDataApi ccdApi;
    private final IdamClient idamClient;

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(requestBreathingSpace.name(), this::submit)
            .forStateTransition(State.CaseIssued, State.BreathingSpace)
            .name("Request breathing space")
            .grant(Permission.CRUD, UserRole.RESPONDENT_SOLICITOR)
            .grant(Permission.R, UserRole.CIVIL_CASE_WORKER)
            .fields()
            .page("request-breathing-space")
            .pageLabel("Confirm")
            .label("confirmation-info", "You are about to request breathing space")
            .mandatory(PcsCase::getBreathingSpaceSeconds)
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        int breathingSpaceSeconds = Integer.parseInt(eventPayload.caseData().getBreathingSpaceSeconds());
        startBreathingSpaceTimer(caseReference, breathingSpaceSeconds);
    }

    private void startBreathingSpaceTimer(long caseReference, int breathingSpaceSeconds) {
        TimerTask task = new TimerTask() {
            public void run() {
                System.out.println("Exiting breathing space");
                String idamToken = idamClient.getAccessToken("caseworker@pcs.com", "password");
                String s2sToken = generateDummyS2SToken();
                String userId = idamClient.getUserInfo(idamToken).getUid();

                var startEventResponse1 = ccdApi.startEvent(idamToken,
                                                            s2sToken,
                                                            Long.toString(caseReference),
                                                            EventId.exitBreathingSpace.name()
                );

                var content = CaseDataContent.builder()
                    //  .data(PcsCase.builder().build())
                    .event(Event.builder().id(EventId.exitBreathingSpace.name()).build())
                    .eventToken(startEventResponse1.getToken())
                    .build();

                ccdApi.submitEventForCaseWorker(idamToken, s2sToken, userId, "CIVIL", CaseType.getCaseType(),
                                    Long.toString(caseReference), true, content);

            }
        };
        Timer timer = new Timer("breathing-space-timer");

        timer.schedule(task, breathingSpaceSeconds * 1000L);
    }


    private String generateDummyS2SToken() {
        return JWT.create().withSubject("ccd_gw").withIssuedAt(new Date()).sign(Algorithm.HMAC256("secret"));
    }

}
