/*
create_db.sql

A SQL script for creating the database tables.

(c) 2002 Harri Kaimio

Version: $Id: create_db.sql,v 1.5 2003/01/11 13:37:54 kaimio Exp $
*/

/* Create the photos table */
create table photos (
	photo_id INT not null,
	shoot_time datetime,
	shooting_place varchar(30),
	photographer varchar(30),
	f_stop float,
	focal_length float,
	shutter_speed float,
	camera varchar(30),
	lens varchar(30),
	film varchar(30),
	film_speed float,
	description text,

	PRIMARY KEY( photo_id ),
	FULLTEXT( shooting_place, description )
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

create table volumes (
	volume_id varchar(30) NOT NULL,
	root_path varchar(255) NOT NULL,

	PRIMARY KEY( volume_id )
);

create table image_instances (
	volume_id varchar(30) NOT NULL /* REFERENCES volumes( volume_id ) */,
	fname varchar(255) NOT NULL,
	photo_id integer NOT NULL REFERENCES photos( photo_id ),
	width integer,
	height integer,
	instance_type ENUM ( "original", "modified", "thumbnail" ) NOT NULL,
	PRIMARY KEY( volume_id, fname )
);

