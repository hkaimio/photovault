package photovault.swingui;
// $Id

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import imginfo.*;
import java.io.*;


/**
   PhotoInfoDlg is a simple wrapper that wraps a PhotoInfoeditor to a diaplog frame.
*/

public class PhotoInfoDlg extends JDialog {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfoDlg.class.getName() );

    /**
       Constructor. Creates a PhotoInfoDlg with the specified window as a parent.
       @param owner Owner of the dialog
       @param modal If true, a modal dialog is created
    */
    public PhotoInfoDlg( Frame owner, boolean modal, PhotoInfo photo ) {
	super( owner, modal );
	ctrl = new PhotoInfoController();
	ctrl.setPhoto( photo );
	    
	createUI();
    }

    /**
       Creates the UI components needed for this dialog.
    */
    protected void createUI() {
	editor = new PhotoInfoEditor( ctrl );
	getContentPane().add( editor, BorderLayout.NORTH );

	// Create a pane for the buttols
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
	JButton cancelBtn = new JButton( "Cancel" );
	cancelBtn.addActionListener( new ActionListener() {
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
	buttonPane.add(cancelBtn);
	getContentPane().add( buttonPane, BorderLayout.SOUTH );

	getRootPane().setDefaultButton( okBtn );

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

