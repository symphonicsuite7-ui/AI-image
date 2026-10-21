CREATE DATABASE IF NOT EXISTS `ai_image`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `ai_image`;

CREATE TABLE IF NOT EXISTS `image_history` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `prompt` TEXT NOT NULL COMMENT 'Image generation prompt',
    `image_url` VARCHAR(500) NOT NULL COMMENT 'Public image URL',
    `model` VARCHAR(100) NOT NULL COMMENT 'Image generation model',
    `size` VARCHAR(50) NOT NULL COMMENT 'Image size',
    `quality` VARCHAR(20) NOT NULL COMMENT 'Image quality',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI image generation history';
