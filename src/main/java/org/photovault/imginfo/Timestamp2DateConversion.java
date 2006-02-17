/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Foobar; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.imginfo;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import java.util.Date;
import java.sql.Timestamp;

/** This class maps SQL TIMESTAMP datatype to Java's Date. In the process nanosecond
    precision is lost.
*/

public class Timestamp2DateConversion implements FieldConversion {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Timestamp2DateConversion.class.getName() );
    
    public Object javaToSql( Object src ) {

	log.debug( "javaToSql: " + src );
	if ( src instanceof Date ) {
	    return new Timestamp( ((Date)src).getTime() );
	}
	return src;
    } 

    public Object sqlToJava( Object src ) {
	log.debug( "sqlToJava: " + src );

	if ( src instanceof Timestamp ) {
	    return new Date( ((Timestamp)src).getTime() );
	}
	return src;
	

    }

}