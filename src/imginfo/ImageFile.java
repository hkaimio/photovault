// ImageFile.java

package imginfo;

import dbhelper.*;
import java.sql.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.io.*;

/**
   This class abstracts an image file, i.e. single instance of a photo that is stored in certain
   place and certain format.
*/

public class ImageFile {

    /**
       Creates a new image file object. The object is persistent, i.e. it is stored in database
       @param dirname Name of the directory in which the file is stored
       @param fname name of the image file
       @param photo PhotoInfo object that represents the content of the image file
       @return A ImageFile object
    */
    public static ImageFile create( String dirname, String fname, PhotoInfo photo ) {
	String sql = "INSERT INTO image_files VALUES ( ?, ?, ?, ?, ?, ? )";
	ImageFile f = new ImageFile();
	f.dirname = dirname;
	f.fname = fname;
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
	    stmt.setString( 1, dirname );
	    stmt.setString( 2, fname );
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
       Retrieves the info record for a image file basewd on its name and path.
       @param dirname Directory of the image file
       @param fname Finle name for the image
       @return ImageFile object representing the image
       @throws PhotoNotFound exception if the object can not be retrieved.
    */
    
    public static ImageFile retrieve( String dirname, String fname ) throws PhotoNotFoundException {
	String sql = "SELECT * FROM image_files WHERE dirname = ? AND fname = ?";
	ImageFile ifile = null;
	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setString( 1, dirname );
	    stmt.setString( 2, fname );
	    ResultSet rs = stmt.executeQuery();
	    ifile = createFromResultSet( rs );
	    if ( ifile == null ) {
		throw new PhotoNotFoundException();
	    }
	    rs.close();
	    stmt.close();
		
	} catch ( SQLException e ) {
	    System.err.println( "Error fetching image file from db: " + e.getMessage() );
	    throw new PhotoNotFoundException();
	}
	return ifile;    }

    /**
       Retrieve all instances of a specified photo from DB
       @param photo The PhotoInfo whose instances to retrieve
       @return ArrayList containing the photoInfo objects
    */

    public static ArrayList retrieveInstances( PhotoInfo photo ) {
	ArrayList instances = new ArrayList();
	String sql = "select * FROM image_files WHERE photo_id = " + photo.getUid();
	try {
	    Connection conn = ImageDb.getConnection();
	    Statement stmt = conn.createStatement( );
	    ResultSet rs = stmt.executeQuery( sql );
	    ImageFile f = createFromResultSet( rs );
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

	
	
	
    private static ImageFile createFromResultSet( ResultSet rs ) {
	ImageFile ifile = null;
	try {
	    if ( rs.next() ) {
		ifile = new ImageFile();
		ifile.dirname = rs.getString( "dirname" );
		ifile.fname = rs.getString( "fname" );
		ifile.photoUid = rs.getInt( "photo_id" );
		ifile.width = rs.getInt( "width" );
		ifile.height = rs.getInt( "height" );
		String strFileHist = rs.getString( "filehist" );
		if ( strFileHist.equals( "original" ) ) {
		    ifile.fileHistory = FILE_HISTORY_ORIGINAL;
		} else if ( strFileHist.equals( "modified" ) ) {
		    ifile.fileHistory = FILE_HISTORY_MODIFIED;
		} else {
		    ifile.fileHistory = FILE_HISTORY_THUMBNAIL;
		}
		// TODO: Add other parameters
	    }
	} catch ( SQLException e ) {
	    // Something went wrong, so let's not return the created object
	    ifile = null;
	    System.err.println( "Error reading ImageFile from DB: " + e.getMessage() );
	}
	return ifile;
    }
	
    /**
       Updates the corresponding record in database to match any modifications to the object
    */
    public void updateDB() {
	String sql = "UPDATE image_files SET photo_id = ?, width = ?, height = ?, filehist = ? WHERE dirname = ? AND fname = ?";
	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setInt( 1, photoUid );
	    stmt.setInt( 2, width );
	    stmt.setInt( 3, height );
	    String strFileHist = null;
	    switch ( fileHistory ) {
	    case FILE_HISTORY_ORIGINAL :
		strFileHist = "original";
		break;
	    case FILE_HISTORY_MODIFIED:
		strFileHist = "modified";
		break;
	    case FILE_HISTORY_THUMBNAIL:
		strFileHist = "thumbnail";
		break;
	    default:
		System.err.println( "This is not an allowed value" );
	    }
	    stmt.setString( 4, strFileHist );
	    stmt.setString( 5, dirname );
	    stmt.setString( 6, fname );
	    stmt.executeUpdate();
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error updating image file in DB: " + e.getMessage() );
	}
    }

    /**
       Deletes the image file object from database.
    */
    public void delete() {
	String sql = "DELETE FROM image_files WHERE  dirname = ? AND fname = ?";
	try {
	    Connection conn = ImageDb.getConnection();
	    PreparedStatement stmt = conn.prepareStatement( sql );
	    stmt.setString( 1, dirname );
	    stmt.setString( 2, fname );
	    stmt.executeUpdate();
	    stmt.close();
	} catch ( SQLException e ) {
	    System.err.println( "Error deletin image file from DB: " + e.getMessage() );
	}
    }

    /**
       Opens the image file specified by fname & dirname properties and reads the rest of fields from that
       @throws IOException if the image cannot be read.
    */	    
    protected void readImageFile() throws IOException {
	File f = new File( dirname, fname );

	// Find the JPEG image reader
	// TODO: THis shoud decode also other readers from fname
	Iterator readers = ImageIO.getImageReadersByFormatName("jpg");
	ImageReader reader = (ImageReader)readers.next();
	ImageInputStream iis = ImageIO.createImageInputStream( f );
	
	reader.setInput( iis, true );
	
	width = reader.getWidth( 0 );
	height = reader.getHeight( 0 );

	iis.close();
	
    }
    
    String dirname;
    
    /**
     * Get the value of dirname.
     * @return value of dirname.
     */
    public String getDirname() {
	return dirname;
    }
    
    /**
     * Set the value of dirname.
     * @param v  Value to assign to dirname.
     */
    public void setDirname(String  v) {
	this.dirname = v;
    }
    String fname;
    
    /**
     * Get the value of fname.
     * @return value of fname.
     */
    public String getFname() {
	return fname;
    }
    
    /**
     * Set the value of fname.
     * @param v  Value to assign to fname.
     */
    public void setFname(String  v) {
	this.fname = v;
    }
    int width;
    
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
    final public static int FILE_HISTORY_ORIGINAL = 1;
    /**
       The file has been created from original image by e.g. changing resolution, file format,
       by doing some image procesing...
    */
    final public static int FILE_HISTORY_MODIFIED = 2;
    /**
       The file is intended to be used only as a thumbnail, not by other appilcations
    */
    final public static int FILE_HISTORY_THUMBNAIL = 3;
    
    int fileHistory;
    
    /**
     * Get the value of fileHistory.
     * @return value of fileHistory.
     */
    public int getFileHistory() {
	return fileHistory;
    }
    
    /**
     * Set the value of fileHistory.
     * @param v  Value to assign to fileHistory.
     */
    public void setFileHistory(int  v) {
	this.fileHistory = v;
    }

    int photoUid;
    
}
