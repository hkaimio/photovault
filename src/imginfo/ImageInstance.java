// ImageInstance.java

package imginfo;

import dbhelper.*;
import org.odmg.*;
import java.sql.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.io.*;

/**
   This class abstracts a single instance of a image that is stored in a file.
*/

public class ImageInstance {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ImageInstance.class.getName() );


    /**
       Creates a new image file object. The object is persistent,
       i.e. it is stored in database
       @param volume Volume in which the instance is stored
       @param imageFile File object pointing to the image instance
       file
       @param photo PhotoInfo object that represents the content of
       the image file
       @return A ImageInstance object

    */
    public static ImageInstance create( Volume volume, File imageFile, PhotoInfo photo ) {

	log.debug( "Creating instance, volume = " + volume.getName() + " photo = " + photo.getUid()
		   + " image file = " + imageFile.getName() );
	// Initialize transaction context
	ODMGXAWrapper txw = new ODMGXAWrapper();
	
	ImageInstance f = new ImageInstance();
	// Set up the primary key fields before locking the object
	f.volume = volume;
	f.volumeId = volume.getName();
	f.imageFile = imageFile;
	f.fname = imageFile.getName();
	txw.lock( f, Transaction.WRITE );
	log.debug( "locked instance" );
	
	f.photoUid = photo.getUid();
	// Read the rest of fields from the image file
	try {
	    f.readImageFile();
	} catch (IOException e ) {
	    txw.abort();
	    log.warn( "Error opening image file: " + e.getMessage() );
	    // The image does not exist, so it cannot be read!!!
	    return null;
	}
	txw.commit();
	return f;
    }

    /**
       Retrieves the info record for a image file based on its name and path.
       @param volume Volume in which the instance is stored
       @param fname Finle name for the image
       @return ImageFile object representing the image
       @throws PhotoNotFoundException exception if the object can not be retrieved.
    */
    
    public static ImageInstance retrieve( Volume volume, String fname ) throws PhotoNotFoundException {
	String oql = "select instance from " + ImageInstance.class.getName()
	    + " where volumeId = \"" + volume.getName() + "\" and fname = \"" + fname + "\"";

	DList instances = null;

	// Get transaction context
	ODMGXAWrapper txw = new ODMGXAWrapper();
	Implementation odmg = ODMG.getODMGImplementation();
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( oql );
	    instances = (DList) query.execute();
	    txw.commit();
	} catch ( Exception e ) {
	    txw.abort();
	    return null;
	}
	
	ImageInstance instance = (ImageInstance) instances.get( 0 );
	return instance;
    }

    /**
       Inits the complex attributes vulome and imageFile. Since these
       are not mapped directly to database columns, this function will
       be called by OJB Rowreader to initialize these correctly after
       the object has been read from database.
    */
    protected void initFileAttrs() {
	volume = Volume.getVolume( volumeId );
	try {
	    imageFile = volume.mapFileName( fname );
	} catch ( Exception e ) {
	    log.warn( "Error while initializing imageFile: " + e.getMessage() );
	}
    }
    
    
    // Unnecessary when using OJB
//     /**
//        Retrieve all instances of a specified photo from DB
//        @param photo The PhotoInfo whose instances to retrieve
//        @return ArrayList containing the photoInfo objects
//     */

//     public static ArrayList retrieveInstances( PhotoInfo photo ) {
// 	ArrayList instances = new ArrayList();
// 	String sql = "select * FROM image_instances WHERE photo_id = " + photo.getUid();
// 	try {
// 	    Connection conn = ImageDb.getConnection();
// 	    Statement stmt = conn.createStatement( );
// 	    ResultSet rs = stmt.executeQuery( sql );
// 	    ImageInstance f = createFromResultSet( rs );
// 	    while ( f != null ) {
// 		instances.add( f );
// 		f = createFromResultSet( rs );
// 	    }
// 	    rs.close();
// 	    stmt.close();
// 	} catch ( SQLException e ) {
// 	    log.warn( "Error fetching image file from db: " + e.getMessage() );
// 	}
// 	return instances;
//     }

	
	
    // TODO: remove this
    private static ImageInstance createFromResultSet( ResultSet rs ) {
	ImageInstance instance = null;
	try {
	    if ( rs.next() ) {
		instance = new ImageInstance();
		String volName =  rs.getString( "volume_id" );
		instance.volume = Volume.getVolume( volName );
		String fname =  rs.getString( "fname" );
		try {
		    instance.imageFile = instance.volume.mapFileName( fname );
		} catch ( FileNotFoundException e ) {
		    System.err.println( "File not found: " + e.getMessage() );
		}
		instance.photoUid = rs.getInt( "photo_id" );
		instance.width = rs.getInt( "width" );
		instance.height = rs.getInt( "height" );
		instance.rotated = rs.getDouble( "rotated" );
		String strInstanceType = rs.getString( "instance_type" );
		if ( strInstanceType.equals( "original" ) ) {
		    instance.instanceType = INSTANCE_TYPE_ORIGINAL;
		} else if ( strInstanceType.equals( "modified" ) ) {
		    instance.instanceType = INSTANCE_TYPE_MODIFIED;
		} else {
		    instance.instanceType = INSTANCE_TYPE_THUMBNAIL;
		}
		// TODO: Add other parameters
	    }
	} catch ( SQLException e ) {
	    // Something went wrong, so let's not return the created object
	    instance =  null;
	    System.err.println( "Error reading ImageInstance from DB: " + e.getMessage() );
	}
	return instance;
    }
	
//     /**
//        Updates the corresponding record in database to match any modifications to the object
//     */
//     public void updateDB() {
// 	String sql = "UPDATE image_instances SET photo_id = ?, width = ?, height = ?, rotated = ?, instance_type = ? WHERE volume_id = ? AND fname = ?";
// 	try {
// 	    Connection conn = ImageDb.getConnection();
// 	    PreparedStatement stmt = conn.prepareStatement( sql );
// 	    stmt.setInt( 1, photoUid );
// 	    stmt.setInt( 2, width );
// 	    stmt.setInt( 3, height );
// 	    stmt.setDouble( 4, rotated );
// 	    String strInstanceType = null;
// 	    switch ( instanceType ) {
// 	    case INSTANCE_TYPE_ORIGINAL :
// 		strInstanceType = "original";
// 		break;
// 	    case INSTANCE_TYPE_MODIFIED:
// 		strInstanceType = "modified";
// 		break;
// 	    case INSTANCE_TYPE_THUMBNAIL:
// 		strInstanceType = "thumbnail";
// 		break;
// 	    default:
// 		log.warn( "This is not an allowed value" );
// 	    }
// 	    stmt.setString( 5, strInstanceType );
// 	    stmt.setString( 6, volume.getName() );
// 	    stmt.setString( 7, imageFile.getName() );
// 	    stmt.executeUpdate();
// 	    stmt.close();
// 	} catch ( SQLException e ) {
// 	    log.warn( "Error updating image instance in DB: " + e.getMessage() );
// 	}
//     }

    /**
       Deletes the ImageInstance object from database.
    */
    public void delete() {
	Implementation odmg = ODMG.getODMGImplementation();
	Database db = ODMG.getODMGDatabase();
	
	ODMGXAWrapper txw = new ODMGXAWrapper();
	db.deletePersistent( this );
	imageFile.delete();
	txw.commit();
    }

    /**
       Opens the image file specified by fname & dirname properties and reads the rest of fields from that
       @throws IOException if the image cannot be read.
    */	    
    protected void readImageFile() throws IOException {

	// Find the JPEG image reader
	// TODO: THis shoud decode also other readers from fname
	Iterator readers = ImageIO.getImageReadersByFormatName("jpg");
	ImageReader reader = (ImageReader)readers.next();
	ImageInputStream iis = ImageIO.createImageInputStream( imageFile );
	
	reader.setInput( iis, true );
	
	width = reader.getWidth( 0 );
	height = reader.getHeight( 0 );

	iis.close();
	
    }

    /**
       Id of the volume (for OJB)
    */
    String volumeId;
    /**
       reference to the volume where the image file is located
    */
    Volume volume;
    
    /**
     * Get the value of volume.
     * @return value of volume.
     */
    public Volume getVolume() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return volume;
    }
    
    /**
     * Set the value of volume.
     * @param v  Value to assign to volume.
     */
    public void setVolume(Volume  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.volume = v;
	volumeId = volume.getName();
	txw.commit();
    }

    /**
       The image file
    */
    File imageFile;

    /**
       File name of the image file without directory (as returner by imageFile.getName()).
       Used as OJB reference.
    */
    String fname;

    
    /**
     * Get the value of imageFile.
     * @return value of imageFile.
     */
    public File getImageFile() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
        log.debug( "ImageFile = " + imageFile + " volume = " + volume.getName() + " base dir = " + volume.getBaseDir() );
	return imageFile;
    }
    
    /**
     * Set the value of imageFile.
     * @param v  Value to assign to imageFile.
     */
    public void setImageFile(File  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.imageFile = v;
	fname = imageFile.getName();
	txw.commit();
    }

    /**
       Returns the file path relative to the volume base directory
    */

    public String getRelativePath() {
	String baseDir = null;
	try {
	    baseDir = volume.getBaseDir().getCanonicalPath();
	} catch ( IOException e ) {}
	    
	String filePath = null;
	try {
	    filePath = imageFile.getCanonicalPath();
	} catch ( IOException e ) {}
	
	// Make a sanity check that the image file is uder the base directory
	if ( !filePath.substring( 0, baseDir.length() ).equals( baseDir ) ) {
	    log.warn( "ERROR: " + filePath + " not under " + baseDir );
	    return "";
	}

	String relPath = filePath.substring( baseDir.length() );
	return relPath;
    }

    
    private int width;
    
    /**
     * Get the value of width.
     * @return value of width.
     */
    public int getWidth() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return width;
    }
    
    /**
     * Set the value of width.
     * @param v  Value to assign to width.
     */
    public void setWidth(int  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.width = v;
	txw.commit();
    }
    int height;
    
    /**
     * Get the value of height.
     * @return value of height.
     */
    public int getHeight() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return height;
    }
    
    /**
     * Set the value of height.
     * @param v  Value to assign to height.
     */
    public void setHeight(int  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.height = v;
	txw.commit();
    }

    // File history
    /**
       The file is a original
    */
    final public static int INSTANCE_TYPE_ORIGINAL = 1;
    /**
       The file has been created from original image by e.g. changing resolution, file format,
       by doing some image procesing...
    */
    final public static int INSTANCE_TYPE_MODIFIED = 2;
    /**
       The file is intended to be used only as a thumbnail, not by other appilcations
    */
    final public static int INSTANCE_TYPE_THUMBNAIL = 3;
    
    int instanceType = INSTANCE_TYPE_ORIGINAL;
    
    /**
     * Get the value of instanceType.
     * @return value of instanceType.
     */
    public int getInstanceType() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return instanceType;
    }
    
    /**
     * Set the value of instanceType.
     * @param v  Value to assign to instanceType.
     */
    public void setInstanceType(int  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.instanceType = v;
	txw.commit();
    }

    double rotated;
    
    /**
     * Get the amount this instance is rotated compared to the original image.
     * @return value of rotated.
     */
    public double getRotated() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return rotated;
    }
    
    /**
     * Set the amount this image is rotated when compared to the original image
     * @param v  Value to assign to rotated.
     */
    public void setRotated(double  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.rotated = v;
	txw.commit();
    }
    
    int photoUid;
    
}
