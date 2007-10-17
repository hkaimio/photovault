/*
  Copyright (c) 2006-2007 Harri Kaimio
 
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.command.CommandException;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.ChangePhotoInfoCommand;
import org.photovault.imginfo.CopyImageDescriptor;
import org.photovault.imginfo.CreateCopyImageCommand;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.FileLocation;
import org.photovault.imginfo.ImageDescriptorBase;
import org.photovault.imginfo.ImageFile;
import org.photovault.imginfo.ImageFileDAO;
import org.photovault.imginfo.ModifyImageFileCommand;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.imginfo.Volume;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.taskscheduler.BackgroundTask;

/**
 Background task for indexing a single file. It checks whether the file matches 
 database state and updates database and possibly creates new ImageFile and 
 PhotoInfo instances so that the file is included in database.
 @author harri
 */
public class IndexFileTask extends BackgroundTask {
    static Log log = LogFactory.getLog( IndexFileTask.class.getName() );
    
    /**
     The file that will be indexed
     */
    private File f;
    
    /**
     Folder corresponding to the directory in which the file is. Created photos
     will be added to this folder.
     */
    private PhotoFolder folder;
    
    /**
     Volume of the file
     */
    private ExternalVolume volume;
    
    /**
     ImageFile that describes the file.
     */
    private ImageFile ifile;
    
    /**
     MD5 hash of the file
     */
    private byte[] hash = null;
    private ExtVolIndexerEvent currentEvent = new ExtVolIndexerEvent( this );
    
    /**
     Reference to the {@link DIrectoryIndexer} that created this instance. The 
     directory indexer is notified of the result of indexing operation.
     */
    private DirectoryIndexer dirIndexer;
    
    /**
     Creates a new IndexFileTask
     @param f The file that will be idnexed
     @param folder PhotoFolder corresponding to f's directory
     @param vol Volume of f.
     @param dir DirectoryIndexer that created this instance.
     */
    public IndexFileTask( File f, PhotoFolder folder, ExternalVolume vol, DirectoryIndexer dir ) {
        this.f = f;
        this.folder = folder;
        this.volume = vol;
        this.dirIndexer = dir;
    }
    
    /**
     Run the actual idnexing operation
     */
    public void run( ) {
        indexFile();
        if ( dirIndexer != null ) {
            dirIndexer.fileIndexerCompleted( this );
        }
    }
    
    /**
     Run the actual idnexing operation (called by run())
     */
    private void indexFile() {
        log.debug( "entry: indexFile " + f.getAbsolutePath() );
        currentEvent.setIndexedFile( f );
        
        DAOFactory daoFactory = DAOFactory.instance(HibernateDAOFactory.class );
        ImageFileDAO ifDAO = daoFactory.getImageFileDAO();
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        // Check if the instance already exists n database
        ifile = ifDAO.findFileInLocation( volume, volume.mapFileToVolumeRelativeName( f ) );
        if ( ifile != null ) {
            log.debug( "found existing file" );
            FileLocation fileLoc = null;
            for ( FileLocation loc : ifile.getLocations() ) {
                if ( loc.getFile().equals( f ) ) {
                    fileLoc = loc;
                    break;
                }
            }
            
            boolean matchesFile = true;
            if ( f.length() == ifile.getFileSize() ) {
                if ( f.lastModified() != fileLoc.getLastModified() ) {
                    hash = ImageFile.calcHash( f );
                    if ( !hash.equals( ifile.getHash()  ) ) {
                        matchesFile = false;
                    }
                }
            } else {
                matchesFile = false;
            }
            
            // There is an existing instance, check whether the data matches
            if ( matchesFile ) {
                // TODO: return the immge file
//                PhotoInfo photo = oldInstance.getPhoto();
                log.debug( "File is consistent with DB" );
                return;                
            } else {
                ModifyImageFileCommand deleteCmd = new ModifyImageFileCommand( ifile  );
                deleteCmd.removeLocation( fileLoc );
                log.debug( "File is not consistent with the one in DB, removing location" );
                try {
                    cmdHandler.executeCommand( deleteCmd );
                } catch (CommandException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        /*
         If we reach here, the file is new (or changed) after last indexing.
         Check first if it is a copy of an existing instance
         */
        if ( hash == null ) {
            hash = ImageFile.calcHash( f );
        }
        
        ifile = ifDAO.findImageFileWithHash( hash );

        ModifyImageFileCommand cmd = null;
        if (ifile != null) {
            /**
            Yes, this is a known file. Just information about the new location
            to the database.
             */
            cmd = new ModifyImageFileCommand(ifile);
            currentEvent.setResult(ExtVolIndexerEvent.RESULT_NEW_INSTANCE);
        } else {
            /*
            The file is not known to Photovault. Create a new ImageFile
            object & PhotoInfo that regards it as its original.
             */
            try {
                cmd = new ModifyImageFileCommand( f, hash );
                currentEvent.setResult( ExtVolIndexerEvent.RESULT_NEW_PHOTO );
            } catch ( Exception e ) {
                currentEvent.setResult( ExtVolIndexerEvent.RESULT_NOT_IMAGE );
                return;
            }
        }
        cmd.addLocation(
                new FileLocation( volume,
                volume.mapFileToVolumeRelativeName( f ) ) );
        try {
            cmdHandler.executeCommand( cmd );
            
            for ( PhotoInfo p : cmd.getCreatedPhotos() ) {
                createPreviewInstances( p, daoFactory );
            }
            // Add all photos associated with this image to current folder
            ifile = ifDAO.findById( cmd.getImageFile().getId(), false );
            ImageDescriptorBase img = ifile.getImage( "image#0" );
            OriginalImageDescriptor origImage =
                    (img instanceof OriginalImageDescriptor)
                    ? (OriginalImageDescriptor) img
                    : ((CopyImageDescriptor) img).getOriginal(  );
            Set<PhotoInfo> photos = origImage.getPhotos(  );
            List<Integer> photoIds = new ArrayList<Integer>(  );
            for ( PhotoInfo p : photos ) {
                photoIds.add( p.getId(  ) );
            }

            ChangePhotoInfoCommand addFolderCmd =
                    new ChangePhotoInfoCommand( photoIds );
            addFolderCmd.addToFolder( folder );
            cmdHandler.executeCommand( addFolderCmd );
        } catch ( CommandException ex ) {
            log.warn( "Exception in modifyImageFileCommand: " + ex.getMessage() );
            currentEvent.setResult( ExtVolIndexerEvent.RESULT_NOT_IMAGE );
            return;
        }

        currentEvent.setPhoto( null );        
        log.debug( "exit: indexFile " + f.getAbsolutePath() );
        ifile = cmd.getImageFile();
    }

    /**
     Create the needed preview instances for a photo
     @param p The photo
     */
    private void createPreviewInstances(PhotoInfo p, DAOFactory f ) throws CommandException {        
        Volume vol = f.getVolumeDAO().getDefaultVolume();
        CreateCopyImageCommand cmd = new CreateCopyImageCommand( p, vol, 100, 100 );
        cmd.setLowQualityAllowed( true );
        cmdHandler.executeCommand( cmd );
    }    
}
