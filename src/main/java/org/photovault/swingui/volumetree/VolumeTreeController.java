/*
  Copyright (c) 2011 Harri Kaimio
  
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

package org.photovault.swingui.volumetree;

import java.util.List;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.swingui.PhotoFolderTreeEvent;
import org.photovault.swingui.framework.AbstractController;
import org.photovault.swingui.framework.PersistenceController;

/**
 * Controller for the volume tree pane in main user interface.
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class VolumeTreeController extends PersistenceController 
implements TreeWillExpandListener, TreeSelectionListener {

    private static final Log log = LogFactory.getLog( VolumeTreeController.class );
    private final DefaultTreeModel model;
    private final JTree view;
    private final DefaultMutableTreeNode rootNode; 

    public VolumeTreeController( AbstractController parent ) {
        super( parent );
        rootNode = new DefaultMutableTreeNode( );
        model = new DefaultTreeModel( rootNode );
        view = new JTree( model );
        view.setShowsRootHandles( true );
        view.setRootVisible( true );
        view.addTreeWillExpandListener( this );
        view.addTreeSelectionListener( this );
        SwingUtilities.invokeLater( new Runnable() {

            public void run() {
                initVolumes();
            }
        } );
    }

    public JTree getView() {
        return view;
    }

    void addVolume( VolumeTreeNode node ) {
        DefaultMutableTreeNode tn = new DefaultMutableTreeNode( node );
        DefaultMutableTreeNode loadingNode = new DefaultMutableTreeNode( "loading" );
        tn.add( loadingNode );
        rootNode.add( tn );
    }

    private void initVolumes() {
        QueryVolumesTask task = new QueryVolumesTask( getDAOFactory().getVolumeDAO(), this );
        task.execute();
    }

    public void treeWillExpand( TreeExpansionEvent event ) throws ExpandVetoException {
        DefaultMutableTreeNode expandingNode = 
                (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
        Object o = expandingNode.getUserObject();
        if ( o instanceof VolumeTreeNode ) {
            VolumeTreeNode node = (VolumeTreeNode) o;
            if ( node.path.equals(  "" ) && node.state == NodeState.UNINITIALIZED ) {
                node.state = NodeState.INITIALIZING;
                log.debug( "expand volume  " + node.volId );
                QuerySubfoldersTask fillTreeTask = new QuerySubfoldersTask( 
                        ((HibernateDAOFactory)getDAOFactory()).getSession(),
                        this, expandingNode );
                fillTreeTask.execute();
            }
        }
        
    }

    public void treeWillCollapse( TreeExpansionEvent event ) throws ExpandVetoException {
    }

    public void valueChanged( TreeSelectionEvent e ) {
        DefaultMutableTreeNode node = 
                (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
        VolumeTreeNode dir = (VolumeTreeNode) node.getUserObject();
        
        // Test
        ExtDirPhotos photoQuery = new ExtDirPhotos( dir.volId, dir.path );
        fireEvent( new PhotoFolderTreeEvent( this, photoQuery ) );
        List<PhotoInfo> photos = photoQuery.queryPhotos( getPersistenceContext() );
        log.debug( "Directory " + dir.volId + ":" + dir.path + " selected. " + photos.size() + " photos." );
    }
    
}
