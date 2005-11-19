package photovault.swingui;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import imginfo.*;
import java.io.*;
import photovault.common.PVDatabase;
import photovault.common.PhotovaultSettings;

/**
 * LoginDlg implementfs a simåple dialog for asking which database will be used 
 * as well as username & password for database connection. <p>
 *
 * LoginDlg works tightly together with the @see Photovault main allpication class.
 */
public class LoginDlg extends JFrame {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfoDlg.class.getName() );

    static final String DIALOG_TITLE = "Login";
    Photovault app;
    /**
       Constructor. 
       @param app Owner of the dialog
    */
    public LoginDlg( Photovault app ) {
	super( "Login to Photovault");
	this.app = app;
	SwingUtilities.invokeLater(new java.lang.Runnable() {
		public void run() {
		    createUI();
		}
	    });
    }

    /**
     * Returns the user name that has been entered to the dialog.
     */
    public String getUsername() {
	return idField.getText();
    }

    /**
     * Returns the password entered to the dialog.
     */
    public String getPassword() {
	char[] pass = passField.getPassword();
	return new String( pass );
    }

    /**
     * Return the name of selected database.
     */
    public String getDb() {
	return dbField.getSelectedItem().toString();
    }
    
    boolean okPressed = false;
    
    /**
       Creates the UI components needed for this dialog.
    */
    protected void createUI() {

        GridBagConstraints labelConstraints = new GridBagConstraints();
	labelConstraints.insets = new Insets( 2, 20, 2, 2 );
	labelConstraints.anchor = GridBagConstraints.EAST;
	labelConstraints.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
	labelConstraints.fill = GridBagConstraints.NONE;      //reset to default
	labelConstraints.weightx = 0.0;                       //reset to default
	
        GridBagConstraints fieldConstraints = new GridBagConstraints();
	fieldConstraints.insets = new Insets( 2, 2, 2, 20 );
	fieldConstraints.anchor = GridBagConstraints.WEST;
	fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;     //end row
	fieldConstraints.weightx = 1.0;
	
	JPanel loginPane = new JPanel();
	GridBagLayout gb = new GridBagLayout();
	loginPane.setLayout( gb );

	JLabel idLabel = new JLabel( "Username" );
	labelConstraints.insets = new Insets( 20, 20, 4, 4 );
	gb.setConstraints( idLabel, labelConstraints );
	loginPane.add( idLabel );
	idField = new JTextField( 15 );
	fieldConstraints.insets = new Insets( 20, 4, 4, 20 );
	gb.setConstraints( idField, fieldConstraints );
	loginPane.add( idField );

	JLabel passLabel = new JLabel( "Password" );
	labelConstraints.insets = new Insets( 4, 20, 4, 4 );
	gb.setConstraints( passLabel, labelConstraints );
	loginPane.add( passLabel );
	passField = new JPasswordField( 15 );
	fieldConstraints.insets = new Insets( 4, 4, 4, 20 );
	gb.setConstraints( passField, fieldConstraints );
	loginPane.add( passField );

        Collection databases = PhotovaultSettings.getDatabases();
        Vector dbNames = new Vector();
        Iterator iter = databases.iterator();
        while ( iter.hasNext() ) {
            PVDatabase db = (PVDatabase) iter.next();
            dbNames.add( db.getName() );
        }
        Object[] dbs = dbNames.toArray();
	JLabel dbLabel = new JLabel( "Database" );
	gb.setConstraints( dbLabel, labelConstraints );
	loginPane.add( dbLabel );
	dbField = new JComboBox( dbs );
	gb.setConstraints( dbField, fieldConstraints );
	loginPane.add( dbField );

 	getContentPane().add( loginPane, BorderLayout.NORTH );

	// Create a pane for the buttols
	JButton okBtn = new JButton( "OK" );
	final LoginDlg dlg = this; 
	okBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
// 		    setVisible( false );
		    app.login( dlg );
		}
	    } );
		    
	JButton cancelBtn = new JButton( "Cancel" );
	cancelBtn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    System.exit( 0 );
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

	setResizable( false );
	pack();
        
        // Center the ialog on screen
        int w = getSize().width;
        int h = getSize().height;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width-w)/2;
        int y = (screenSize.height-h)/2;
        setLocation( x, y );
                
    }

    

    JTextField idField;
    JPasswordField passField;
    JComboBox dbField;
}