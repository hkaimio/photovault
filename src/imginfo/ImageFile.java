// ImageFile.java

package imginfo;

/**
   This class abstracts a image file, i.e. single instance of a photo that is stored in certain
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
    public static ImageFile create( String driname, String fname, PhotoInfo photo ) {
	// TODO: Implement this
	return null;
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
    
    public void updateDB() {
	// TODO: implement this
    }
}
