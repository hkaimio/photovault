/*
create_db.sql

A SQL script for creating the database tables.

(c) 2002 Harri Kaimio

Version: $Id: create_db.sql,v 1.3 2002/12/02 21:07:10 kaimio Exp $
*/

/* Create the photos table */
create table photos (
	photo_id INT not null,
	shoot_time datetime,
	shooting_place varchar(30),
	photographer varchar(30),
	f_stop float,
	focal_length float,

	PRIMARY KEY( photo_id )
);

/* Create the image_files table */

create table image_files ( 
	dirname varchar(255) NOT NULL,
	fname varchar(30) NOT NULL,
	photo_id integer NOT NULL REFERENCES photos( photo_id ),
	width integer,
	height integer,
	filehist ENUM ( "original", "modified", "thumbnail" ) NOT NULL,
	
	PRIMARY KEY( dirname, fname )
);