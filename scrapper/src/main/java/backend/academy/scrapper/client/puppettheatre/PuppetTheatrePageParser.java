package backend.academy.scrapper.client.puppettheatre;

import java.net.URI;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class PuppetTheatrePageParser {
    private static final Pattern MONTH_CLASS = Pattern.compile("(?:^|\\s)item_mounth-(\\d{4}-\\d{2})(?:\\s|$)");
    private static final Pattern COMPACT_SESSION =
            Pattern.compile("(?<!\\d)(\\d{1,2}\\.\\d{1,2}\\.\\d{4}).*?(\\d{1,2}:\\d{2})(?!\\d)");
    private static final Pattern ANTI_BOT = Pattern.compile(
            "checking\\s+your\\s+browser|cloudflare\\s+challenge|cf-chl-|challenge-platform|imunify360|"
                    + "protected\\s+by\\s+anubis|not\\s+a\\s+bot|captcha",
            Pattern.CASE_INSENSITIVE);
    private static final Locale RUSSIAN_LOCALE = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter DAY_MONTH = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMMM")
            .toFormatter(RUSSIAN_LOCALE);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ROOT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm", Locale.ROOT);
    private static final Set<String> EXTERNAL_TICKET_HOSTS = Set.of("tce.by", "www.tce.by");
    private static final Clock DEFAULT_CLOCK = Clock.system(ZoneId.of("Europe/Minsk"));

    private final URI afishaUri;
    private final Clock clock;

    public PuppetTheatrePageParser() {
        this(PuppetTheatreClient.AFISHA_URI, DEFAULT_CLOCK);
    }

    PuppetTheatrePageParser(URI afishaUri) {
        this(afishaUri, DEFAULT_CLOCK);
    }

    PuppetTheatrePageParser(URI afishaUri, Clock clock) {
        this.afishaUri = afishaUri;
        this.clock = clock;
    }

    public PuppetTheatrePage parse(String html) {
        if (html == null || ANTI_BOT.matcher(html).find()) {
            throw new PuppetTheatreException(
                    PuppetTheatreCheckResult.ANTIBOT, "Puppet theatre returned an anti-bot page");
        }

        Document document = Jsoup.parse(html, afishaUri.toString());
        List<Element> cards = document.select(".afisha_item");

        Set<String> months = new LinkedHashSet<>();
        Map<String, PuppetTheatreSession> sessions = new LinkedHashMap<>();
        try {
            for (Element card : cards) {
                addSession(parseCard(card, extractMonth(card)), months, sessions);
            }
            for (Element link : document.select("a[href]")) {
                if (hasParentWithClass(link, "afisha_item")) {
                    continue;
                }
                parseCompactSession(link).ifPresent(session -> addSession(session, months, sessions));
            }
        } catch (PuppetTheatreException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PuppetTheatreException(
                    PuppetTheatreCheckResult.PARSE_ERROR,
                    "Puppet theatre returned an incompatible afisha card",
                    exception);
        }
        if (sessions.isEmpty()) {
            throw new PuppetTheatreException(
                    PuppetTheatreCheckResult.EMPTY_PAGE, "Puppet theatre returned no supported afisha entries");
        }
        return new PuppetTheatrePage(List.copyOf(months), List.copyOf(sessions.values()));
    }

    private void addSession(
            PuppetTheatreSession session, Set<String> months, Map<String, PuppetTheatreSession> sessions) {
        months.add(YearMonth.from(session.date()).toString());
        sessions.putIfAbsent(session.snapshotKey(), session);
    }

    private Optional<PuppetTheatreSession> parseCompactSession(Element link) {
        String title = normalizeText(link.text());
        if (title.isBlank()) {
            return Optional.empty();
        }

        for (Element element = link.parent();
                element != null && !element.tagName().equals("body");
                element = element.parent()) {
            if (element.select("a[href]").size() != 1) {
                continue;
            }
            String text = normalizeText(element.text());
            Matcher matcher = COMPACT_SESSION.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            String dateText = matcher.group(1);
            String timeText = matcher.group(2);
            if (matcher.find()) {
                return Optional.empty();
            }
            String href = link.attr("href").trim();
            if (href.isBlank()) {
                return Optional.empty();
            }
            URI ticketUri = afishaUri.resolve(href);
            if (!isAllowedTicketUri(ticketUri)) {
                return Optional.empty();
            }
            try {
                LocalDate date = LocalDate.parse(dateText, FULL_DATE);
                LocalTime time = LocalTime.parse(timeText, TIME);
                return Optional.of(new PuppetTheatreSession(title, date, time, ticketUri.toString()));
            } catch (DateTimeParseException exception) {
                throw new PuppetTheatreException(
                        PuppetTheatreCheckResult.PARSE_ERROR, "Invalid compact afisha date or time", exception);
            }
        }
        return Optional.empty();
    }

    private boolean hasParentWithClass(Element element, String className) {
        return element.parents().stream().anyMatch(parent -> parent.hasClass(className));
    }

    private Optional<YearMonth> extractMonth(Element card) {
        for (Element element = card; element != null; element = element.parent()) {
            Matcher matcher = MONTH_CLASS.matcher(element.className());
            if (matcher.find()) {
                try {
                    return Optional.of(YearMonth.parse(matcher.group(1)));
                } catch (DateTimeParseException exception) {
                    throw new PuppetTheatreException(
                            PuppetTheatreCheckResult.PARSE_ERROR, "Invalid afisha month marker", exception);
                }
            }
        }
        return Optional.empty();
    }

    private PuppetTheatreSession parseCard(Element card, Optional<YearMonth> markedMonth) {
        String title = requiredText(card, ".afisha-title", "title");
        String dayText = requiredText(card, ".afisha-day", "date");
        String timeText = requiredText(card, ".afisha-time", "time");
        Element link = card.selectFirst("a.afisha_item-hover[href]");
        if (link == null) {
            throw parseError("Puppet theatre afisha card has no ticket link");
        }

        LocalDate date = parseDate(dayText, markedMonth);
        LocalTime time;
        try {
            time = LocalTime.parse(timeText, TIME);
        } catch (DateTimeParseException exception) {
            throw new PuppetTheatreException(PuppetTheatreCheckResult.PARSE_ERROR, "Invalid afisha time", exception);
        }

        URI ticketUri = afishaUri.resolve(link.attr("href").trim());
        if (!isAllowedTicketUri(ticketUri)) {
            throw parseError("Puppet theatre ticket link points to an unexpected origin");
        }
        return new PuppetTheatreSession(title, date, time, ticketUri.toString());
    }

    private LocalDate parseDate(String value, Optional<YearMonth> markedMonth) {
        MonthDay monthDay;
        try {
            int weekdaySeparator = value.indexOf(',');
            String dayAndMonth = weekdaySeparator < 0 ? value : value.substring(0, weekdaySeparator);
            monthDay = MonthDay.parse(dayAndMonth.trim(), DAY_MONTH);
        } catch (DateTimeParseException exception) {
            throw new PuppetTheatreException(PuppetTheatreCheckResult.PARSE_ERROR, "Invalid afisha date", exception);
        }

        YearMonth month = markedMonth.orElseGet(() -> inferYearMonth(monthDay.getMonthValue()));
        if (monthDay.getMonthValue() != month.getMonthValue()) {
            throw parseError("Afisha date and month marker differ");
        }
        try {
            return month.atDay(monthDay.getDayOfMonth());
        } catch (DateTimeException exception) {
            throw new PuppetTheatreException(PuppetTheatreCheckResult.PARSE_ERROR, "Invalid afisha date", exception);
        }
    }

    private YearMonth inferYearMonth(int month) {
        YearMonth currentMonth = YearMonth.now(clock);
        YearMonth inferredMonth = YearMonth.of(currentMonth.getYear(), month);
        return inferredMonth.isBefore(currentMonth) ? inferredMonth.plusYears(1) : inferredMonth;
    }

    private String requiredText(Element card, String selector, String field) {
        Element element = card.selectFirst(selector);
        String value = element == null ? "" : normalizeText(element.text());
        if (value.isBlank()) {
            throw parseError("Puppet theatre afisha card has no " + field);
        }
        return value;
    }

    private String normalizeText(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private boolean isAllowedTicketUri(URI uri) {
        if (uri.getRawUserInfo() != null) {
            return false;
        }
        if (hasExpectedOrigin(uri)) {
            return true;
        }
        return "https".equalsIgnoreCase(uri.getScheme())
                && effectivePort(uri) == 443
                && uri.getHost() != null
                && EXTERNAL_TICKET_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    private boolean hasExpectedOrigin(URI uri) {
        return uri.getScheme() != null
                && uri.getHost() != null
                && afishaUri.getScheme().equalsIgnoreCase(uri.getScheme())
                && afishaUri.getHost().equalsIgnoreCase(uri.getHost())
                && effectivePort(afishaUri) == effectivePort(uri)
                && uri.getRawUserInfo() == null;
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private PuppetTheatreException parseError(String message) {
        return new PuppetTheatreException(PuppetTheatreCheckResult.PARSE_ERROR, message);
    }
}
