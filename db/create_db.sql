/*
create_db.sql

A SQL script for creating the database tables.

(c) 2002 Harri Kaimio

Version: $Id: create_db.sql,v 1.7 2003/02/08 20:04:39 kaimio Exp $
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
	pref_rotation float,
	orig_fname varchar(30),
	description text,

	PRIMARY KEY( photo_id ),
	FULLTEXT( shooting_place, description )
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
	rotated float,
	instance_type ENUM ( "original", "modified", "thumbnail" ) NOT NULL,
	PRIMARY KEY( volume_id, fname )
);

