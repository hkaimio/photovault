// ImageInstance.java

package imginfo;

import dbhelper.*;
import java.sql.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.io.*;

/**
   This class abstracts a single instance of a image that is stored in a file.
*/

public class ImageInstance {

    /**
       Creates a new image file object. The object is persistent, i.e. it is stored in database
       @param volume Volume in which the instance is stored
       @param imageFile File object pointing to the image instance file
       @param photo PhotoInfo object that represents the content of the image file
       @return A ImageInstance object
    */
    public static ImageInstance create( Volume volume, File imageFile, PhotoInfo photo ) {
	String sql = "INSERT INTO image_instances VALUES ( ?, ?, ?, ?, ?, ? )";
	ImageInstance f = new ImageInstance();
	f.volume = volume;
	f.imageFile = imageFile;
	f.photoUid = photo.getUid();
	// Read the rest of fields from the image file
	try {
	    f.readImageFile();
	} catch (IOException e ) {
	    System.err.println( "Error opening image file: " + e.getMessage() );
	    // The image does not exist, so it cannot be read!!!
	    return null;
	}
	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setString( 1, volume.getName() );
	    stmt.setString( 2, imageFile.getName() );
	    stmt.setInt( 3, photo.getUid() );
	    stmt.setInt( 4, f.getWidth() ); // width
	    stmt.setInt( 5, f.getHeight() ); // height
	    stmt.setString( 6, "original" );
	    stmt.executeUpdate();
	    stmt.close();
	} catch  (SQLException e ) {
	    System.err.println( "Error creating ImageFile: " + e.getMessage() );
	    // Something went wrong, do not return this image file!!!
	    f = null;
	}
	return f;
    }

    /**
       Retrieves the info record for a image file based on its name and path.
       @param dirname Directory of the image file
       @param fname Finle name for the image
       @return ImageFile object representing the image
       @throws PhotoNotFound exception if the object can not be retrieved.
    */
    
    public static ImageInstance retrieve( Volume volume, String fname ) throws PhotoNotFoundException {
	String sql = "SELECT * FROM image_instances WHERE volume_id = ? AND fname = ?";
	ImageInstance instance = null;
	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setString( 1, volume.getName() );
	    stmt.setString( 2, fname );
	    ResultSet rs = stmt.executeQuery();
	    instance = createFromResultSet( rs );
	    if ( instance == null ) {
		throw new PhotoNotFoundException();
	    }
	    rs.close();
	    stmt.close();
		
	} catch ( SQLException e ) {
	    System.err.println( "Error fetching image file from db: " + e.getMessage() );
	    throw new PhotoNotFoundException();
	}
	return instance;
    }
    
    /**
       Retrieve all instances of a specified photo from DB
       @param photo The PhotoInfo whose instances to retrieve
       @return ArrayList containing the photoInfo objects
    */

    public static ArrayList retrieveInstances( PhotoInfo photo ) {
	ArrayList instances = new ArrayList();
	String sql = "select * FROM image_instances WHERE photo_id = " + photo.getUid();
	try {
	    Connection conn = ImageDb.getConnection();
	    Statement stmt = conn.createStatement( );
	    ResultSet rs = stmt.executeQuery( sql );
	    ImageInstance f = createFromResultSet( rs );
	    while ( f != null ) {
		instances.add( f );
		f = createFromResultSet( rs );
	    }
	    rs.close();
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error fetching image file from db: " + e.getMessage() );
	}
	return instances;
    }

	
	
	
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
	
    /**
       Updates the corresponding record in database to match any modifications to the object
    */
    public void updateDB() {
	String sql = "UPDATE image_instances SET photo_id = ?, width = ?, height = ?, instance_type = ? WHERE volume_id = ? AND fname = ?";
	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setInt( 1, photoUid );
	    stmt.setInt( 2, width );
	    stmt.setInt( 3, height );
	    String strInstanceType = null;
	    switch ( instanceType ) {
	    case INSTANCE_TYPE_ORIGINAL :
		strInstanceType = "original";
		break;
	    case INSTANCE_TYPE_MODIFIED:
		strInstanceType = "modified";
		break;
	    case INSTANCE_TYPE_THUMBNAIL:
		strInstanceType = "thumbnail";
		break;
	    default:
		System.err.println( "This is not an allowed value" );
	    }
	    stmt.setString( 4, strInstanceType );
	    stmt.setString( 5, volume.getName() );
	    stmt.setString( 6, imageFile.getName() );
	    stmt.executeUpdate();
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error updating image instance in DB: " + e.getMessage() );
	}
    }

    /**
       Deletes the ImageInstance object from database.
    */
    public void delete() {
	// Delete the database record for this image instance
	String sql = "DELETE FROM image_instances WHERE volume_id = ? AND fname = ?";
	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setString( 1, volume.getName() );
	    stmt.setString( 2, imageFile.getName() );
	    stmt.executeUpdate();
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error deletin image file from DB: " + e.getMessage() );
	}
	// Delete the actual image instance from volume
	imageFile.delete();
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
    Volume volume;
    
    /**
     * Get the value of volume.
     * @return value of volume.
     */
    public Volume getVolume() {
	return volume;
    }
    
    /**
     * Set the value of volume.
     * @param v  Value to assign to volume.
     */
    public void setVolume(Volume  v) {
	this.volume = v;
    }
    File imageFile;
    
    /**
     * Get the value of imageFile.
     * @return value of imageFile.
     */
    public File getImageFile() {
	return imageFile;
    }
    
    /**
     * Set the value of imageFile.
     * @param v  Value to assign to imageFile.
     */
    public void setImageFile(File  v) {
	this.imageFile = v;
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
	    System.err.println( "ERROR: " + filePath + " not under " + baseDir );
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
	return width;
    }
    
    /**
     * Set the value of width.
     * @param v  Value to assign to width.
     */
    public void setWidth(int  v) {
	this.width = v;
    }
    int height;
    
    /**
     * Get the value of height.
     * @return value of height.
     */
    public int getHeight() {
	return height;
    }
    
    /**
     * Set the value of height.
     * @param v  Value to assign to height.
     */
    public void setHeight(int  v) {
	this.height = v;
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
    
    int instanceType;
    
    /**
     * Get the value of instanceType.
     * @return value of instanceType.
     */
    public int getInstanceType() {
	return instanceType;
    }
    
    /**
     * Set the value of instanceType.
     * @param v  Value to assign to instanceType.
     */
    public void setInstanceType(int  v) {
	this.instanceType = v;
    }

    int photoUid;
    
}
