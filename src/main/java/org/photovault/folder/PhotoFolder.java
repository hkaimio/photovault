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
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.folder;


import java.lang.UnsupportedOperationException;
import java.lang.UnsupportedOperationException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoCollectionChangeEvent;
import org.photovault.imginfo.PhotoCollectionChangeListener;
import org.photovault.imginfo.PhotoInfo;
import javax.persistence.*;

/**
   Implements a folder that can contain both PhotoInfos and other folders
*/

@Entity
@Table( name = "photo_collections" )
public class PhotoFolder implements PhotoCollection {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoFolder.class.getName() );

    /**
     Maximum length of the "name" property
     */
    static public final int NAME_LENGTH = 30;
    
    public PhotoFolder() {
	//	subfolders = new Vector();
	changeListeners = new Vector();
    }

    UUID uuid = null;
    
    @Column( name = "collection_uuid" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )
    public UUID getUUID() {
        if ( uuid == null ) {
            setUUID( UUID.randomUUID() );
        }
        return uuid;
    }    
    
    public void setUUID( UUID uuid ) {
	this.uuid = uuid;
	modified();
    }
    /**
       Persistent ID for this folder
    */
    
    int folderId;
    
    /**
     * Get the value of folderId.
     * @return value of folderId.
     */
    @Id 
    @GeneratedValue( generator = "folder_id_gen", strategy = GenerationType.TABLE )
    @TableGenerator( name="folder_id_gen", table="unique_keys", pkColumnName="id_name", 
                     pkColumnValue="folder_id_gen", valueColumnName="next_val", initialValue=10 )
    @Column( name = "collection_id" )
    public int getFolderId() {
	return folderId;
    }
    
    protected void setFolderId( int newId ) {
        folderId = newId;
    }
    /**
       Name of this folder
    */
    String name;
    
    /**
     * Get the value of name.
     * @return value of name.
     */
    @Column( name = "collection_name" )
    public String getName() {
	return name;
    }
    
    /**
     * Set the value of name.
     * @param v  Value to assign to name.
     */
    public void setName(String  v) {
	checkStringProperty( "Name", v, NAME_LENGTH );
	this.name = v;
	modified();
    }
    String description;
    
    /**
     * Get the value of description.
     * @return value of description.
     */
    @Column( name = "collection_desc" )
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
    Date creationDate = null;
    
    /**
     * Get the value of creationDate.
     * @return value of creationDate.
     */
    @Transient
    public Date getCreationDate() {
	return creationDate != null ? (Date) creationDate.clone()  : null;
    }

    // Implementation of PhotoCollection interface

    Set<PhotoInfo> photos = new HashSet();
    
    @ManyToMany( cascade  = { CascadeType.PERSIST, CascadeType.MERGE } )
    @org.hibernate.annotations.Cascade({
               org.hibernate.annotations.CascadeType.SAVE_UPDATE })    
    @JoinTable( name = "collection_photos",
                joinColumns = {@JoinColumn( name = "collection_id" ) },
                inverseJoinColumns = {@JoinColumn( name = "photo_id" ) } )
    public Set<PhotoInfo> getPhotos() {
        // TODO: Make test case to verify if this works with Hibernate
        return photos;
    }
    
    protected void setPhotos( Set<PhotoInfo> newPhotos ) {
        this.photos = newPhotos;
        modified();
    }
    
    /**
     * Returns the number of photos in the folder
     */
    @Transient
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
	    photos = new HashSet();
	}
	if ( !photos.contains( photo ) ) {
	    photo.addedToFolder( this );
	    photos.add( photo );
	    modified();
	}
    }

    /**
       remove a photo from the collection. If the photo does not exist in collection, does nothing
    */
    public void removePhoto( PhotoInfo photo ) {
	if ( photos == null ) {
	    return;
	}
	photo.removedFromFolder( this );
	photos.remove( photo );
	modified();
    }
    

    /**
       Returns the numer of subfolders this folder has.
    */
    @Transient
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
	if ( subfolders == null ) {
	    subfolders = new HashSet<PhotoFolder>();
	}
        subfolder.parent = this;
	subfolders.add( subfolder );
	modified();
        subfolder.modified();
	// Inform all parents & their that the structure has changed
	subfolderStructureChanged( this );
    }

    /**
       Removes a subfolder
    */
    protected void removeSubfolder( PhotoFolder subfolder ) {
	if ( subfolder == null ) {
	    return;
	}
	subfolders.remove( subfolder );
	modified();
	// Inform all parents & their that the structure has changed
	subfolderStructureChanged( this );
    }
    
    @OneToMany( mappedBy="parentFolder", cascade  = { CascadeType.PERSIST, CascadeType.MERGE } )
    @org.hibernate.annotations.Cascade({
               org.hibernate.annotations.CascadeType.SAVE_UPDATE })    
    public Set<PhotoFolder> getSubfolders() {
        return subfolders;
    }    
    protected void setSubfolders( Set<PhotoFolder> newSubfolders ) {
        subfolders = newSubfolders;
        modified();
	subfolderStructureChanged( this );        
    }
    
    /**
       All subfolders for this folder
    */
    Set<PhotoFolder> subfolders = new HashSet<PhotoFolder>();
    
    /**
       Returns the parent of this folder or null if this is a top-level folder
    */
    @ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @org.hibernate.annotations.Cascade( {org.hibernate.annotations.CascadeType.SAVE_UPDATE } )
    @JoinColumn( name = "parent", nullable = true )
    public PhotoFolder getParentFolder() {
	return parent;
    }

    protected void setParentFolder( PhotoFolder newParent ) {
	this.parent = newParent;
	modified();
    }
    
    /**
     Set a new parent for this folder
     @param newParent New parent for this folder
     */
    public void reparentFolder( PhotoFolder newParent ) {
        if ( parent != null ) {
	    parent.removeSubfolder( this );
	}
	this.parent = newParent;
	if ( parent != null ) {
	    parent.subfolders.add( this );
	    subfolderStructureChanged( parent );
            parent.modified();            
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
    int parentId = -1;
    
    /**
     Checks that a string is no longer tha tmaximum length allowed for it
     @param propertyName The porperty name used in error message
     @param value the new value
     @param maxLength Maximum length for the string
     @throws IllegalArgumentException if value is longer than maxLength
     */
    void checkStringProperty( String propertyName, String value, int maxLength ) 
    throws IllegalArgumentException {
        if ( value.length() > maxLength ) {
            throw new IllegalArgumentException( propertyName 
                    + " cannot be longer than " + maxLength + " characters" );
        }
    }
    
    
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
       @throws IllegalArgumentException if name is longer than @see NAME_LENGTH
    */
    public static PhotoFolder create( String name, PhotoFolder parent ) {
        
        PhotoFolder folder = new PhotoFolder();
        try {
            folder.setName( name );
            folder.uuid = UUID.randomUUID();
            folder.reparentFolder( parent );
        } catch (IllegalArgumentException e ) {
            throw e;
        } 
        return folder;
    }


    /**
       Creates & returns a new persistent PhotoFolder object
       @param uuid UUID for the created folder
       @param parent Parent folder of the new folder
    */
    public static PhotoFolder create(UUID uuid, PhotoFolder parent) {
        PhotoFolder folder = new PhotoFolder();
        try {
            folder.uuid = uuid;
            folder.name = "";
            folder.setParentFolder( parent );
        } catch (IllegalArgumentException e ) {
            throw e;
        }
        return folder;
    }    

    /**
       Returns the root folder for the PhotoFolder hierarchy, i.e. the folder with id 1.
     @deprecated Does not work with Hibernate, use PhotoFolderDAO#findRootFolder() instead.
    */
    public static PhotoFolder getRoot() {
        throw new UnsupportedOperationException( 
                "PhotoFolder#getRoot() not implemented, use PhotoFolderDAO#findRootFolder() instead");
    }
    
    /**
     Get folder by its UUID.
     @param uuid UUID for t6he folder to retrieve
     @return The given folder or <code>null</code> if not found     
     @deprecated Does not work with Hibernate, use PhotoFolderDAO#findByUUID() instead.
     */
    public static PhotoFolder getFolderByUUID(UUID uuid) {
        throw new UnsupportedOperationException( 
                "PhotoFolder#getFolderByUUID() not implemented, use PhotoFolderDAO#findByUUID() instead");
    }

    /**
     Returns the root folder with givenId.
     @param id id of the folder to retrieve
     @return The given folder or <code>null</code> if not found
     @deprecated Does not work with Hibernate, use PhotoFolderDAO#findById() instead.
    */
    public static PhotoFolder getFolderById( int id ) {
        throw new UnsupportedOperationException( 
                "PhotoFolder#getFolderById() not implemented, use PhotoFolderDAO#findById() instead");
    }
    
    static PhotoFolder rootFolder = null;

    /**
       Deletes all resources from this folder
    */
    public void delete() {
	// First make sure that this object is deleted from its parent's subfolders list (if it has a parent)
	reparentFolder( null );

	// Then notify all photos belonging to this folder
	if ( photos != null ) {
	    Iterator photoIter = photos.iterator();
	    while ( photoIter.hasNext() ) {
		PhotoInfo photo = (PhotoInfo) photoIter.next();
		photo.removedFromFolder( this );
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
