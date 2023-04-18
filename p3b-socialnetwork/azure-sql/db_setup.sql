-- MySQL version: 5.7.11+
-- Usage:
-- mysql -h ${MYSQL_HOST} -u ${MYSQL_NAME} --password=${MYSQL_PWD}

-- create the database
drop database if exists reddit_db;
create database reddit_db;
use reddit_db;


-- create the user table
drop table if exists `users`;
create table `users` (
    `username` varchar(140) default null,
    `pwd` varchar(140) default null,
    `profile_photo_url` varchar(140) default null
);

-- Step 3 create an index
create index user_index ON users(username);

-- Step 4 load data to the users table
load data local infile 'users.csv' into table users columns terminated by ',' LINES TERMINATED BY '\n';
