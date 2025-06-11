package uk.gov.hmcts.reform.pcs.notify.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class EmailStateTest {

    @Test
    void testAllArgsConstructorAndGetters() {
        Map<String, Object> personalisation = Map.of("key1", "value1", "key2", 123);

        EmailState emailState = new EmailState(
            "id1",
            "test@example.com",
            "template123",
            personalisation,
            "ref-001",
            "replyTo-01",
            "notif-001"
        );

        assertThat(emailState.getId()).isEqualTo("id1");
        assertThat(emailState.getEmailAddress()).isEqualTo("test@example.com");
        assertThat(emailState.getTemplateId()).isEqualTo("template123");
        assertThat(emailState.getPersonalisation()).isEqualTo(personalisation);
        assertThat(emailState.getReference()).isEqualTo("ref-001");
        assertThat(emailState.getEmailReplyToId()).isEqualTo("replyTo-01");
        assertThat(emailState.getNotificationId()).isEqualTo("notif-001");
    }

    @Test
    void testDefaultConstructor() {
        EmailState emailState = new EmailState();

        assertThat(emailState.getId()).isNull();
        assertThat(emailState.getEmailAddress()).isNull();
        assertThat(emailState.getTemplateId()).isNull();
        assertThat(emailState.getPersonalisation()).isNull();
        assertThat(emailState.getReference()).isNull();
        assertThat(emailState.getEmailReplyToId()).isNull();
        assertThat(emailState.getNotificationId()).isNull();
    }

    @Test
    void testBuilder() {
        Map<String, Object> personalisation = Map.of("a", "b");

        EmailState emailState = EmailState.builder()
            .id("id2")
            .emailAddress("builder@example.com")
            .templateId("template-456")
            .personalisation(personalisation)
            .reference("ref-002")
            .emailReplyToId("replyTo-02")
            .notificationId("notif-002")
            .build();

        assertThat(emailState)
            .extracting(
                EmailState::getId,
                EmailState::getEmailAddress,
                EmailState::getTemplateId,
                EmailState::getPersonalisation,
                EmailState::getReference,
                EmailState::getEmailReplyToId,
                EmailState::getNotificationId
            )
            .containsExactly(
                "id2",
                "builder@example.com",
                "template-456",
                personalisation,
                "ref-002",
                "replyTo-02",
                "notif-002"
            );
    }

    @Test
    void testWithNotificationIdReturnsNewInstanceWithUpdatedNotificationId() {
        EmailState original = EmailState.builder()
            .id("id3")
            .emailAddress("orig@example.com")
            .templateId("template-789")
            .notificationId("oldNotif")
            .build();

        EmailState updated = original.withNotificationId("newNotif");

        assertThat(updated).isNotSameAs(original);
        assertThat(updated)
            .extracting(
                EmailState::getId,
                EmailState::getEmailAddress,
                EmailState::getTemplateId,
                EmailState::getNotificationId
            )
            .containsExactly(
                "id3",
                "orig@example.com",
                "template-789",
                "newNotif"
            );

        // Original instance remains unchanged
        assertThat(original.getNotificationId()).isEqualTo("oldNotif");
    }

    @Test
    void testEqualsAndHashCode() {
        EmailState emailState1 = EmailState.builder()
            .id("id4")
            .emailAddress("eq@example.com")
            .templateId("template-x")
            .notificationId("notif-x")
            .build();

        EmailState emailState2 = EmailState.builder()
            .id("id4")
            .emailAddress("eq@example.com")
            .templateId("template-x")
            .notificationId("notif-x")
            .build();

        assertThat(emailState1).isEqualTo(emailState2);
        assertThat(emailState1.hashCode()).hasSameHashCodeAs(emailState2.hashCode());
    }
}
