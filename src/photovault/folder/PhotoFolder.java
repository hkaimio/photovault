// $Id: PhotoFolder.java,v 1.2 2003/02/22 13:10:55 kaimio Exp $

package photovault.folder;


import imginfo.*;
import org.odmg.*;
import org.apache.ojb.odmg.*;
import java.util.*;

/**
   Implements a folder that can contain both PhotoInfos and other folders
*/

public class PhotoFolder implements PhotoCollection {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoFolder.class.getName() );

    public PhotoFolder() {
	subfolders = new Vector();
	changeListeners = new Vector();
    }
    
    /**
       Persistent ID for this folder
    */
    
    int folderId;
    
    /**
     * Get the value of folderId.
     * @return value of folderId.
     */
    public int getFolderId() {
	return folderId;
    }
    
    /**
       Name of this folder
    */
    String name;
    
    /**
     * Get the value of name.
     * @return value of name.
     */
    public String getName() {
	return name;
    }
    
    /**
     * Set the value of name.
     * @param v  Value to assign to name.
     */
    public void setName(String  v) {
	this.name = v;
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
    Date creationDate;
    
    /**
     * Get the value of creationDate.
     * @return value of creationDate.
     */
    public Date getCreationDate() {
	return creationDate;
    }

    // Implementation of PhotoCollection interface

    /**
     * Returns the number of photos in the folder
     */
    public int getPhotoCount()
    {
	// TODO: implement this imginfo.PhotoCollection method
	return 0;
    }

    /**
     * Returns a photo from the folder with given order number. Valid values [0..getPhotoCount[
     * @param param1 <description>
     * @return <description>
     */
    public PhotoInfo getPhoto(int param1)
    {
	// TODO: implement this imginfo.PhotoCollection method
	return null;
    }

    /**
       Add a new photo to the folder
    */
    public void addPhoto( PhotoInfo photo ) {
	// TODO: implement this method
    }
    

    /**
       Returns the numer of subfolders this folder has.
    */
    public int getSubfolderCount() {
	return subfolders.size();
    }

    /**
       Returns s subfolder with given order number.
    */
    public PhotoFolder getSubfolder( int num ) {
	return (PhotoFolder) subfolders.get( num );
    }

    /**
       Adds a new subfolder to this folder. This method is called by setParentFolder().
    */
    protected void addSubfolder( PhotoFolder subfolder ) {
	subfolders.add( subfolder );
	modified();
    }

    /**
       Removes a subfolder
    */
    protected void removeSubfolder( PhotoFolder subfolder ) {
	subfolders.remove( subfolder );
	modified();
    }
    
    /**
       All subfolders for this folder
    */
    Vector subfolders = null;
    
    /**
       Returns the parent of this folder or null if this is a top-level folder
    */
    public PhotoFolder getParentFolder() {
	return parent;
    }

    public void setParentFolder( PhotoFolder newParent ) {
	// If the parent is already set, remove the folder from its subfolders
	if ( parent != null ) {
	    parent.removeSubfolder( this );
	}
	this.parent = newParent;
	if ( parent != null ) {
	    parent.addSubfolder( this );
	}
	modified();
	
    }
    
    /**
       Parent of this folder or <code>null</code> if this is a top-level folder
    */
    PhotoFolder parent;

    /**
       Id of the parend folder (needed for persistence to work correctly)
    */
    int parentId;
    
    /**
     * Adds a new listener that will be notified of changes to the collection. Note that listeners are transient!!
     * @param listener The new listener
     */
    public void addPhotoCollectionChangeListener(PhotoCollectionChangeListener listener)
    {
	changeListeners.add( listener );
    }

    /**
     * Removes a listener from this collection.
     * @param l The listener that will be removed
     */
    public void removePhotoCollectionChangeListener(PhotoCollectionChangeListener l)
    {
	changeListeners.remove( l );
    }

    /**
       This method is called after the collection has been modified in some way. It notifies listeners and checks that the
       folder is part of some transaction. If it is not, a new transaction will be created and committed.
    */
    protected void modified() {
	// Find the current transaction or create a new one
	Transaction tx = odmg.currentTransaction();
	boolean mustCommit = false;
	if ( tx == null ) {
	    tx = odmg.newTransaction();
	    tx.begin();
	    mustCommit = true;
	}

	// Add  folder to the transaction
	tx.lock( this, Transaction.WRITE );

	// Notify the listeners. This is done in the same transaction context so that potential modifications to other
	// persistent objects are also saved
	notifyListeners();
	
	// If a new transaction was created, commit it
	if ( mustCommit ) {
	    tx.commit();
	}
    }
    
    protected void notifyListeners() {
	PhotoCollectionChangeEvent ev = new PhotoCollectionChangeEvent( this );
	Iterator iter = changeListeners.iterator();
	while ( iter.hasNext() ) {
	    PhotoCollectionChangeListener l = (PhotoCollectionChangeListener) iter.next();
	    l.photoCollectionChanged( ev );
	}
    }
	
    Vector changeListeners = null;

    /**
       Creates & returns a new persistent PhotoFolder object
       @param name Name of the new folder
       @param parent Parent folder of the new folder
    */
    public static PhotoFolder create( String name, PhotoFolder parent ) {
	getODMGImplementation();
	getODMGDatabase();

	// Get the current transaction of create a new
	Transaction tx = odmg.currentTransaction();
	boolean mustCommit = false;
	if ( tx == null ) {
	    tx = odmg.newTransaction();
	    tx.begin();
	    mustCommit = true;
	}
	PhotoFolder folder = new PhotoFolder();
	folder.setName( name );
	folder.setParentFolder( parent );
	tx.lock( folder, Transaction.WRITE );
	if ( mustCommit ) {
	    tx.commit();
	}
	return folder;
    }

    /**
       Returns the root folder for the PhotoFolder hierarchy, i.e. the folder with id 0.
    */
    public static PhotoFolder getRoot() {
	getODMGImplementation();
	getODMGDatabase();
	DList folders = null;
	Transaction tx = odmg.newTransaction();
	tx.begin();
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = 1" );
	    folders = (DList) query.execute();
	} catch ( Exception e ) {
	    tx.abort();
	    return null;
	}
	PhotoFolder rootFolder = (PhotoFolder) folders.get( 0 );
	return rootFolder;
    }

    /**
       Deletes this object from the persistent repository
    */
    public void delete() {
	getODMGImplementation();
	getODMGDatabase();
	Transaction tx = odmg.newTransaction();
	tx.begin();
	// First make sure that this object is deleted from its parent's subfolders list (if it has a parent)
	setParentFolder( null );
	try {
	    db.deletePersistent( this );
	    tx.commit();
	} catch ( Exception e ) {
	    tx.abort();
	    log.warn( "Error deleteing photo folder: " + e.getMessage() );
	}
    }
    
    
    // Functions to get the ODMG persistence layer handles. This should really be moved into
    // its own helper class

    static Implementation odmg = null;
    public static Implementation getODMGImplementation() {
	if ( odmg == null ) {
	    odmg = OJB.getInstance();
	}
	return odmg;
    }

    static Database db = null;
    public static Database getODMGDatabase() {
	if ( db == null ) {
	    db = odmg.newDatabase();
	    try {
		db.open( "repository.xml", Database.OPEN_READ_WRITE );
	    } catch ( ODMGException e ) {
		log.warn( "Could not open database: " + e.getMessage() );
		db = null;
	    }
	}
	return db;
    }

    public static void main( String[] args ) {
	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	Implementation odmg = getODMGImplementation();
	Database db = getODMGDatabase();
	Transaction tx = odmg.newTransaction();
	tx.begin();
	DList folders = null;
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = 0" );
	    folders = (DList) query.execute();
	    tx.commit();
	} catch ( Exception e ) {
	    tx.abort();
	    log.error( e.getMessage() );
	}

	Iterator iter = folders.iterator();
	boolean found = false;
	log.debug( "Starting to go thourh..." );
	while ( iter.hasNext() ) {
	    PhotoFolder folder = (PhotoFolder) iter.next();
	    log.debug( "Folder " + folder.getName() );
	    if ( folder.getFolderId() == 0 ) {
		found = true;
		log.info( "Found!!!" );
	    }
	}
    }

    /**
       Converts the folder object to String.
       @return The name of the folder
    */
    public String toString() {
	return name;
    }
}    
