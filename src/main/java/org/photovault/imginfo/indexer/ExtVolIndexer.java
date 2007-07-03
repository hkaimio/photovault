/*
  Copyright (c) 2006 Harri Kaimio
 
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
import java.util.Vector;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.odmg.HasBroker;
import org.odmg.Implementation;
import org.odmg.Transaction;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;

/**
 ExtVolIndexer implements a background task for indexing all files in an
 external volume. It can be used either to index a new external volume or
 to resync Photovault index with an existing external volume if it is probable
 that the volume content has been modified by user.
 
 */
public class ExtVolIndexer implements Runnable {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ExtVolIndexer.class.getName() );
    
    /**
     Creates a new instance of ExtVolIndexer
     @param vol The volume to be indexed
     */
    public ExtVolIndexer( ExternalVolume vol ) {
        volume = vol;
        if ( vol.getFolderId() >= 0 ) {
            topFolder = PhotoFolder.getFolderById( vol.getFolderId() );
        }
    }
    
    /** The volume that is indexed by this instance */
    ExternalVolume volume = null;
    
    /**
     Folder used as top of created folder gierarchy or <code>null</code>
     if no folders should be created
     */
    private PhotoFolder topFolder = null;
    
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
    
    /**
     Indexes a single file to Photovault database.
     <ul>
     <li>First Photovault checks if the file is already indexed. If the instance
     is found in database and hash or both file zise and last modification time 
     match this is assumed to be an existing instance.</li>
     <li>
     If the file is not found Photovault tries to find an existing instance with 
     the same checksum. If such is found, Photovault assumes that this is another 
     copy of the same file and adds it as an instance to that PhotoInfo. </li>
     <li>
     If no such instance is found Photovault creates a new PhotoInfo and
     adds this image as an original instance of it and create a thumbnail on
     default volume.
     </li>
     </ul>
     @param f File to be indexed
     @return The PhotoInfo object this file was added to or <code>null</code>
     if Photovault was not able to index this file.
     */
    PhotoInfo indexFile( File f ) {
        log.debug( "entry: indexFile " + f.getAbsolutePath() );
        currentEvent.setIndexedFile( f );
        indexedFileCount++;
        
        // Check if the instance already exists n database
        ImageInstance oldInstance = null;
        try {
            oldInstance = ImageInstance.retrieve( volume, 
                    volume.mapFileToVolumeRelativeName(f ) );
        } catch ( PhotoNotFoundException e ) {
            // No action, there just were no matching instances in database
        }
        if ( oldInstance != null ) {
            // There is an existing instance, check whether the data matches
            if ( oldInstance.doConsistencyCheck() ) {
                PhotoInfo photo = null;
                try {
                    photo = PhotoInfo.retrievePhotoInfo(oldInstance.getPhotoUid());
                } catch (PhotoNotFoundException ex) {
                    ex.printStackTrace();
                }
                return photo;
            } else {
                PhotoInfo photo = null;
                try {
                    photo = PhotoInfo.retrievePhotoInfo(oldInstance.getPhotoUid());
                } catch (PhotoNotFoundException ex) {
                    ex.printStackTrace();
                }
                photo.removeInstance( oldInstance );
            }
        }
        
        
        // Check whether this is really an image file
        
        ODMGXAWrapper txw = new ODMGXAWrapper();
        ImageInstance instance = null;
        try {
            instance = ImageInstance.create( volume, f );
        } catch ( Exception e ) {
            currentEvent.setResult( ExtVolIndexerEvent.RESULT_ERROR );
            return null;
        }
        if ( instance == null ) {
            currentEvent.setResult( ExtVolIndexerEvent.RESULT_NOT_IMAGE );
            /*
             ImageInstance already aborts transaction if reading image file
             was unsuccessfull.
             */
            return null;
        }
        byte[] hash = instance.getHash();
        
        // Check whether there is already an image instance with the same hash
        PhotoInfo matchingPhotos[] = PhotoInfo.retrieveByOrigHash( hash );
        PhotoInfo photo = null;
        if ( matchingPhotos != null && matchingPhotos.length > 0 ) {
            // If yes then get the PhotoInfo and add this file as an instance with
            // the same type as the one with same hash. If only PhotoInfo with no
            // instances add as original for that
            photo = matchingPhotos[0];
            photo.addInstance( instance );
            currentEvent.setResult( ExtVolIndexerEvent.RESULT_NEW_INSTANCE );
            newInstanceCount++;
        } else {
            photo = PhotoInfo.create();
            photo.addInstance( instance );
            photo.updateFromOriginalFile();
            txw.flush();
            // Create a thumbnail for this photo
            photo.getThumbnail();
            currentEvent.setResult( ExtVolIndexerEvent.RESULT_NEW_PHOTO );
            newInstanceCount++;
            newPhotoCount++;
        }
        currentEvent.setPhoto( photo );
        txw.commit();
        log.debug( "exit: indexFile " + f.getAbsolutePath() );
        return photo;
    }
    
    /**
     Indexes all images in a given directory and its subdirectories.
     <p>
     Photovault calls indexFile() for each file in the directory and
     indexDirectory() for each subdirectory.
     <p>
     In addition, the method checks if there are instances that according to the
     database should reside in this directory but that are not present. If such
     isntances are found they are deleted from database.
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
        HashMap photoInstanceCounts = new HashMap();
        HashSet foldersNotFound = new HashSet();
        if ( folder != null ) {
            for ( int n = 0; n < folder.getPhotoCount(); n++ ) {
                photoInstanceCounts.put( folder.getPhoto( n ), new Integer( 0 ) );
            }
            for ( int n = 0; n < folder.getSubfolderCount(); n++ ) {
                foldersNotFound.add( folder.getSubfolder( n ) );
            }
        }
        
        
        
        File files[] = dir.listFiles();
        // Count the files
        int fileCount = 0;
        int subdirCount = 0;
        for ( int n = 0; n < files.length; n++ ) {
            if ( files[n].isDirectory() ) {
                subdirCount++;
            } else {
                fileCount++;
            }
        }

        ProgressCalculator c = new ProgressCalculator( startPercent, endPercent, 
                fileCount, subdirCount );
        
        int nFile = 0;
        int nDir = 0;
        for ( int n = 0; n < files.length; n++ ) {
            File f = files[n];
            if ( f.isDirectory() ) {
                // Create the matching folder
                PhotoFolder subfolder = null;
                if ( folder != null ) {
                    String folderName = f.getName();
                    if ( folderName.length() > PhotoFolder.NAME_LENGTH ) {
                        folderName = folderName.substring( 0, PhotoFolder.NAME_LENGTH );
                    }
                    subfolder = findSubfolderByName( folder, folderName );
                    if ( subfolder == null ) {
                        subfolder = PhotoFolder.create( folderName, folder );
                        newFolderCount++;
                    } else {
                        foldersNotFound.remove( subfolder );
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
                indexDirectory( f, subfolder, subdirStart, subdirEnd );
                percentComplete = c.getProgress();
            } else {
                if ( f.canRead() ) {
                    currentEvent = new ExtVolIndexerEvent( this );
                    PhotoInfo p = indexFile( f );
                    if ( p != null ) {
                        if ( photoInstanceCounts.containsKey( p ) ) {
                            // The photo is already in this folder
                            int refCount = ((Integer)photoInstanceCounts.get( p ) ).intValue();
                            photoInstanceCounts.remove( p );
                            photoInstanceCounts.put( p, new Integer( refCount+1 ));
                        } else {
                            // The photo is not yet in this folder
                            folder.addPhoto( p );
                            photoInstanceCounts.put( p, new Integer( 1 ));
                        }
                    }
                    nFile++;
                    c.setProcessedFiles( nFile );
                    percentComplete = c.getProgress();
                    notifyListeners( currentEvent );
                }
            }
        }
        
        /*
         Check if some of the photos that were in folder before were not found in 
         this directory
         */
        Iterator iter = photoInstanceCounts.keySet().iterator();
        while ( iter.hasNext() ) {
            PhotoInfo p = (PhotoInfo ) iter.next();
            int refCount = ((Integer)photoInstanceCounts.get( p )).intValue();
            if ( refCount == 0 ) {
                folder.removePhoto( p );
            }
        }
        
        // Delete folders that were not anymore found
         
        iter = foldersNotFound.iterator();
        while ( iter.hasNext() ) {
            PhotoFolder subfolder = (PhotoFolder)iter.next();
            subfolder.delete();
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
	    result = broker.getCollectionByQuery( q );
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
        this.topFolder = topFolder;
        if ( volume != null ) {
            volume.setFolderId( topFolder.getFolderId() );
        }
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
        try {
            startTime = new Date();
            indexDirectory( volume.getBaseDir(), topFolder, 0, 100 );
            cleanupInstances();
            notifyListenersIndexingComplete();
        } catch( Throwable t ) {
            StringWriter strw = new StringWriter();
            strw.write( "Error indexing " + volume.getBaseDir().getAbsolutePath() );
            strw.write( "\n" );
            t.printStackTrace( new PrintWriter( strw ) );
            log.error( strw.toString() );   
            log.error( t );
            notifyListenersIndexingError( t.getMessage() );
        }
    }
    
    // Listener support
    
    /**
     Set of listreners that are notified about progress in indexing
     */
    private HashSet listeners = new HashSet();
    
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
}


