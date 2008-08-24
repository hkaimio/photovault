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


import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.hibernate.Session;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoCollectionChangeEvent;
import org.photovault.imginfo.PhotoCollectionChangeListener;
import org.photovault.imginfo.PhotoInfo;
import javax.persistence.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
   Implements a folder that can contain both PhotoInfos and other folders
*/

@Entity
@Table( name = "pv_folders" )
public class PhotoFolder implements PhotoCollection {

    static Log log = LogFactory.getLog( PhotoFolder.class.getName() );

    /**
     UUID of the root folder. TODO: this should not be needed, instead we should 
     have database support for several folder trees as some trees can be 
     replicated from other databases.
     */
    public static final UUID ROOT_UUID = 
            UUID.fromString( "939db304-2dec-4a59-ae24-8e1a6b165105" );
    
    /**
     Maximum length of the "name" property
     */
    static public final int NAME_LENGTH = 30;
    
    public static class PhotoFolderComparator implements Comparator<PhotoFolder> {
        public int compare(PhotoFolder o1, PhotoFolder o2) {
            String name1 = o1.getName();
            String name2 = o2.getName();
            if ( name1 == null ) name1 = "";
            if ( name2 == null ) name2 = "";
            return name1.compareTo( name2 );
        }
        
    }
    
    public PhotoFolder()  {
	//	subfolders = new Vector();
	changeListeners = new Vector();
    }

    UUID uuid = null;
    
    @Id
    @Column( name = "folder_uuid" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )
    public UUID getUuid() {
        if ( uuid == null ) {
            setUuid( UUID.randomUUID() );
        }
        return uuid;
    }    
    
    public void setUuid( UUID uuid ) {
	this.uuid = uuid;
	modified();
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

    /**
     External directory that is synchronized with this folder or 
     <code>null</code>
     */
    ExternalDir extDir = null;
    
    /**
     Get the external directory that is synchronized with this folder
     @return The external directory or <code>null</code> if no such directory 
     exists.
     */
    @Embedded
    public ExternalDir getExternalDir() {
        return extDir;
    }
    
    /**
     Set the external directory that is synchronized with this folder
     @param dir The external directory
     */
    public void setExternalDir( ExternalDir dir ) {
        extDir = dir;
    }
    
    // Implementation of PhotoCollection interface

    Set<PhotoInfo> photos = new HashSet<PhotoInfo>();
    
    @ManyToMany( cascade  = { CascadeType.PERSIST, CascadeType.MERGE } )
    @org.hibernate.annotations.Cascade({
               org.hibernate.annotations.CascadeType.SAVE_UPDATE })    
    @JoinTable( name = "pv_collection_photos",
                joinColumns = {@JoinColumn( name = "collection_uuid" ) },
                inverseJoinColumns = {@JoinColumn( name = "photo_uuid" ) } )
    public Set<PhotoInfo> getPhotos() {
        // TODO: Make test case to verify if this works with Hibernate
        return photos;
    }
               
    
    /**
     Query for photos in this folder
     @param session The session in which the query is executed
     @return List of associated photos
     */
    public List<PhotoInfo> queryPhotos( Session session ) {
        @SuppressWarnings( "unchecked" )
        List<PhotoInfo> res = 
                session.createQuery( "from PhotoInfo p where :f member of p.folders" ).
                setEntity( "f", this ).list(  );
        return res;
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
        return photos.toArray( new PhotoInfo[photos.size()] )[num];
    }

    /**
       Add a new photo to the folder
    */
    public void addPhoto( PhotoInfo photo ) {
	if ( photos == null ) {
	    photos = new HashSet<PhotoInfo>();
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
     All known associations from this folder to photos.
     */
    Set<FolderPhotoAssociation> photoAssociations = 
            new HashSet<FolderPhotoAssociation>();

    /**
     Add an association to a photo
     @param a The association
     @throws IllegalStateException if a is really associated with another folder
     */
    void addPhotoAssociation( FolderPhotoAssociation a ) {
        photoAssociations.add( a );
        a.setFolder( this );
    }

    /**
     Get all associations from this folder to photos
     @return
     */
    @OneToMany( mappedBy="folder" )
    Set<FolderPhotoAssociation> getPhotoAssociations() {
        return photoAssociations;        
    }
    
    /**
     Set the photo associations from this folder. For Hibernate.
     @param s Set of all associations
     */
    void setPhotoAssociations( Set<FolderPhotoAssociation> s ) {
        photoAssociations = s;
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
     TODO: This is awfully inefficient but {@link PhotoFolderTreeModel}
     needs random access to this collection
     */
    public PhotoFolder getSubfolder( int num ) {
        if ( subfolders == null || num >= subfolders.size() ) {
            throw new ArrayIndexOutOfBoundsException();
        }
        //
        return subfolders.toArray( new PhotoFolder[subfolders.size()] )[num];
    }
    
    /**
     Adds a new subfolder to this folder. This method is called by setParentFolder().
     */
    protected void addSubfolder( PhotoFolder subfolder ) {
	if ( subfolders == null ) {
	    subfolders = new TreeSet<PhotoFolder>();
	}
        subfolder.parent = this;
	getSubfolders().add( subfolder );
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
	getSubfolders().remove( subfolder );
	modified();
	// Inform all parents & their that the structure has changed
	subfolderStructureChanged( this );
    }
    
    @OneToMany( mappedBy="parentFolder", cascade  = { CascadeType.PERSIST, CascadeType.MERGE } )
    @org.hibernate.annotations.Cascade({
               org.hibernate.annotations.CascadeType.SAVE_UPDATE })    
    @org.hibernate.annotations.Sort( type=org.hibernate.annotations.SortType.COMPARATOR,
               comparator=PhotoFolder.PhotoFolderComparator.class )
    public SortedSet<PhotoFolder> getSubfolders() {
        return subfolders;
    }    
    protected void setSubfolders( SortedSet<PhotoFolder> newSubfolders ) {
        subfolders = newSubfolders;
        modified();
	subfolderStructureChanged( this );        
    }
    
    /**
       All subfolders for this folder
    */
    SortedSet<PhotoFolder> subfolders = new TreeSet<PhotoFolder>( new PhotoFolderComparator() );
    
    /**
       Returns the parent of this folder or null if this is a top-level folder
    */
    @ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @org.hibernate.annotations.Cascade( {org.hibernate.annotations.CascadeType.SAVE_UPDATE } )
    @JoinColumn( name = "parent_uuid", nullable = true )
    // This is needed since OJB set parent to 0 for the root folder.
    @org.hibernate.annotations.NotFound( action = org.hibernate.annotations.NotFoundAction.IGNORE )
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
	    parent.getSubfolders().add( this );
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
	
    List<PhotoCollectionChangeListener> changeListeners = null;

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
