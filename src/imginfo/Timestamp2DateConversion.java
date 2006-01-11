package imginfo;

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