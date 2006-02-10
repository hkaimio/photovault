package photovault.swingui;
// $Id

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.photovault.imginfo.*;
import java.io.*;
import photovault.folder.*;

/**
   This is a simple dialog for selecting a folder.
*/

public class PhotoFolderSelectionDlg extends JDialog {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoFolderSelectionDlg.class.getName() );

    static final String DIALOG_TITLE = "Select folder";
    
    /**
       Constructor. Creates a dialog with the specified window as a parent.
       @param owner Owner of the dialog
       @param modal If true, a modal dialog is created
    */
    public PhotoFolderSelectionDlg( Frame owner, boolean modal ) {
	super( owner, DIALOG_TITLE, modal );
	    
	createUI();
    }

    PhotoFolderTree tree = null;

    PhotoFolder selectedFolder = null;

    /**
       Return the selected folder or null if no folder is selected
    */
    public PhotoFolder getSelectedFolder() {
	return selectedFolder;
    }
	
    
    /**
       Creates the UI components needed for this dialog.
    */
    protected void createUI() {
	tree = new PhotoFolderTree();
	getContentPane().add( tree, BorderLayout.NORTH );

	// Create a pane for the buttols
	JButton okBtn = new JButton( "OK" );
	okBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    try {
			selectedFolder = tree.getSelected();
		    } catch ( Exception ex ) {
			log.warn( "problem while saving changes: " + ex.getMessage() );
		    }
		    setVisible( false );
		}
	    } );
		    

	JButton cancelBtn = new JButton( "Cancel" );
	cancelBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    selectedFolder = null;
		    setVisible( false );
		}
	    } );
	    
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add(okBtn);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	buttonPane.add(cancelBtn);
	getContentPane().add( buttonPane, BorderLayout.SOUTH );

	getRootPane().setDefaultButton( okBtn );

	pack();
    }

    /**
       Shows the dialog.
       @return True if user selected folder, false otherwise.
    */
	
    public boolean showDialog() {
	setVisible( true );
	return (selectedFolder != null);
    }

}    
