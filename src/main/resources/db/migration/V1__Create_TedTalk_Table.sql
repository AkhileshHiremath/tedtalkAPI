-- V1__Create_TedTalk_Table.sql
-- Create the ted_talk table for storing TED talk data

CREATE TABLE "ted_talk" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "author" VARCHAR(255) NOT NULL,
    "date" TIMESTAMP NOT NULL,
    "views" BIGINT NOT NULL,
    "likes" BIGINT NOT NULL,
    "link" VARCHAR(500) NOT NULL
);
