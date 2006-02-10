// $Id: OJBInstanceTypeConversion.java,v 1.1 2003/03/01 19:43:17 kaimio Exp $

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
