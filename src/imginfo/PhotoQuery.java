// PhotoQuery.java

package imginfo;

import java.util.*;
import java.sql.*;
import dbhelper.*;
import org.odmg.*;
import org.apache.ojb.broker.query.*;
import org.apache.ojb.broker.*;
import org.apache.ojb.odmg.*;



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

    /**
       Restrict resultfs to photos that have the specified field in
       the reange
       @param field Code of the field used in the query
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
    
    /**
       Returns the type of criteria set for certain field
    */
    public int getCriteriaType( int field ) {
	return 0;
    }
    

    
    /** Executes the actual query to database. This method is not
     * called directly by clients but is executed on demand after the
     * query has been modified and results are needed.
     */
    protected void query() {
	photos.clear();
	ODMGXAWrapper txw = new ODMGXAWrapper();
	Implementation odmg = ODMG.getODMGImplementation();

	Transaction tx = odmg.currentTransaction();

	try {
	    PersistenceBroker broker = ((HasBroker) tx).getBroker();
	    
	    Criteria crit = new Criteria();
	    // Go through all the fields and create the criteria
	    for ( int n = 0; n < criterias.length; n++ ) {
		if ( criterias[n] != null ) {
		    criterias[n].setupQuery( crit );
		}
	    }

	    QueryByCriteria q = new QueryByCriteria( PhotoInfo.class, crit );
	    Collection result = broker.getCollectionByQuery( q );
	    photos.addAll( result );
	    txw.commit();
	} catch ( Exception e ) {
	    log.warn( "Error executing query: " + e.getMessage() );
	    e.printStackTrace( System.out );
	    txw.abort();
	}
	    
	queryModified = false;
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
     * @param photoCollectionChangeListener a <code>PhotoCollectionChangeListener</code> value
     */
    public void addPhotoCollectionChangeListener(PhotoCollectionChangeListener l) {
	listeners.add( l );
    }

    /**
     * Describe <code>removePhotoCollectionChangeListener</code> method here.
     *
     * @param photoCollectionChangeListener a <code>PhotoCollectionChangeListener</code> value
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

    boolean queryModified;
    Vector photos = null;
    Vector listeners = null;

    public static int FIELD_SHOOTING_TIME   = 0;
    public static int FIELD_FULLTEXT        = 1;
    public static int FIELD_DESCRIPTION     = 2;
    public static int FIELD_SHOOTING_PLACE  = 3;
    public static int FIELD_PHOTOGRAPHER    = 4;
    public static int FIELD_FSTOP           = 5;
    public static int FIELD_FOCAL_LENGTH    = 6;
    public static int FIELD_SHUTTER_SPEED   = 7;
    public static int FIELD_CAMERA          = 8;
    public static int FIELD_LENS            = 9;
    public static int FIELD_FILM            = 10;
    public static int FIELD_FILM_SPEED      = 11;
    
    static QueryField fields[] = null;
    QueryFieldCriteria criterias[] = null;
    
    {
	fields = new QueryField[12];
	fields[FIELD_SHOOTING_TIME] = new QueryField( "shootTime" );
	fields[FIELD_FULLTEXT] = new QueryField( "shooting_place,description" );
	fields[FIELD_DESCRIPTION] = new QueryField( "description" );
	fields[FIELD_SHOOTING_PLACE] = new QueryField( "shootingPlace" );
	fields[FIELD_PHOTOGRAPHER] = new QueryField( "photographer" );
	fields[FIELD_FSTOP] = new QueryField( "FStop" );
	fields[FIELD_FOCAL_LENGTH] = new QueryField( "focalLength" );
	fields[FIELD_SHUTTER_SPEED] = new QueryField( "shutterSpeed" );
	fields[FIELD_CAMERA] = new QueryField( "camera" );
	fields[FIELD_LENS] = new QueryField( "lens" );
	fields[FIELD_FILM] = new QueryField( "film" );
	fields[FIELD_FILM_SPEED] = new QueryField( "filmSpeed" );
    }

}