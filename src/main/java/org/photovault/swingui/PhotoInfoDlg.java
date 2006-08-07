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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.photovault.imginfo.*;
import java.io.*;
import org.photovault.imginfo.PhotoInfo;


/**
   PhotoInfoDlg is a simple wrapper that wraps a PhotoInfoeditor to a diaplog frame.
*/

public class PhotoInfoDlg extends JDialog {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfoDlg.class.getName() );

    static final String DIALOG_TITLE = "Edit photo info";
    /**
       Constructor. Creates a PhotoInfoDlg with the specified window as a parent.
       @param owner Owner of the dialog
       @param modal If true, a modal dialog is created
    */
    public PhotoInfoDlg( Frame owner, boolean modal, PhotoInfo photo ) {
	super( owner, DIALOG_TITLE, modal );
	ctrl = new PhotoInfoController();
	    
	createUI();
	ctrl.setPhoto( photo );
    }

    public PhotoInfoDlg( Frame owner, boolean modal, PhotoInfo[] photos ) {
	super( owner, DIALOG_TITLE, modal );
	ctrl = new PhotoInfoController();
	    
	createUI();
	ctrl.setPhotos( photos );
    }


    /**
       Creates the UI components needed for this dialog.
    */
    protected void createUI() {
	editor = new PhotoInfoEditor( ctrl );
	getContentPane().add( editor, BorderLayout.NORTH );
		    
	JButton okBtn = new JButton( "OK" );
	okBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    try {
			ctrl.save();
			photoChanged = true;
		    } catch ( Exception ex ) {
			log.warn( "problem while saving changes: " + ex.getMessage() );
		    }
                    setVisible( false );
		}
	    } );

            JButton applyBtn = new JButton( "Apply" );
	applyBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    try {
			ctrl.save();
			photoChanged = true;
		    } catch ( Exception ex ) {
			log.warn( "problem while saving changes: " + ex.getMessage() );
		    }
		}
	    } );
            
        JButton discardBtn = new JButton( "Discard" );
	discardBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    ctrl.discard();
		}
	    } );

        JButton closeBtn = new JButton( "Close" );
	closeBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    ctrl.discard();
		    setVisible( false );
		}
	    } );
	    
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add(okBtn);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	buttonPane.add(applyBtn);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	buttonPane.add(discardBtn);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	buttonPane.add(closeBtn);
	getContentPane().add( buttonPane, BorderLayout.SOUTH );

	getRootPane().setDefaultButton( applyBtn );

	pack();
    }

    /**
       Shows the dialog.
       @return Ture if the dialog modified the photo data, false otherwise.
    */
	
    public boolean showDialog() {
	photoChanged = false;
	setVisible( true );
	return photoChanged;
    }

    public void setPhoto( PhotoInfo photo ) {
	ctrl.setPhoto( photo );
    }

    public void setPhotos( PhotoInfo[] photos ) {
	ctrl.setPhotos( photos );
    }

  /**
       Simple main program to aid running & testing this dialog without invoking application
    */
    public static void main( String[] args ) {
	try {
	    org.apache.log4j.lf5.DefaultLF5Configurator.configure();
	} catch ( Exception e ) {}
	log.info( "Starting application" );

	// Parse the arguments
	PhotoInfo photo = null;
	log.debug( "Number of args" + args.length );
	log.debug( args.toString() );
	if ( args.length == 2 ) {
	    if ( args[0].equals( "-f" ) ) {
		File f = new File( args[1] );
		try {
		    log.debug( "Getting file " + f.getPath() );
		    photo = PhotoInfo.addToDB( f );
		} catch ( Exception e ) {
		    log.warn( e.getMessage() );
		}
	    } else if ( args[0].equals( "-id" ) ) {
		try {
		    int id = Integer.parseInt( args[1] );
		    log.debug( "Getting photo " + id );
		    photo = PhotoInfo.retrievePhotoInfo( id );
		} catch ( Exception e ) {
		    log.warn( e.getMessage() );
		}
	    }
	}
	JFrame f = new JFrame("PhotoInfoDlg test");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                 System.exit(0);
            }
	    });
	JButton button = new JButton("Edit photo");	
	final PhotoInfoDlg dlg = new PhotoInfoDlg( f, true, photo );
	button.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    log.info( "Showing the dialog" );
		    boolean modified = dlg.showDialog();
		    log.info( "back from showDialog()" );
		}
	    });

	
        JPanel contentPane = new JPanel();
        f.setContentPane(contentPane);
	contentPane.add( button);
	f.pack();
	f.setVisible( true );
    }
	
	
    

    PhotoInfoEditor editor = null;
    PhotoInfoController ctrl = null;
    /**
       Indicates whether the photo inforamtion was changed (by pressing OK or Apply)
    */
    boolean photoChanged = false;
    public boolean isPhotoChanged() {
	return photoChanged;
    }

}

