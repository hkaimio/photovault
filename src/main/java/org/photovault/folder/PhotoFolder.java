/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Foobar; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.folder;


import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.imginfo.*;
import org.odmg.*;
import org.apache.ojb.odmg.*;
import java.util.*;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoCollectionChangeEvent;
import org.photovault.imginfo.PhotoCollectionChangeListener;
import org.photovault.imginfo.PhotoInfo;

/**
   Implements a folder that can contain both PhotoInfos and other folders
*/

public class PhotoFolder implements PhotoCollection {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoFolder.class.getName() );

    public PhotoFolder() {
	//	subfolders = new Vector();
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
    Date creationDate = null;
    
    /**
     * Get the value of creationDate.
     * @return value of creationDate.
     */
    public Date getCreationDate() {
	return creationDate != null ? (Date) creationDate.clone()  : null;
    }

    // Implementation of PhotoCollection interface

    Collection photos = null;
    
    /**
     * Returns the number of photos in the folder
     */
    public int getPhotoCount()
    {
	if ( photos == null ) {
	    return 0;
	}
	return photos.size();
    }

    /**
     * Returns a photo from the folder with given order number. Valid values [0..getPhotoCount[
     * @param num order number of the photo
     * @return The photo with the given number
     */
    public PhotoInfo getPhoto(int num)
    {
	if ( photos == null || num >= photos.size() ) {
	    throw new ArrayIndexOutOfBoundsException();
	}
	if ( photos instanceof List ) {
	    return (PhotoInfo) ((List)photos).get( num );
	}
	log.warn( "Cannot currently find photos if collection is not a list" );
	return null;
    }

    /**
       Add a new photo to the folder
    */
    public void addPhoto( PhotoInfo photo ) {
	if ( photos == null ) {
	    photos = new Vector();
	}
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	if ( !photos.contains( photo ) ) {
            txw.lock( photo, Transaction.WRITE );
	    photo.addedToFolder( this );
	    photos.add( photo );
	    modified();
	}
	txw.commit();
    }

    /**
       remove a photo from the collection. If the photo does not exist in collection, does nothing
    */
    public void removePhoto( PhotoInfo photo ) {
	if ( photos == null ) {
	    return;
	}
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
        txw.lock( photo, Transaction.WRITE );
	photo.removedFromFolder( this );
	photos.remove( photo );
	modified();
	txw.commit();
    }
    

    /**
       Returns the numer of subfolders this folder has.
    */
    public int getSubfolderCount() {
	if ( subfolders == null ) {
	    return 0;
	}
	return subfolders.size();
    }

    /**
       Returns s subfolder with given order number.
    */
    public PhotoFolder getSubfolder( int num ) {
	if ( subfolders == null || num >= subfolders.size() ) {
	    throw new ArrayIndexOutOfBoundsException();
	}
	if ( subfolders instanceof List ) {
	    return (PhotoFolder) ((List)subfolders).get( num );
	}
	log.warn( "Cannot currently find subfolders if collection si not a list" );
	return null;
    }

    /**
       Adds a new subfolder to this folder. This method is called by setParentFolder().
    */
    protected void addSubfolder( PhotoFolder subfolder ) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	if ( subfolders == null ) {
	    subfolders = new Vector();
	}
        // TODO: lock subfolder???
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
	if ( subfolder == null ) {
	    return;
	}
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
        txw.lock( subfolder, Transaction.WRITE );
	subfolders.remove( subfolder );
	modified();
	// Inform all parents & their that the structure has changed
	subfolderStructureChanged( this );
	txw.commit();
    }
    
    /**
       All subfolders for this folder
    */
    Collection subfolders = null;
    
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
    int parentId = -1;
    
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

	ODMGXAWrapper txw = new ODMGXAWrapper();
	PhotoFolder folder = new PhotoFolder();
	folder.setName( name );
	folder.setParentFolder( parent );
	txw.lock( folder, Transaction.WRITE );
	txw.commit();
	return folder;
    }

    /**
       Returns the root folder for the PhotoFolder hierarchy, i.e. the folder with id 1.
    */
    public static PhotoFolder getRoot() {
        PhotoFolder rootFolder = PhotoFolder.rootFolder;
        if ( rootFolder == null ) {
            ODMGXAWrapper txw = new ODMGXAWrapper();
            Implementation odmg = ODMG.getODMGImplementation();
	
            List folders = null;
            boolean mustCommit = false;
            try {
                OQLQuery query = odmg.newOQLQuery();
                query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = 1" );                
                folders = (List) query.execute();
            } catch ( Exception e ) {
                txw.abort();
                return null;
            }
            rootFolder = (PhotoFolder) folders.get( 0 );
            PhotoFolder.rootFolder = rootFolder;
            // If a new transaction was created, commit it
            txw.commit();
        }
	return rootFolder;
    }

    /**
     Returns the root folder with givenId.
     @param id id of the folder to retrieve
     @return The given folder or <code>null</code> if not found
    */
    public static PhotoFolder getFolderById( int id ) {
        PhotoFolder f = null;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        
        List folders = null;
        boolean mustCommit = false;
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create( "select folders from " + PhotoFolder.class.getName() 
                        + " where folderId = " + id );
            folders = (List) query.execute();
        } catch ( Exception e ) {
            txw.abort();
            return null;
        }
        f = (PhotoFolder) folders.get( 0 );
        // If a new transaction was created, commit it
        txw.commit();
        return f;
    }

    
    
    static PhotoFolder rootFolder = null;

    /**
       Deletes this object from the persistent repository
    */
    public void delete() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	// Find the current transaction or create a new one
	boolean mustCommit = false;

	// First make sure that this object is deleted from its parent's subfolders list (if it has a parent)
	setParentFolder( null );

	// Then notify all photos belonging to this folder
	if ( photos != null ) {
	    Iterator photoIter = photos.iterator();
	    while ( photoIter.hasNext() ) {
		PhotoInfo photo = (PhotoInfo) photoIter.next();
		photo.removedFromFolder( this );
	    }
	}
	Database db = ODMG.getODMGDatabase();
	db.deletePersistent( this );
	
	txw.commit();
    }
    


    /**
       Converts the folder object to String.
       @return The name of the folder
    */
    public String toString() {
	return name;
    }
}    
