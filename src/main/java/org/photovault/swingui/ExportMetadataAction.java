/*
  Copyright (c) 200 Harri Kaimio
 
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


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.TreeSet;
import java.util.Vector;
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
import org.photovault.imginfo.xml.XmlExportListener;
import org.photovault.imginfo.xml.XmlExporter;
import org.photovault.swingui.export.ExportDlg;

/**
 */
class ExportMetadataAction extends AbstractAction implements XmlExportListener {
    
    /**
     Constructor.
     */
    public ExportMetadataAction( String text, ImageIcon icon,
            String desc, int mnemonic) {
        super( text, icon );
        putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, new Integer( mnemonic) );
        putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_S, ActionEvent.CTRL_MASK ) );
    }
        
    public void actionPerformed( ActionEvent ev ) {    
        File exportFile = null;
        JFileChooser saveDlg = new JFileChooser();
        int retval = saveDlg.showDialog( null, "Export database to XML" );
        if ( retval == JFileChooser.APPROVE_OPTION ) {
            final File f = saveDlg.getSelectedFile();
            progressDlg = new ProgressDlg( null, true );
            progressDlg.setTitle( "Exporting..." );
            progressDlg.setStatus( "Gathering photos...");
            progressDlg.setProgressPercent( 0 );
            final ExportMetadataAction tthis = this;
            Thread importThread = new Thread() {
                public void run() {
                    try {
                        BufferedWriter writer = new BufferedWriter( new FileWriter( f ) );
                        XmlExporter exporter = new XmlExporter( writer );
                        exporter.addListener( tthis );
                        exporter.write();
                        writer.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    
                }
            };
            
            importThread.start();
            progressDlg.setVisible( true );
        }
    }

    ProgressDlg progressDlg;
    
    public void xmlExportStatus(XmlExporter exporter, int status) {
        switch ( status ) {
            case XmlExporter.EXPORTER_STATE_EXPORTING_FOLDERS:
                progressDlg.setStatus( "Exporting folder hierarchy..." );
                break;
            case XmlExporter.EXPORTER_STATE_EXPORTING_PHOTOS:
                progressDlg.setStatus( "Exporting photos..." );
                break;
            case XmlExporter.EXPORTER_STATE_COMPLETED:
                progressDlg.completed();
                break;
            default:
                JOptionPane.showMessageDialog( null, 
                        "Invalid state for exporter: " + status, 
                        "Exporting error", JOptionPane.ERROR_MESSAGE );
        }
    }

    public void xmlExportError(XmlExporter exporter, String message) {
                JOptionPane.showMessageDialog( null, 
                        message, 
                        "Exporting error", JOptionPane.ERROR_MESSAGE );
    }

    public void xmlExportObjectExported(XmlExporter exporter, Object obj) {
        int totalPhotoCount = exporter.getTotalPhotoCount();
        int exportedPhotoCount = exporter.getExportedPhotoCount();
        if ( totalPhotoCount > 0 ) {
            int percentage = (exportedPhotoCount * 100 ) / totalPhotoCount;
            progressDlg.setProgressPercent( percentage );
        }
    }
}