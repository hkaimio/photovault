// $Id: PhotoFolder.java,v 1.4 2003/02/25 20:57:11 kaimio Exp $

package photovault.folder;


import imginfo.*;
import org.odmg.*;
import org.apache.ojb.odmg.*;
import java.util.*;
import dbhelper.*;

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
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.name = v;
	modified();
	txw.commit();
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
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.description = v;
	modified();
	txw.commit();
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
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	subfolders.add( subfolder );
	modified();
	// Inform all parents & their that the structure has changed
	subfolderStructureChanged( this );
	txw.commit();
    }

    /**
       Removes a subfolder
    */
    protected void removeSubfolder( PhotoFolder subfolder ) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	subfolders.remove( subfolder );
	modified();
	// Inform all parents & their that the structure has changed
	subfolderStructureChanged( this );
	txw.commit();
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
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	// If the parent is already set, remove the folder from its subfolders
	if ( parent != null ) {
	    parent.removeSubfolder( this );
	}
	this.parent = newParent;
	if ( parent != null ) {
	    parent.addSubfolder( this );
	}
	modified();
	txw.commit();
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
       This method is called after the collection has been modified in some way. It notifies listeners.
    */
    protected void modified() {
	// Find the current transaction or create a new one
	log.debug( "Field modified" );
	
	// Notify the listeners. This is done in the same transaction context so that potential modifications to other
	// persistent objects are also saved
	notifyListeners();

	// Notify also parents if there are any
	if ( parent != null ) {
	    parent.subfolderChanged( this );
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
       This folder is changed by a subfolder to inform parent that it has been changed. It fires a @see PhotoCollectionEvent to
       all @see PhotoCollectionListeners registered to this object.
    */
    protected void subfolderChanged( PhotoFolder subfolder ) {
	PhotoFolderEvent e = new PhotoFolderEvent( this, subfolder, null );
	fireSubfolderChangedEvent( e );

	// Inform also parents
	if ( parent != null ) {
	    parent.subfolderChanged( subfolder );
	}
    }

    /**
       This method is called by a subfolder when its structure has changed (e.g. it has a new subfoder).
    */
    protected void subfolderStructureChanged( PhotoFolder subfolder ) {
	PhotoFolderEvent e = new PhotoFolderEvent( this, subfolder, null );
	fireStructureChangedEvent( e );

	// Inform also the parent
	if ( parent != null ) {
	    parent.subfolderStructureChanged( subfolder );
	}
    }
	
    
    
    /**
       Fires a PhotoFolderEvent to all listeners of this object that implement the @see PhotoFolderListener interface
       (istead of @see PhotoCollectionChangeEvent)
    */
    protected void fireStructureChangedEvent( PhotoFolderEvent e ) {
	log.debug( "in fireStructureChangeEvent" );
	Iterator iter = changeListeners.iterator();
	while ( iter.hasNext() ) {
	    PhotoCollectionChangeListener l = (PhotoCollectionChangeListener) iter.next();
	    if ( PhotoFolderChangeListener.class.isInstance( l ) ) {
		log.debug( "Found PhotoFolderListener" );
		PhotoFolderChangeListener pfl = (PhotoFolderChangeListener) l;
		pfl.structureChanged( e );
	    }
	}
    }
    
    /**
       Fires a PhotoFolderEvent to all listeners of this object that implement the @see PhotoFolderChangeListener interface
       (istead of @see PhotoCollectionChangeEvent)
    */
    protected void fireSubfolderChangedEvent( PhotoFolderEvent e ) {
	Iterator iter = changeListeners.iterator();
	while ( iter.hasNext() ) {
	    PhotoCollectionChangeListener l = (PhotoCollectionChangeListener) iter.next();
	    if ( PhotoFolderChangeListener.class.isInstance( l ) ) {
		PhotoFolderChangeListener pfl = (PhotoFolderChangeListener) l;
		pfl.subfolderChanged( e );
	    }
	}
    }
    
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
	Transaction tx = odmg.currentTransaction();
	boolean mustCommit = false;
	if ( tx == null ) {
	    tx = odmg.newTransaction();
	    tx.begin();
	    mustCommit = true;
	}
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = 1" );
	    folders = (DList) query.execute();
	} catch ( Exception e ) {
	    tx.abort();
	    return null;
	}
	PhotoFolder rootFolder = (PhotoFolder) folders.get( 0 );
	// If a new transaction was created, commit it
	if ( mustCommit ) {
	    tx.commit();
	}
	return rootFolder;
    }

    /**
       Deletes this object from the persistent repository
    */
    public void delete() {
	getODMGImplementation();
	getODMGDatabase();
	// Find the current transaction or create a new one
	Transaction tx = odmg.currentTransaction();
	boolean mustCommit = false;
	if ( tx == null ) {
	    tx = odmg.newTransaction();
	    tx.begin();
	    mustCommit = true;
	}
	// First make sure that this object is deleted from its parent's subfolders list (if it has a parent)
	setParentFolder( null );
	db.deletePersistent( this );

	// If a new transaction was created, commit it
	if ( mustCommit ) {
	    tx.commit();
	}
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
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = 1" );
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

    // Functions to get the ODMG persistence layer handles. This should really be moved into
    // its own helper class

    static Implementation odmg = null;
    public static Implementation getODMGImplementation() {
	if ( odmg == null ) {
	    odmg = ODMG.getODMGImplementation();
	}
	return odmg;
    }

    static Database db = null;
    public static Database getODMGDatabase() {
	if ( db == null ) {
	    db = ODMG.getODMGDatabase();
	}
	return db;
    }

    // Init ODMG fields at creation time
    {
	
	getODMGImplementation();
	getODMGDatabase();
    }
    
}    
