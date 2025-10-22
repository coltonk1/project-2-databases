-- Create the database.
create database if not exists csx370_mb_platform;

-- Use the created database.
use csx370_mb_platform;

-- Create the user table.
create table if not exists user (
    userId int auto_increment,
    username varchar(255) not null,
    password varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    primary key (userId),
    unique (username),
    constraint username_min_length check (char_length(trim(username)) >= 2),
    constraint firstName_min_length check (char_length(trim(firstName)) >= 2),
    constraint lastName_min_length check (char_length(trim(lastName)) >= 2)
);

-- Create the posts table.
create table if not exists posts (
    postId int auto_increment,
    authorId int not null,
    body text not null,
    createdAt datetime default current_timestamp,
    primary key (postId),
    foreign key (authorId) REFERENCES user(userId),
    constraint body_min_length check (char_length(trim(body)) >= 1)
);

-- Create hashtags table.
create table if not exists hashtags (
    postId int not null,
    tag varchar(100) not null,
    primary key (postId, tag),
    foreign key (postId) references posts(postId)
);

-- Create comments table.
create table if not exists comments (
    commentId int auto_increment,
    postId int not null,
    authorId int not null,
    body text not null,
    createdAt datetime default current_timestamp,
    primary key (commentId),
    foreign key (postId) references posts(postId),
    foreign key (authorId) references user(userId),
    constraint body_min_length2 check (char_length(trim(body)) >= 1)
);

-- Create likes table.
create table if not exists likes (
    userId int not null,
    postId int not null,
    createdAt datetime default current_timestamp,
    primary key (userId, postId),
    foreign key (userId) references user(userId),
    foreign key (postId) references posts(postId)
);

-- Create bookmarks table.
create table if not exists bookmarks (
    userId int not null,
    postId int not null,
    createdAt datetime default current_timestamp,
    primary key (userId, postId),
    foreign key (userId) references user(userId),
    foreign key (postId) references posts(postId)
);

-- Create follows table.
create table if not exists follows (
    userId int not null,
    userIdFollowed int not null,
    primary key (userId, userIdFollowed),
    foreign key (userId) references user(userId),
    foreign key (userIdFollowed) references user(userId)
);