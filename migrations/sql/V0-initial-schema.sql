CREATE TABLE chats
(
    id         BIGINT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE links
(
    id         BIGSERIAL PRIMARY KEY,
    url        TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chats_links
(
    chat_id BIGINT REFERENCES chats (id) ON DELETE CASCADE,
    link_id BIGINT REFERENCES links (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, link_id)
);

CREATE TABLE tags
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE chat_link_tags
(
    chat_id BIGINT REFERENCES chats (id) ON DELETE CASCADE,
    link_id BIGINT REFERENCES links (id) ON DELETE CASCADE,
    tag_id  BIGINT REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, link_id, tag_id)
);

CREATE TABLE filters
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE chat_link_filters
(
    chat_id   BIGINT REFERENCES chats (id) ON DELETE CASCADE,
    link_id   BIGINT REFERENCES links (id) ON DELETE CASCADE,
    filter_id BIGINT REFERENCES filters (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, link_id, filter_id)
)
