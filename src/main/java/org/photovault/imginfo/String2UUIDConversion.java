/*
  Copyright (c) 2007 Harri Kaimio
  
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
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.imginfo;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import java.util.UUID;

/** This class maps SQL TIMESTAMP datatype to Java's Date. In the process nanosecond
    precision is lost.
*/

public class String2UUIDConversion implements FieldConversion {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( String2UUIDConversion.class.getName() );
    
    public Object javaToSql( Object src ) {

	log.debug( "javaToSql: " + src );
	if ( src instanceof UUID ) {
	    return ((UUID)src).toString();
	}
	return src;
    } 

    public Object sqlToJava( Object src ) {
	log.debug( "sqlToJava: " + src );

	if ( src instanceof String ) {
	    return UUID.fromString( (String) src );
	}
	return src;
	

    }

}