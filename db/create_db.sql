/*
create_db.sql

A SQL script for creating the database tables.

(c) 2002 Harri Kaimio

Version: $Id: create_db.sql,v 1.1 2002/12/01 07:44:14 kaimio Exp $
*/

/* Create the photos table */
create table photos (
	photo_id varchar(20) not null,
	shoot_time datetime,
	shooting_place varchar(30),
	photographer varchar(30),
	f_stop float,
	focal_length float,

	PRIMARY KEY( photo_id )
);

