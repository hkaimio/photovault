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


import java.awt.Container;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IllegalFormatException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.omg.SendingContext.RunTime;
import org.photovault.common.PhotovaultException;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.swingui.export.ExportDlg;

/**
 This action class implements exporting of all the selected images from a certain thumbnail
 view.
 */
class ExportSelectedAction extends AbstractAction implements SelectionChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ExportSelectedAction.class.getName() );
    
    /**
     Constructor.
     @param view The view this action object is associated with. The action gets
     the selection to export from this view.
     */
    public ExportSelectedAction( PhotoCollectionThumbView view, String text, ImageIcon icon,
            String desc, int mnemonic) {
        super( text, icon );
        this.view = view;
        putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, new Integer( mnemonic) );
        putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_S, 
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
        view.addSelectionChangeListener( this );
        setEnabled( view.getSelectedCount() > 0 );
    }
    
    public void selectionChanged( SelectionChangeEvent e ) {
        setEnabled( view.getSelectedCount() > 0 );
    }
    
    public void actionPerformed( ActionEvent ev ) {
        File exportFile = null;
        if ( view.getSelectedCount() > 1 ) {
            exportFile = new File( "image_$n.jpg" );
        } else {
            exportFile = new File( "image.jpg" );
        }
        ExportDlg dlg = new ExportDlg( null, true );
        dlg.setFilename( exportFile.getAbsolutePath() );
        
        int retval = dlg.showDialog();
        if ( retval == ExportDlg.EXPORT_OPTION ) {
            Container c = view.getTopLevelAncestor();
            Cursor oldCursor = c.getCursor();
            c.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
            String exportFileTmpl = dlg.getFilename();
            int exportWidth = dlg.getImgWidth();
            int exportHeight = dlg.getImgHeight();
            Collection selection = view.getSelection();
            if ( selection != null ) {
                if ( selection.size() > 1 ) {
                    // Ensure that the numbering order is the same is in current view
                    PhotoInfo exportPhotos[] 
                            = (PhotoInfo[]) selection.toArray( new PhotoInfo[selection.size() ]);
                    Comparator comp = view.getPhotoOrderComparator();
                    if ( comp != null ) {
                        Arrays.sort( exportPhotos, comp );
                    }
                    String format = getSequenceFnameFormat( exportFileTmpl );
                    BrowserWindow w = null;
                    ExportThread exporter = new ExportThread( this, exportPhotos, 
                            format, exportWidth, exportHeight );
                    Thread t = new Thread( exporter );
                    setEnabled( false );
                    t.start();
                } else {
                    Iterator iter = selection.iterator();
                    if ( iter.hasNext() ) {
                        PhotoInfo photo = (PhotoInfo) iter.next();
                        try {
                            photo.exportPhoto( new File( exportFileTmpl ),
                                    exportWidth, exportHeight );
                        } catch (PhotovaultException ex) {
                            JOptionPane.showMessageDialog( view.getRootPane(),
                                    ex.getMessage(),
                                    "Error exporting image",
                                    JOptionPane.ERROR_MESSAGE );
                        }
                    }
                }
            }
            c.setCursor( oldCursor );
        }
    }
    
    static class ExportThread implements Runnable {
        
        private PhotoInfo exportPhotos[];
        
        private String format;
        
        private int exportWidth;
        
        private int exportHeight;
        
        private ExportSelectedAction owner;
        
        ExportThread( ExportSelectedAction owner, PhotoInfo[] exportPhotos, 
                String fnameFormat, int width, int height ) {
            this.exportPhotos = exportPhotos;
            this.format = fnameFormat;
            this.exportWidth = width;
            this.exportHeight = height;
            this.owner = owner;
        }
        
        public void run() {
            for ( int n = 0; n < exportPhotos.length; n++  ) {
                try {
                    String fname;
                    try {
                        fname = String.format(format, new Integer(n + 1));
                    } catch (IllegalFormatException e ) {
                        owner.exportError( "Cannot format file name: \n" + e.getMessage() );
                        break;
                    }
                    int percent = (n) * 100 / exportPhotos.length;
                    owner.exportingPhoto( this, fname, percent );
                    File f = new File( fname );
                    PhotoInfo photo = exportPhotos[n];
                    int triesLeft = 1;
                    boolean succeeded = false;
                    while ( !succeeded && triesLeft > 0 ) {
                        try {
                            photo.exportPhoto( f, exportWidth, exportHeight );
                            succeeded = true;
                        } catch ( PhotovaultException e ) {
                            owner.exportError( e.getMessage() );
                        } catch ( OutOfMemoryError e ) {
                            /*
                             Often we end here because minor GC did not succeed 
                             to free enough memory from young generations. Let's 
                             try again, second time Java will do major gc round 
                             and this might be succesful.
                             */
                            if ( triesLeft > 0 ) {
                                Runtime rt = Runtime.getRuntime();
                                long freeMem = rt.freeMemory();
                                long totMem = rt.totalMemory();
                                log.error( "Out of memory while exporting " + 
                                        fname + ", trying again" );
                                System.gc();
                                long freeMemAfter = rt.freeMemory();
                                long totMemAfter = rt.totalMemory();
                                log.error( "Free memory " + freeMem + "->" + 
                                        freeMemAfter + ", freed " + 
                                        ((totMem-freeMem) - (totMemAfter-freeMemAfter)) + 
                                        " bytes" );
                            } else {
                                owner.exportError( "Out of memory while exporting " + 
                                        fname + 
                                        "\nTry closing some windows or increasing heap size");
                            }
                        }
                        triesLeft--;
                    }
                } catch ( Throwable t ) {
                    
                    owner.exportError( t.getMessage() );
                    t.printStackTrace();
                }
            }
            owner.exportDone( this );
        }
    };
    
    /**
     Returns a proper filename in a numbered sequence. Examples:
     <table>
     <tr>
     <td>pattern</td>
     <td>example file names</td>
     </tr>
     <tr>
     <td>photo.jpg</td>
     <td>photo1.jpg, photo2.jpg</td>
     </tr>
     <tr>
     <td>photo_$n.jpg</td>
     <td>photo_1.jpg, photo_2.jpg, ..., photo_10000.jpg, ...</td>
     </tr>
     <tr>
     <td>photo_$4n.jpg</td>
     <td>photo_0001.jpg, photo_0002.jpg, ..., photo_10000.jpg, ...</td>
     </tr>
     </table>
     */
    
    String getSequenceFnameFormat( String seqBase ) {
        seqBase = seqBase.replaceAll( "%", "%%" );
        StringBuffer formatStrBuf = new StringBuffer( seqBase );
        Pattern seqNumPattern = Pattern.compile( "\\$(\\d*)n");
        Matcher m = seqNumPattern.matcher( seqBase );
        if ( m.find() ) {
            int start = m.start();
            int end = m.end();
            
            //Check padding
            String padStr = m.group( 1 );
            /*
             Check for case in which padStr contains only zeros since format()
             throws exception for format string like %00d.
             */
            if ( padStr.matches( "^0*$") ) {
                padStr = "1";
            }
            if ( padStr.length() > 0 ) {
                padStr = "0" + padStr;
            }
            String seqNumFormat = "%1$" + padStr + "d";
            formatStrBuf.replace( start, end, seqNumFormat );
        } else {
            // No format template found, add number just before extension
            Pattern extPattern = Pattern.compile( "\\.[^\\.]+$" );
            int seqNumPos = seqBase.length();
            Matcher extMatcher = extPattern.matcher( seqBase );
            if ( extMatcher.find() ) {
                seqNumPos = extMatcher.start();
            }
            formatStrBuf.insert( seqNumPos, "%d" );
        }
        return formatStrBuf.toString();
    }
    
    /**
     This method is called by Exporter before starting to export a new photo
     @param exporter the exporter calling
     @param fname Name of the file to be created
     @param percent Percentage of export operation completed, 0..100
     */
    private void exportingPhoto(ExportThread exporter, String fname, int percent) {
        StringBuffer msgBuf = new StringBuffer( "Exporting " );
        msgBuf.append( fname ).append( " - " ).append( percent ).append( " %" );
        String msg = msgBuf.toString();
        fireStatusChangeEvent( msg );
    }
    
    /**
     This method is called by exporter thread after it has finished all the export
     operation. Clear the status messages & enable this action so that it can
     perform new export operations.
     */
    private void exportDone(ExportThread exporter) {
        fireStatusChangeEvent( "" );
        setEnabled( true );
    }
    
    /**
     This method is called if there happens an error in the exporting thread.
     It shows the given error message in error dialog.
     @param msg Error message that is displayed to user
     */
    private void exportError( final String msg ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog( view.getRootPane(),
                        msg,
                        "Error exporting images",
                        JOptionPane.ERROR_MESSAGE );
            }
        } );
    }
    
    /**
     List of @see StatusChangeListener interested in changes in this object
     */
    private Vector listeners = new Vector();
    
    public void addStatusChangeListener( StatusChangeListener l ) {
        synchronized( listeners ) {
            listeners.add( l );
        }
    }
    
    public void removeStatusChangeListener( StatusChangeListener l ) {
        synchronized( listeners ) {
            listeners.remove( l );
        }
    }
    
    void fireStatusChangeEvent( String msg ) {
        StatusChangeEvent e = new StatusChangeEvent( this, msg );
        synchronized( listeners ) {
            Iterator iter = listeners.iterator();
            while ( iter.hasNext() ) {
                StatusChangeListener l = (StatusChangeListener) iter.next();
                l.statusChanged( e );
            }
        }
    }
    
    PhotoCollectionThumbView view;
}