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
       @param strPhotoId ID of the photo to be retrieved
    */
    public static PhotoInfo retrievePhotoInfo( String strPhotoId ) throws PhotoNotFoundException {
	initDB();

	String sql = "SELECT * from photos where photo_id=\"" + strPhotoId +"\"";
	PhotoInfo photo = new PhotoInfo();
	try {
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		throw new PhotoNotFoundException();
	    }
	    photo.shootingPlace = rs.getString( "shooting_place" );
	    photo.photographer = rs.getString( "photographer" );
	    photo.FStop = rs.getFloat( "f_stop" );
	    photo.focalLength = rs.getFloat( "focal_length" );
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

    java.util.Date shootTime;
    
    /**
     * Get the value of shootTime.
¨     * @return value of shootTime.
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
    float FStop;
    
    /**
     * Get the value of FStop.
     * @return value of FStop.
     */
    public float getFStop() {
	return FStop;
    }
    
    /**
     * Set the value of FStop.
     * @param v  Value to assign to FStop.
     */
    public void setFStop(float  v) {
	this.FStop = v;
    }
    float focalLength;
    
    /**
     * Get the value of focalLength.
     * @return value of focalLength.
     */
    public float getFocalLength() {
	return focalLength;
    }
    
    /**
     * Set the value of focalLength.
     * @param v  Value to assign to focalLength.
     */
    public void setFocalLength(float  v) {
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
	     
}
