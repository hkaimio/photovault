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

package org.photovault.swingui;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.swingui.export.ExportDlg;

/**
   This action class implements exporting of all the selected images from a certain thumbnail
   view.
*/
class ExportSelectedAction extends AbstractAction implements SelectionChangeListener {

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
	putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_S, ActionEvent.CTRL_MASK ) );
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
                Iterator iter = selection.iterator();
                if ( selection.size() > 1 ) {
                    String format = getSequenceFnameFormat( exportFileTmpl );
                    int n = 1;
                    while ( iter.hasNext() ) {
                        String fname = String.format( format, new Integer( n ) );
                        File f = new File( fname );
                        PhotoInfo photo = (PhotoInfo) iter.next();
                        photo.exportPhoto( f, exportWidth, exportHeight );
                        n++;
                    }
                } else {
                    if ( iter.hasNext() ) {
                        PhotoInfo photo = (PhotoInfo) iter.next();
                        photo.exportPhoto( new File( exportFileTmpl ), 
                                exportWidth, exportHeight );
                    }
                }
            }
            c.setCursor( oldCursor );
        }
    }

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
    
    PhotoCollectionThumbView view;
}