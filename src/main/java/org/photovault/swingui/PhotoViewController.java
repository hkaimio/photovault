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
import java.util.Collection;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.photovault.command.CommandHandler;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.folder.PhotoFolderModifiedEvent;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.imginfo.PhotoInfoModifiedEvent;
import org.photovault.swingui.framework.AbstractController;
import org.photovault.swingui.framework.DefaultEvent;
import org.photovault.swingui.framework.DefaultEventListener;
import org.photovault.swingui.framework.PersistenceController;

/**
 Controller for the componenst actually used for viewing or editing photos. This 
 controller handles interaction between {@link PhotoCollectionThumbView} that shows
 all photos in certain collection as thumbnails, {@link JAIPhotoViewer} that 
 displays the selected photo as a preview and related editing dialogs (property, 
 cropping, color editing etc.)
 */

public class PhotoViewController extends PersistenceController {
    
    PhotoInfoDAO photoDAO = null;
    
    PhotoFolderDAO folderDAO = null;

    private PhotoCollectionThumbView thumbPane;

    private JAIPhotoViewer previewPane;

    private JScrollPane thumbScroll;

    private JPanel collectionPane;
    
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
        registerEventListener( PhotoFolderModifiedEvent.class, new DefaultEventListener<PhotoFolder>() {
            public void handleEvent(DefaultEvent<PhotoFolder> event) {
                PhotoCollection currentCollection = getCollection();
                if ( currentCollection != null && 
                        currentCollection instanceof PhotoFolder ) {
                    if ( ((PhotoFolder)currentCollection).getFolderId() == event.getPayload().getFolderId() ) {
                        getPersistenceContext().merge( event.getPayload() );
                    }
                }
            }
        });
        
        registerEventListener( PhotoInfoModifiedEvent.class, new DefaultEventListener<PhotoInfo>() {
            public void handleEvent(DefaultEvent<PhotoInfo> event) {
                /*
                 TODO: We should chech whether the photo is already part of this 
                 context
                 */
                        PhotoInfo p = (PhotoInfo) getPersistenceContext().merge( event.getPayload()  );
            }
        });
        registerEventListener( PhotoFolderModifiedEvent.class, new DefaultEventListener<PhotoFolder>() {
            public void handleEvent(DefaultEvent<PhotoFolder> event) {
                PhotoCollection currentCollection = thumbPane.getCollection();
                if ( currentCollection instanceof PhotoFolder && 
                        ((PhotoFolder)currentCollection).getFolderId() == event.getPayload().getFolderId() ) {
                    getPersistenceContext().merge( event.getPayload()  );
                }
            }
        });
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

    /**
     Set the collection that is contorlled by this controller.
     @param c The collection, this can (and most probably is) an detached 
     instance of folder or an unexecuted query.
     */
    void setCollection(PhotoCollection c) {
        PhotoCollection contextCollection = null;
        if ( c instanceof PhotoFolder ) {
            contextCollection = (PhotoCollection) getPersistenceContext().merge( c );
        }
        thumbPane.setCollection( contextCollection );
    }
    
    PhotoCollection getCollection() {
        return thumbPane.getCollection();
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
