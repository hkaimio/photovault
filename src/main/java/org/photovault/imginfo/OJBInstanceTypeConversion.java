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

import org.apache.ojb.broker.accesslayer.conversions.*;

public class OJBInstanceTypeConversion implements FieldConversion {

    public Object javaToSql( Object src ) {

	if ( src instanceof Integer ) {
	    int type = ((Integer)src).intValue();
	    switch( type ) {
	    case ImageInstance.INSTANCE_TYPE_ORIGINAL:
		return "original";
	    case ImageInstance.INSTANCE_TYPE_MODIFIED:
		return "modified";
	    case ImageInstance.INSTANCE_TYPE_THUMBNAIL:
		return "thumbnail";
	    default:
		return src;
	    }
	} 
	return src;
    }

    public Object sqlToJava( Object src ) {
	if ( src instanceof String ) {
	    String strInstanceType = (String)src;
	    if ( strInstanceType.equals( "original" ) ) {
		return new Integer( ImageInstance.INSTANCE_TYPE_ORIGINAL );
	    } else if ( strInstanceType.equals( "modified" ) ) {
		return new Integer( ImageInstance.INSTANCE_TYPE_MODIFIED );
	    } else if ( strInstanceType.equals( "thumbnail" ) ) {
		return new Integer( ImageInstance.INSTANCE_TYPE_THUMBNAIL );
	    }
	}
	return src;
    }
}
