package backend.academy.bot.command.command;

import static backend.academy.bot.command.Utils.SPACE_SPLIT_REGEX;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.SOURCES)
@Component
public class SourcesCommand implements TelegramCommand {
    private static final String USAGE_MESSAGE = "Использование: " + BotCommandInfo.SOURCES.commandName();
    private static final String SOURCES_MESSAGE =
            """
            Поддерживаемые источники:

            GitHub — репозиторий:
            https://github.com/spring-projects/spring-boot

            Stack Overflow — конкретный вопрос:
            https://stackoverflow.com/questions/11227809/why-is-processing-a-sorted-array-faster

            Ticketpro — конкретная площадка:
            https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/

            Найти и выбрать площадку можно здесь:
            https://www.ticketpro.by/koncertnye-ploshhadki/

            Важно: сам каталог отслеживать нельзя. Откройте нужную площадку и скопируйте ссылку её страницы.

            Театр кукол — прямая афиша театра:
            https://puppet-minsk.by/afisha

            Этот источник отслеживается отдельно от площадки театра на Ticketpro.

            Добавьте выбранную ссылку через /track.
            """;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String[] messageParts = update.message().text().trim().split(SPACE_SPLIT_REGEX);

        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, USAGE_MESSAGE));
            return;
        }

        bot.execute(new SendMessage(chatId, SOURCES_MESSAGE));
    }
}
