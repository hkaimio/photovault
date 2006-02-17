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

import java.util.*;
import java.sql.*;
import org.odmg.*;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;

/**
   DateRangeQuery is a simple query that retrieves all photos that have 
   been taken between specified dates. It is made for demonstration and
   testing purposes.
*/

public class DateRangeQuery implements PhotoCollection {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( DateRangeQuery.class.getName() );


    public DateRangeQuery() {
	photos = new Vector();
	listeners = new Vector();
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
	modified();
    }

    public java.util.Date getStartDate() {
	return startDate;
    }
    
    public void setEndDate( java.util.Date date ) {
	endDate = date;
	modified();
    }

    public java.util.Date getEndDate() {
	return endDate;
    }

    /**
       Executes an SQL query towards the database according to the object state and stores
       results in photos array.
    */
    protected void query() {
	photos.clear();

	// Set up the query
	
	StringBuffer oqlBuf = new StringBuffer( "select photos from " + PhotoInfo.class.getName() );

	// Params for the OQL query
	Vector params = new Vector();
	int paramCount = 0;

	// Sche if there is start or end date set and modify the query accordingly
	if ( startDate != null || endDate != null ) {
	    oqlBuf.append( " where " );
	    if ( startDate != null ) {
		++ paramCount;
		params.add( startDate );
		oqlBuf.append( "shootTime >= $" + paramCount + " " );
	    }
	    if ( endDate != null ) {
		if ( startDate != null ) {
		    oqlBuf.append( "and " );
		}
		++ paramCount;
		params.add( endDate );
		oqlBuf.append( "shootTime <= $" + paramCount + " " );
	    }
	}
	log.debug( "Date rage query: " + oqlBuf.toString() );

	// Get transaction context
	ODMGXAWrapper txw = new ODMGXAWrapper();
	Implementation odmg = ODMG.getODMGImplementation();

	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( oqlBuf.toString() );
	    Iterator paramsIter = params.iterator();
	    while ( paramsIter.hasNext() ) {
		query.bind( paramsIter.next() );
	    }
	    List result = (List) query.execute();
	    photos.addAll( result );
	    txw.commit();
	} catch (Exception e ) {
	    log.warn( "Error executing query: " + e.getMessage() );
	    txw.abort();
	}

	rangeModified = false;
    }

    public void addPhotoCollectionChangeListener( PhotoCollectionChangeListener l ) {
	listeners.add( l );
    }

    public void removePhotoCollectionChangeListener( PhotoCollectionChangeListener l ) {
	listeners.remove( l );
    }

    protected void modified() {
	rangeModified = true;
	notifyListeners();
    }
    
    protected void notifyListeners() {
	PhotoCollectionChangeEvent e = new PhotoCollectionChangeEvent( this );
	Iterator iter = listeners.iterator();
	while ( iter.hasNext() ) {
	    PhotoCollectionChangeListener l = (PhotoCollectionChangeListener) iter.next();
	    l.photoCollectionChanged( e );
	}
    }
	
    java.util.Date startDate = null;
    java.util.Date endDate = null;
    boolean rangeModified;
    Vector photos = null;
    Vector listeners = null;
}
