// $Id: TestPhotoFolderTreeModel.java,v 1.1 2003/02/22 13:10:55 kaimio Exp $

package photovault.swingui;

import photovault.folder.*;
import junit.framework.*;
import org.apache.ojb.odmg.*;
import org.odmg.*;	

public class TestPhotoFolderTreeModel extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( TestPhotoFolderTreeModel.class.getName() );

    Implementation odmg = null;
    Database db = null;
    PhotoFolder rootFolder = null;
    Transaction tx = null;
    PhotoFolderTreeModel model = null;
    
    public void setUp() {
	odmg = OJB.getInstance();
	db = odmg.newDatabase();
	try {
	    db.open( "repository.xml", Database.OPEN_READ_WRITE );
	} catch ( ODMGException e ) {
	    //	    log.warn( "Could not open database: " + e.getMessage() );
	    db = null;
	}

	tx = odmg.newTransaction();
	tx.begin();
	DList folders = null;
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName()  + " where name = \"subfolderTest\"" );
	    folders = (DList) query.execute();
	    tx.commit();
	} catch ( Exception e ) {
	    tx.abort();
	    fail( e.getMessage() );
	}
	rootFolder = (PhotoFolder) folders.get(0);
	model = new PhotoFolderTreeModel();
	model.setRoot( rootFolder );
    }

    public void tearDown() {
    }

    public static Test suite() {
	return new TestSuite( TestPhotoFolderTreeModel.class );
    }

    public static void main( String[] args ) {
	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }

    public void testChildRetrieval() {
	assertEquals( "Number of children don't match", 4, model.getChildCount( rootFolder ) );
	assertEquals( "Subfolder name does not match", "Subfolder2", model.getChild( rootFolder, 1 ).toString() );
    }

}
