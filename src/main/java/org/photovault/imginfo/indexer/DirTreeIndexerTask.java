/*
  Copyright (c) 2007 Harri Kaimio
 
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

package org.photovault.imginfo.indexer;

import java.io.File;
import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.taskscheduler.BackgroundTask;

/**
 This background task replicates a file system directory tree into Photovault 
 {@link PhotoFolder} hierarchy or changes an existing fodler hierarchy to 
 match a directory tree. It is used as the first step of indexing an
 external volume.
 */
public class DirTreeIndexerTask extends BackgroundTask {
    private static Log log = LogFactory.getLog( DirTreeIndexerTask.class.getName() );
    
    private File topDir;
    private ExternalVolume vol;
    private int folderCount = 0;
    private int newFolderCount = 0;
    private int deletedFolderCount = 0;
    private boolean createDirIndexers;
    private LinkedList<DirectoryIndexer> dirIndexers = 
            new LinkedList<DirectoryIndexer>();

    /**
     Constructs a new tree indexer
     @param topDir The top directory of the tree to index
     @param topFolder The folder corresponding to topDir
     */
    public DirTreeIndexerTask( File topDir,
            ExternalVolume vol, boolean createDirIndexers ) {
        this.topDir = topDir;
        this.vol = vol;
        this.createDirIndexers = createDirIndexers;
    }
    
    /**
     Constructs a new tree indexer
     @param topDir The top directory of the tree to index
     @param topFolder The folder corresponding to topDir
     */    
    public DirTreeIndexerTask( File topDir, ExternalVolume vol ) {
        this( topDir, vol, false );
    }
    /**
     Run the task
     */
    @Override
    public void run() {
        indexDirectory( topDir );
        
    }

    /**
     Synchronize a subtree to given folder hierarchy.
     @param dir The top directory
     @param folder the corresponding top folder
     */
    private void indexDirectory( File dir ) {

        if ( createDirIndexers ) {
            dirIndexers.add( new DirectoryIndexer( dir, vol ) );
        }

        File[] dirEntries = dir.listFiles();
        for ( File d : dirEntries ) {
            if ( d.isDirectory() && !d.getName().equals( ".photovault_volume" ) ) {
                folderCount++;
                indexDirectory( d );
            }
        }

    }

    /**
     Get the total number of directories checked
     @return Number of checked directories
     */
    public int getFolderCount() {
        return folderCount;
    }

    /**
     Get the number of new folders created
     
     @return Number of new folders created
     */
    public int getNewFolderCount() {
        return newFolderCount;
    }

    /**
     Get the number of folders deletes as he corresponding directory was not 
     found
     @return NUmber of deleted folders
     */
    public int getDeletedFolderCount() {
        return deletedFolderCount;
    }

    /**
     Get the top directory where the indexing starts
     @return Top directory
     */
    public File getTopDir() {
        return topDir;
    }

    /**
     Get the external volume we are indexing
     @return volume
     */
    public ExternalVolume getVol() {
        return vol;
    }

    /**
     Get the directory indexers for all found directoiries
     @return
     */
    public LinkedList<DirectoryIndexer> getDirIndexers() {
        return dirIndexers;
    }
}
