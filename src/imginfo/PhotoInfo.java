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
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.imaging.jpeg.*;

/**
   PhotoInfo represents information about a single photograph
   TODO: write a decent doc!!!
*/
public class PhotoInfo {

    public PhotoInfo() {
	changeListeners = new HashSet();
    }
    
    /**
       Static method to load photo info from database by photo id
       @param photoId ID of the photo to be retrieved
    */
    public static PhotoInfo retrievePhotoInfo( int photoId ) throws PhotoNotFoundException {
	String sql = "SELECT * from photos where photo_id=\"" + photoId +"\"";
	PhotoInfo photo = null;
	try {
	    Connection conn  = ImageDb.getConnection();
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery( sql );
	    if ( rs.next() ) {
		photo = createFromResultSet( rs );
	    }
	    rs.close();
	    stmt.close();
	    if ( photo == null ) {
		throw new PhotoNotFoundException();
	    }
// 	    if ( !rs.next() ) {
// 		throw new PhotoNotFoundException();
// 	    }
// 	    photo.uid = rs.getInt( "photo_id" );
// 	    photo.shootingPlace = rs.getString( "shooting_place" );
// 	    photo.photographer = rs.getString( "photographer" );
// 	    photo.FStop = rs.getDouble( "f_stop" );
// 	    photo.focalLength = rs.getDouble( "focal_length" );
// 	    photo.shootTime = rs.getDate( "shoot_time" );
// 	    photo.shutterSpeed = rs.getDouble( "shutter_speed" );
// 	    photo.camera = rs.getString( "camera" );
// 	    photo.lens = rs.getString( "lens" );
// 	    photo.film = rs.getString( "film" );
// 	    photo.filmSpeed = rs.getInt( "film_speed" );
// 	    photo.description = rs.getString( "description" );
	    
	} catch (SQLException e ) {
	    System.err.println( "Error fetching record: " + e.getMessage() );
	    // TODO: Actually this is not the right exception for this purpose
	    throw new PhotoNotFoundException();
	}
	
	return photo;
    }

    /**
       This method reads a PhotoInfo structure from the current position in ResultSet.
       @param rs The ResultSet
       @return PhotoInfo object created or null if not succesful
    */
    public static PhotoInfo createFromResultSet( ResultSet rs ) {
	PhotoInfo photo = null;
	try {
	    photo = new PhotoInfo();
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
	    photo.prefRotation = rs.getDouble( "pref_rotation" );
	    photo.description = rs.getString( "description" );
	} catch ( SQLException e ) {
	    System.err.println( "Error fetching record: " + e.getMessage() );
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
	    stmt.executeUpdate( "INSERT INTO photos values ( " + photo.uid + ", NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)" );
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
	photo.updateFromFileMetadata( f );
	photo.updateDB();
	return photo;
    }

    /**
       This method reads the metadata from image file and updates the PhotoInfo record from it
       @param f The file to read
    */
    void updateFromFileMetadata( File f ) {
	ExifDirectory exif = null;
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(f);
	    if ( metadata.containsDirectory( ExifDirectory.class ) ) {
		try {
		    exif = (ExifDirectory) metadata.getDirectory( ExifDirectory.class );
		} catch ( MetadataException e ) {
		}
	    } else {
		// No known directory was found no reason to continue
		return;
	    }
        } catch (FileNotFoundException e) {
	    // If error, just return - this is just an additional 'nice-if-succesful' operation.
	    // If there is no metadata this will happen...
            return;
        }
	// Shooting date
	try {
	    java.util.Date origDate = exif.getDate( exif.TAG_DATETIME_ORIGINAL );
	    setShootTime( origDate );
	    System.err.println( "TAG_DATETIME_ORIGINAL: " + origDate.toString() );
	} catch ( MetadataException e ) {
	    System.err.println( "Error reading origDate: " + e.getMessage() );
	}

	// Exposure
	try {
	    double fstop = exif.getDouble( exif.TAG_FNUMBER );
	    System.err.println( "TAG_FNUMBER: " + fstop );
	    setFStop( fstop );
	} catch ( MetadataException e ) {
	    System.err.println( "Error reading origDate: " + e.getMessage() );
	}
	try {
	    double sspeed = exif.getDouble( exif.TAG_EXPOSURE_TIME );
	    setShutterSpeed( sspeed );
	    System.err.println( "TAG_EXPOSURE_TIME: " + sspeed );
	} catch ( MetadataException e ) {
	    System.err.println( "Error reading origDate: " + e.getMessage() );
	}
	try {
	    double flen = exif.getDouble( exif.TAG_FOCAL_LENGTH );
	    setFocalLength( flen );
	} catch ( MetadataException e ) {
	    System.err.println( "Error reading origDate: " + e.getMessage() );
	}
	try {
	    int filmSpeed = exif.getInt( exif.TAG_ISO_EQUIVALENT );
	    setFilmSpeed( filmSpeed );
	} catch ( MetadataException e ) {
	    System.err.println( "Error reading origDate: " + e.getMessage() );
	}

	// Camera name. Put here both camera manufacturer and model
	String maker = exif.getString( exif.TAG_MAKE );
	String model = exif.getString( exif.TAG_MODEL );
	setCamera( maker + " " + model );

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
	String sql = "UPDATE photos SET shooting_place = ?, photographer = ?, f_stop = ?, focal_length = ?, shoot_time = ?, shutter_speed = ?, camera = ?, lens = ?, film = ?, film_speed = ?, pref_rotation = ?, description = ? WHERE photo_id = ?";
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
	    stmt.setDouble( 11, prefRotation );
	    stmt.setString( 12, description );
	    stmt.setInt( 13, uid );
	    stmt.executeUpdate();
	    stmt.close();
	} catch (SQLException e ) {
	    System.err.println( "Error executing update: " + e.getMessage() );
	}
    }

    /**
       Adds a new listener to the list that will be notified of modifications to this object
       @param l reference to the listener
    */
    public void addChangeListener( PhotoInfoChangeListener l ) {
	changeListeners.add( l );
    }

    /**
       Removes a listenre
    */
    public void removeChangeListener( PhotoInfoChangeListener l ) {
	changeListeners.remove( l );
    }

    private void notifyListeners( PhotoInfoChangeEvent e ) {
	Iterator iter = changeListeners.iterator();
	while ( iter.hasNext() ) {
	    PhotoInfoChangeListener l = (PhotoInfoChangeListener) iter.next();
	    l.photoInfoChanged( e );
	}
    }

    protected void modified() {
	notifyListeners( new PhotoInfoChangeEvent( this ) );
    }

    /**
       set of the listeners that should be notified of any changes to this object
    */
    HashSet changeListeners = null;
    
    
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
       @return The new instance
       @see ImageInstance class documentation for details.
    */
    public ImageInstance addInstance( Volume volume, File instanceFile, int instanceType ) {
	ArrayList origInstances = getInstances();
	ImageInstance instance = ImageInstance.create( volume, instanceFile, this );
	instance.setInstanceType( instanceType );
	instance.updateDB();
	origInstances.add( instance );
	return instance;
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

    /**
       Returns a thumbnail of this image. If no thumbnail instance is yetavailable, creates a
       new instance on the default volume. Otherwise loads an existing thumbnail instance. <p>

       If thumbnail creation fails of if there is no image instances available at all, returns
       a default thumbnail image.
    */
    public Thumbnail getThumbnail() {
	if ( thumbnail == null ) {
	    // First try to find an instance from existing instances
	    ImageInstance original = null;
	    if ( instances == null ) {
		getInstances();
	    }
	    for ( int n = 0; n < instances.size(); n++ ) {
		ImageInstance instance = (ImageInstance) instances.get( n );
		if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL
		     && instance.getRotated() == prefRotation ) {
		    System.err.println( "Found existing thumbnail" );
		    thumbnail = Thumbnail.createThumbnail( this, instance.getImageFile() );
		    break;
		} 
	    }
	    if ( thumbnail == null ) {
		// Next try to create a new thumbnail instance
		createThumbnail();
	    }
	}
	if ( thumbnail == null ) {
	    // Thumbnail creating was not successful, most probably because there is no available instance
	    return Thumbnail.getDefaultThumbnail();
	}
	    
	return thumbnail;
    }

    Thumbnail thumbnail = null;

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
	    // If there are no instances, no thumbnail can be created
	    System.err.println( "Error - no original image was found!!!" );
	    return;
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

	AffineTransform xform = photovault.image.ImageXform.getFittingXform( maxThumbWidth, maxThumbHeight,
									     prefRotation -original.getRotated(),
									     origWidth, origHeight );
	
// 	// Determine if the image must be rotated
// 	double thumbRotation = (prefRotation - original.getRotated()) * (Math.PI/180.0);
// 	System.err.println( "thumbnail rotation: " + thumbRotation );
	
// 	// Thhen create the xform
// 	AffineTransform at = new AffineTransform();
//  	at.rotate( thumbRotation );
	
// 	// Determine the required target size of the thumbnail
// 	float[] corners = {0.0f,              0.0f,
// 			   0.0f,              (float) origHeight,
// 			   (float) origWidth, (float) origHeight,
// 			   (float) origWidth, 0.0f };
// 	at.transform( corners, 0, corners, 0, 4 );
// 	float minX = corners[0];
// 	float maxX = corners[0];
// 	float minY = corners[1];
// 	float maxY = corners[1];
// 	for ( int n = 2; n < corners.length; n += 2 ) {
// 	    if ( corners[n+1] < minY ) {
// 		minY = corners[n+1];
// 	    }
// 	    if ( corners[n+1] > maxY ) {
// 		maxY = corners[n+1];
// 	    }
// 	    if ( corners[n] < minX ) {
// 		minX = corners[n];
// 	    }
// 	    if ( corners[n] > maxX ) {
// 		maxX = corners[n];
// 	    }
// 	}

// 	System.err.println( "Translating by " + (-minX) + ", " + (-minY) );

// 	double rotatedWidth = maxX - minX;
// 	double rotatedHeight = maxY - minY;
// 	double widthScale = (double) maxThumbWidth / rotatedWidth;
// 	double heightScale = (double) maxThumbHeight / rotatedHeight;
// 	double scale = widthScale;
// 	if ( heightScale < widthScale ) {
// 	    scale = heightScale;
// 	}	
// 	at.preConcatenate( at.getTranslateInstance( scale*(-minX), scale*(-minY) ) );
// 	at.scale( scale, scale );
// 	int thumbWidth = (int) (scale * rotatedWidth);
// 	int thumbHeight = (int)(scale * rotatedHeight);
	
// 	System.err.println( "Scaling to thumbnail from (" + rotatedWidth + ", " + rotatedHeight + " by " + scale );

	// Create the target image
	AffineTransformOp atOp = new AffineTransformOp( xform, AffineTransformOp.TYPE_BILINEAR );
	
	BufferedImage thumbImage = atOp.filter( origImage, null );

	// Save it
	try {	    
	    ImageIO.write( thumbImage, "jpg", thumbnailFile );
	} catch ( IOException e ) {
	    System.err.println( "Error writing thumbnail: " + e.getMessage() );
	}

	// add the created instance to this perdsisten object
	ImageInstance thumbInstance = addInstance( volume, thumbnailFile,
		     ImageInstance.INSTANCE_TYPE_THUMBNAIL );
	thumbInstance.setRotated( prefRotation -original.getRotated() );
	thumbInstance.updateDB();
	
	thumbnail = Thumbnail.createThumbnail( this, thumbnailFile );
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
	modified();
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
	modified();
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
	modified();
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
	modified();
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
	modified();
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
	modified();
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
	modified();
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
	modified();
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
	modified();
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
	modified();
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
	modified();
    }

    double prefRotation;
    
    /**
     Get the preferred rotation for this image in degrees. Positive values indicate that the image should be
     rotated clockwise.
     @return value of prefRotation.
     */
    public double getPrefRotation() {
	return prefRotation;
    }
    
    /**
     * Set the value of prefRotation.
     * @param v  Value to assign to prefRotation.
     */
    public void setPrefRotation(double  v) {
	if ( v != prefRotation ) {
	    // Rotation changes, invalidate the thumbnail
	    thumbnail = null;
	}
	this.prefRotation = v;
	modified();
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
	modified();
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
