// DateRangeQuery.java


package imginfo;

import java.util.*;
import java.sql.*;
import dbhelper.*;

/**
   DateRangeQuery is a simple query that retrieves all photos that have 
   been taken between specified dates. It is made for demonstration and
   testing purposes.
*/

public class DateRangeQuery implements PhotoCollection {
    public DateRangeQuery() {
	photos = new Vector();
    }

    public int getPhotoCount() {
	if ( rangeModified ) {
	    query();
	}
	return photos.size();
    }

    public PhotoInfo getPhoto ( int num ) {
	if ( rangeModified ) {
	    query();
	}
	return (PhotoInfo) photos.elementAt( num );
    }

    public void setStartDate( java.util.Date date ) {
	startDate = date;
	rangeModified = true;
    }

    public java.util.Date getStartDate() {
	return startDate;
    }
    
    public void setEndDate( java.util.Date date ) {
	endDate = date;
	rangeModified = true;
    }

    public java.util.Date getEndDate() {
	return endDate;
    }

    protected void query() {
	StringBuffer sqlBuf = new StringBuffer( "select * from photos" );
	if ( startDate != null || endDate != null ) {
	    sqlBuf.append( " where " );
	    if ( startDate != null ) {
		sqlBuf.append( "shoot_time >= ? " );
	    }
	    if ( endDate != null ) {
		if ( startDate != null ) {
		    sqlBuf.append( "and " );
		}
		sqlBuf.append( "shoot_time <= ?" );
	    }
	}
	System.err.println( "Date rage query: " + sqlBuf.toString() );

	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sqlBuf.toString() );
	    int param = 1;
	    if ( startDate != null ) {
		stmt.setDate( param, new java.sql.Date( startDate.getTime() ) );
		param++;
	    }
	    if ( endDate != null ) {
		stmt.setDate( param, new java.sql.Date( endDate.getTime() ) );
		param++;
	    }
	    ResultSet rs = stmt.executeQuery();
	    while( rs.next() ) {
		PhotoInfo photo = PhotoInfo.createFromResultSet( rs );
		if ( photo == null ) {
		    System.err.println( "Photo not created correctly" );
		}
		photos.add( photo );
	    }
	} catch ( SQLException e ) {
	    System.err.println( "Error executying dateRangeQuery: " + e.getMessage() );
	}
	rangeModified = false;
    }
	    
	
    java.util.Date startDate = null;
    java.util.Date endDate = null;
    boolean rangeModified;
    Vector photos = null;
}
