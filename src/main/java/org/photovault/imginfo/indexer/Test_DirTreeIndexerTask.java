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
import java.io.IOException;
import org.hibernate.classic.Session;
import org.hibernate.context.ManagedSessionContext;
import org.photovault.command.CommandException;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.folder.CreatePhotoFolderCommand;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.FileUtils;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author harri
 */
public class Test_DirTreeIndexerTask extends PhotovaultTestCase {
    private Session session;
    DAOFactory daoFactory = DAOFactory.instance( HibernateDAOFactory.class );;
    
    File topDir;
    PhotoFolder topFolder;
    ExternalVolume vol;
    
    PhotovaultCommandHandler cmdHandler;
    
    @BeforeClass
    public void setUpEnv() throws IOException, CommandException {
        topDir = File.createTempFile( "pv_dir_indexer_test_", "" );
        topDir.delete();
        topDir.mkdir();
        
        session = HibernateUtil.getSessionFactory().openSession();
        ManagedSessionContext.bind( (org.hibernate.classic.Session) session );
        ((HibernateDAOFactory) daoFactory).setSession( session );
        cmdHandler = new PhotovaultCommandHandler( session );
        
        vol = new ExternalVolume( topDir.getName(), topDir.getAbsolutePath() );
        session.save( vol );
        
        CreatePhotoFolderCommand cmd = 
                new CreatePhotoFolderCommand(null, topDir.getName(), "" );
        cmdHandler.executeCommand( cmd );
        topFolder = cmd.getCreatedFolder();
        
    }
    
    @AfterClass    
    @Override
    public void tearDown() {
        FileUtils.deleteTree( topDir );
        session.close();
    }

    @Test
    public void testDirTreeSync() {
        File d1 = new File( topDir, "dir1" );
        d1.mkdir();
        for ( int n = 1; n < 20 ; n++ ) {
            File d = new File( d1, "subdir" + n );
            d.mkdir();
        }
        DirTreeIndexerTask indexer = new DirTreeIndexerTask(topDir, topFolder, vol);
        indexer.setSession(session);
        indexer.setCommandHandler(cmdHandler);
        indexer.run();
        
        assertEquals( 20, indexer.getFolderCount() );
        assertEquals( 20, indexer.getNewFolderCount() );
        assertEquals( 0, indexer.getDeletedFolderCount() );
        
        session.clear();
        PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
        topFolder = folderDAO.findById( topFolder.getFolderId(), false );
        assertEquals( 1, topFolder.getSubfolders().size() );
        PhotoFolder subfolder = topFolder.getSubfolders().iterator().next();
        assertEquals( 19, subfolder.getSubfolders().size() );
    }
    
    @Test(dependsOnMethods={"testDirTreeSync"})
    public void testDirModification() {
        File deldir = new File( topDir, "dir1/subdir1" );
        deldir.delete();
        File newdir = new File( topDir, "newdir" );
        newdir.mkdir();

        DirTreeIndexerTask indexer = new DirTreeIndexerTask(topDir, topFolder, vol);
        indexer.setSession(session);
        indexer.setCommandHandler(cmdHandler);
        indexer.run();
        
        assertEquals( 20, indexer.getFolderCount() );
        assertEquals( 1, indexer.getNewFolderCount() );
        assertEquals( 1, indexer.getDeletedFolderCount() );
        
        session.clear();
        PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
        topFolder = folderDAO.findById( topFolder.getFolderId(), false );
        assertEquals( 2, topFolder.getSubfolders().size() );
        PhotoFolder subfolder = topFolder.getSubfolders().iterator().next();
        assertEquals( 18, subfolder.getSubfolders().size() );
        
    }
}
