// $Id: Test_PhotoFolderTreeModel.java,v 1.2 2003/02/23 21:43:42 kaimio Exp $

package photovault.swingui;

import photovault.folder.*;
import junit.framework.*;
import javax.swing.event.*;
import org.apache.ojb.odmg.*;
import org.odmg.*;
import photovault.test.PhotovaultTestCase;
import dbhelper.ODMG;

public class Test_PhotoFolderTreeModel extends PhotovaultTestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_PhotoFolderTreeModel.class.getName() );

    Implementation odmg = null;
    //Database db = null;
    PhotoFolder rootFolder = null;
    Transaction tx = null;
    PhotoFolderTreeModel model = null;
    
    /**
     Sets upt the model for the cases so that "subfolderTest" folder is set up 
     as the root folder.
     */
    public void setUp() {
	odmg = ODMG.getODMGImplementation();

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
	return new TestSuite( Test_PhotoFolderTreeModel.class );
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

    /**
       Test listener for treeModelChanges
    */

    class TestTreeModelListener implements TreeModelListener {
	// implementation of javax.swing.event.TreeModelListener interface

	public boolean nodesChanged = false;
	public boolean nodesInserted = false;
	public boolean nodesRemoved = false;
	public boolean structChanged = false;

	Object[] path = null;;
	PhotoFolder source = null;
	
	/**
	 *
	 * @param param1 <description>
	 */
	public void treeNodesChanged(TreeModelEvent e)
	{
	    nodesChanged = true;
	    source = (PhotoFolder) e.getSource();
	    path = e.getPath();
	}

	/**
	 *
	 * @param param1 <description>
	 */
	public void treeNodesInserted(TreeModelEvent e)
	{
	    nodesInserted = true;
	    source = (PhotoFolder) e.getSource();
	    path = e.getPath();
	}

	/**
	 *
	 * @param param1 <description>
	 */
	public void treeNodesRemoved(TreeModelEvent e)
	{
	    nodesRemoved = true;
	    source = (PhotoFolder)e.getSource();
	    path = e.getPath();
	}

	/**
	 *
	 * @param param1 <description>
	 */
	public void treeStructureChanged(TreeModelEvent e )
	{
	    structChanged = true;
	    source = (PhotoFolder)e.getSource();
	    path = e.getPath();
	}


    }

    /**
       Tests that listeners are notified when a folder is changed
    */
    public void testListener() {
	TestTreeModelListener l = new TestTreeModelListener();
	model.addTreeModelListener( l );

	PhotoFolder f = rootFolder.getSubfolder( 2 );
	f.setDescription( "treeModelListener test string" );
	assertTrue( "Listener was not notified of strucuture change", l.structChanged );
	model.removeTreeModelListener( l );
	l.structChanged = false;
	f.setDescription( "treeModelListener test string2" );
	assertFalse( "Listener was  notified of strucuture change after removal", l.structChanged );
    }

    /**
       Test modifications to the root node. This is a special case since TreePath cannot be empty...
    */
    public void testRootModifications() {
// 	model.setRoot( PhotoFolder.getRoot() );
      
      log.warn( "testRootModifications" );

 	model.setRoot( rootFolder );
	TestTreeModelListener l = new TestTreeModelListener();
	model.addTreeModelListener( l );
	rootFolder.setDescription( "Root description" );
	assertTrue( "Listener was not notified of strucuture change", l.structChanged );
	l.structChanged = false;

	PhotoFolder newFolder = PhotoFolder.create( "Test folder", rootFolder );
	assertTrue( "Listener was not notified of strucuture change", l.structChanged );
	l.structChanged = false;

	newFolder.delete();
	assertTrue( "Listener was not notified of strucuture change", l.structChanged );
	l.structChanged = false;
	
	log.warn( "finished testRootFolderModifications" );
	
    }
}
