// PhotoCollectionThumbView.java

package photovault.swingui;

import imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.text.*;
import java.util.*;
import photovault.folder.*;
import java.awt.geom.*;
import java.text.*;
import java.awt.font.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;



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
	       PhotoCollectionChangeListener, PhotoInfoChangeListener {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoCollectionThumbView.class.getName() );
    
    /**
     * Creates a new <code>PhotoCollectionThumbView</code> instance.
     *
     */
    public PhotoCollectionThumbView() {
        super();
        createUI();
	thumbCreatorThread = new ThumbCreatorThread( this );
	thumbCreatorThread.start();
    }

    ThumbCreatorThread thumbCreatorThread;
    
    /**
     * Returns the currently displayed photo collection or <code>null</code> if none specified
     */
    public PhotoCollection getCollection() {
        return photoCollection;
    }

    

    PhotoCollection photoCollection = null;
    
    /**
     * Set the collection that should be viewed
     * @param v  Value to assign to collection.
     */
    public void setCollection(PhotoCollection  v) {
      if ( photoCollection != null ) {
	photoCollection.removePhotoCollectionChangeListener( this );
      }
        
      photoCollection = v;
      photoCollection.addPhotoCollectionChangeListener( this );
      refreshPhotoChangeListeners();
      
      revalidate();
      repaint();
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
	
	photos.removeAllElements();
	
	// Add the change listeners to all photos so that we are aware of modifications
	for ( int n = 0; n < photoCollection.getPhotoCount(); n++ ) {
	    PhotoInfo photo = photoCollection.getPhoto( n );
	    photo.addChangeListener( this );
	    photos.add( photo );
	}
    }	
    

    PhotoCollection collection;
    Vector photos = new Vector();

    
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
    int columnCount = 0;


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
    
    void createUI() {
      photoTransferHandler = new PhotoCollectionTransferHandler( this );
        setTransferHandler( photoTransferHandler );

	setAutoscrolls( true );
	
        addMouseListener( this );
        addMouseMotionListener( this );

        

        // Create the popup menu
        popup = new JPopupMenu();
	editSelectionPropsAction = 
	    new EditSelectionPropsAction( this, "Properties...", null, 
					  "Edit properties of the selected photos",
					  KeyEvent.VK_P );
        JMenuItem propsItem = new JMenuItem( editSelectionPropsAction );
	showSelectedPhotoAction =
	    new ShowSelectedPhotoAction( this, "Show image", null,
					 "Show the selected phot(s)",
					 KeyEvent.VK_S );
        JMenuItem showItem = new JMenuItem( showSelectedPhotoAction );
	rotateCWAction =
	    new RotateSelectedPhotoAction( this, 90, "Rotate 90 deg CW",
					   null, "Rotates the selected photo",
					   KeyEvent.VK_R );
        JMenuItem rotateCW = new JMenuItem( rotateCWAction );
	rotateCCWAction
	    = new RotateSelectedPhotoAction( this, 270, "Rotate 90 deg CCW",
					     null, "Rotates the selected photo",
					     KeyEvent.VK_W );
        JMenuItem rotateCCW = new JMenuItem( rotateCCWAction );
	rotate180degAction = new RotateSelectedPhotoAction( this, 180, "Rotate 180 deg", null, "Rotates the selected photo", KeyEvent.VK_R );
        JMenuItem rotate180deg = new JMenuItem( rotate180degAction );
        JMenuItem addToFolder = new JMenuItem( "Add to folder..." );
        addToFolder.addActionListener( this );
        addToFolder.setActionCommand( PHOTO_ADD_TO_FOLDER_CMD );
	exportSelectedAction = new ExportSelectedAction( this, "Export selected...", null, "Export the selected photos to from archive database to image files", KeyEvent.VK_X );
	JMenuItem exportSelected = new JMenuItem( exportSelectedAction );

	// Create the Quality submenu
	JMenu qualityMenu = new JMenu( "Quality" );
	String qualityStrings[] = { "Unevaluated", "Top", "Good", "OK", "Poor", "Unusable" };
	for ( int n = 0; n < qualityStrings.length; n++ ) {
	    AbstractAction qualityAction
		= new SetPhotoQualityAction( this, n, qualityStrings[n], null,
					     "Set quality of selected phots to \"" + qualityStrings[n] + "\"",
					     null );
	    JMenuItem qualityMenuItem = new JMenuItem( qualityAction );
	    qualityMenu.add( qualityMenuItem );
	}

	
        popup.add( showItem );
        popup.add( propsItem );
        popup.add( rotateCW );
        popup.add( rotateCCW );
        popup.add( rotate180deg );
	popup.add( qualityMenu );
        popup.add( addToFolder );
        popup.add( exportSelected );
        MouseListener popupListener = new PopupListener();
        addMouseListener( popupListener );
               
    }


    // Popup menu actions
    private static final String PHOTO_ADD_TO_FOLDER_CMD = "addToFolder";
    private AbstractAction exportSelectedAction;
    private AbstractAction editSelectionPropsAction;
    private AbstractAction showSelectedPhotoAction;
    private AbstractAction rotateCWAction;
    private AbstractAction rotateCCWAction;
    private AbstractAction rotate180degAction;

    public AbstractAction getExportSelectedAction() {
	return exportSelectedAction;
    }

    public AbstractAction getEditSelectionPropsAction() {
	return editSelectionPropsAction;
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
    
    public void paint( Graphics g ) {
        super.paint( g );
        Graphics2D g2 = (Graphics2D) g;
        Rectangle clipRect = g.getClipBounds();
        Dimension compSize = getSize();
        columnCount = (int)(compSize.getWidth()/columnWidth);

        int photoCount = 0;
        if ( photoCollection != null ) {
            photoCount = photoCollection.getPhotoCount();
        }

        int col = 0;
        int row = 0;
        Rectangle thumbRect = new Rectangle();
        for ( int i = 0; i < photoCount; i++ ) {
            thumbRect.setBounds(col*columnWidth, row*rowHeight, columnWidth, rowHeight );
            if ( thumbRect.intersects( clipRect ) ) {
                PhotoInfo photo = photoCollection.getPhoto( i );
                paintThumbnail( g2, photo, col*columnWidth, row*rowHeight, selection.contains( photo ) );
            }
            col++;
            if ( col >= columnCount ) {
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
    
    private void paintThumbnail( Graphics2D g2, PhotoInfo photo, int startx, int starty, boolean isSelected ) {

        // Current position in which attributes can be drawn
        int ypos = starty + rowHeight/2;

        if ( photo != null ) {
	    Thumbnail thumbnail = null;
	    if ( photo.hasThumbnail() ) {
		thumbnail = photo.getThumbnail();
	    } else {
		thumbnail = Thumbnail.getDefaultThumbnail();
		if ( !thumbCreatorThread.isBusy() ) {
		    thumbCreatorThread.createThumbnail( photo );
		}
	    }

	    // Find the position for the thumbnail
	    BufferedImage img = thumbnail.getImage();
	    int x = startx + (columnWidth - img.getWidth())/(int)2;
	    int y = starty + (rowHeight -  img.getHeight())/(int)2;
            
	    g2.drawImage( img, new AffineTransform( 1f, 0f, 0f, 1f, x, y ), null );
	    if ( isSelected ) {
		Stroke prevStroke = g2.getStroke();
		Color prevColor = g2.getColor();
		g2.setStroke( new BasicStroke( 3.0f) );
		g2.setColor( Color.BLUE );
		g2.drawRect( x, y, img.getWidth(), img.getHeight() );
		g2.setColor( prevColor );
		g2.setStroke( prevStroke );
	    }
	    // Increase ypos so that attributes are drawn under the image
	    ypos += ((int)img.getHeight())/2 + 4;
	    
	
	    // Draw the attributes
	    Color prevBkg = g2.getBackground();
	    if ( isSelected ) {
		g2.setBackground( Color.BLUE );
	    }
	    Font attrFont = new Font( "Arial", Font.PLAIN, 10 );
	    FontRenderContext frc = g2.getFontRenderContext();
	    if ( showDate && photo.getShootTime() != null ) {
		FuzzyDate fd = new FuzzyDate( photo.getShootTime(), photo.getTimeAccuracy() );
		
// 		long accuracy = ((long) photo.getTimeAccuracy() ) * 24 * 3600 * 1000;
// 		log.warn( "Accuracy = " + accuracy );
// 		String dateStr = "";
// 		if ( accuracy > 0 ) {
// 		    double accuracyFormatLimits[] = {0, 3, 14, 180};
// 		    String accuracyFormatStrings[] = {
// 			"dd.MM.yyyy",
// 			"'wk' w yyyy",
// 			"MMMM yyyy",
// 			"yyyy"
// 		    };

// 		    // Find the correct format to use
// 		    double dblAccuracy = photo.getTimeAccuracy();
// 		    String formatStr =accuracyFormatStrings[0];
// 		    for ( int i = 1; i < accuracyFormatLimits.length; i++ ) {
// 			if ( dblAccuracy < accuracyFormatLimits[i] ) {
// 			    break;
// 			}
// 			formatStr =accuracyFormatStrings[i];
// 		    }
			
// 		    // Show the limits of the accuracy range
// 		    DateFormat df = new SimpleDateFormat( formatStr );
// 		    Date lower = new Date( photo.getShootTime().getTime() - accuracy );
// 		    Date upper = new Date( photo.getShootTime().getTime() + accuracy );
// 		    String lowerStr = df.format( lower );
// 		    String upperStr = df.format( upper );
// 		    dateStr = lowerStr;
// 		    if ( !lowerStr.equals( upperStr ) ) {
// 			dateStr += " - " + upperStr;
// 		    }
// 		} else {
// 		    DateFormat df = new SimpleDateFormat( "dd.MM.yyyy k:mm" );
// 		    dateStr = df.format( photo.getShootTime() );
// 		}

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
	}
    }


    public Dimension getPreferredSize() {
        int prefWidth = 4 * columnWidth;
        int prefHeight = rowHeight;
        if ( photoCollection != null ) {
            prefHeight += rowHeight * (int)(photoCollection.getPhotoCount()/4);
        }
        return new Dimension( prefWidth, prefHeight );

    }

    // implementation of java.awt.event.ActionListener interface

    /**
     * ActionListener implementation, is called when a popup menu item is selected
     * @param  <description>
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
       @param n index of the photo in curren PhotoCollection
    */
    protected void repaintPhoto( int n ) {
	if ( n >= 0 ) {
	    int row = (int) (n / columnCount);
	    int col = n - (row * columnCount);
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
        int photoNum = row * columnCount + col;
        log.warn( "Located photo # " + photoNum ); 
        
        if ( photoNum < photoCollection.getPhotoCount() ) {
            Rectangle imgRect = getPhotoBounds( photoNum );
            if ( imgRect.contains( new Point( x, y ) ) ) {
                photo = photoCollection.getPhoto( photoNum );
            }
        }
        return photo;
    }

    protected Rectangle getPhotoBounds( int photoNum ) {
        if ( photoNum < photoCollection.getPhotoCount() ) {
            PhotoInfo photoCandidate = photoCollection.getPhoto( photoNum );
            log.warn( "Checking bounds" );

            // Check whether the click was inside the thumbnail or not
            int width = 100;
            int height = 75;
            Thumbnail thumb = photoCandidate.getThumbnail();
            if ( thumb != null ) {
                BufferedImage img = thumb.getImage();
                width = img.getWidth();
                height = img.getHeight();
            }
	    int row = (int) photoNum / columnCount;
	    int col = photoNum - row*columnCount;
            int imgX = col * columnWidth + (columnWidth - width)/(int)2;
            int imgY = row * rowHeight + (rowHeight -  height)/(int)2;
            Rectangle imgRect = new Rectangle( imgX, imgY, width, height );
	    return imgRect;
	}
	return null;
    }


    /**
       This method is called by @see ThumbnailCreatorThread after it has
       created a thumbnail. The method checks if there are still photos
       with no thumbnail and if such a photo exists it orders the thread
       to create a thumbnail for it.
    */
    public void thumbnailCreated( PhotoInfo photo ) {
	repaintPhoto( photo );

	Container parent = getParent();
	Rectangle viewRect = null;
	if ( parent instanceof JViewport ) {
	    viewRect = ((JViewport)parent).getViewRect();
	}

	PhotoInfo nextPhoto = null;
	
	// Walk through all phoso until we find a photo that is visible
	// and does not have a thumbnail
	for ( int n = 0; n < photoCollection.getPhotoCount(); n++ ) {
            PhotoInfo photoCandidate = photoCollection.getPhoto( n );
	    if ( !photoCandidate.hasThumbnail() ) {
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
	if ( nextPhoto != null && !thumbCreatorThread.isBusy() ) {
	    final PhotoInfo p = nextPhoto;
// 	    SwingUtilities.invokeLater( new Runnable() {
// 		    public void run() {
			thumbCreatorThread.createThumbnail( p );
// 		    }
// 		});
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
	if ( dragType == DRAG_TYPE_SELECT ) {

	    // Find out which photos are selected
	    for ( int n = 0; n < photoCollection.getPhotoCount(); n++ ) {
		Rectangle photoRect = getPhotoBounds( n );
		if ( dragSelectionRect.intersects( photoRect ) ) {
		    selection.add( photoCollection.getPhoto( n ) );
		    repaintPhoto( photoCollection.getPhoto( n ) );
		}
		fireSelectionChangeEvent();
	    }
	    
	    
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
     * @param mouseEvent a <code>MouseEvent</code> value
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

}