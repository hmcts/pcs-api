package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmptyStringLocalDateSerializerTest {

    private final EmptyStringLocalDateSerializer serializer = new EmptyStringLocalDateSerializer();

    @Test
    void shouldWriteDateStringWhenPresent() throws Exception {
        JsonGenerator gen = mock(JsonGenerator.class);
        LocalDate date = LocalDate.of(2021, 4, 16);

        serializer.serialize(Optional.of(date), gen, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(gen).writeString(captor.capture());
        assertThat(captor.getValue()).isEqualTo("2021-04-16");
    }

    @Test
    void shouldWriteEmptyStringWhenAbsent() throws Exception {
        JsonGenerator gen = mock(JsonGenerator.class);

        serializer.serialize(Optional.empty(), gen, null);

        verify(gen).writeString("");
    }
}