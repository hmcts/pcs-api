package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.event.NonProdSupport;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.nonprod.CaseSupportHelper.JSON;
import static uk.gov.hmcts.reform.pcs.ccd.service.nonprod.CaseSupportHelper.LOCATION_PATTERN;

@ExtendWith(MockitoExtension.class)
class CaseSupportHelperTest {

    @InjectMocks
    private CaseSupportHelper underTest;

    @Mock
    private Resource resource1;
    @Mock
    private Resource resource2;
    @Mock
    private Resource resource3;

    @Mock
    private ResourcePatternResolver resourcePatternResolver;

    @Test
    void shouldReturnDynamicListWithAllResourceFilenamesWhenNonprodDirectoryContainsMultipleFiles() throws IOException {
        // Given
        when(resource1.getFilename()).thenReturn("test-file-one.json");
        when(resource2.getFilename()).thenReturn("test-file-two.json");
        when(resource3.getFilename()).thenReturn("test-file-three.json");

        Resource[] resources = {resource1, resource2, resource3};
        when(resourcePatternResolver.getResources(LOCATION_PATTERN + "*")).thenReturn(resources);

        // When
        DynamicList result = underTest.getNonProdFilesList();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getListItems()).hasSize(3);
        assertThat(result.getValue().getLabel()).isEqualTo("Please select ...");

        List<String> labels = result.getListItems().stream()
            .map(DynamicListElement::getLabel)
            .toList();

        assertThat(labels).containsExactlyInAnyOrder(
            "test file one",
            "test file two",
            "test file three"
        );

        // Verify all codes are unique UUIDs
        List<UUID> codes = result.getListItems().stream()
            .map(DynamicListElement::getCode)
            .toList();

        assertThat(codes).hasSize(3).doesNotHaveDuplicates();
    }

    @Test
    void shouldGenerateUuidCodesFromFilenameBytesForEachDynamicListElement() throws IOException {
        // Given
        String filename1 = "test-file-one.json";
        String filename2 = "test-file-two.json";

        when(resource1.getFilename()).thenReturn(filename1);
        when(resource2.getFilename()).thenReturn(filename2);

        Resource[] resources = {resource1, resource2};
        when(resourcePatternResolver.getResources(LOCATION_PATTERN + "*")).thenReturn(resources);

        UUID expectedCode1 = UUID.nameUUIDFromBytes(filename1.getBytes());
        UUID expectedCode2 = UUID.nameUUIDFromBytes(filename2.getBytes());

        // When
        DynamicList result = underTest.getNonProdFilesList();

        // Then
        assertThat(result.getListItems()).hasSize(2);
        List<UUID> actualCodes = result.getListItems().stream().map(DynamicListElement::getCode).toList();
        assertThat(actualCodes).containsExactlyInAnyOrder(expectedCode1, expectedCode2);
    }

    @Test
    void shouldReplaceHyphensWithSpacesAndRemoveJsonExtensionInLabels() throws IOException {
        // Given
        when(resource1.getFilename()).thenReturn("test-file-one.json");
        when(resource2.getFilename()).thenReturn("another-test-file.json");
        when(resource3.getFilename()).thenReturn("final-test-file.json");

        Resource[] resources = {resource1, resource2, resource3};
        when(resourcePatternResolver.getResources(LOCATION_PATTERN + "*")).thenReturn(resources);

        // When
        DynamicList result = underTest.getNonProdFilesList();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getListItems()).hasSize(3);
        List<String> labels = result.getListItems().stream().map(DynamicListElement::getLabel).toList();
        assertThat(labels).containsExactlyInAnyOrder(
            "test file one",
            "another test file",
            "final test file"
        );
    }

    @Test
    void shouldReturnDynamicListWithDefaultPleaseSelectValueElement() throws IOException {
        // Given
        when(resource1.getFilename()).thenReturn("test-file.json");
        Resource[] resources = {resource1};
        when(resourcePatternResolver.getResources(LOCATION_PATTERN + "*")).thenReturn(resources);

        // When
        DynamicList result = underTest.getNonProdFilesList();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue().getLabel()).isEqualTo("Please select ...");
        assertThat(result.getListItems()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyDynamicListBuilderWhenIOExceptionOccursWhileReadingResources() throws IOException {
        // Given
        when(resourcePatternResolver.getResources(LOCATION_PATTERN + "*"))
            .thenThrow(new IOException("Failed to read resources"));

        // When
        DynamicList result = underTest.getNonProdFilesList();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getListItems()).isNull();
        assertThat(result.getValue()).isNull();
    }

    @Test
    void shouldReturnFirstMatchingResourceWhenGetNonProdResourceIsCalledWithValidLabel() throws IOException {
        // Given
        String label = "test file one";
        String expectedName = "test-file-one";
        String resourcePath = LOCATION_PATTERN + expectedName + JSON;

        Resource expectedResource = resource1;
        when(resourcePatternResolver.getResources(resourcePath))
            .thenReturn(new Resource[]{expectedResource});

        // When
        Resource result = underTest.getNonProdResource(label);

        // Then
        assertThat(result).isSameAs(expectedResource);
    }

    @Test
    void shouldThrowNoSuchElementExceptionWhenGetNonProdResourceCannotFindMatchingResource() throws IOException {
        // Given
        String label = "non existent file";
        String expectedName = "non-existent-file";
        String resourcePath = LOCATION_PATTERN + expectedName + JSON;

        when(resourcePatternResolver.getResources(resourcePath)).thenReturn(new Resource[]{});

        // When/Then
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            underTest.getNonProdResource(label)
        ).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void shouldOnlyBeActiveInLocalDevAndPreviewProfiles() {
        // Given
        Profile profileAnnotation = CaseSupportHelper.class.getAnnotation(Profile.class);

        // Then
        assertThat(profileAnnotation).isNotNull();
        assertThat(profileAnnotation.value()).containsExactlyInAnyOrder("local", "dev", "preview");
    }

}






