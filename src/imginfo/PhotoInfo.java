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
public class PhotoInfo {

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
	    photo.shutterSpeed = rs.getDouble( "shutter_speed" );
	    photo.camera = rs.getString( "camera" );
	    photo.lens = rs.getString( "lens" );
	    photo.film = rs.getString( "film" );
	    photo.filmSpeed = rs.getInt( "film_speed" );
	    photo.description = rs.getString( "description" );
	    
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
	    stmt.executeUpdate( "INSERT INTO photos values ( " + photo.uid + ", NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)" );
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
	photo.addInstance( vol, f, ImageInstance.INSTANCE_TYPE_ORIGINAL );
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
	    getInstances();
	}
	for ( int i = 0; i < instances.size(); i++ ) {
	    ImageInstance f = (ImageInstance) instances.get( i );
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
	String sql = "UPDATE photos SET shooting_place = ?, photographer = ?, f_stop = ?, focal_length = ?, shoot_time = ?, shutter_speed = ?, camera = ?, lens = ?, film = ?, film_speed = ?, description = ? WHERE photo_id = ?";
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
	    stmt.setDouble( 6, shutterSpeed );
	    stmt.setString( 7, camera );
	    stmt.setString( 8, lens );
	    stmt.setString( 9, film );
	    stmt.setInt( 10, filmSpeed );
	    stmt.setString( 11, description );
	    stmt.setInt( 12, uid );
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
       @param volume Volume in which the instance is stored
       @param instanceName File name of the instance
       @instanceType Type of the instance - original, modified or thumbnail.
       @see ImageInstance class documentation for details.
    */
    public void addInstance( Volume volume, File instanceFile, int instanceType ) {
	ArrayList origInstances = getInstances();
	ImageInstance instance = ImageInstance.create( volume, instanceFile, this );
	instance.setInstanceType( instanceType );
	origInstances.add( instance );
    }

    /**
       Returns the number of instances of this photo that are stored in database
    */
    public int getNumInstances() {
	// Count number of instances from database
	String sql = "SELECT COUNT(*) FROM image_instances WHERE photo_id = " + uid;
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
	    instances = ImageInstance.retrieveInstances( this );
	}
	return instances;
    }
    
    ArrayList instances = null;

    /**
       Return a single image instance based on its order number
       @param instanceNum Number of the instance to return
       @throws IndexOutOfBoundsException if instanceNum is < 0 or >= the number of instances 
    */
    public ImageInstance getInstance( int instanceNum ) throws IndexOutOfBoundsException {
	ImageInstance instance =  (ImageInstance) getInstances().get(instanceNum );
	return instance;
    }

    

    /** Creates a new thumbnail for this image on specific volume
	@param volume The volume in which the instance is to be created
    */
    protected void createThumbnail( Volume volume ) {

	// Find the original image to use as a staring point
	ImageInstance original = null;
	if ( instances == null ) {
	    getInstances();
	}
	for ( int n = 0; n < instances.size(); n++ ) {
	    ImageInstance instance = (ImageInstance) instances.get( n );
	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
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
	    origImage = ImageIO.read( original.getImageFile() );
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
	addInstance( volume, thumbnailFile,
		     ImageInstance.INSTANCE_TYPE_THUMBNAIL );
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
    double shutterSpeed;
    
    /**
     * Get the value of shutterSpeed.
     * @return value of shutterSpeed.
     */
    public double getShutterSpeed() {
	return shutterSpeed;
    }
    
    /**
     * Set the value of shutterSpeed.
     * @param v  Value to assign to shutterSpeed.
     */
    public void setShutterSpeed(double  v) {
	this.shutterSpeed = v;
    }
    String camera;
    
    /**
     * Get the value of camera.
     * @return value of camera.
     */
    public String getCamera() {
	return camera;
    }
    
    /**
     * Set the value of camera.
     * @param v  Value to assign to camera.
     */
    public void setCamera(String  v) {
	this.camera = v;
    }
    String lens;
    
    /**
     * Get the value of lens.
     * @return value of lens.
     */
    public String getLens() {
	return lens;
    }
    
    /**
     * Set the value of lens.
     * @param v  Value to assign to lens.
     */
    public void setLens(String  v) {
	this.lens = v;
    }
    String film;
    
    /**
     * Get the value of film.
     * @return value of film.
     */
    public String getFilm() {
	return film;
    }
    
    /**
     * Set the value of film.
     * @param v  Value to assign to film.
     */
    public void setFilm(String  v) {
	this.film = v;
    }
    int filmSpeed;
    
    /**
     * Get the value of filmSpeed.
     * @return value of filmSpeed.
     */
    public int getFilmSpeed() {
	return filmSpeed;
    }
    
    /**
     * Set the value of filmSpeed.
     * @param v  Value to assign to filmSpeed.
     */
    public void setFilmSpeed(int  v) {
	this.filmSpeed = v;
    }
    String description;
    
    /**
     * Get the value of description.
     * @return value of description.
     */
    public String getDescription() {
	return description;
    }
    
    /**
     * Set the value of description.
     * @param v  Value to assign to description.
     */
    public void setDescription(String  v) {
	this.description = v;
    }

    static private boolean isEqual( Object o1, Object o2 ) {
	if ( o1 == null ) {
	    if ( o2 == null ) {		
		return true;
	    } else {
		return false;		
	    }
	}
	return o1.equals( o2 );
    }
    
    public boolean equals( Object obj ) {
	if ( obj.getClass() != this.getClass() ) {
	    return false;
	}
	PhotoInfo p = (PhotoInfo)obj;
	return ( isEqual( p.photographer, this.photographer )
		 && isEqual( p.shootingPlace, this.shootingPlace )
		 && isEqual( p.shootTime, this.shootTime )
		 && isEqual(p.description, this.description )
		 && isEqual( p.camera, this.camera )
		 && isEqual( p.lens, this.lens )
		 && isEqual( p.film, this.film )
		 && p.shutterSpeed == this.shutterSpeed
		 && p.filmSpeed == this.filmSpeed
		 && p.focalLength == this.focalLength
		 && p.FStop == this.FStop
		 && p.uid == this.uid );
    }
}
