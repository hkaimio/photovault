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

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.photovault.imginfo.VolumeManager;

/**
 * Background task for querying subfolders of given folder from database
 * @author Harri Kaimio
 * @since 0.6.0
 */
class QuerySubfoldersTask extends SwingWorker<List<DefaultMutableTreeNode>, Void> {

    private static String queryStr =
            "select distinct dir_name from pv_image_locations where volume_id = :vol order by dir_name";
    private final Session session;
    private final DefaultMutableTreeNode volumeRoot;
    private final VolumeTreeController ctrl;
    private final UUID volId;
    private final DefaultTreeModel treeModel;
    
    public QuerySubfoldersTask( Session s, VolumeTreeController ctrl, DefaultMutableTreeNode volRoot ) {
        session = s;
        this.volumeRoot = volRoot;
        this.ctrl = ctrl;
        VolumeTreeNode volData = (VolumeTreeNode) volumeRoot.getUserObject();
        volId = volData.volId;
        treeModel = (DefaultTreeModel) ctrl.getView().getModel();
    }


    @Override
    protected List<DefaultMutableTreeNode> doInBackground() throws Exception {
        List<String> dirs = null;
        File volumeBase = VolumeManager.instance().getVolumeMountPoint( volId );
        if ( volumeBase != null ) {
            dirs = getSubdirsFromFs( volumeBase );
        } else {
            dirs = getSubdirsFromDb();
        }
        List<DefaultMutableTreeNode> childBranches = new ArrayList();
        Deque<DefaultMutableTreeNode> path = new ArrayDeque();
        DefaultMutableTreeNode parent = null;
        String parentPath = "";
        for ( Object dir : dirs ) {
            String dirPath = (String) dir;
            String name = dirPath;
            int nameStart = dirPath.lastIndexOf( "/" ) + 1;
            if ( nameStart >= 0 ) {
                name = dirPath.substring( nameStart );
            }
            VolumeTreeNode userObject =
                    new VolumeTreeNode( volId, name, dirPath );
            userObject.state = NodeState.READY;
            // Find the parent directory
            
            
            parentPath = parent != null ? 
                    ((VolumeTreeNode)parent.getUserObject()).path  : "";
            while ( !dirPath.startsWith( parentPath ) ) {
                if ( path.isEmpty() ) {
                    parent = null;
                    break;
                } else {
                    parent = path.removeFirst();
                    parentPath = parent != null ? ((VolumeTreeNode) parent.
                            getUserObject()).path : "";
                }
            }
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode( userObject );
            path.addFirst( newNode );
            if ( parent == null ) {
                childBranches.add( newNode );
            } else {
                parent.add( newNode );
            }
            parent = newNode;
            parentPath = parent != null
                    ? ((VolumeTreeNode) parent.getUserObject()).path : "";
        }
        return childBranches;
    }

    private List<String> getSubdirsFromDb() {
        Query q = session.createSQLQuery( queryStr )
                        .setString( "vol", volId.toString() );
        List dirs = q.list();
        return dirs;
    }
    
    private List<String> getSubdirsFromFs( File baseDir ) {
        List<String> dirs = new ArrayList();
        appendSubdirs( baseDir, null, dirs );
        Collections.sort( dirs );
        return dirs;
    }
    
    private void appendSubdirs( File dir, String basePath, List<String> dirs ) {
        for ( File f: dir.listFiles() ) {
            if ( f.isDirectory() && !".photovault_volume".equals( f.getName() ) ) {
                String path = basePath == null ? 
                        f.getName() :
                        basePath + "/" + f.getName();
                dirs.add( path );
                appendSubdirs( f, path, dirs );
            }
        }
    }
    
    @Override
    protected void done() {
        try {
            List<DefaultMutableTreeNode> res = get();
            for ( DefaultMutableTreeNode branch : res ) {
                treeModel.insertNodeInto( branch, volumeRoot, volumeRoot.getChildCount() );
            }
            VolumeTreeNode userData = (VolumeTreeNode) volumeRoot.getUserObject();
            userData.state = NodeState.READY;
            // model.volumeInitialized( volumeRoot );
        } catch ( InterruptedException ex ) {
            Logger.getLogger( QuerySubfoldersTask.class.getName() ).
                    log( Level.SEVERE, null, ex );
        } catch ( ExecutionException ex ) {
            Logger.getLogger( QuerySubfoldersTask.class.getName() ).
                    log( Level.SEVERE, null, ex );
        }
    }

}
