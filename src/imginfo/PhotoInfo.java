// PhotoInfo.java

package imginfo;
import java.util.*;
import java.sql.*;

/**
   PhotoInfo represents information about a single photograph
   TODO: write a decent doc!!!
*/
class PhotoInfo {

    /**
       Static method to load photo info from database by photo id
       @param photoId ID of the photo to be retrieved
    */
    public static PhotoInfo retrievePhotoInfo( int photoId ) throws PhotoNotFoundException {
	initDB();

	String sql = "SELECT * from photos where photo_id=\"" + photoId +"\"";
	PhotoInfo photo = new PhotoInfo();
	try {
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		throw new PhotoNotFoundException();
	    }
	    photo.uid = rs.getInt( "photo_id" );
	    photo.shootingPlace = rs.getString( "shooting_place" );
	    photo.photographer = rs.getString( "photographer" );
	    photo.FStop = rs.getDouble( "f_stop" );
	    photo.focalLength = rs.getDouble( "focal_length" );
	    photo.shootTime = rs.getDate( "shoot_time" );
	    rs.close();
	    stmt.close();
	} catch (SQLException e ) {
	    System.err.println( "Error fetching record: " + e.getMessage() );
	    // TODO: Actually this is not the right exception for this purpose
	    throw new PhotoNotFoundException();
	}
	
	return photo;
    }

    /**
       Createas a new persistent PhotoInfo object and stores it in database
       (just a dummy onject with no meaningful field values)
       @return A new PhotoInfo object
    */
    public static PhotoInfo create() {
	PhotoInfo photo = new PhotoInfo();
	
	photo.uid = newUid();

	// Store the object in database
	try {
	    Statement stmt = conn.createStatement();
	    stmt.executeUpdate( "INSERT INTO photos values ( " + photo.uid + ", NULL, NULL, NULL, NULL, NULL)" );
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error creating new PhotoInfo: " + e.getMessage() );
	}
	return photo;
    }
    
    

    /**
       Updates  the object state to database
    */
    public void updateDB() {
	String sql = "UPDATE photos SET shooting_place = ?, photographer = ?, f_stop = ?, focal_length = ?, shoot_time = ? WHERE photo_id = ?";
	try {
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setString( 1, shootingPlace );
	    stmt.setString( 2, photographer );
	    stmt.setDouble( 3, FStop );
	    stmt.setDouble( 4, focalLength );
	    // setDate requires a java.sql.Date object, so make a cast
	    if ( shootTime != null ) {
		stmt.setDate( 5, new java.sql.Date( shootTime.getTime() ) );
	    } else {		
		stmt.setDate( 5, null );
	    }
	    stmt.setInt( 6, uid );
	    stmt.executeUpdate();
	    stmt.close();
	} catch (SQLException e ) {
	    System.err.println( "Error executing update: " + e.getMessage() );
	}
    }

    private int uid;

    /**
       Returns the uid of the object
    */
    public int getUid() {
	return uid;
    }
    
    java.util.Date shootTime;
    
    /**
     * Get the value of shootTime. Note that shoot time can also be 
     null (to mean that the time is unspecified)1
     @return value of shootTime.
     */
    public java.util.Date getShootTime() {
	return shootTime;
    }
    
    /**
     * Set the value of shootTime.
     * @param v  Value to assign to shootTime.
     */
    public void setShootTime(java.util.Date  v) {
	this.shootTime = v;
    }
    
    String desc;
    
    /**
     * Get the value of desc.
     * @return value of desc.
     */
    public String getDesc() {
	return desc;
    }
    
    /**
     * Set the value of desc.
     * @param v  Value to assign to desc.
     */
    public void setDesc(String  v) {
	this.desc = v;
    }
    double FStop;
    
    /**
     * Get the value of FStop.
     * @return value of FStop.
     */
    public double getFStop() {
	return FStop;
    }
    
    /**
     * Set the value of FStop.
     * @param v  Value to assign to FStop.
     */
    public void setFStop(double  v) {
	this.FStop = v;
    }
    double focalLength;
    
    /**
     * Get the value of focalLength.
     * @return value of focalLength.
     */
    public double getFocalLength() {
	return focalLength;
    }
    
    /**
     * Set the value of focalLength.
     * @param v  Value to assign to focalLength.
     */
    public void setFocalLength(double  v) {
	this.focalLength = v;
    }
    String shootingPlace;
    
    /**
     * Get the value of shootingPlace.
     * @return value of shootingPlace.
     */
    public String getShootingPlace() {
	return shootingPlace;
    }
    
    /**
     * Set the value of shootingPlace.
     * @param v  Value to assign to shootingPlace.
     */
    public void setShootingPlace(String  v) {
	this.shootingPlace = v;
    }
    String photographer;
    
    /**
     * Get the value of photographer.
     * @return value of photographer.
     */
    public String getPhotographer() {
	return photographer;
    }
    
    /**
     * Set the value of photographer.
     * @param v  Value to assign to photographer.
     */
    public void setPhotographer(String  v) {
	this.photographer = v;
    }

    
    // Database routines
    private static Connection conn = null;

    private static void initDB() {
	if ( conn != null ) {
	    // Database is already initalized!!!
	    return;
	}
	try {
	    Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
	} catch ( Exception e ) {
	    System.err.println( "DB driver not found" );
	}

	try {
	    conn = DriverManager.getConnection( "jdbc:mysql:///pv_test", "harri", "r1t1rat1" );
	} catch ( SQLException e ) {
	    System.err.println( "ERROR: Could not create DB connection: "
				+ e.getMessage() );
	}
    }

    /**
       This fuction generates a unique integer uid for usage as a database key.
       @return unique integer
    */

    private static int newUid() {
	int uid = -1;
	try {
	    Statement stmt = conn.createStatement();
	    stmt.executeUpdate( "UPDATE sequence SET id = LAST_INSERT_ID( id+1 )" );
	    ResultSet rs = stmt.executeQuery( "SELECT LAST_INSERT_ID()" );
	    if ( rs.next() ) {
		uid = rs.getInt( 1 );
	    }
	} catch ( SQLException e ) {
	    System.err.println( "Error generating uid: " + e.getMessage() );
	}
	return uid;
    }
    
}
