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
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;



/**
   This class implements the default thumbnail view for photo
   collections. Some of the planned features include:
   <ul>
   <li> Either vertically or horizontally scrollable view with multiple columns  </li>

   <li> Multiple selection for thumbnails </li>

   <li> Automatic fetching and creation of thumbnails on background if these do not exist 
   </li>
   </ul>

   @author Harri Kaimio

*/

public class PhotoCollectionThumbView
    extends JPanel
    implements MouseMotionListener, MouseListener, ActionListener, PhotoCollectionChangeListener {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoCollectionThumbView.class.getName() );
    
    /**
     * Creates a new <code>PhotoCollectionThumbView</code> instance.
     *
     */
    public PhotoCollectionThumbView() {
        super();
        createUI();
    }

    /**
     * Returns the currently displayed photo collection or <code>null</code> of none specified
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
      revalidate();
      repaint();
    }

    

    PhotoCollection collection;

    
    /**
     * Get a currently selected photos
     * @return Collection of currently selected photos or <code>null</code> if none is selected
     */

    public Collection getSelection( ) {
        return new HashSet( selection );
    }


    HashSet selection = new HashSet();
    
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
        
        addMouseListener( this );
        addMouseMotionListener( this );

        

        // Create the popup menu
        popup = new JPopupMenu();
        JMenuItem propsItem = new JMenuItem( "Properties" );
        propsItem.addActionListener( this );
        propsItem.setActionCommand( PHOTO_PROPS_CMD );
        JMenuItem showItem = new JMenuItem( "Show image" );
        showItem.addActionListener( this );
        showItem.setActionCommand( PHOTO_SHOW_CMD );
        JMenuItem rotateCW = new JMenuItem( "Rotate 90 deg CW" );
        rotateCW.addActionListener( this );
        rotateCW.setActionCommand( PHOTO_ROTATE_CW_CMD );
        JMenuItem rotateCCW = new JMenuItem( "Rotate 90 deg CCW" );
        rotateCCW.addActionListener( this );
        rotateCCW.setActionCommand( PHOTO_ROTATE_CCW_CMD );
        JMenuItem rotate180deg = new JMenuItem( "Rotate 180 degrees" );
        rotate180deg.addActionListener( this );
        rotate180deg.setActionCommand( PHOTO_ROTATE_180_CMD );
        JMenuItem addToFolder = new JMenuItem( "Add to folder..." );
        addToFolder.addActionListener( this );
        addToFolder.setActionCommand( PHOTO_ADD_TO_FOLDER_CMD );
        popup.add( showItem );
        popup.add( propsItem );
        popup.add( rotateCW );
        popup.add( rotateCCW );
        popup.add( rotate180deg );
        popup.add( addToFolder );
        MouseListener popupListener = new PopupListener();
        addMouseListener( popupListener );
               
    }


    // Popup menu actions
    private static final String PHOTO_PROPS_CMD = "photoProps";
    private static final String PHOTO_SHOW_CMD = "photoShow";
    private static final String PHOTO_ROTATE_CW_CMD = "rotateCW";
    private static final String PHOTO_ROTATE_CCW_CMD = "rotateCCW";
    private static final String PHOTO_ROTATE_180_CMD = "rotate180";
    private static final String PHOTO_ADD_TO_FOLDER_CMD = "addToFolder";


    
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
    }

    boolean showDate = true;
    boolean showPlace = true;
    
    private void paintThumbnail( Graphics2D g2, PhotoInfo photo, int startx, int starty, boolean isSelected ) {

        // Current position in which attributes can be drawn
        int ypos = starty + rowHeight/2;

        if ( photo != null ) {
            Thumbnail thumbnail = photo.getThumbnail();
        
            if ( thumbnail != null ) {
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
            }
            
            // Draw the attributes
            Color prevBkg = g2.getBackground();
            if ( isSelected ) {
                g2.setBackground( Color.BLUE );
            }
            Font attrFont = new Font( "Arial", Font.PLAIN, 10 );
            FontRenderContext frc = g2.getFontRenderContext();
            if ( showDate && photo.getShootTime() != null ) {
                DateFormat df = new SimpleDateFormat( "dd.MM.yyyy k:mm" );
                String dateStr = df.format( photo.getShootTime() );
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
        if ( cmd == PHOTO_PROPS_CMD ) {
            showSelectionPropsDialog();
        } else if ( cmd == PHOTO_SHOW_CMD ) {
            showSelectedPhoto();
        } else if ( cmd == PHOTO_ROTATE_CW_CMD ) {
            rotateSelectedPhoto( 90 );
        } else if ( cmd == PHOTO_ROTATE_CCW_CMD ) {
            rotateSelectedPhoto( -90 );
        } else if ( cmd == PHOTO_ROTATE_180_CMD ) {
            rotateSelectedPhoto( 180 );
        } else if ( cmd == PHOTO_ADD_TO_FOLDER_CMD ) {
            queryForNewFolder();
        }
    }
    

    public void photoCollectionChanged( PhotoCollectionChangeEvent e ) {
	revalidate();
	repaint();
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
            int imgX = col * columnWidth + (columnWidth - width)/(int)2;
            int imgY = row * rowHeight + (rowHeight -  height)/(int)2;
            Rectangle imgRect = new Rectangle( imgX, imgY, width, height );
            log.warn( "Checking in rectangle (" + imgX + ", " + imgY + ") - (" +
                      width + ", " + height + ")" );
            if ( imgRect.contains( new Point( x, y ) ) ) {
                photo = photoCandidate;
            }
        }
        return photo;
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


        PhotoInfo clickedPhoto = getPhotoAtLocation( mouseEvent.getX(), mouseEvent.getY() );
        if ( clickedPhoto != null ) {
            if ( mouseEvent.isControlDown() ) {
                photoClickedCtrlDown( clickedPhoto );
            } else {
                photoClickedNoModifiers( clickedPhoto );
            }
        } else {
            // The click was between photos. Clear the selection
            if ( !mouseEvent.isControlDown() ) {
                selection.clear();
            }
        }
        repaint();
    }

    private void photoClickedCtrlDown( PhotoInfo clickedPhoto ) {
        if ( selection.contains( clickedPhoto ) ) {
            selection.remove( clickedPhoto );
        } else {
            selection.add( clickedPhoto );
        }
    }
                 

    private void photoClickedNoModifiers( PhotoInfo clickedPhoto ) {
        selection.clear();
        selection.add( clickedPhoto );
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
    }

    /**
     * 
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mouseReleased(MouseEvent mouseEvent) {
        firstMouseEvent = null;
    }


    // Implementation of java.awt.event.MouseMotionListener

    /**
     * Describe <code>mouseDragged</code> method here.
     *
     * @param mouseEvent a <code>MouseEvent</code> value
     */
    public void mouseDragged(MouseEvent e ) {
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
    
    /**
       Show the PhotoInfoEditor dialog for the selected photo
    */
    public void showSelectionPropsDialog() {

        if ( selection.size() == 0 ) {
            return;
        }
        Iterator iter = selection.iterator();
        PhotoInfo[] selectedPhotos = new PhotoInfo[selection.size()];
        int i = 0;
        while ( iter.hasNext() && i < selectedPhotos.length ) {
            selectedPhotos[i++] = (PhotoInfo) iter.next();
        }
	
        // Try to find the frame in which this component is in
        Frame frame = null;
        Container c = getTopLevelAncestor();
        if ( c instanceof Frame ) {
            frame = (Frame) c;
        }

        if (propertyDlg == null ) {
            propertyDlg = new PhotoInfoDlg( frame, true, selectedPhotos );
        } else {
            propertyDlg.setPhotos( selectedPhotos );
        }

        propertyDlg.showDialog();
    }
    
    JFrame frame = null;
    PhotoViewer viewer = null;

    /**
       Shows the selected photo in a popup window
    */
    public void showSelectedPhoto() {
        try {
            Collection selectedPhotos = getSelection();
            Iterator iter = selectedPhotos.iterator();
            while ( iter.hasNext() ) {
                PhotoInfo photo = (PhotoInfo) iter.next();
                JFrame frame = new JFrame( "Photo" );
                final PhotoViewer viewer = new PhotoViewer();
                frame.getContentPane().add( viewer, BorderLayout.CENTER );
                frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

                // This is a WAR for a memory management problem. For
                // some reason the frame and objects owned by it seem
                // not to be garbage collected. So we free the large
                // image buffers to avoid massive memory leak.
                
                frame.addWindowListener( new WindowAdapter() {
                        public void windowClosing( WindowEvent e ) {
                            viewer.setPhoto( null );
                        }
                    } );
                log.warn( "Created frame" );
                viewer.setPhoto( photo );
                log.warn( "Photo set" );
                frame.pack();
                log.warn( "Frame packed" );
                frame.setVisible( true );
                log.warn( "Frame visible" );
            }
        } catch ( Throwable e ) {
            System.err.println( "Out of memory error" );
            log.warn( e );
            e.printStackTrace();
        }
    }

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

    /**
       Rotate selected photo by specified amount
       @param rot Rotation in degrees, positive means clockwise
    */
    public void rotateSelectedPhoto( double rot ) {
        Collection selectedPhotos = getSelection();
        Iterator iter = selectedPhotos.iterator();
        while ( iter.hasNext() ) {
            PhotoInfo photo = (PhotoInfo) iter.next();
            if ( photo != null ) {
                double curRot = photo.getPrefRotation();
                photo.setPrefRotation( curRot + rot );
            }
        }
    }
}