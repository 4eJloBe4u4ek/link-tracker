![Build](https://github.com/central-university-dev/backend-academy-2025-spring-template/actions/workflows/build.yaml/badge.svg)

# Link Tracker

Проект сделан в рамках курса Академия Бэкенда.

Приложение для отслеживания обновлений контента по ссылкам.
При появлении новых событий отправляется уведомление в Telegram.

Проект написан на `Java 23` с использованием `Spring Boot 3`.

Проект состоит из 2-х приложений:
* Bot
* Scrapper

Для работы требуется БД `PostgreSQL`. Присутствует опциональная зависимость на `Kafka`.

# Инструкция по запуску проекта:

1. Клонирование репозитория
   git clone https://github.com/central-university-dev/java-4eJloBe4u4ek.git
2. Настройка токенов необходимо создать файл .env и указать:
   TELEGRAM_TOKEN=your_telegram_bot_token
   SO_TOKEN_KEY=your_stackoverflow_key
   SO_ACCESS_TOKEN=your_stackoverflow_access_token
   GITHUB_TOKEN=your_github_personal_access_token
3. Запустить в IDE ScrapperApplication и BotApplication

Для дополнительной справки: [HELP.md](./HELP.md)
