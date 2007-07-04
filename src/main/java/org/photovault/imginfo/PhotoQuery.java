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
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.imginfo;

import java.util.*;
import java.sql.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.odmg.*;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.FuzzyDate;

/**
   PhotoQuery class can be used to search for photos using wide variety of criterias.
*/
public class PhotoQuery implements PhotoCollection {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoQuery.class.getName() );


    public PhotoQuery() {
	photos = new Vector();
	listeners = new Vector();
	queryModified = true;

	criterias = new QueryFieldCriteria[fields.length];
    }
    
    Session session;
    
    /**
     Set the Hibernate session in which the query is made.
     TODO: This should be a constructor parameter.
     @param session The session that will be used.
     */
    public void setSession( Session session ) {
        this.session = session;
    }

    /**
       Restrict results to photos that have the specified field in
       given range
       @param fieldNum Code of the field used in the query
       @param lower Lower bound for the range. If null, the query will
       be regarded as "less than upper"
       @param upper Upper bound for the query. If null, the query will
       be regarded as "greated than lower"
    */
    public void setFieldCriteriaRange( int fieldNum, Object lower, Object upper ) {
	QueryRangeCriteria c = new QueryRangeCriteria( fields[fieldNum], lower, upper );
	criterias[fieldNum] = c;
	modified();
    }

    /**
       Set the fulltext search criteria
       @param text The text to search
    */
    public void setFulltextCriteria( String text ) {
	QueryFulltextCriteria c = new QueryFulltextCriteria( fields[FIELD_FULLTEXT], text );
	criterias[FIELD_FULLTEXT] = c;
	modified();
    }

    /**
       Sets a "LIKE" criteria to a specified field
       @param text The text to search
    */
    public void setLikeCriteria( int field, String text ) {
	QueryLikeCriteria c = new QueryLikeCriteria( fields[field], text );
	criterias[field] = c;
	modified();
    }

    /** Set a fuzzy date criteria for finding items (possibly)
	matching a date range
	@param field the date field used in search
	@param accuracyField The field containing accuracy info about
	the date
	@param date FuzzyTime describing the date range
	@param strictness Strictness used when selecting objects into
	results. See {@link QueryFuzzyTimeCriteria#setStrictness()} for more info
	about possible values and their semantics.  */

    public void
	setFuzzyDateCriteria( int field, int accuracyField,
			      FuzzyDate date, int strictness ) {
	QueryFuzzyTimeCriteria c = new QueryFuzzyTimeCriteria( fields[field],
							       fields[accuracyField] );
	c.setDate( date );
	c.setStrictness( strictness );
	criterias[field] = c;
	modified();
    }

    /**
       Limits query to photos that belong to a given fodler and its
       subfolders
       @param folder The folder into which the query is limited. If
       null all photos will be included in the query
    */
    public void limitToFolder( PhotoFolder folder ) {
	limitFolder = folder;
    }

    /**
       Clears all criterias from the query
    */
    public void clear() {
	for ( int n = 0; n < criterias.length; n++ ) {
	    criterias[n] = null;
	}
    }

    /**
       Clears criteria for a certain field
    */
    public void clearField( int field ) {
	criterias[field] = null;
    }
    
    /** Executes the actual query to database. This method is not
     * called directly by clients but is executed on demand after the
     * query has been modified and results are needed.
     */
    protected void query() {
	log.debug( "Entry: PhotoQuery.query" );
	photos.clear();
	try {
	    Criteria crit = session.createCriteria( PhotoInfo.class );
	    // Go through all the fields and create the criteria
	    for ( int n = 0; n < criterias.length; n++ ) {
		if ( criterias[n] != null ) {
		    criterias[n].setupQuery( crit );
		}
	    }

	    if ( limitFolder != null ) {
 		Collection folders = getSubfolderIds( limitFolder );
 		crit.createCriteria( "folders" ).add( Restrictions.in( "folderId", folders ));
	    }
	    
	    Collection result = crit.list();
	    photos.addAll( result );
	} catch ( Exception e ) {
	    log.warn( "Error executing query: " + e.getMessage() );
	    e.printStackTrace( System.out );
	}
	    
	queryModified = false;
	log.debug( "Exit: PhotoQuery.query" );
    }
    

    /**
       Create a list of folderIds in a given folder hierarchy
     @param folder root folder of the hierarchy
     @return List of ids of all folders in the hierarchy
    */

    private List<Integer> getSubfolderIds( PhotoFolder folder ) {
        List<Integer> folders = new ArrayList<Integer>();
        folders.add( folder.getFolderId() );
	appendSubfolderIds( folders, folder );
	return folders;
    }

    /**
     Append folder ids of given folder to a list
     @param folders List to which the ids are added
     @param folder Root folder for the hierarchy     
     */
    private void appendSubfolderIds( List<Integer> folders, PhotoFolder folder ) {
	for ( PhotoFolder subfolder : folder.getSubfolders() ) {
	    folders.add( subfolder.getFolderId() );
	    appendSubfolderIds( folders, subfolder );
	}
    }

    // Implementation of imginfo.PhotoCollection

    /**
     * Describe <code>getPhotoCount</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getPhotoCount() {
	if ( queryModified ) {
	    query();
	}
	return photos.size();
    }

    /**
     * Describe <code>getPhoto</code> method here.
     *
     * @param n an <code>int</code> value
     * @return a <code>PhotoInfo</code> value
     */
    public PhotoInfo getPhoto(int n) {
	if ( queryModified ) {
	    query();
	}
	return (PhotoInfo) photos.elementAt( n );
    }

    /**
     * Describe <code>addPhotoCollectionChangeListener</code> method here.
     *
     * @param l a <code>PhotoCollectionChangeListener</code> value
     */
    public void addPhotoCollectionChangeListener(PhotoCollectionChangeListener l) {
	listeners.add( l );
    }

    /**
     * Describe <code>removePhotoCollectionChangeListener</code> method here.
     *
     * @param l a <code>PhotoCollectionChangeListener</code> value
     */
    public void removePhotoCollectionChangeListener(PhotoCollectionChangeListener l) {
	listeners.remove( l );
    }

    /**
       This method is called whenever the query is modified in any
       way. It invalidates current query results & notifies all
       listeners*/
    protected void modified() {
	queryModified = true;
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

    /**
     True if the photos collection is dirty and must fe requeried from database
     */
    boolean queryModified;
    
    /**
     Photos found by the query
     */
    Vector photos = null;
    
    /**
     Listeners that will be notified about changes to this query
     */
    Vector listeners = null;
    
    /**
     If not null, limit query results to photos that belong to this folder or
     any of its subfolders.
     */
    PhotoFolder limitFolder = null;

    public static final int FIELD_SHOOTING_TIME          = 0;
    public static final int FIELD_SHOOTING_TIME_ACCURACY = 1;
    public static final int FIELD_FULLTEXT               = 2;
    public static final int FIELD_DESCRIPTION            = 3;
    public static final int FIELD_SHOOTING_PLACE         = 4;
    public static final int FIELD_PHOTOGRAPHER           = 5;
    public static final int FIELD_FSTOP                  = 6;
    public static final int FIELD_FOCAL_LENGTH           = 7;
    public static final int FIELD_SHUTTER_SPEED          = 8;
    public static final int FIELD_CAMERA                 = 9;
    public static final int FIELD_LENS                   = 10;
    public static final int FIELD_FILM                   = 11;
    public static final int FIELD_FILM_SPEED             = 12;
    
    static QueryField fields[] = null;
    QueryFieldCriteria criterias[] = null;
    
    static {
	fields = new QueryField[13];
	fields[FIELD_SHOOTING_TIME] = new QueryField( "shootTime", "shoot_time" );
	fields[FIELD_SHOOTING_TIME_ACCURACY] = new QueryField( "timeAccuracy", "time_accuracy" );
	fields[FIELD_FULLTEXT] = new QueryField( "shooting_place,description" );
	fields[FIELD_DESCRIPTION] = new QueryField( "description" );
	fields[FIELD_SHOOTING_PLACE] = new QueryField( "shootingPlace", "shooting_place" );
	fields[FIELD_PHOTOGRAPHER] = new QueryField( "photographer" );
	fields[FIELD_FSTOP] = new QueryField( "FStop" );
	fields[FIELD_FOCAL_LENGTH] = new QueryField( "focalLength" );
	fields[FIELD_SHUTTER_SPEED] = new QueryField( "shutterSpeed", "shutter_speed" );
	fields[FIELD_CAMERA] = new QueryField( "camera" );
	fields[FIELD_LENS] = new QueryField( "lens" );
	fields[FIELD_FILM] = new QueryField( "film" );
	fields[FIELD_FILM_SPEED] = new QueryField( "filmSpeed", "film_speed" );
    }

}