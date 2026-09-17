package backend.academy.scrapper.client.puppettheatre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PuppetTheatrePageParserTest {
    private final PuppetTheatrePageParser parser = new PuppetTheatrePageParser();

    @Test
    void shouldParseProductionShapedAfishaMarkup() throws IOException {
        String html = readResource("/puppet-theatre/afisha-sample.html");

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.months()).containsExactly("2026-09", "2026-10");
        assertThat(result.sessions())
                .containsExactly(
                        new PuppetTheatreSession(
                                "Волшебное путешествие",
                                LocalDate.of(2026, 9, 19),
                                LocalTime.of(11, 0),
                                "https://puppet-minsk.by/spektakli/spektakli-dlya-detej/item/303-volshebnoe-puteshestvie#tickets"),
                        new PuppetTheatreSession(
                                "Пансион «Belvedere»",
                                LocalDate.of(2026, 10, 2),
                                LocalTime.of(19, 30),
                                "https://puppet-minsk.by/spektakli/spektakli-dlya-vzroslykh/item/315-pansion-belvedere#tickets"));
    }

    @Test
    void shouldParseCompactSaleMarkupReconstructedFromScreenshots() throws IOException {
        String html = readResource("/puppet-theatre/afisha-compact-sale-sample.html");

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.months()).containsExactly("2026-06", "2026-07");
        assertThat(result.sessions())
                .containsExactly(
                        new PuppetTheatreSession(
                                "Хутар",
                                LocalDate.of(2026, 6, 20),
                                LocalTime.of(19, 0),
                                "https://tce.by/events/khutar"),
                        new PuppetTheatreSession(
                                "Записки юного врача",
                                LocalDate.of(2026, 6, 27),
                                LocalTime.of(19, 0),
                                "https://tce.by/events/zapiski-yunogo-vracha"),
                        new PuppetTheatreSession(
                                "MPOIBA",
                                LocalDate.of(2026, 6, 27),
                                LocalTime.of(19, 0),
                                "https://tce.by/events/mroiva"),
                        new PuppetTheatreSession(
                                "MPOIBA",
                                LocalDate.of(2026, 7, 3),
                                LocalTime.of(19, 0),
                                "https://tce.by/events/mroiva"));
    }

    @Test
    void shouldParseCompactSaleMarkupWithoutDependingOnTableTags() {
        String html =
                """
                <main>
                  <nav><a href="/">Главная</a></nav>
                  <div class="sale-row">
                    <span>22.07.2026</span><span>19:00</span>
                    <span><a href="https://www.tce.by/events/zapiski">Записки юного врача</a></span>
                  </div>
                </main>
                <footer><a href="
                    https://www.facebook.com/belpuppet">Facebook</a></footer>
                """;

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.sessions())
                .containsExactly(new PuppetTheatreSession(
                        "Записки юного врача",
                        LocalDate.of(2026, 7, 22),
                        LocalTime.of(19, 0),
                        "https://www.tce.by/events/zapiski"));
    }

    @Test
    void shouldParseCardsAndCompactSaleRowsWhenBothArePresent() {
        String html =
                """
                <main>
                  <div class="afisha_item item_mounth-2026-09">
                    <div class="afisha-day">20 Сентября, Вс</div>
                    <div class="afisha-time">11:00</div>
                    <div class="afisha-title">Буратино</div>
                    <a class="afisha_item-hover" href="/spektakli/buratino#tickets"></a>
                  </div>
                  <div class="sale-row">
                    <span>22.09.2026<br>19:00</span>
                    <a href="https://tce.by/events/khutar">Хутар</a>
                  </div>
                </main>
                """;

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.sessions()).hasSize(2);
        assertThat(result.sessions()).extracting(PuppetTheatreSession::title).containsExactly("Буратино", "Хутар");
    }

    @Test
    void shouldParseEveryCardAcrossAllAvailableMonths() {
        String html =
                """
                <html><body>
                  <div class="afisha_item item_mounth-2026-09">
                    <div class="afisha-day">19 Сентября, Сб</div>
                    <div class="afisha-time">11:00</div>
                    <div class="afisha-title"> Волшебное   путешествие </div>
                    <a class="afisha_item-hover" href="/spektakli/volshebnoe-puteshestvie#tickets">Купить билет</a>
                  </div>
                  <div class="afisha_item item_mounth-2026-10">
                    <div class="afisha-day">2 Октября, Пт</div>
                    <div class="afisha-time">19:30</div>
                    <div class="afisha-title">Пансион «Belvedere»</div>
                    <a class="afisha_item-hover" href="/spektakli/pansion-belvedere#tickets">Купить билет</a>
                  </div>
                  <div class="afisha_item item_mounth-2026-11">
                    <div class="afisha-day">1 Ноября, Вс</div>
                    <div class="afisha-time">12:05</div>
                    <div class="afisha-title">Мойдодыр</div>
                    <a class="afisha_item-hover" href="https://puppet-minsk.by/spektakli/mojdodyr#tickets">Купить билет</a>
                  </div>
                </body></html>
                """;

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.months()).containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(result.sessions())
                .containsExactly(
                        new PuppetTheatreSession(
                                "Волшебное путешествие",
                                LocalDate.of(2026, 9, 19),
                                LocalTime.of(11, 0),
                                "https://puppet-minsk.by/spektakli/volshebnoe-puteshestvie#tickets"),
                        new PuppetTheatreSession(
                                "Пансион «Belvedere»",
                                LocalDate.of(2026, 10, 2),
                                LocalTime.of(19, 30),
                                "https://puppet-minsk.by/spektakli/pansion-belvedere#tickets"),
                        new PuppetTheatreSession(
                                "Мойдодыр",
                                LocalDate.of(2026, 11, 1),
                                LocalTime.of(12, 5),
                                "https://puppet-minsk.by/spektakli/mojdodyr#tickets"));
    }

    @Test
    void shouldKeepSamePerformanceAtDifferentTimesAsDifferentSessions() {
        String html =
                """
                <div class="afisha_item item_mounth-2026-09">
                  <div class="afisha-day">20 Сентября, Вс</div><div class="afisha-time">11:00</div>
                  <div class="afisha-title">Буратино</div>
                  <a class="afisha_item-hover" href="/spektakli/buratino#tickets"></a>
                </div>
                <div class="afisha_item item_mounth-2026-09">
                  <div class="afisha-day">20 Сентября, Вс</div><div class="afisha-time">14:00</div>
                  <div class="afisha-title">Буратино</div>
                  <a class="afisha_item-hover" href="/spektakli/buratino#tickets"></a>
                </div>
                """;

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.sessions()).hasSize(2);
        assertThat(result.sessions().get(0).snapshotKey())
                .isNotEqualTo(result.sessions().get(1).snapshotKey());
        assertThat(result.sessions())
                .extracting(PuppetTheatreSession::time)
                .containsExactly(LocalTime.of(11, 0), LocalTime.of(14, 0));
    }

    @Test
    void shouldParseSingleCurrentMonthWithoutMonthMarker() {
        PuppetTheatrePageParser parser = new PuppetTheatrePageParser(
                PuppetTheatreClient.AFISHA_URI, Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC));
        String html =
                """
                <div class="afisha_item">
                  <div class="afisha-day">19 Сентября, Сб</div><div class="afisha-time">11:00</div>
                  <div class="afisha-title">Буратино</div>
                  <a class="afisha_item-hover" href="/spektakli/buratino#tickets"></a>
                </div>
                """;

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.months()).containsExactly("2026-09");
        assertThat(result.sessions())
                .singleElement()
                .extracting(PuppetTheatreSession::date)
                .isEqualTo(LocalDate.of(2026, 9, 19));
    }

    @Test
    void shouldInferNextYearForUnmarkedMonthAfterNewYear() {
        PuppetTheatrePageParser parser = new PuppetTheatrePageParser(
                PuppetTheatreClient.AFISHA_URI, Clock.fixed(Instant.parse("2026-12-15T10:00:00Z"), ZoneOffset.UTC));
        String html =
                """
                <div class="afisha_item">
                  <div class="afisha-day">2 Января, Сб</div><div class="afisha-time">11:00</div>
                  <div class="afisha-title">Буратино</div>
                  <a class="afisha_item-hover" href="/spektakli/buratino#tickets"></a>
                </div>
                """;

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.months()).containsExactly("2027-01");
        assertThat(result.sessions())
                .singleElement()
                .extracting(PuppetTheatreSession::date)
                .isEqualTo(LocalDate.of(2027, 1, 2));
    }

    @Test
    void shouldUseMonthMarkerFromParentContainer() {
        String html =
                """
                <div class="afisha_listcontainer item_mounth-2026-09">
                  <div class="afisha_item">
                    <div class="afisha-day">19 Сентября, Сб</div><div class="afisha-time">11:00</div>
                    <div class="afisha-title">Буратино</div>
                    <a class="afisha_item-hover" href="/spektakli/buratino#tickets"></a>
                  </div>
                </div>
                """;

        PuppetTheatrePage result = parser.parse(html);

        assertThat(result.months()).containsExactly("2026-09");
        assertThat(result.sessions())
                .singleElement()
                .extracting(PuppetTheatreSession::date)
                .isEqualTo(LocalDate.of(2026, 9, 19));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "<html><body><h1>Афиша</h1></body></html>",
                "<html><body>Checking your browser before accessing</body></html>",
                "<html><body><div class='cf-chl-bypass'>Cloudflare challenge</div></body></html>",
                "<html><body><script>window.Imunify360=true</script></body></html>",
                "<html><title>Making sure you're not a bot!</title></html>",
                "<html><script src='/cdn-cgi/challenge-platform/x.js'></script></html>"
            })
    void shouldRejectEmptyOrAntiBotPage(String html) {
        assertThatThrownBy(() -> parser.parse(html))
                .isInstanceOf(PuppetTheatreException.class)
                .extracting("result")
                .isIn(PuppetTheatreCheckResult.EMPTY_PAGE, PuppetTheatreCheckResult.ANTIBOT);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "<html><body>Checking your browser before accessing</body></html>",
                "<html><body><div class='cf-chl-bypass'>Cloudflare challenge</div></body></html>",
                "<html><body><script>window.Imunify360=true</script></body></html>",
                "<html><title>Making sure you're not a bot!</title></html>",
                "<html><script src='/cdn-cgi/challenge-platform/x.js'></script></html>"
            })
    void shouldClassifyKnownChallengeAsAntibot(String html) {
        assertThatThrownBy(() -> parser.parse(html))
                .isInstanceOf(PuppetTheatreException.class)
                .extracting("result")
                .isEqualTo(PuppetTheatreCheckResult.ANTIBOT);
    }

    @Test
    void shouldRejectCardWithInconsistentMonthMarker() {
        String html =
                """
                <div class="afisha_item item_mounth-2026-09">
                  <div class="afisha-day">20 Октября, Вт</div><div class="afisha-time">11:00</div>
                  <div class="afisha-title">Буратино</div>
                  <a class="afisha_item-hover" href="/spektakli/buratino#tickets"></a>
                </div>
                """;

        assertThatThrownBy(() -> parser.parse(html))
                .isInstanceOf(PuppetTheatreException.class)
                .hasMessageContaining("month");
    }

    @Test
    void shouldRejectPerformanceLinkFromAnotherOrigin() {
        String html =
                """
                <div class="afisha_item item_mounth-2026-09">
                  <div class="afisha-day">20 Сентября, Вс</div><div class="afisha-time">11:00</div>
                  <div class="afisha-title">Буратино</div>
                  <a class="afisha_item-hover" href="https://example.com/spektakli/buratino#tickets"></a>
                </div>
                """;

        assertThatThrownBy(() -> parser.parse(html))
                .isInstanceOf(PuppetTheatreException.class)
                .hasMessageContaining("origin");
    }

    @Test
    void shouldNotTreatUnknownExternalSaleLinkAsAnAfishaEntry() {
        String html =
                """
                <div class="sale-row">
                  <span>20.06.2026<br>19:00</span>
                  <a href="https://example.com/events/khutar">Хутар</a>
                </div>
                """;

        assertThatThrownBy(() -> parser.parse(html))
                .isInstanceOf(PuppetTheatreException.class)
                .extracting("result")
                .isEqualTo(PuppetTheatreCheckResult.EMPTY_PAGE);
    }

    private String readResource(String path) throws IOException {
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream(path))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
