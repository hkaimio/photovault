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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
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
import org.photovault.imginfo.xml.XmlExporter;
import org.photovault.imginfo.xml.XmlImporter;
import org.photovault.swingui.export.ExportDlg;

/**
 Import metadata from XML file to database
 */
class ImportXMLAction extends AbstractAction {
    
    /**
     Constructor.
     */
    public ImportXMLAction( String text, ImageIcon icon,
            String desc, int mnemonic) {
        super( text, icon );
        putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, new Integer( mnemonic) );
        putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_S, ActionEvent.CTRL_MASK ) );
    }
        
    public void actionPerformed( ActionEvent ev ) {    
        File importFile = null;
        JFileChooser importDlg = new JFileChooser();
        int retval = importDlg.showDialog( null, "Export database to XML" );
        if ( retval == JFileChooser.APPROVE_OPTION ) {
            File f = importDlg.getSelectedFile();
            try {
                BufferedReader reader = new BufferedReader( new FileReader( f ) );
                XmlImporter importer = new XmlImporter( reader );
                importer.importData();
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}