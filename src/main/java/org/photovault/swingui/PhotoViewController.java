/*
  Copyright (c) 2007, Harri Kaimio
  
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

package org.photovault.swingui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.command.CommandExecutedEvent;
import org.photovault.command.DataAccessCommand;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.imginfo.ChangePhotoInfoCommand;
import org.photovault.imginfo.CreateCopyImageCommand;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.swingui.framework.AbstractController;
import org.photovault.swingui.framework.DefaultEvent;
import org.photovault.swingui.framework.DefaultEventListener;
import org.photovault.swingui.framework.PersistenceController;
import org.photovault.swingui.taskscheduler.SwingWorkerTaskScheduler;

/**
 Controller for the componenst actually used for viewing or editing photos. This 
 controller handles interaction between {@link PhotoCollectionThumbView} that shows
 all photos in certain collection as thumbnails, {@link JAIPhotoViewer} that 
 displays the selected photo as a preview and related editing dialogs (property, 
 cropping, color editing etc.)
 */

public class PhotoViewController extends PersistenceController {
    
    private static Log log = 
            LogFactory.getLog( PhotoViewController.class.getName() );
    
    PhotoInfoDAO photoDAO = null;
    
    PhotoFolderDAO folderDAO = null;

    private PhotoCollectionThumbView thumbPane;

    private JAIPhotoViewer previewPane;

    private JScrollPane thumbScroll;

    private JPanel collectionPane;
    
    /**
     Photos currently in model.
     */
    private List<PhotoInfo> photos = new ArrayList<PhotoInfo>();
    
    /** Creates a new instance of PhotoViewController */
    public PhotoViewController( Container view, AbstractController parentController ) {
        super( view, parentController );
        photoDAO = getDAOFactory().getPhotoInfoDAO();
        folderDAO = getDAOFactory().getPhotoFolderDAO();
        ImageIcon rotateCWIcon = getIcon( "rotate_cw.png" );
        ImageIcon rotateCCWIcon = getIcon( "rotate_ccw.png" );
        ImageIcon rotate180DegIcon = getIcon( "rotate_180.png" );


        registerAction( "rotate_cw", new RotateSelectedPhotoAction( this, 90, 
                "Rotate CW",  rotateCWIcon, 
                "Rotates the selected photo 90 degrees clockwise", KeyEvent.VK_R ) );        
        registerAction( "rotate_ccw", new RotateSelectedPhotoAction( this, 270,
                "Rotate CCW",  rotateCCWIcon, 
                "Rotates the selected photo 90 degrees counterclockwise", KeyEvent.VK_L ) );
        registerAction( "rotate_180", new RotateSelectedPhotoAction( this, 180,
                "Rotate 180 degrees",  rotate180DegIcon, 
                "Rotates the selected photo 180 degrees counterclockwise", KeyEvent.VK_T  ) );
        
        // Create the UI controls
	thumbPane = new PhotoCollectionThumbView( this, null );
        thumbPane.addSelectionChangeListener( new SelectionChangeListener() {
            public void selectionChanged(SelectionChangeEvent e) {
                thumbSelectionChanged( e );
            }            
        });
        previewPane = new JAIPhotoViewer( this );
        
        // Create the split pane to display both of these components
        
        thumbScroll = new JScrollPane( thumbPane );
        thumbPane.setBackground( Color.WHITE );
        thumbScroll.getViewport().setBackground( Color.WHITE );
        
        collectionPane = new JPanel();
        collectionPane.add( thumbScroll );
        collectionPane.add( previewPane );
        setupLayoutPreviewWithHorizontalIcons();
        
        /*
         Register action so that we are notified of changes to currently 
         displayed folder
         */
        registerEventListener( CommandExecutedEvent.class, new DefaultEventListener<DataAccessCommand>() {
            public void handleEvent(DefaultEvent<DataAccessCommand> event) {
                DataAccessCommand cmd = event.getPayload();
                if ( cmd instanceof ChangePhotoInfoCommand ) {
                    photoChangeCommandExecuted( (ChangePhotoInfoCommand)cmd );
                } else if ( cmd instanceof CreateCopyImageCommand ) {
                    imageCreated( (CreateCopyImageCommand)cmd );
                }
            }
        });
    }

    /**
     * Comparator used to sort photos visible in thumbnail view
     */
    Comparator photoComparator;

    /**
     * Set the comparator used to sort photos in thumbnail view
     * @param c
     */
    void setPhotoComparator( Comparator c ) {
        photoComparator = c;
        updateThumbView();
    }

    /**
     * Get the comparator used to sort photos in thumbnail view
     * @return
     */
    Comparator getPhotoComparator() {
        return photoComparator;
    }

    /**
     * Update the thumb view to show photos is the {@link #photos} collections,
     * sorted by {@link #photoComparator}
     */
    private void updateThumbView() {
        if ( photoComparator != null && photos != null ) {
            Collections.sort( photos, photoComparator );
        }
        thumbPane.setPhotos( photos );
    }

    /**
     This method is called after a {@link ChangePhotoInfoCommand} has been 
     executed somewhere in the application. It applies the modifications
     to current model.
     @param cmd The executed command
     */
    void photoChangeCommandExecuted( ChangePhotoInfoCommand cmd ) {
        if ( collection instanceof PhotoFolder ) {
            // Does this command impact our current folder?
            switch ( cmd.getFolderState( (PhotoFolder) collection ) ) {
            case ADDED:
                addPhotos( cmd.getChangedPhotos() );
                updateThumbView();
                break;
            case REMOVED:
                removePhotos( cmd.getChangedPhotos() );
                // None of the affected photos can belong to the model anymore
                // so no need for further checks.
                updateThumbView();
                return;
            default:
                // No impact to this folder
                break;
            }
        }
        
        // Update photos that belong to this collection
        for ( PhotoInfo p: cmd.getChangedPhotos() ) {
            if ( containsPhoto( p ) ) {
                PhotoInfo mergedPhoto = (PhotoInfo) getPersistenceContext().merge( p );
            }
        }
        thumbPane.setPhotos( photos );
    }


    private void imageCreated( CreateCopyImageCommand cmd ) {
        PhotoInfo p = cmd.getPhoto();
        log.debug( "image created for photo " + p.getUuid() );
        if ( containsPhoto( p ) ) {
            PhotoInfo mergedPhoto = (PhotoInfo) getPersistenceContext().merge( p );
            log.debug( "merged chages to photo " + mergedPhoto.getUuid() );
            if ( !mergedPhoto.hasThumbnail() ) {
                log.error( "he photo does not have a thumbnail!!!" );
                getPersistenceContext().update( mergedPhoto );
            }
        }
    }
    
    /**
     Add all photos from a collection to current model if they are not yet
     part of it.
     @param newPhotos Collection of (potentially detached) photos
     */
    private void addPhotos( Collection<PhotoInfo> newPhotos ) {
        for ( PhotoInfo p: newPhotos ) {
            if ( !containsPhoto( p ) ) {
                photos.add( (PhotoInfo) getPersistenceContext().merge( p ) );
            }
        }
    }
    
    /**
     Remove photos from current model if they belong to it
     @param removePhotos Collection of photos that will be removed. Potentially 
     detached instances.
     */
    private void removePhotos( Collection<PhotoInfo> removePhotos ) {
        Set<UUID> removeIds = new TreeSet<UUID>();
        for ( PhotoInfo p : removePhotos ) {
            removeIds.add( p.getUuid() );
        }
        
        ListIterator<PhotoInfo> iter = photos.listIterator();
        while ( iter.hasNext() ) {
            if ( removeIds.contains( iter.next().getUuid() ) ) {
                iter.remove();
            }
        }
    }
    
    /**
     Check whether a given photo belongs currently to the model.
     @param photo Potentially detached photo instance
     @return true if the model contains an instance of the same photo, false
     otherwise.
     */
    private boolean containsPhoto( PhotoInfo photo ) {
        for ( PhotoInfo p : photos ) {
            if ( p.getUuid().equals( photo.getUuid() ) ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     This method is called when selection in the thumbnail view changes.
     @param e The selection event
     */
    void thumbSelectionChanged( SelectionChangeEvent e ) {
       Collection selection = thumbPane.getSelection();
       if ( selection.size() == 1 ) {
           Cursor oldCursor = getView().getCursor();
           getView().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
           PhotoInfo selected = (PhotoInfo) (selection.toArray())[0];
            try {
                previewPane.setPhoto( selected );
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog( getView(), 
                        "Image file for this photo was not found", "File not found",
                        JOptionPane.ERROR_MESSAGE );
            }   
           getView().setCursor( oldCursor );
       } else {
            try {
                previewPane.setPhoto( null );
            } catch (FileNotFoundException ex) {
                // No exception expected when calling with null
            }
       }
       this.fireEvent( e );
    }
    
    public Collection getSelection() {
        return thumbPane.getSelection();
    }
    
    /**
     * Sets up the window layout so that the collection is displayed as one vertical
     * column with preview image on right
     */
    protected void setupLayoutPreviewWithVerticalIcons() {
        GridBagLayout layout = new GridBagLayout();
        collectionPane.setLayout( layout );

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 0.0;
        c.gridx = 0;

        // Minimum size is the size of one thumbnail
        thumbScroll.setMinimumSize( new Dimension( 170, 150 ));
        thumbScroll.setPreferredSize( new Dimension( 170, 150 ));
        thumbScroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        thumbScroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
        thumbScroll.setVisible( true );
        layout.setConstraints( thumbScroll, c );
        thumbPane.setColumnCount( 1 );
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        // c.gridheight = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.weightx = 1.0;
        layout.setConstraints( previewPane, c );
        previewPane.setVisible( true );
        getView().validate();
    }
    
     /**
     * Sets up the window layout so that the collection is displayed as one horizontal
     * row with preview image above it.
     */
    protected void setupLayoutPreviewWithHorizontalIcons() {
        GridBagLayout layout = new GridBagLayout();
        collectionPane.setLayout( layout );
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.0;
        c.weightx = 1.0;
        c.gridy = 1;

        // Minimum size is the size of one thumbnail
        thumbScroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        thumbScroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
        thumbScroll.setMinimumSize( new Dimension( 150, 180 ));
        thumbScroll.setPreferredSize( new Dimension( 150, 180 ));
        thumbScroll.setVisible( true );
        layout.setConstraints( thumbScroll, c );
        thumbPane.setRowCount( 1 );
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        // c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 0;
        c.weighty = 1.0;
        layout.setConstraints( previewPane, c );
        previewPane.setVisible( true );
        getView().validate();
    }   
    
    /**
     Hide the preview pane
     */
    public void setupLayoutNoPreview() {
        GridBagLayout layout = new GridBagLayout();
        collectionPane.setLayout( layout );

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridy = 0;

        // Minimum size is the size of one thumbnail
        thumbScroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        thumbScroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
        thumbScroll.setMinimumSize( new Dimension( 150, 200 ));
        thumbScroll.setVisible( true );
        layout.setConstraints( thumbScroll, c );
        thumbPane.setRowCount( -1 );
        thumbPane.setColumnCount( -1 );
        
        previewPane.setVisible( false );
        getView().validate();        
    }

    /**
     Show only preview image, no thumbnail pane.
     */
    public void setupLayoutNoThumbs() {
        GridBagLayout layout = new GridBagLayout();
        collectionPane.setLayout( layout );
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.0;
        c.weightx = 0.0;
        c.gridy = 1;

        // Minimum size is the size of one thumbnail
//        thumbScroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED );
//        thumbScroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
//        thumbScroll.setMinimumSize( new Dimension( 150, 180 ));
        layout.setConstraints( thumbScroll, c );
        thumbPane.setRowCount( 1 );
        thumbScroll.setVisible( false );
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        // c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 0;
        c.weighty = 1.0;
        c.weightx = 1.0;
        layout.setConstraints( previewPane, c );
        previewPane.setVisible( true );
        getView().validate();
    }
    
    
    public SwingWorkerTaskScheduler getBackgroundTaskScheduler() {
        return (SwingWorkerTaskScheduler) Photovault.getInstance().getTaskScheduler();
    }
    
    /**
     Get the component showing thumbnails
     */
    PhotoCollectionThumbView getThumbPane() {
        return thumbPane;
    }
    
    /**
     Get the preview pane component
     */
    JAIPhotoViewer getPreviewPane() {
        return previewPane;
    }
    
    JPanel getCollectionPane() {
        return collectionPane;
    }

    PhotoCollection collection = null;

    /**
     Set the collection that is contorlled by this controller.
     @param c The collection, this can (and most probably is) an detached 
     instance of folder or an unexecuted query.
     */
    void setCollection(PhotoCollection c) {
        collection = c;
        photos = null;
        /*
         Clear Hibernate cache to avoid memory leaks and race conditions with
         command events from already executed commands.
         */
        getPersistenceContext().clear();
        if ( c != null ) {
            photos = c.queryPhotos(getPersistenceContext());
        }
        updateThumbView();
    }
    
    PhotoCollection getCollection() {
        return collection;
    }    
    
    /**
     Loads an icon using class loader of this class
     @param resouceName Name of the icon reosurce to load
     @return The icon or <code>null</code> if no image was found using the given
     resource name.
     */
    private ImageIcon getIcon( String resourceName ) {
        ImageIcon icon = null;
        java.net.URL iconURL = PhotoViewController.class.getClassLoader().getResource(
                resourceName );
        if ( iconURL != null ) {
            icon = new ImageIcon( iconURL );
        }
        return icon;
    }
    
    

}
