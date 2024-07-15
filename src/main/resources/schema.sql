CREATE DATABASE IF NOT EXISTS guestbook;

ALTER DATABASE guestbook
  DEFAULT CHARACTER SET utf8
  DEFAULT COLLATE utf8_general_ci;

USE guestbook;

CREATE TABLE IF NOT EXISTS post (
	id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
	name varchar(40),
	writeDate varchar(100),
	content varchar(400),
	attachedFile varchar(250)
) engine=InnoDB;