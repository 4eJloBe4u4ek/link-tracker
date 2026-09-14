package backend.academy.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.exception.UnsupportedLinkException;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {
    private static final Long CHAT_ID = 42L;
    private static final int MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH = 255;
    private static final String TICKETPRO_VENUE_URL_PREFIX = "https://www.ticketpro.by/koncertnye-ploshhadki/";

    @Mock
    private LinkOperationRepository linkOperationRepository;

    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(linkOperationRepository, new LinkTypeResolver());
    }

    @Test
    void shouldRejectUnsupportedLinkBeforeRepositoryAccess() {
        // Arrange
        AddLinkRequest request = new AddLinkRequest("https://example.com/", List.of(), List.of());

        // Act
        UnsupportedLinkException exception =
                assertThrows(UnsupportedLinkException.class, () -> linkService.addLink(CHAT_ID, request));

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Источник не поддерживается: https://example.com/");
        verifyNoInteractions(linkOperationRepository);
    }

    @Test
    void shouldSaveSupportedGithubRepositoryWithoutChangingUrl() {
        // Arrange
        String url = "https://github.com/spring-projects/spring-boot";
        AddLinkRequest request = new AddLinkRequest(url, List.of("java"), List.of());
        TrackedLink storedLink =
                new TrackedLink(1L, url, request.tags(), request.filters(), LocalDateTime.MIN, LocalDateTime.MIN);
        when(linkOperationRepository.addLink(CHAT_ID, url, request.tags(), request.filters()))
                .thenReturn(storedLink);

        // Act
        LinkResponse response = linkService.addLink(CHAT_ID, request);

        // Assert
        assertThat(response.url()).isEqualTo(url);
        verify(linkOperationRepository).addLink(CHAT_ID, url, request.tags(), request.filters());
    }

    @Test
    void shouldSaveSupportedTicketproVenueWithoutChangingUrl() {
        // Arrange
        String url = "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/";
        AddLinkRequest request = new AddLinkRequest(url, List.of(), List.of());
        TrackedLink storedLink =
                new TrackedLink(1L, url, request.tags(), request.filters(), LocalDateTime.MIN, LocalDateTime.MIN);
        when(linkOperationRepository.addLink(CHAT_ID, url, request.tags(), request.filters()))
                .thenReturn(storedLink);

        // Act
        LinkResponse response = linkService.addLink(CHAT_ID, request);

        // Assert
        assertThat(response.url()).isEqualTo(url);
        verify(linkOperationRepository).addLink(CHAT_ID, url, request.tags(), request.filters());
    }

    @Test
    void shouldRejectLegacyTicketproUrlBeforeRepositoryAccess() {
        // Arrange
        AddLinkRequest request = new AddLinkRequest("https://puppet-minsk.by/afisha", List.of(), List.of());

        // Act & Assert
        assertThrows(UnsupportedLinkException.class, () -> linkService.addLink(CHAT_ID, request));

        // Assert
        verifyNoInteractions(linkOperationRepository);
    }

    @Test
    void shouldRejectOverlongTicketproVenueBeforeRepositoryAccess() {
        // Arrange
        String url = canonicalVenueUrl(MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH + 1);
        AddLinkRequest request = new AddLinkRequest(url, List.of(), List.of());

        // Act & Assert
        assertThrows(UnsupportedLinkException.class, () -> linkService.addLink(CHAT_ID, request));

        // Assert
        verifyNoInteractions(linkOperationRepository);
    }

    @Test
    void shouldRejectNullLinkBeforeRepositoryAccess() {
        // Arrange
        AddLinkRequest request = new AddLinkRequest(null, List.of(), List.of());

        // Act & Assert
        assertThrows(UnsupportedLinkException.class, () -> linkService.addLink(CHAT_ID, request));

        // Assert
        verifyNoInteractions(linkOperationRepository);
    }

    private static String canonicalVenueUrl(int length) {
        return TICKETPRO_VENUE_URL_PREFIX + "a".repeat(length - TICKETPRO_VENUE_URL_PREFIX.length() - 1) + "/";
    }
}
