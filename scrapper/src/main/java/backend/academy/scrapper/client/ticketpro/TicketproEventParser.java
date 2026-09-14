package backend.academy.scrapper.client.ticketpro;

import backend.academy.scrapper.config.ScrapperConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TicketproEventParser {
    private static final String VENUE_HEADING_SELECTOR = "h1";
    private static final String JSON_LD_SCRIPT_SELECTOR = "script[type=application/ld+json]";
    private static final String LAST_PAGE_LINK_SELECTOR = "link[rel=last]";
    private static final String JSON_LD_GRAPH = "@graph";
    private static final String JSON_LD_TYPE = "@type";
    private static final Set<String> EVENT_TYPES =
            Set.of("Event", "http://schema.org/Event", "https://schema.org/Event");
    private static final Set<String> IN_STOCK_VALUES =
            Set.of("InStock", "http://schema.org/InStock", "https://schema.org/InStock");
    private static final int MAX_PAGES = 20;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TICKETPRO_COMPACT_OFFSET_DATE_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssxx", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern ANTI_BOT_CHALLENGE = Pattern.compile(
            "\\bAnubis\\b|\\bImunify360\\b|Checking\\s+your\\s+browser|browser\\s+challenge", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final URI ticketproBaseUri;

    public TicketproEventParser(ScrapperConfig scrapperConfig, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.ticketproBaseUri = URI.create(scrapperConfig.ticketpro().baseUrl());
    }

    public VenuePage parseVenuePage(String html) {
        if (ANTI_BOT_CHALLENGE.matcher(html).find()) {
            throw new IllegalStateException("Ticketpro returned an anti-bot challenge");
        }

        Document document = Jsoup.parse(html, ticketproBaseUri.toString());
        String venueName = extractVenueName(document);

        Map<String, TicketproEvent> events = new LinkedHashMap<>();
        for (Element script : document.select(JSON_LD_SCRIPT_SELECTOR)) {
            collectEvents(parseJsonLd(script.data()), events);
        }
        return new VenuePage(venueName, List.copyOf(events.values()), extractLastPage(document));
    }

    private String extractVenueName(Document document) {
        Element heading = document.selectFirst(VENUE_HEADING_SELECTOR);
        if (heading == null || heading.text().isBlank()) {
            throw new IllegalStateException("Ticketpro venue page has no heading");
        }
        return heading.text().trim();
    }

    private int extractLastPage(Document document) {
        Element lastPageLink = document.selectFirst(LAST_PAGE_LINK_SELECTOR);
        if (lastPageLink == null) {
            return 1;
        }

        String page = UriComponentsBuilder.fromUriString(lastPageLink.attr("href"))
                .build()
                .getQueryParams()
                .getFirst("page");
        if (page == null) {
            throw new IllegalStateException("Ticketpro returned invalid venue pagination");
        }

        final int lastPage;
        try {
            lastPage = Integer.parseInt(page);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Ticketpro returned invalid venue pagination", exception);
        }
        if (lastPage < 1) {
            throw new IllegalStateException("Ticketpro returned invalid venue pagination");
        }
        if (lastPage > MAX_PAGES) {
            throw new IllegalStateException("Ticketpro returned too many venue pages");
        }
        return lastPage;
    }

    private JsonNode parseJsonLd(String json) {
        try {
            return objectMapper
                    .reader()
                    .with(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                    .readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Ticketpro returned malformed JSON-LD", exception);
        }
    }

    private void collectEvents(JsonNode node, Map<String, TicketproEvent> events) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectEvents(child, events));
            return;
        }
        if (node.has(JSON_LD_GRAPH)) {
            collectEvents(node.get(JSON_LD_GRAPH), events);
        }
        if (!isEvent(node)) {
            return;
        }

        toAvailableEvent(node).ifPresent(event -> events.putIfAbsent(event.snapshotKey(), event));
    }

    private boolean isEvent(JsonNode node) {
        JsonNode type = node.get(JSON_LD_TYPE);
        if (type == null) {
            return false;
        }
        if (type.isTextual()) {
            return EVENT_TYPES.contains(type.asText());
        }
        if (type.isArray()) {
            for (JsonNode value : type) {
                if (EVENT_TYPES.contains(value.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<TicketproEvent> toAvailableEvent(JsonNode event) {
        Optional<JsonNode> availableOffer = findAvailableOffer(event.get("offers"));
        if (availableOffer.isEmpty()) {
            return Optional.empty();
        }

        String title = decodedText(event, "name");
        String startDateValue = text(event, "startDate");
        String endDateValue = text(event, "endDate");
        String eventUrl = decodedText(event, "url");
        if (title.isBlank() || startDateValue.isBlank() || eventUrl.isBlank()) {
            throw new IllegalStateException("Ticketpro event has no name, startDate or url");
        }

        URI resolvedUrl = ticketproBaseUri.resolve(eventUrl);
        if (!hasExpectedOrigin(resolvedUrl)) {
            throw new IllegalStateException("Ticketpro event points to an unexpected origin");
        }

        EventDateTime dateTime = parseDateTime(startDateValue, endDateValue);
        return Optional.of(new TicketproEvent(
                title, dateTime.date(), dateTime.time(), formatPrice(availableOffer.get()), resolvedUrl.toString()));
    }

    private Optional<JsonNode> findAvailableOffer(JsonNode offers) {
        if (offers == null || offers.isNull()) {
            return Optional.empty();
        }
        if (offers.isArray()) {
            for (JsonNode offer : offers) {
                if (isInStock(offer)) {
                    return Optional.of(offer);
                }
            }
            return Optional.empty();
        }
        return isInStock(offers) ? Optional.of(offers) : Optional.empty();
    }

    private boolean isInStock(JsonNode offer) {
        String availability = text(offer, "availability");
        return IN_STOCK_VALUES.contains(availability);
    }

    private EventDateTime parseDateTime(String startValue, String endValue) {
        try {
            TicketproDateTime start = parseTicketproDateTime(startValue);
            TicketproDateTime end = endValue.isBlank() ? start : parseTicketproDateTime(endValue);
            if (end.date().isBefore(start.date())
                    || (start.timestamp() != null
                            && end.timestamp() != null
                            && end.timestamp().isBefore(start.timestamp()))) {
                throw new IllegalStateException("Ticketpro event has an invalid date range");
            }
            String date = start.date().equals(end.date())
                    ? DISPLAY_DATE.format(start.date())
                    : DISPLAY_DATE.format(start.date()) + "–" + DISPLAY_DATE.format(end.date());

            String time = start.time() == null ? "" : DISPLAY_TIME.format(start.time());
            return new EventDateTime(date, time);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("Ticketpro event has an invalid date", exception);
        }
    }

    private TicketproDateTime parseTicketproDateTime(String value) {
        if (!value.contains("T")) {
            return new TicketproDateTime(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE), null, null);
        }

        DateTimeParseException colonOffsetFailure;
        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(value);
            return new TicketproDateTime(dateTime.toLocalDate(), dateTime.toLocalTime(), dateTime);
        } catch (DateTimeParseException exception) {
            colonOffsetFailure = exception;
        }

        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(value, TICKETPRO_COMPACT_OFFSET_DATE_TIME);
            return new TicketproDateTime(dateTime.toLocalDate(), dateTime.toLocalTime(), dateTime);
        } catch (DateTimeParseException exception) {
            exception.addSuppressed(colonOffsetFailure);
            throw exception;
        }
    }

    private String formatPrice(JsonNode offer) {
        String currency = text(offer, "priceCurrency");
        String lowPrice = firstNonBlank(text(offer, "lowPrice"), text(offer, "price"));
        String highPrice = firstNonBlank(text(offer, "highPrice"), lowPrice);
        if (lowPrice.isBlank()) {
            return "";
        }

        String low = normalizePrice(lowPrice);
        String high = normalizePrice(highPrice);
        String price = low.equals(high) ? low : low + "–" + high;
        return currency.isBlank() ? price : price + " " + currency;
    }

    private String normalizePrice(String price) {
        try {
            return new BigDecimal(price).stripTrailingZeros().toPlainString().replace('.', ',');
        } catch (NumberFormatException ignored) {
            return price;
        }
    }

    private String decodedText(JsonNode node, String field) {
        return HtmlUtils.htmlUnescape(text(node, field)).trim();
    }

    private String text(JsonNode node, String field) {
        if (node == null) {
            return "";
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private boolean hasExpectedOrigin(URI uri) {
        return ticketproBaseUri.getScheme().equalsIgnoreCase(uri.getScheme())
                && ticketproBaseUri.getHost().equalsIgnoreCase(uri.getHost())
                && effectivePort(ticketproBaseUri) == effectivePort(uri)
                && uri.getRawUserInfo() == null;
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return switch (uri.getScheme().toLowerCase(Locale.ROOT)) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }

    public record VenuePage(String venueName, List<TicketproEvent> events, int lastPage) {}

    private record EventDateTime(String date, String time) {}

    private record TicketproDateTime(LocalDate date, LocalTime time, OffsetDateTime timestamp) {}
}
