// PhotoInfo.java

package imginfo;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;
import java.lang.Integer;
import dbhelper.*;
import javax.imageio.*;
import java.awt.image.*;
import java.awt.geom.*;

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
	String sql = "SELECT * from photos where photo_id=\"" + photoId +"\"";
	PhotoInfo photo = new PhotoInfo();
	try {
	    Connection conn  = ImageDb.getConnection();
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
	
	photo.uid = ImageDb.newUid();

	// Store the object in database
	try {
	    Connection conn = ImageDb.getConnection();
	    Statement stmt = conn.createStatement();
	    stmt.executeUpdate( "INSERT INTO photos values ( " + photo.uid + ", NULL, NULL, NULL, NULL, NULL)" );
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error creating new PhotoInfo: " + e.getMessage() );
	}
	return photo;
    }
    

    /**
       Add a new image to the database. This method first copies a given image file to the database volume.
       It then extracts the information it can from the image file and stores a corresponding entry in DB
       @param imgFile File object that describes the image file that is to be added to the database
       @return The PhotoInfo object describing the new file.
       @throws PhotoNotFoundException if the file given as imgFile argument does not exist or is unaccessible.
    */
    public static PhotoInfo addToDB( File imgFile )  throws PhotoNotFoundException {
	Volume vol = Volume.getDefaultVolume();
	File f = vol.getFilingFname( imgFile );

	// Copy the file to the archive
	try {
	    FileInputStream in = new FileInputStream( imgFile );
	    FileOutputStream out = new FileOutputStream( f );
	    byte buf[] = new byte[1024];
	    int nRead = 0;
	    int offset = 0;
	    while ( (nRead = in.read( buf )) > 0 ) {
		out.write( buf, 0, nRead );
		offset += nRead;
	    }
	    out.close();
	    in.close();
	} catch ( Exception e ) {
	    System.err.println( "Error copying file: " + e.getMessage() );
	    throw new PhotoNotFoundException();
	}
	    

	// Create the image
	PhotoInfo photo = PhotoInfo.create();
	photo.addInstance( f.getParent(), f.getName(), ImageFile.FILE_HISTORY_ORIGINAL );
	java.util.Date shootTime = new java.util.Date( imgFile.lastModified() );
	photo.setShootTime( shootTime );
	photo.updateDB();
	return photo;
    }
    
	
    /**
       Deletes the PhotoInfo and all related instances from database
    */
    public void delete() {
	String sql = "DELETE FROM photos WHERE photo_id = " + uid;
	// First delete all instances
	if ( instances == null ) {
	    instances = ImageFile.retrieveInstances( this );
	}
	for ( int i = 0; i < instances.size(); i++ ) {
	    ImageFile f = (ImageFile) instances.get( i );
	    f.delete();
	}

	// Then delete the PhotoInfo itself
	try {
	    Connection conn = ImageDb.getConnection();
	    Statement stmt = conn.createStatement( );
	    stmt.executeUpdate( sql );
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error deletin image file from DB: " + e.getMessage() );
	}
    }
	

    /**
       Updates  the object state to database
    */
    public void updateDB() {
	String sql = "UPDATE photos SET shooting_place = ?, photographer = ?, f_stop = ?, focal_length = ?, shoot_time = ? WHERE photo_id = ?";
	try {
	    Connection conn = ImageDb.getConnection();
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
    
    /**
       Adds a new instance of the photo into the database.
       @param dirname Directory where the image instance is stored
       @param fname File name of the instance
       @instanceType Type of the instance - original, modified or thumbnail.
       See ImageFile class documentation for details.
    */
    public void addInstance( String dirname, String fname, int instanceType ) {
	ArrayList origInstances = getInstances();
	ImageFile f = ImageFile.create( dirname, fname, this );
	f.setFileHistory( instanceType );
	origInstances.add( f );
    }

    /**
       Returns the number of instances of this photo that are stored in database
    */
    public int getNumInstances() {
	// Count number of instances from database
	String sql = "SELECT COUNT(*) FROM image_files WHERE photo_id = " + uid;
	int numInstances = 0;
	try {
	    Connection conn = ImageDb.getConnection();
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery( sql );
	    if ( rs.next() ) {
		numInstances = rs.getInt( 1 );
	    } else {
		// If execution comes here there is some weird error, e.g. sntax error in SQL
		System.err.println( "No records returned by: " + sql );
	    }
	} catch ( SQLException e ) {
	    System.err.println( "Error counting number of instances: " + e.getMessage() );
	}
	return numInstances;
    }


    /**
       Returns the arrayList that cotnains all instances of this file
    */
    public ArrayList getInstances() {
	if ( instances  == null ) {
	    instances = ImageFile.retrieveInstances( this );
	}
	return instances;
    }
    
    ArrayList instances = null;

    /**
       Return a single image instance based on its order number
       @param instanceNum Number of the instance to return
       @throws IndexOutOfBoundsException if instanceNum is < 0 or >= the number of instances 
    */
    public ImageFile getInstance( int instanceNum ) throws IndexOutOfBoundsException {
	ImageFile instance =  (ImageFile) getInstances().get(instanceNum );
	return instance;
    }

    

    /** Creates a new thumbnail for this image on specific volume
	@param volume The volume in which the instance is to be created
    */
    protected void createThumbnail( Volume volume ) {
	
	
	// Find the original image to use as a staring point
	ImageFile original = null;
	if ( instances == null ) {
	    instances = ImageFile.retrieveInstances( this );
	}
	for ( int n = 0; n < instances.size(); n++ ) {
	    ImageFile instance = (ImageFile) instances.get( n );
	    if ( instance.getFileHistory() == ImageFile.FILE_HISTORY_ORIGINAL ) {
		original = instance;
		break;
	    } 
	}
	if ( original == null ) {
	    System.err.println( "Error - no original image was found!!!" );
	}
	
	// Read the image
	BufferedImage origImage = null;
	try {
	    origImage = ImageIO.read( original.getFile() );
	} catch ( IOException e ) {
	    System.err.println( "Error reading image: " + e.getMessage() );
	    return;
	}
	
	// Find where to store the file in the target volume
	File thumbnailFile = volume.getInstanceName( this, "jpg" );
	
	// Shrink the image to desired state and save it
	// Find first the correct transformation for doing this
	int origWidth = origImage.getWidth();
	int origHeight = origImage.getHeight();
	int maxThumbWidth = 100;
	int maxThumbHeight = 100;
	float widthScale = (float) maxThumbWidth / (float) origWidth;
	float heightScale = (float) maxThumbHeight / (float) origHeight;
	float scale = widthScale;
	if ( heightScale < widthScale ) {
	    scale = heightScale;
	}
	
	System.err.println( "Scaling to thumbnail from (" + origWidth + ", " + origHeight + " by " + scale );
	// Thhen create the xform
	AffineTransform at = new AffineTransform();
	at.scale( scale, scale );
	AffineTransformOp scaleOp = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );

	// Create the target image
	int thumbWidth = (int)(((float)origWidth) * scale);
	int thumbHeight = (int)(((float)origHeight) * scale);
	System.err.println( "Thumbnail size (" + thumbWidth + ", " + thumbHeight + ")" );
	BufferedImage thumbImage = new BufferedImage( thumbWidth, thumbHeight,
						      origImage.getType() );
	scaleOp.filter( origImage, thumbImage );

	// Save it
	try {	    
	    ImageIO.write( thumbImage, "jpg", thumbnailFile );
	} catch ( IOException e ) {
	    System.err.println( "Error writing thumbnail: " + e.getMessage() );
	}

	// add the created instance to this perdsisten object
	addInstance( thumbnailFile.getParent(), thumbnailFile.getName(),
		     ImageFile.FILE_HISTORY_THUMBNAIL );
    }

    /** Creates a new thumbnail on the default volume
     */
    protected void createThumbnail() {
	Volume vol = Volume.getDefaultVolume();
	createThumbnail( vol );
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

}
