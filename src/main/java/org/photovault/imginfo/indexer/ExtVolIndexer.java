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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.odmg.HasBroker;
import org.hibernate.Session;
import org.hibernate.context.ManagedSessionContext;
import org.odmg.Implementation;
import org.odmg.Transaction;
import org.photovault.command.CommandException;
import org.photovault.command.CommandHandler;
import org.photovault.command.DataAccessCommand;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.folder.CreatePhotoFolderCommand;
import org.photovault.folder.DeletePhotoFolderCommand;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.ChangePhotoInfoCommand;
import org.photovault.imginfo.CopyImageDescriptor;
import org.photovault.imginfo.CreateCopyImageCommand;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.FileLocation;
import org.photovault.imginfo.ImageDescriptorBase;
import org.photovault.imginfo.ImageFile;
import org.photovault.imginfo.ImageFileDAO;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.ImageInstanceDAO;
import org.photovault.imginfo.ModifyImageFileCommand;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.imginfo.Volume;
import org.photovault.imginfo.VolumeBase;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;

/**
 ExtVolIndexer implements a background task for indexing all files in an
 external volume. It can be used either to index a new external volume or
 to resync Photovault index with an existing external volume if it is probable
 that the volume content has been modified by user.
 
 */
public class ExtVolIndexer implements Runnable {

    static private Log log = LogFactory.getLog( ExtVolIndexer.class.getName() );
    
    /**
     Creates a new instance of ExtVolIndexer
     @param vol The volume to be indexed
     */
    public ExtVolIndexer( ExternalVolume vol ) {
        volume = vol;
        if ( vol.getFolderId() >= 0 ) {
            topFolderId = vol.getFolderId();
        }
    }
    
    /** The volume that is indexed by this instance */
    ExternalVolume volume = null;
    
    CommandHandler commandHandler = null;
    
    public void setCommandHandler( CommandHandler ch ) {
        commandHandler = ch;
    }
    
    /**
     Folder used as top of created folder gierarchy or <code>null</code>
     if no folders should be created
     */
    private Integer topFolderId = null;
    
    private ExtVolIndexerEvent currentEvent = null;
    
    /**
     Etimate of current progress in indexing operation. Values 0..100
     */
    private int percentComplete;
     
    static class ProgressCalculator {
        
        /**
         Start for progress counter
         */
        int start;
        
        /**
         End for progress counter
         */
        int end;
        
        /**
         end-start
         */
        int delta;
        
        /**
         Total number of files
         */
        int files;
        
        /**
         Total number of subdirectories
         */
        int subdirs;
        
        /**
         Weight given for each subdirectory in calculation when compared to 
         local files.
         */
        int subdirWeight;
        
        /**
         Weight given for files in local directory when compared to subdirectories
         */
        int curdirFileWeight;

        /**
         Number of processed files 
         */
        int processedFiles;
        
        /**
         Number of processed subdirs
         */
        int processedSubdirs;

        public ProgressCalculator( int start, int end, 
                int files, int subdirs) {
            this.start = start;
            this.end = end;
            delta = end-start;
            this.files = files;
            this.subdirs = subdirs;
            subdirWeight = (files > 0 ? files : 1);
            curdirFileWeight = ( files > 0 ? 1 : 0);
        }

        public void setProcessedSubdirs( int n ) {
            processedSubdirs = n;
        }

        public void setProcessedFiles( int n ) {
            processedFiles = n;
        }

        public int getProgress() {
            int progress = (delta * (processedSubdirs * subdirWeight + processedFiles)) /
                    (subdirWeight * ( subdirs + curdirFileWeight ));
            return start + progress;
        }
    }
    
    static class UpdateInstanceCheckTimeCommand extends DataAccessCommand {
        public UpdateInstanceCheckTimeCommand( ImageInstance i ) {
            this.id = i.getHibernateId();
        }
        
        ImageInstance.InstanceId id;
        
        public void execute() throws CommandException {
            ImageInstanceDAO instDAO = daoFactory.getImageInstanceDAO();
            ImageInstance i = instDAO.findById( id, false );
            i.setCheckTime( new Date() );
        }
    }
    
    /**
     Indexes a single file to Photovault database.
     <ul>
     <li>First Photovault checks if the file is already indexed. If there is
     a file in this path according to database and its hash or both file size 
     and last modification time match this is assumed to be an existing file.</li>
     <li>
     If the file is not found Photovault tries to find an existing file with 
     the same hash. If such is found, Photovault assumes that this is another 
     copy of the same file and adds a new location for that file. </li>
     <li>
     If no such file is found Photovault creates a new ImageFile and PhotoInfo 
     with the image stored in location "image#0" in this file as original. 
     After that it creates a thumbnail on default volume.
     </li>
     </ul>
     @param f File to be indexed
     @return The ImageFile object this file was associated with or <code>null</code>
     if Photovault was not able to index this file.
     */
    ImageFile indexFile( File f, DAOFactory daoFactory ) {
        log.debug( "entry: indexFile " + f.getAbsolutePath() );
        currentEvent.setIndexedFile( f );
        indexedFileCount++;
        
        ImageFileDAO ifDAO = daoFactory.getImageFileDAO();
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        // Check if the instance already exists n database
        ImageFile file = ifDAO.findFileInLocation( volume, volume.mapFileToVolumeRelativeName( f ) );
        byte[] hash = null;
        if ( file != null ) {
            log.debug( "found existing file" );
            FileLocation fileLoc = null;
            for ( FileLocation loc : file.getLocations() ) {
                if ( loc.getFile().equals( f ) ) {
                    fileLoc = loc;
                    break;
                }
            }
            
            boolean matchesFile = true;
            if ( f.length() == file.getFileSize() ) {
                if ( f.lastModified() != fileLoc.getLastModified() ) {
                    hash = ImageFile.calcHash( f );
                    if ( !hash.equals( file.getHash() ) ) {
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
                return file;                
            } else {
                ModifyImageFileCommand deleteCmd = new ModifyImageFileCommand( file );
                deleteCmd.removeLocation( fileLoc );
                log.debug( "File is not consistent with the one in DB, removing location" );
                try {
                    commandHandler.executeCommand( deleteCmd );
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
        
        file = ifDAO.findImageFileWithHash( hash );

        ModifyImageFileCommand cmd = null;
        if (file != null) {
            /**
            Yes, this is a known file. Just information about the new location
            to the database.
             */
            cmd = new ModifyImageFileCommand(file);
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
                return null;
            }
        }
        cmd.addLocation(
                new FileLocation( volume,
                volume.mapFileToVolumeRelativeName( f ) ) );
        try {
            commandHandler.executeCommand( cmd );
            newInstanceCount++;
            if ( currentEvent.getResult() == ExtVolIndexerEvent.RESULT_NEW_PHOTO ) {
                newPhotoCount++;
            }
            
            for ( PhotoInfo p : cmd.getCreatedPhotos() ) {
                createPreviewInstances( p, daoFactory );
            }
            
        } catch (CommandException ex) {
            log.warn( "Exception in modifyImageFileCommand: " + ex.getMessage() );
            currentEvent.setResult( ExtVolIndexerEvent.RESULT_NOT_IMAGE );
            return null;
        }

        currentEvent.setPhoto( null );
        log.debug( "exit: indexFile " + f.getAbsolutePath() );
        return cmd.getImageFile();
    }
    
    /**
    Indexes all images in a given directory and its subdirectories.
    <p>
    Photovault calls indexFile() for each file in the directory and
    indexDirectory() for each subdirectory.
    <p>
    In addition, the method checks if there are files that according to the
    database should reside in this directory but that are not present. These are 
     removed from the database and the corresponding PhotoInfo objects are removed
     from the folder.
    @param dir The directory that will be indexed
    @parm folder Found photos will be added to this folder and new subfolder will
    be created for each subdirectory found (unless there already is a subfolder
    with same case-sensitive name. If <code>null</code> The photos will not be
    added to any folder.
    @param startPercent This is used for determining the completeness of the
    indexing operation. Based on indexing of upper level directories is has been
    estimated that indexing this subhierarchy will advance the indexing operation
    from startPErcent to endPercent. indexDirector will subsequently divide this
    to the operations it performs.
    @param endPercent See above.
     */
    void indexDirectory( File dir, PhotoFolder folder, int startPercent, int endPercent ) {
        log.debug( "entry: indexDirectory " + dir.getAbsolutePath() );
        /**
         Maintain information how many instances for the photos that were previously 
         added to the folder is found
         */
        HashMap<Integer,Integer> photoInstanceCounts = new HashMap<Integer,Integer>();
        HashSet<UUID> foldersNotFound = new HashSet<UUID>();
        if ( folder != null ) {
            for ( PhotoInfo photo : folder.getPhotos() ) {
                photoInstanceCounts.put( photo.getId(), new Integer( 0 ) );
            }
            for ( PhotoFolder f : folder.getSubfolders() ) {
                foldersNotFound.add(f.getUUID() );
            }
        }
        
        
        
        File entries[] = dir.listFiles();
        // Count the files
        List<File> files = new ArrayList<File>();
        List<File> subdirs = new ArrayList<File>();
        int fileCount = 0;
        int subdirCount = 0;
        for ( File entry : entries ) {
            if ( entry.isDirectory() ) {
                subdirCount++;
                subdirs.add( entry );
            } else {
                fileCount++;
                files.add( entry );
            }
        }

        ProgressCalculator c = new ProgressCalculator( startPercent, endPercent, 
                fileCount, subdirCount );
        
        int nFile = 0;
        int nDir = 0;
        // Index first files in this directory
        for ( File f : files ) {
            if ( f.canRead() ) {
                currentEvent = new ExtVolIndexerEvent( this );
                Session photoSession = HibernateUtil.getSessionFactory().openSession();
                Session oldSession = ManagedSessionContext.bind( (org.hibernate.classic.Session) photoSession);
                ImageFile ifile = indexFile( f, DAOFactory.instance( HibernateDAOFactory.class ) );
                photoSession.close();
                ManagedSessionContext.bind( (org.hibernate.classic.Session) oldSession);
                if ( ifile != null ) {
                    // Find the photo(s) associated with this instance
                    ifile = (ImageFile) oldSession.merge( ifile );
                    ImageDescriptorBase img = null;
                    img = ifile.getImage( "image#0" );
                    
                    Set<PhotoInfo> photos = getPhotosBasedOnImage( img );
                    for ( PhotoInfo p : photos ) {
                        if ( photoInstanceCounts.containsKey( p.getId() ) ) {
                            log.debug( "Photo is already in folder" );
                            // The photo is already in this folder
                            int refCount = photoInstanceCounts.get( p.getId() ).intValue();
                            photoInstanceCounts.remove( p.getId() );
                            photoInstanceCounts.put( p.getId(), new Integer( refCount+1 ));
                        } else {
                            // The photo is not yet in this folder
                            log.debug( "adding photo " + p.getId() + " to folder " + folder.getFolderId() );
                            ChangePhotoInfoCommand cmd = new ChangePhotoInfoCommand( p.getId() );
                            cmd.addToFolder( folder );
                            try {
                                commandHandler.executeCommand( cmd );
                            } catch (CommandException ex) {
                                ex.printStackTrace();
                            }
                            photoInstanceCounts.put( p.getId(), new Integer( 1 ));
                        }
                    }
                }
                nFile++;
                c.setProcessedFiles( nFile );
                percentComplete = c.getProgress();
                notifyListeners( currentEvent );
            }
        }
        
        /*
         Check if some of the photos that were in folder before were not found in 
         this directory
         */
        
        Iterator iter = photoInstanceCounts.keySet().iterator();
        Set<Integer> photoIdsNotFound = new HashSet<Integer>();
        while ( iter.hasNext() ) {
            Integer photoId = (Integer) iter.next();
            int refCount = (photoInstanceCounts.get(photoId)).intValue();
            if ( refCount == 0 ) {
                photoIdsNotFound.add( photoId );
                log.debug( "photo " + photoId + " not found during indexing" );
            }
        }        
        if ( photoIdsNotFound.size() > 0 ) {
            ChangePhotoInfoCommand removeFromFolderCmd = new ChangePhotoInfoCommand( photoIdsNotFound );
            removeFromFolderCmd.removeFromFolder( folder );
            try {
                log.debug( "Deleting unseen photos" );
                commandHandler.executeCommand( removeFromFolderCmd );
            } catch (CommandException ex) {
                ex.printStackTrace();
            }
        }
        // Index now all subdirectories
        for ( File subdir : subdirs ) {
            // Create the matching folder
            PhotoFolder subfolder = null;
            if ( folder != null ) {
                String folderName = subdir.getName();
                if ( folderName.length() > PhotoFolder.NAME_LENGTH ) {
                    folderName = folderName.substring( 0, PhotoFolder.NAME_LENGTH );
                }
                subfolder = findSubfolderByName( folder, folderName );
                if ( subfolder == null ) {
                    CreatePhotoFolderCommand createFolder = 
                            new CreatePhotoFolderCommand( folder, folderName,
                            "imported from " + subdir.getAbsolutePath() );
                    try {
                        commandHandler.executeCommand( createFolder );
                    } catch (CommandException ex) {
                        ex.printStackTrace();
                    }
                    subfolder = createFolder.getCreatedFolder();
                    newFolderCount++;
                } else {
                    foldersNotFound.remove( subfolder.getUUID() );
                }
            }
            /*
             Calclate the start & end percentages to use when indexing this
             directory. Formula goes so that we estimate that to index current
             dirctory completely we must index files in subDirCount+1 directories
             (all subdirs + current directory). So we divide endPercent - startPercent
             into this many steps)
             */

            int subdirStart = c.getProgress();
            nDir++;
            c.setProcessedSubdirs( nDir );
            int subdirEnd = c.getProgress();
            indexDirectory( subdir, subfolder, subdirStart, subdirEnd );
            percentComplete = c.getProgress();
        }
        

        
        // Delete folders that were not anymore found
         
        iter = foldersNotFound.iterator();
        while ( iter.hasNext() ) {
            PhotoFolder subfolder = (PhotoFolder)iter.next();
            DeletePhotoFolderCommand deleteFolderCmd = new DeletePhotoFolderCommand( subfolder );
            try {
                commandHandler.executeCommand( deleteFolderCmd );
            } catch (CommandException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    
    /**
     Removes instance records that were not updated during indexing.
     */
    protected void cleanupInstances() {
        Criteria crit = new Criteria();
        crit.addEqualTo( "instances.volumeId", volume.getName() );
        Criteria dateCrit = new Criteria();
        dateCrit.addIsNull( "instances.checkTime" );
        Criteria cutoffDateCrit = new Criteria();
        cutoffDateCrit.addLessThan( "instances.checkTime", startTime );
        dateCrit.addOrCriteria( cutoffDateCrit );
        crit.addAndCriteria( dateCrit );
        
        ODMGXAWrapper txw = new ODMGXAWrapper();
	Implementation odmg = ODMG.getODMGImplementation();
	Transaction tx = odmg.currentTransaction();
        Collection result = null;
	try {
	    PersistenceBroker broker = ((HasBroker) tx).getBroker();
	    QueryByCriteria q = new QueryByCriteria( PhotoInfo.class, crit );
        } catch ( Exception e ) {
            e.printStackTrace();
        }        

        // Now go through all the photos with stray instances
        Iterator photoIter = result.iterator();
        while ( photoIter.hasNext() ) {
            PhotoInfo p = (PhotoInfo) photoIter.next();
            Set<ImageInstance> instances = p.getInstances();
            List <ImageInstance> purge = new ArrayList<ImageInstance>();
            for ( ImageInstance inst : instances ) {
                Date checkTime = inst.getCheckTime();
                if ( inst.getVolume() == volume 
                        && (checkTime == null || checkTime.before( startTime )) ) {
                    purge.add( inst );
                }
            }
            for ( ImageInstance inst : purge ) {
                p.removeInstance( inst );
            }
        }
        txw.commit();
    }
    
    /**
     The indexer can create a folder hierarchy that matches the directory hierarchy
     in external volume if user so wants. If there already is a directory with
     same name under this folder the indexer uses it, otherwise it creates new
     folderst for each directory.
     @param topFolder used as top of the structure or <code>null</code> if no folders
     should be created.
     */
    public void setTopFolder(PhotoFolder topFolder) {
        this.topFolderId = topFolder.getFolderId();
        if ( volume != null ) {
            volume.setFolderId( topFolder.getFolderId() );
        }
    }

    /**
     Create the needed preview instances for a photo
     @param p The photo
     */
    private void createPreviewInstances(PhotoInfo p, DAOFactory f ) throws CommandException {        
        Volume vol = f.getVolumeDAO().getDefaultVolume();
        CreateCopyImageCommand cmd = new CreateCopyImageCommand( p, vol, 100, 100 );
        cmd.setLowQualityAllowed( true );
        commandHandler.executeCommand( cmd );
    }
    
    /**
     Finds a subfolder with given name. This is classified as protected so that also
     test code can use the same method - however, this service should really be offered
     by PhotoFolder API.
     @param folder The fodler to search
     @param name The name to look for
     @return The subfolder with matching name or <code>null</code> if none found.
     If there are multiple subfolders with the matching name an arbitrary one will
     be returned.
     */
    private PhotoFolder findSubfolderByName(PhotoFolder folder, String name ) {
        PhotoFolder subfolder = null;
        for ( int n = 0; n < folder.getSubfolderCount(); n++ ) {
            PhotoFolder candidate = folder.getSubfolder( n );
            if ( name.equals( candidate.getName() ) ) {
                subfolder = candidate;
                break;
            }
        }
        return subfolder;
    }
    
    /**
     Run the actual indexing operation.
     */
    public void run() {
        Session photoSession = null;
        Session oldSession = null;
        try {
            photoSession = HibernateUtil.getSessionFactory().openSession();
            oldSession = ManagedSessionContext.bind( (org.hibernate.classic.Session) photoSession);
            
            startTime = new Date();
            PhotoFolder topFolder = null;
            if ( topFolderId != null ) {
                DAOFactory daoFactory = DAOFactory.instance( HibernateDAOFactory.class );
                topFolder = daoFactory.getPhotoFolderDAO().findById( topFolderId, false );
            }
            indexDirectory( volume.getBaseDir(), topFolder, 0, 100 );
            // cleanupInstances();
            notifyListenersIndexingComplete();
        } catch( Throwable t ) {
            StringWriter strw = new StringWriter();
            strw.write( "Error indexing " + volume.getBaseDir().getAbsolutePath() );
            strw.write( "\n" );
            t.printStackTrace( new PrintWriter( strw ) );
            log.error( strw.toString() );   
            log.error( t );
            notifyListenersIndexingError( t.getMessage() );
        } finally {
            if ( photoSession != null ) {
                photoSession.close();
            }
                ManagedSessionContext.bind( (org.hibernate.classic.Session) oldSession);                
        }
    }
    
    // Listener support
    
    /**
     Set of listreners that are notified about progress in indexing
     */
    private Set<ExtVolIndexerListener> listeners = 
            new HashSet<ExtVolIndexerListener>();
    
    /**
     Add a new listener to the set which will be notified about new indexing
     events.
     @param l The listener object
     */
    public void addIndexerListener( ExtVolIndexerListener l ) {
        listeners.add( l );
    }
    /**
     Remove a new listener from the set which will be notified about new indexing
     events.
     @param l The listener object that will be removed.
     */
    public void removeIndexerListener( ExtVolIndexerListener l ) {
        listeners.remove( l );
    }
    
    /**
     Notifies all listeners that a new file has been indexed.
     @param e The indexer event object that describes the event
     */
    private void notifyListeners( ExtVolIndexerEvent e ) {
        Iterator iter = listeners.iterator();
        while ( iter.hasNext() ) {
            ExtVolIndexerListener l = (ExtVolIndexerListener) iter.next();
            l.fileIndexed( e );
        }
    }
    
    /**
     Notify all listeners that indexing operation for the whole volume has
     been completed.
     */
    private void notifyListenersIndexingComplete() {
        Iterator iter = listeners.iterator();
        while ( iter.hasNext() ) {
            ExtVolIndexerListener l = (ExtVolIndexerListener) iter.next();
            l.indexingComplete( this );
        }
    }
    
    /**
     Notify all listeners that an error happened during indexing
     */
    private void notifyListenersIndexingError( String message ) {
        Iterator iter = listeners.iterator();
        while ( iter.hasNext() ) {
            ExtVolIndexerListener l = (ExtVolIndexerListener) iter.next();
            l.indexingError( message );
        }
    }
    
    
    
    // Statistics
    
    /**
     Count of new photos created during indexing
     */
    private int newPhotoCount = 0;
    /**
     Count of new photo instances created during indexing
     */
    private int newInstanceCount = 0;
    
    /**
     Count of new folders created during indexing
     */
    private int newFolderCount = 0;
    
    /**
     Total count of files checked while indexing
     */
    private int indexedFileCount = 0;
    
    /**
     Get the count of new photos created during indexing operation.
     */
    public int getNewPhotoCount() {
        return newPhotoCount;
    }
    
    /**
     Get the count of new photo instances created during indexing operation.
     */
    public int getNewInstanceCount() {
        return newInstanceCount;
    }
    
    /**
     Get the count of new folders created during indexing operation.
     */
    public int getNewFolderCount() {
        return newFolderCount;
    }
    
    /**
     Get the total number of files indexed during the operation.
     */
    public int getIndexedFileCount() {
        return indexedFileCount;
    }
    
    /**
     Returns info on how far the indexing operation has progresses as percentage
     complete
     @return Value in range 0..100;
     */
    public int getPercentComplete() {
        return percentComplete;
    }
    
    Date startTime = null;
    
    /**
     Get the starting time of the last started indexing operation.
     @return Starting time or <code>null</code> if no indexing operation started
     */
    public Date getStartTime() {
        return (startTime != null) ? (Date) startTime.clone() : null;
    }
    /**
     Utility method to find out {@link PhotoInfo}s based on given image
     @param img The image
     @return Set of photos that are based in this image (if it is original) or
     its original.
     */
    private Set<PhotoInfo> getPhotosBasedOnImage(ImageDescriptorBase img) {
        if ( img instanceof OriginalImageDescriptor ) {
            return ((OriginalImageDescriptor)img).getPhotos();
        }
        return ((CopyImageDescriptor)img).getOriginal().getPhotos();
    }
}


