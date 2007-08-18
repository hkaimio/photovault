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

package org.photovault.swingui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoCollectionChangeEvent;
import org.photovault.imginfo.PhotoCollectionChangeListener;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoChangeEvent;
import org.photovault.imginfo.PhotoInfoChangeListener;
import org.photovault.imginfo.SortedPhotoCollection;
import org.photovault.imginfo.Thumbnail;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.Volume;
import org.photovault.imginfo.VolumeBase;



/**
   This class implements the default thumbnail view for photo
   collections. Some of the planned features include:
   
   <ul> <li> Either vertically or horizontally scrollable view with
   multiple columns </li>

   <li> Multiple selection for thumbnails </li>

   <li> Automatic fetching and creation of thumbnails on background if
   these do not exist </li> </ul>

   <h1> Selection & drag-n-drop logic</h1>

   <ul> <li> If mouse is pressed between images, the drag is
   interpreted as a drag selection </li>

   <li> If mouse is pressed on top of a thumbnail the drag is
   interpreted as a drag-n-drop operation </li> </ul>

   @author Harri Kaimio

*/

public class PhotoCollectionThumbView
    extends JPanel
    implements MouseMotionListener, MouseListener, ActionListener,
	       PhotoCollectionChangeListener, PhotoInfoChangeListener,
               Scrollable {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoCollectionThumbView.class.getName() );
    
    PhotoViewController ctrl;
    /**
     Default constructor
     */
    public PhotoCollectionThumbView() {
        this( null, null );
    }
    
    /**
     Creates a new <code>PhotoCollectionThumbView</code> instance.
     @param collection Initial collection to show. If this collection is nonempty,
     select the first photo in it when creating the control.
     */
    public PhotoCollectionThumbView( PhotoViewController ctrl, PhotoCollection collection ) {
        super();
        this.ctrl = ctrl;
        if ( collection != null ) {
            photoCollection = new SortedPhotoCollection( collection );
        }
        createUI();
        if ( collection != null && collection.getPhotoCount() > 0 ) {
            selection.add( collection.getPhoto( 0 ) );
        }
//	thumbCreatorThread = new ThumbCreatorThread( this );
//	thumbCreatorThread.start();
    }

    // ThumbCreatorThread thumbCreatorThread;
    
    PhotoInstanceCreator lastInstanceCreator;
    
    /**
     * Returns the currently displayed photo collection or <code>null</code> if none specified 
     <p>
     Note that PhotoCollectionThumbView can internally sort the collection, so the order
     of photos in returned collection may be different than the display order.
     If you need the collection in same sorting order as displayed use 
     {@link #getSortedCollection()} instead.
     */
    public PhotoCollection getCollection() {
        if ( photoCollection != null ) {
            return photoCollection.getOrigCollection();
        }
        return null;
    }

    public SortedPhotoCollection getSortedCollection() {
        return photoCollection;
    }
    

    SortedPhotoCollection photoCollection = null;
    
    /**
     * Set the collection that should be viewed
     * @param v  Value to assign to collection.
     */
    public void setCollection(PhotoCollection  v) {
      if ( photoCollection != null ) {
	photoCollection.removePhotoCollectionChangeListener( this );
      }
        
      photoCollection = new SortedPhotoCollection( v );
      if ( photoOrderComparator != null ) {
          photoCollection.setComparator( photoOrderComparator );
      }
      photoCollection.addPhotoCollectionChangeListener( this );
      refreshPhotoChangeListeners();
      
      revalidate();
      repaint();
    }

    /**
     * Comparator object that is used for sorting thumbnails
     */
    Comparator photoOrderComparator;
    
    /**
     * Sets the comparator that is used for ordering the thumbnails in this view. After
     * calling this method the whole window is repainted.
     * @param c The comparator object to be used
     */
    public void setPhotoOrderComparator( Comparator c ) {
        photoOrderComparator = c;
        if ( photoCollection != null ) {
            photoCollection.setComparator( c );
        }
    }
    
    /**
     * Returns the comparator object tjat is currently used for ordering thumbnails
     * in this view
     */
    public Comparator getPhotoOrderComparator() {
        return photoOrderComparator;
    }
    
    public void setPhotos( List photos ) {
        for ( PhotoInfo photo : this.photos ) {
	    photo.removeChangeListener( this );
	}
	
	photos.clear();
	
        for ( Object o : photos ) {
            PhotoInfo photo = (PhotoInfo) o;
	    photo.addChangeListener( this );
	    photos.add( photo );
	}
    }
    
    public List<PhotoInfo> getPhotos() {
        return Collections.unmodifiableList( photos );
    }
    
    /**
       Removes all change listeners this photo has added, repopulates the photos array
       from current photoCollection and adds change listeners to all photos in it.
    */
    private void refreshPhotoChangeListeners() {
	// remove change listeners from all existing photos
	Iterator iter = photos.iterator();
	while ( iter.hasNext() ) {
	    PhotoInfo photo = (PhotoInfo) iter.next();
	    photo.removeChangeListener( this );
	}
	
	photos.clear();
	
	// Add the change listeners to all photos so that we are aware of modifications
	for ( int n = 0; n < photoCollection.getPhotoCount(); n++ ) {
	    PhotoInfo photo = photoCollection.getPhoto( n );
	    photo.addChangeListener( this );
	    photos.add( photo );
	}
    }	
    
    PhotoCollection collection;
    List<PhotoInfo> photos = new ArrayList<PhotoInfo>();

    
    /**
     * Get a currently selected photos
     * @return Collection of currently selected photos or <code>null</code> if none is selected
     */

    public Collection getSelection( ) {
        return new HashSet( selection );
    }
    
    /**
       Returns the number of photos that are selected in this view
    */
    public int getSelectedCount() {
	return selection.size();
    }
	
    /**
     Removes given photo from selection if it currently is selected. If photo is 
     removed, a selection change event is sent to all listeners.
     @param p The photo that is removed from selection
     */
    
    public void removeFromSelection( PhotoInfo p ) {
        if ( selection.contains( p ) ) {
            selection.remove( p );
            fireSelectionChangeEvent();
        }
    }

    HashSet selection = new HashSet();


    /**
       Adds a listener to listen for selection changes
    */
    public void addSelectionChangeListener( SelectionChangeListener l ) {
	selectionChangeListeners.add( l );
    }

    /**
       removes a listener for selection changes
    */
    public void removePhotoFolderTreeListener( SelectionChangeListener l ) {
	selectionChangeListeners.remove( l );
    }

    protected void fireSelectionChangeEvent() {
	Iterator iter = selectionChangeListeners.iterator();
	while ( iter.hasNext() ) {
	    SelectionChangeListener l = (SelectionChangeListener) iter.next();
	    l.selectionChanged( new SelectionChangeEvent( this ) );
	}
    }
    
    Vector selectionChangeListeners = new Vector();
    
    int columnWidth = 150;
    int rowHeight = 150;
    int columnCount = 1;
    int rowCount = -1;
    int columnsToPaint = 1;
    /**
     Array of icons used to represent different quality settings in thumbnails
     */
    ImageIcon qualityIcons[] = null;
    
    /**
     Icon to indicate that this image is stored in raw format.
     */
    ImageIcon rawIcon = null;
    JPopupMenu popup = null;

    /**
       This helper class from Java Tutorial handles displaying of popup menu on correct mouse events
    */
    class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }
    }


    TransferHandler photoTransferHandler = null;

    /**
     Icon to display in front of a thumbnail while Photovault is creating a new
     replacement for it.
     */
    ImageIcon creatingThumbIcon = null;
    
    void createUI() {
        photoTransferHandler = new PhotoCollectionTransferHandler( this );
        setTransferHandler( photoTransferHandler );
        
        setAutoscrolls( true );
        
        addMouseListener( this );
        addMouseMotionListener( this );
        
        
        
        // Create the popup menu
        popup = new JPopupMenu();
        ImageIcon propsIcon = getIcon( "view_properties.png" );
        editSelectionPropsAction =
                new EditSelectionPropsAction( this, "Properties...", propsIcon,
                "Edit properties of the selected photos",
                KeyEvent.VK_P );
        JMenuItem propsItem = new JMenuItem( editSelectionPropsAction );
        ImageIcon colorsIcon = getIcon( "colors.png" );
        editSelectionColorsAction =
                new EditSelectionColorsAction( this, null, "Adjust colors...", colorsIcon,
                "Adjust colors of the selected photos",
                KeyEvent.VK_A );
        JMenuItem colorsItem = new JMenuItem( editSelectionColorsAction );
        ImageIcon showIcon = getIcon( "show_new_window.png" );
        showSelectedPhotoAction =
                new ShowSelectedPhotoAction( this, "Show image", showIcon,
                "Show the selected phot(s)",
                KeyEvent.VK_S );
        JMenuItem showItem = new JMenuItem( showSelectedPhotoAction );
        
        
        JMenuItem rotateCW = new JMenuItem( ctrl.getActionAdapter( "rotate_cw" ) );
        JMenuItem rotateCCW = new JMenuItem( ctrl.getActionAdapter( "rotate_ccw" ) );
        JMenuItem rotate180deg = new JMenuItem( ctrl.getActionAdapter( "rotate_180" ) );        
        
        JMenuItem addToFolder = new JMenuItem( "Add to folder..." );
        addToFolder.addActionListener( this );
        addToFolder.setActionCommand( PHOTO_ADD_TO_FOLDER_CMD );
        addToFolder.setIcon( getIcon( "empty_icon.png" ) );
        ImageIcon exportIcon = getIcon( "filesave.png" );
        exportSelectedAction
                = new ExportSelectedAction( this, "Export selected...", exportIcon,
                "Export the selected photos to from archive database to image files",
                KeyEvent.VK_E );
        JMenuItem exportSelected = new JMenuItem( exportSelectedAction );
        
        ImageIcon deleteSelectedIcon = getIcon( "delete_image.png" );
        deleteSelectedAction 
                = new DeletePhotoAction( this, "Delete", deleteSelectedIcon, 
                "Delete selected photos including all of their instances",
                KeyEvent.VK_D );
        JMenuItem deleteSelected = new JMenuItem( deleteSelectedAction );
        
        // Create the Quality submenu
        JMenu qualityMenu = new JMenu( "Quality" );
        qualityMenu.setIcon( getIcon( "empty_icon.png" ) );
        String qualityStrings[] = { "Unevaluated", "Top", "Good", "OK", "Poor", "Unusable" };
        String qualityIconnames[] = { 
            "quality_unevaluated.png", 
            "quality_top.png", 
            "quality_good.png", 
            "quality_ok.png", 
            "quality_poor.png", 
            "quality_unusable.png" 
        };
        qualityIcons = new ImageIcon[qualityStrings.length];
        for ( int n = 0; n < qualityStrings.length; n++ ) {
            qualityIcons[n] = getIcon( qualityIconnames[n] );
            AbstractAction qualityAction
                    = new SetPhotoQualityAction( this, n, qualityStrings[n], 
                    qualityIcons[n],
                    "Set quality of selected phots to \"" + qualityStrings[n] + "\"",
                    null );
            JMenuItem qualityMenuItem = new JMenuItem( qualityAction );
            qualityMenu.add( qualityMenuItem );
        }
        
        rawIcon = getIcon( "raw_icon.png" );
        
        
        popup.add( showItem );
        popup.add( propsItem );
        popup.add( colorsItem );
        popup.add( rotateCW );
        popup.add( rotateCCW );
        popup.add( rotate180deg );
        popup.add( qualityMenu );
        popup.add( addToFolder );
        popup.add( exportSelected );
        popup.add( deleteSelected );
        MouseListener popupListener = new PopupListener();
        addMouseListener( popupListener );
        
        ImageIcon selectNextIcon = getIcon( "next.png" );
        selectNextAction = new ChangeSelectionAction( this,
                ChangeSelectionAction.MOVE_FWD, "Next photo", selectNextIcon,
                "Move to next photo", KeyEvent.VK_N,
                KeyStroke.getKeyStroke( KeyEvent.VK_N, 
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
        
        ImageIcon selectPrevIcon = getIcon( "previous.png" );
        selectPrevAction = new ChangeSelectionAction( this,
                ChangeSelectionAction.MOVE_BACK, "Previous photo", selectPrevIcon,
                "Move to previous photo", KeyEvent.VK_P,
                KeyStroke.getKeyStroke( KeyEvent.VK_P, 
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
        
        creatingThumbIcon = getIcon( "creating_thumb.png" );
    }

    /**
     Loads an icon using class loader of this class
     @param resouceName Name of the icon reosurce to load
     @return The icon or <code>null</code> if no image was found using the given
     resource name.
     */
    private ImageIcon getIcon( String resourceName ) {
        ImageIcon icon = null;
        java.net.URL iconURL = JAIPhotoViewer.class.getClassLoader().getResource( resourceName );
        if ( iconURL != null ) {
            icon = new ImageIcon( iconURL );
        }
        return icon;
    }
    
    /**
     * Sets the shape of the thumbnail grid so that it has specified number of columns.
     * When this is set then row count is adjusted so that all thumbnails fit.
     */
    public void setColumnCount( int c ) {
        columnCount = c;
       // If number of columns is fixed number of rows must be dynamic.
        rowCount = -1;
        columnsToPaint = c;
        revalidate();
        repaint();
    }
    
    /**
     * Return number of columns or -1 if this is adjusted dynamically based on row cound or component size
     */
    public int getColumnCount() {
        return columnCount;
    }
    
    public void setRowCount( int c ) {
        rowCount = c;
        columnCount = -1;
        revalidate();
        repaint();
    }
    
    public int getRowCount() {
        return rowCount;
    }
    
    // Popup menu actions
    private static final String PHOTO_ADD_TO_FOLDER_CMD = "addToFolder";
    private AbstractAction exportSelectedAction;
    private AbstractAction editSelectionPropsAction;
    private AbstractAction editSelectionColorsAction;
    private AbstractAction showSelectedPhotoAction;
    private AbstractAction rotateCWAction;
    private AbstractAction rotateCCWAction;
    private AbstractAction rotate180degAction;
    private AbstractAction selectNextAction;
    private AbstractAction selectPrevAction;
    private AbstractAction deleteSelectedAction;
    

    public AbstractAction getExportSelectedAction() {
	return exportSelectedAction;
    }

    public AbstractAction getEditSelectionPropsAction() {
	return editSelectionPropsAction;
    }

    public AbstractAction getEditSelectionColorsAction() {
	return editSelectionColorsAction;
    }
    
    public AbstractAction getShowSelectedPhotoAction() {
	return showSelectedPhotoAction;
    }

    public AbstractAction getRotateCWActionAction() {
	return rotateCWAction;
    }
    
    public AbstractAction getRotateCCWActionAction() {
	return rotateCCWAction;
    }
    
    public AbstractAction getRotate180degActionAction() {
	return rotate180degAction;
    }
    
    public AbstractAction getSelectNextAction() {
	return selectNextAction;
    }
    
    public AbstractAction getSelectPreviousAction() {
	return selectPrevAction;
    }
        
    public AbstractAction getDeleteSelectedAction() {
        return deleteSelectedAction;
    }
    
    public void paint( Graphics g ) {
        super.paint( g );
        Graphics2D g2 = (Graphics2D) g;
        Rectangle clipRect = g.getClipBounds();
        Dimension compSize = getSize();
        // columnCount = (int)(compSize.getWidth()/columnWidth);
        
        int photoCount = 0;
        if ( photoCollection != null ) {
            photoCount = photoCollection.getPhotoCount();
        }

        // Determine the grid size based on couln & row count
        columnsToPaint = columnCount;
        // if columnCount is not specified determine it based on row count
        if ( columnCount < 0 ) {
            if ( rowCount > 0 ) {
                columnsToPaint = photoCount / rowCount;
                if ( columnsToPaint * rowCount < photoCount ) {
                    columnsToPaint++;
                }
            } else {
                columnsToPaint = (int) ( compSize.getWidth()/columnWidth );
            }
        }
        
        int col = 0;
        int row = 0;
        Rectangle thumbRect = new Rectangle();
        
        for ( int i = 0; i < photoCount; i++ ) {
            thumbRect.setBounds(col*columnWidth, row*rowHeight, columnWidth, rowHeight );
            if ( thumbRect.intersects( clipRect ) ) {
                PhotoInfo photo = photoCollection.getPhoto( i );
                if ( photo != null ) {
                    paintThumbnail( g2, photo, col*columnWidth, row*rowHeight, selection.contains( photo ) );
                }
            }
            col++;
            if ( col >= columnsToPaint ) {
                row++;
                col = 0;
            }
        }

	// Paint the selection rectangle if needed
	if ( dragSelectionRect != null ) {
	    Stroke prevStroke = g2.getStroke();
	    Color prevColor = g2.getColor();
	    g2.setStroke( new BasicStroke( 1.0f) );
	    g2.setColor( Color.BLACK );
	    g2.draw( dragSelectionRect );
	    g2.setColor( prevColor );
	    g2.setStroke( prevStroke );
	    lastDragSelectionRect = dragSelectionRect;
	}
    }

    boolean showDate = true;
    boolean showPlace = true;
    boolean showQuality = true;
    
    private void paintThumbnail( Graphics2D g2, PhotoInfo photo, int startx, int starty, boolean isSelected ) {
        log.debug( "paintThumbnail entry " + photo.getUid() );
        long startTime = System.currentTimeMillis();
        long thumbReadyTime = 0;
        long thumbDrawnTime = 0;
        long endTime = 0;
        // Current position in which attributes can be drawn
        int ypos = starty + rowHeight/2;
        boolean useOldThumbnail = false;
        
        // Create a transaction which will be used for persisten object operations
        // during painting (to avoid creating several short-livin transactions)
        ODMGXAWrapper txw = new ODMGXAWrapper();
        
        Thumbnail thumbnail = null;
        log.debug( "finding thumb" );
        boolean hasThumbnail = photo.hasThumbnail();
        log.debug( "asked if has thumb" );
        if ( hasThumbnail ) {
            log.debug( "Photo " + photo.getUid() + " has thumbnail" );
            thumbnail = photo.getThumbnail();
            log.debug( "got thumbnail" );
        } else {
            /*
             Check if the thumbnail has been just invalidated. If so, use the 
             old one until we get the new thumbnail created.
             */
            thumbnail = photo.getOldThumbnail();
            if ( thumbnail != null ) {
                useOldThumbnail = true;
            } else {
                // No success, use default thumnail.
                thumbnail = Thumbnail.getDefaultThumbnail();
            }

            // The photo does not have a thumbnail, so request one to be created
            if ( lastInstanceCreator == null ) {
                lastInstanceCreator = new PhotoInstanceCreator( this, 
                        ctrl.getDAOFactory().getPhotoInfoDAO(), photo, 
                        (Volume) VolumeBase.getDefaultVolume()  );
                lastInstanceCreator.execute();
            }
            
//            if ( !thumbCreatorThread.isBusy() ) {
//                log.debug( "Create thumbnail for " + photo.getUid() );
//                thumbCreatorThread.createThumbnail( photo );
//                log.debug( "Thumbnail request submitted" );
//            }            
        }
        thumbReadyTime = System.currentTimeMillis();
        
        log.debug( "starting to draw" );
        // Find the position for the thumbnail
        BufferedImage img = thumbnail.getImage();
        if ( img.getWidth() > columnWidth || img.getHeight() > rowHeight ) {
            /*
             If the image is too large for the space reserved for thumbnail, crop
             (yes, this should not be possible but many kinds of miracles do 
             happen. Also this has happened in some weird test cases!!!
             */
            img = img.getSubimage( 0, 0, 
                    Math.min( img.getWidth(), columnWidth ),
                    Math.min( img.getHeight(), rowHeight ) );
        }
        int x = startx + (columnWidth - img.getWidth())/(int)2;
        int y = starty + (rowHeight -  img.getHeight())/(int)2;
        
        log.debug( "drawing thumbnail" );

        // Draw shadow
        int offset = isSelected? 2 : 0;
        int shadowX[] = { x+3-offset, x+img.getWidth()+1+offset, x+img.getWidth()+1+offset };
        int shadowY[] = { y+img.getHeight()+1+offset, y+img.getHeight()+1+offset, y+3-offset };
        GeneralPath polyline =
                new GeneralPath(GeneralPath.WIND_EVEN_ODD, shadowX.length);
        polyline.moveTo(shadowX[0], shadowY[0]);
        for (int index = 1; index < shadowX.length; index++) {
            polyline.lineTo(shadowX[index], shadowY[index] );
        };
        BasicStroke shadowStroke = new BasicStroke(4.0f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER );
        Stroke oldStroke = g2.getStroke();
        g2.setStroke( shadowStroke );
        g2.setColor( Color.DARK_GRAY );
        g2.draw( polyline );
        g2.setStroke( oldStroke );

        // Paint thumbnail
        g2.drawImage( img, new AffineTransform( 1f, 0f, 0f, 1f, x, y ), null );
        if ( useOldThumbnail ) {
            creatingThumbIcon.paintIcon( this, g2, 
                        startx + (columnWidth - creatingThumbIcon.getIconWidth())/(int)2,
                        starty + ( rowHeight - creatingThumbIcon.getIconHeight())/(int)2 );
        }
        log.debug( "Drawn, drawing decorations" );
        if ( isSelected ) {
            Stroke prevStroke = g2.getStroke();
            Color prevColor = g2.getColor();
            g2.setStroke( new BasicStroke( 3.0f) );
            g2.setColor( Color.BLUE );
            g2.drawRect( x, y, img.getWidth(), img.getHeight() );
            g2.setColor( prevColor );
            g2.setStroke( prevStroke );
        }
        
        thumbDrawnTime = System.currentTimeMillis();
        // Increase ypos so that attributes are drawn under the image
        ypos += ((int)img.getHeight())/2 + 9;
        
        
        // Draw the attributes
        
        // Draw the qualoity icon to the upper left corner of the thumbnail            
        int quality = photo.getQuality();
        if ( showQuality && quality != PhotoInfo.QUALITY_UNDEFINED ) {
            ImageIcon qualityIcon = qualityIcons[quality];
            int qx = startx 
                    + (columnWidth-img.getWidth()-qualityIcon.getIconWidth())/(int)2;
            int qy = starty
                    + (rowHeight-img.getHeight()-qualityIcon.getIconHeight())/(int)2;
            qualityIcon.paintIcon( this, g2, qx, qy );
        }
        
        if ( photo.getRawSettings() != null ) {
            // Draw the "RAW" icon
            int rx = startx 
                    + (columnWidth+img.getWidth()-rawIcon.getIconWidth())/(int)2 - 5;
            int ry = starty 
                    + (columnWidth-img.getHeight()-rawIcon.getIconHeight())/(int)2 + 5;
            rawIcon.paintIcon( this, g2, rx, ry );
        }
        Color prevBkg = g2.getBackground();
        if ( isSelected ) {
            g2.setBackground( Color.BLUE );
        } else {
            g2.setBackground( this.getBackground() );
        }
        Font attrFont = new Font( "Arial", Font.PLAIN, 10 );
        FontRenderContext frc = g2.getFontRenderContext();
        if ( showDate && photo.getShootTime() != null ) {
            FuzzyDate fd = new FuzzyDate( photo.getShootTime(), photo.getTimeAccuracy() );
            
            String dateStr = fd.format();
            TextLayout txt = new TextLayout( dateStr, attrFont, frc );
            // Calculate the position for the text
            Rectangle2D bounds = txt.getBounds();
            int xpos = startx + ((int)(columnWidth - bounds.getWidth()))/2 - (int)bounds.getMinX();
            g2.clearRect( xpos-2, ypos-2,
                    (int)bounds.getWidth()+4, (int)bounds.getHeight()+4 );
            txt.draw( g2, xpos, (int)(ypos + bounds.getHeight()) );
            ypos += bounds.getHeight() + 4;
        }
        String shootPlace = photo.getShootingPlace();
        if ( showPlace && shootPlace != null && shootPlace.length() > 0  ) {
            TextLayout txt = new TextLayout( photo.getShootingPlace(), attrFont, frc );
            // Calculate the position for the text
            Rectangle2D bounds = txt.getBounds();
            int xpos = startx + ((int)(columnWidth-bounds.getWidth()))/2 - (int)bounds.getMinX();
            
            g2.clearRect( xpos-2, ypos-2,
                    (int)bounds.getWidth()+4, (int)bounds.getHeight()+4 );
            txt.draw( g2, xpos, (int)(ypos + bounds.getHeight()) );
            ypos += bounds.getHeight() + 4;
        }
        g2.setBackground( prevBkg );
        txw.commit();
        
        endTime = System.currentTimeMillis();
        log.debug( "paintThumbnail: exit " + photo.getUid() );
        log.debug( "Thumb fetch " + (thumbReadyTime - startTime ) + " ms" );
        log.debug( "Thumb draw " + ( thumbDrawnTime - thumbReadyTime ) + " ms" );
        log.debug( "Deacoration draw " + (endTime - thumbDrawnTime ) + " ms" );
        log.debug( "Total " + (endTime - startTime ) + " ms" );
        
    }


    public Dimension getPreferredSize() {
        int prefWidth = 0;
        int prefHeight = 0;
        
        if ( columnCount > 0 ) {
            prefWidth = columnWidth * columnCount;
            prefHeight = rowHeight;
            if ( photoCollection != null ) {
                prefHeight += rowHeight * (int)(photoCollection.getPhotoCount() / columnCount );
            }
        } else if ( rowCount > 0 ) {
            prefHeight = rowHeight * rowCount;
            prefWidth = columnWidth;
            if ( photoCollection != null ) {
                prefWidth += columnWidth * (int)( photoCollection.getPhotoCount() / rowCount );
            }
        } else {
            Dimension compSize = getSize();
            int columns = (int)(compSize.getWidth() / columnWidth);
            prefWidth = columnWidth * columns;
            prefHeight = rowHeight;
            if ( photoCollection != null ) {
                prefHeight += rowHeight * (int)(photoCollection.getPhotoCount() / columns );
            }            
        }
        prefHeight += 10;
        return new Dimension( prefWidth, prefHeight );

    }

    // implementation of java.awt.event.ActionListener interface

    /**
     * ActionListener implementation, is called when a popup menu item is selected
     * @param e The event object
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
	if ( cmd == PHOTO_ADD_TO_FOLDER_CMD ) {
            queryForNewFolder();
        }
    }
    

    public void photoCollectionChanged( PhotoCollectionChangeEvent e ) {
	refreshPhotoChangeListeners();
	revalidate();
	repaint();
    }

    /**
       This method is called when a PhotoInfo that is visible in the
       view is changed. it redraws the thumbnail & texts ascociated
       with it
    */
    public void photoInfoChanged( PhotoInfoChangeEvent ev ) {
	PhotoInfo photo = (PhotoInfo) ev.getSource();
	repaintPhoto( photo );
	// Find the location of the photo

    }

    /**
       Issues a repaint request for a certain photo.
       @param n index of the photo in current PhotoCollection
    */
    protected void repaintPhoto( int n ) {
	if ( n >= 0 ) {
	    int row = (int) (n / columnsToPaint);
	    int col = n - (row * columnsToPaint);
	    repaint( 0, col * columnWidth, row * rowHeight, columnWidth, rowHeight );
	}
    }

    /**
       Issues a repaint request for a certain photo.
       @param photo Photo to be repainted
    */
    protected void repaintPhoto( PhotoInfo photo ) {
	int n = photos.indexOf( photo );
	repaintPhoto( n );
    }

    /**
       Checks which photo is under the specified coordinates
       @return The photo that covers the position, null if the coordinates point to free space
       between photos.
    */
    private PhotoInfo getPhotoAtLocation( int x, int y ) {
        if ( photoCollection == null ) {
            return null;
        }

        PhotoInfo photo = null;
        int row = (int) y / rowHeight;
        int col = (int) x / columnWidth;
        int photoNum = row * columnsToPaint + col;
        log.warn( "Located photo # " + photoNum ); 
        
        if ( photoNum < photoCollection.getPhotoCount() ) {
            Rectangle imgRect = getPhotoBounds( photoNum );
            if ( imgRect.contains( new Point( x, y ) ) ) {
                photo = photoCollection.getPhoto( photoNum );
            }
        }
        return photo;
    }

    /**
     * Get bounding rectangle for the area in which the thumbnail for a given photo
     * is displayed
     * @param photoNum order number of the photo
     * @return Rectangle bounding the photo or null if photoNum is larger than
     * # of photos.
     */
    protected Rectangle getPhotoBounds( int photoNum ) {
        if ( photoNum < photoCollection.getPhotoCount() ) {
            PhotoInfo photoCandidate = photoCollection.getPhoto( photoNum );
            log.debug( "Checking bounds" );

            // Get thumbnail dimensions or use defaults if no thumbnail available
            int width = 100;
            int height = 75;
            Thumbnail thumb = null;
	    if ( photoCandidate.hasThumbnail() ) {
		thumb = photoCandidate.getThumbnail();
	    }
            if ( thumb != null ) {
                BufferedImage img = thumb.getImage();
                width = img.getWidth();
                height = img.getHeight();
            }
	    int row = (int) photoNum / columnsToPaint;
	    int col = photoNum - row*columnsToPaint;
            int imgX = col * columnWidth + (columnWidth - width)/(int)2;
            int imgY = row * rowHeight + (rowHeight -  height)/(int)2;
            Rectangle imgRect = new Rectangle( imgX, imgY, width, height );
	    return imgRect;
	}
	return null;
    }

    /**
      Get bounding rectangle for the table cell in which a given photo is displayed
     <p>
     This method is faster than @see getPhotoBounds since it does not need to 
     query PhotoInfo and its thumbnail (which may in worst case require loading 
     the thumbnail from disk) </p>
     * @param photoNum order number of the photo
     * @return Rectangle bounding the table cell or null if photoNum is larger than
     * # of photos.
     */
    protected Rectangle getPhotoCellBounds( int photoNum ) {
        Rectangle boundsRect = null;
        if ( photoNum >= 0 && photoNum < photoCollection.getPhotoCount() ) {
	    int row = (int) photoNum / columnsToPaint;
	    int col = photoNum - row*columnsToPaint;
            int imgX = col * columnWidth;
            int imgY = row * rowHeight;
            boundsRect = new Rectangle( imgX, imgY, columnWidth, rowHeight );
	}
	return boundsRect;
    }

    /**
       This method is called by @see ThumbnailCreatorThread after it has
       created a thumbnail. The method checks if there are still photos
       with no thumbnail and if such a photo exists it orders the thread
       to create a thumbnail for it.
    */
    public void thumbnailCreated( PhotoInfo photo ) {
	log.debug( "thumbnailCreated for " + photo.getUid() );
        photo = (PhotoInfo) ctrl.getPersistenceContext().merge( photo );
	repaintPhoto( photo );

	Container parent = getParent();
	Rectangle viewRect = null;
	if ( parent instanceof JViewport ) {
	    viewRect = ((JViewport)parent).getViewRect();
	}

	PhotoInfo nextPhoto = null;
	
	// Walk through all photos until we find a photo that is visible
	// and does not have a thumbnail. If all visible photos have a thumbnail
        // but some non-visible ones do not, create a thumbnail for one of those.
	log.debug( "Finding photo without thumbnail" );
	for ( int n = 0; n < photoCollection.getPhotoCount(); n++ ) {
            PhotoInfo photoCandidate = photoCollection.getPhoto( n );
	    log.debug( "Photo " + photoCandidate.getUid() );
	    if ( !photoCandidate.hasThumbnail() ) {
		log.debug( "No thumbnail" );
		Rectangle photoRect = getPhotoBounds( n );
		if ( photoRect.intersects( viewRect )  ) {
		    // This photo is visible so it is a perfect candidate
		    // for thumbnail creation. Do not look further
		    nextPhoto = photoCandidate;
		    break;
		} else if ( nextPhoto == null ) {
		    // Not visible but no photo without thumbnail has been
		    // found previously. Store as a candidate and keep looking.
		    nextPhoto = photoCandidate;
		}		    
	    }
	}
	if ( nextPhoto != null ) {
                lastInstanceCreator = new PhotoInstanceCreator( this, 
                        ctrl.getDAOFactory().getPhotoInfoDAO(), nextPhoto, 
                        (Volume) VolumeBase.getDefaultVolume()  );
                lastInstanceCreator.execute();
//                log.debug( "Making request for the next thumbail, " + p.getUid() );
//                thumbCreatorThread.createThumbnail( p );
//                log.debug( "request submitted" );
	} else {
            lastInstanceCreator = null;
        }
    }
    
    /**
       The mouse press event that started current drag
     */
    MouseEvent firstMouseEvent = null;
    
    // Implementation of java.awt.event.MouseListener

    /**
     * On mouse click select the photo clicked.
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mouseClicked(MouseEvent mouseEvent) {
        log.warn( "mouseClicked (" + mouseEvent.getX() + ", " + mouseEvent.getY() );

	if ( dragJustEnded ) {
	    // Selection was already handled by drag handler so do nothing
	    dragJustEnded = false;
	    return;
	}
	
        PhotoInfo clickedPhoto = getPhotoAtLocation( mouseEvent.getX(), mouseEvent.getY() );
        if ( clickedPhoto != null ) {
            if ( mouseEvent.isControlDown() ) {
                photoClickedCtrlDown( clickedPhoto );
            } else {
                photoClickedNoModifiers( clickedPhoto );
            }
	    // If this was a doublke click open the selected photo(s)
	    if ( mouseEvent.getClickCount() == 2 ) {
		showSelectedPhotoAction.actionPerformed( new ActionEvent( this, 0, null ) );
	    }
        } else {
            // The click was between photos. Clear the selection
            if ( !mouseEvent.isControlDown() ) {
		Object[] oldSelection = selection.toArray();
		selection.clear();
		fireSelectionChangeEvent();
		for ( int n = 0; n < oldSelection.length; n++ ) {
		    PhotoInfo photo = (PhotoInfo) oldSelection[n];
		    repaintPhoto( photo );
		}

            }
        }
        repaintPhoto( clickedPhoto );
    }

    private void photoClickedCtrlDown( PhotoInfo clickedPhoto ) {
        if ( selection.contains( clickedPhoto ) ) {
            selection.remove( clickedPhoto );
        } else {
            selection.add( clickedPhoto );
        }
	fireSelectionChangeEvent();
    }
                 

    private void photoClickedNoModifiers( PhotoInfo clickedPhoto ) {
	// Clear selection & issue repaint requests for all selected photos
	Object[] oldSelection = selection.toArray();
        selection.clear();
	for ( int n = 0; n < oldSelection.length; n++ ) {
	    PhotoInfo photo = (PhotoInfo) oldSelection[n];
	    repaintPhoto( photo );
	}
	    
        selection.add( clickedPhoto );
	fireSelectionChangeEvent();
    }
    
    /**
     * Describe <code>mouseEntered</code> method here.
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mouseEntered(MouseEvent mouseEvent) {
        
    }

    /**
     * Describe <code>mouseExited</code> method here.
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mouseExited(MouseEvent mouseEvent) {
        
    }

    /**
     * Describe <code>mousePressed</code> method here.
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mousePressed(MouseEvent mouseEvent) {
        // save the mouse press event so that we can later decide whether this gesture
        // is intended as a drag
        firstMouseEvent = mouseEvent;

	PhotoInfo photo = getPhotoAtLocation( mouseEvent.getX(), mouseEvent.getY() );
	if ( photo == null ) {
	    dragType = DRAG_TYPE_SELECT;
	    // If ctrl is not down clear the selection
            if ( !mouseEvent.isControlDown() ) {
		Object[] oldSelection = selection.toArray();
		selection.clear();
		fireSelectionChangeEvent();
		for ( int n = 0; n < oldSelection.length; n++ ) {
		    PhotoInfo p = (PhotoInfo) oldSelection[n];
		    repaintPhoto( p );
		}
		
            }
	    dragSelectionRect = new Rectangle( mouseEvent.getX(), mouseEvent.getY(), 0, 0 );
	} else {
	    dragType = DRAG_TYPE_DND;
	}
    }

    int dragType = 0;
    static final int DRAG_TYPE_SELECT = 1;
    static final int DRAG_TYPE_DND = 2;

    /**
       Area covered by current selection drag
    */
    Rectangle dragSelectionRect = null;
    Rectangle lastDragSelectionRect = null;

    boolean dragJustEnded = false;
    
    /**
     * 
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mouseReleased(MouseEvent mouseEvent) {
        firstMouseEvent = null;
	if ( dragType == DRAG_TYPE_SELECT && photoCollection != null ) {
            
            // Find out thumbails inside the selection rectangle
            
            // First lets restrict search to those rows that intersect with selection
            int topRow = (int) dragSelectionRect.getMinY()/rowHeight;
            int bottomRow = ((int) dragSelectionRect.getMaxY()/rowHeight)+1;
            int startPhoto = topRow * columnsToPaint;
            int endPhoto = bottomRow * columnsToPaint;
            if ( endPhoto > photoCollection.getPhotoCount() ) {
                endPhoto = photoCollection.getPhotoCount();
            }
            
            
            ODMGXAWrapper xa = new ODMGXAWrapper();
	    // Find out which photos are selected
	    for ( int n = startPhoto; n < endPhoto; n++ ) {
                /*
                 Performance optimization: Since getPhotoBounds() needs access 
                 to photo thumbnail which may not yet be loaded we will do first 
                 a rough check of if the table cell is in the selection area.
                 */
                Rectangle cellRect = getPhotoCellBounds( n );
                if ( dragSelectionRect.intersects( cellRect ) ) {
                    Rectangle photoRect = getPhotoBounds( n );
                    if ( dragSelectionRect.intersects( photoRect ) ) {
                        selection.add( photoCollection.getPhoto( n ) );
                        repaintPhoto( photoCollection.getPhoto( n ) );
                    }
                }
	    }
            fireSelectionChangeEvent();
	    xa.commit();
	    // Redrw the selection area so that the selection rectangle is not shown anymore
	    Rectangle repaintRect = dragSelectionRect;
	    if ( lastDragSelectionRect != null ) {
		repaintRect = dragSelectionRect.union( lastDragSelectionRect );
	    }
	    repaint( (int) repaintRect.getX()-1, (int) repaintRect.getY()-1,
		     (int) repaintRect.getWidth()+2, (int) repaintRect.getHeight()+2 );

	    dragSelectionRect = null;
	    lastDragSelectionRect = null;
	    // Notify the mouse click handler that it has to do nothing
	    dragJustEnded = true;
	}
    }


    // Implementation of java.awt.event.MouseMotionListener

    /**
     * Describe <code>mouseDragged</code> method here.
     *
     * @param e The event object
     */
    public void mouseDragged(MouseEvent e ) {
	switch ( dragType ) {
	case DRAG_TYPE_SELECT:
	    handleSelectionDragEvent( e );
	    break;
	case DRAG_TYPE_DND:
	    handleDnDDragEvent( e );
	    break;
	default:
	    log.error( "Invalid drag type" );
	}
        
        // Make sure tht current drag location is visible
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);	
    }

    protected void handleSelectionDragEvent( MouseEvent e ) {
	dragSelectionRect = new Rectangle( firstMouseEvent.getX(), firstMouseEvent.getY(), 0, 0 );
	dragSelectionRect.add( e.getX(), e.getY() );

	// Determine which area needs to be redrawn. If there is a selection marker rectangle already drawn
	// the redraw are must be union of the previous and current areas (current area can be also smaller!
	Rectangle repaintRect = dragSelectionRect;
	if ( lastDragSelectionRect != null ) {
	    repaintRect = dragSelectionRect.union( lastDragSelectionRect );
	}
	repaint( (int) repaintRect.getX()-1, (int) repaintRect.getY()-1,
		 (int) repaintRect.getWidth()+2, (int) repaintRect.getHeight()+2 );
    }
    
    protected void handleDnDDragEvent( MouseEvent e ) {
        //Don't bother to drag if no photo is selected
        if ( selection.size() == 0 ) {
            return;
        }

        if (firstMouseEvent != null) {
            log.warn( "considering drag" );
            e.consume();

            
            //If they are holding down the control key, COPY rather than MOVE
            int ctrlMask = InputEvent.CTRL_DOWN_MASK;
            int action = e.isControlDown() ?
                TransferHandler.COPY : TransferHandler.MOVE;

            int dx = Math.abs(e.getX() - firstMouseEvent.getX());
            int dy = Math.abs(e.getY() - firstMouseEvent.getY());
            //Arbitrarily define a 5-pixel shift as the
            //official beginning of a drag.
            if (dx > 5 || dy > 5) {
                log.warn( "Start a drag" );
                //This is a drag, not a click.
                JComponent c = (JComponent)e.getSource();
                //Tell the transfer handler to initiate the drag.
                TransferHandler handler = c.getTransferHandler();
                handler.exportAsDrag(c, firstMouseEvent, action);
                firstMouseEvent = null;
            }
        }   
    }
    
    public void selectNextPhoto() {
        if ( this.getSelectedCount() == 1 ) {
            PhotoInfo selectedPhoto = (PhotoInfo) (selection.toArray())[0];
            int idx = photos.indexOf( selectedPhoto );
            idx++;
            if ( idx >= photos.size() ) {
                idx = photos.size()-1;
            }
            // Scroll so that the selected photo is visible
            Rectangle selectionBounds = getPhotoCellBounds( idx );
            scrollRectToVisible( selectionBounds );

            selection.clear();
            selection.add( photos.get( idx ));
            fireSelectionChangeEvent();            
            repaint();
        }        
    }
    
    public void selectPreviousPhoto() {
        if ( this.getSelectedCount() == 1 ) {
            PhotoInfo selectedPhoto = (PhotoInfo) (selection.toArray())[0];
            int idx = photos.indexOf( selectedPhoto );
            idx--;
            if ( idx < 0 ) {
                idx = 0;
            }

            // Scroll so that the selected photo is visible
            Rectangle selectionBounds = getPhotoCellBounds( idx );
            scrollRectToVisible( selectionBounds );

            selection.clear();
            selection.add( photos.get( idx ));
            fireSelectionChangeEvent();
            repaint();
        }        
    }
    
    public void selectFirstPhoto() {
        if ( photos.size() > 0 ) {
            // Scroll so that the selected photo is visible
            Rectangle selectionBounds = getPhotoCellBounds( 0 );
            scrollRectToVisible( selectionBounds );
            
            selection.clear();
            selection.add( photos.get( 0 ));
            fireSelectionChangeEvent();
            repaint();
        }
    }
    /**
     * Describe <code>mouseMoved</code> method here.
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mouseMoved(MouseEvent mouseEvent) {
        
    }
    
    PhotoInfoDlg propertyDlg = null;
    
    
    JFrame frame = null;
    PhotoViewer viewer = null;


    /**
       Queries the user for a new folder into which the photo will be added.
    */
    public void queryForNewFolder() {
        // Try to find the frame in which this component is in
        Frame frame = null;
        Container c = getTopLevelAncestor();
        if ( c instanceof Frame ) {
            frame = (Frame) c;
        }

        PhotoFolderSelectionDlg dlg = new PhotoFolderSelectionDlg( frame, true );
        if ( dlg.showDialog() ) {
            PhotoFolder folder = dlg.getSelectedFolder();
            // A folder was selected, so add the selected photo to this folder
            Collection selectedPhotos = getSelection();
            Iterator iter = selectedPhotos.iterator();
            while ( iter.hasNext() ) {
                PhotoInfo photo = (PhotoInfo) iter.next();
                if ( photo != null ) {
                    folder.addPhoto ( photo );
                }
            }
        }
    }

    // IMplementation of Scrollable interface
    
    
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /**
     Get the amount that needs to be scrolled when pressing scroll bar button.
     @return number of pixels to scroll until next image thumbnail
     */ 
    public int getScrollableUnitIncrement(Rectangle visibleRect,
            int orientation, int direction ) {
        //Get the current position.
        int currentPosition = 0;
        int incrementUnit = 0;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
            incrementUnit = columnWidth;
        } else {
            currentPosition = visibleRect.y;
            incrementUnit = rowHeight;
        }
        
        //Return the number of pixels between currentPosition
        //and the nearest tick mark in the indicated direction.
        if (direction  < 0) {
            int newPosition = currentPosition -
                    (currentPosition / incrementUnit) * incrementUnit;
            return (newPosition == 0) ? incrementUnit : newPosition;
        } else {
            return ((currentPosition / incrementUnit) + 1) * incrementUnit
                    - currentPosition;
        }     
    }

    /**
     Get the number of pixels to xcroll if user clicks on scroll bar outside 
     handle.
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL)
            return visibleRect.width - columnWidth;
        else
            return visibleRect.height - rowHeight;
    }

    /**
     Returns whether containing Scroll pane should force width of the component 
     to match its width.
     @return Returns <code>true</code> if the component is in "no preview" mode.
     */
    public boolean getScrollableTracksViewportWidth() {
        boolean shouldTrack = false;
        if ( rowCount < 0 && columnCount < 0 ) {
            shouldTrack = true;
        }
        return shouldTrack;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

}
