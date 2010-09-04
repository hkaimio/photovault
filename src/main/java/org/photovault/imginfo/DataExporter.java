/*
  Copyright (c) 2009 Harri Kaimio

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

package org.photovault.imginfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.imginfo.dto.PhotoChangeSerializer;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.replication.Change;
import org.photovault.replication.ChangeDTO;
import org.photovault.replication.ChangeFactory;
import org.photovault.replication.DTOResolver;
import org.photovault.replication.DTOResolverFactory;
import org.photovault.replication.FieldConflictBase;
import org.photovault.replication.ObjectHistory;
import org.photovault.replication.ObjectHistoryDTO;
import org.photovault.replication.VersionedObjectEditor;
import org.photovault.replication.XStreamChangeSerializer;
import org.photovault.swingui.PhotoInfoEditor;

/**
 * Export or import the contents of a Photovault database history.
 *
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class DataExporter {

    Log log = LogFactory.getLog( DataExporter.class );

    public void DataExporter() {
    }

    /**
     * Export the folder hierarchy of current database
     * @param os stream in which the history will be written
     * @param df factory used for accessing the folders
     * @throws IOException
     */
    public void exportFolders( ObjectOutputStream os, DAOFactory df )
            throws IOException {
        PhotoFolderDAO folderDAO = df.getPhotoFolderDAO();
        PhotoFolder root = folderDAO.findRootFolder();
        exportFolderHierarchy( root, os );
    }


    /**
     * Export all photos in current database
     * @param os stream in which the history will be written
     * @param df factory used for accessing the folders
     * @throws IOException
     */
    public void exportPhotos( ObjectOutputStream os, DAOFactory df ) 
            throws IOException {
        PhotoInfoDAO photoDAO = df.getPhotoInfoDAO();
        List<PhotoInfo> allPhotos = photoDAO.findAll();
        for ( PhotoInfo p : allPhotos ) {
            ObjectHistory<PhotoInfo> h = p.getHistory();
            ObjectHistoryDTO<PhotoInfo> dto = new ObjectHistoryDTO( h );
            os.writeObject( dto );
        }
    }

    public void exportPhotos( File zipFile, DAOFactory df ) throws FileNotFoundException, IOException {
        PhotoInfoDAO photoDAO = df.getPhotoInfoDAO();
        List<PhotoInfo> allPhotos = photoDAO.findAll();
        FileOutputStream os = new FileOutputStream( zipFile );
        ZipOutputStream zipo = new ZipOutputStream( os );
        int photoCount = 0;
        for ( PhotoInfo p : allPhotos ) {
            addPhotoHistory( p, zipo );
            photoCount++;
            log.debug( "" + photoCount + " photos exported" );
        }
        zipo.close();
    }

    /**
     * Export the folder hierarchy below a given folder to externam file
     * @param f The top folder of the hierarchy
     * @param os Strean in which the exported data will be written.
     * @throws IOException
     */
    private void exportFolderHierarchy( PhotoFolder f, ObjectOutputStream os )
            throws IOException {
        ObjectHistory<PhotoFolder> h = f.getHistory();
        ObjectHistoryDTO<PhotoFolder> dto = new ObjectHistoryDTO( h );
        os.writeObject( dto );
        for ( PhotoFolder child : f.getSubfolders() ) {
            exportFolderHierarchy( child, os);
        }
    }


    /**
     * Import changes from a export file
     * @param is stream used to read the file
     * @param df factory for accessing current database and persistiong new
     * objects
     * @throws IOException
     */
    public void importChanges( ObjectInputStream is, DAOFactory df )
            throws IOException {
        ObjectHistoryDTO dto = null;
        int folderCount = 0;
        int photoCount = 0;
        int totalCount = 0;
        try {
            dto = (ObjectHistoryDTO) is.readObject();
        } catch ( ClassNotFoundException ex ) {
            log.error( ex );
        }
        HibernateDAOFactory hdf = (HibernateDAOFactory) df;
        Session session = hdf.getSession();
        PhotoFolderDAO folderDao = df.getPhotoFolderDAO();
        PhotoInfoDAO photoDao = df.getPhotoInfoDAO();
        DTOResolverFactory rf = df.getDTOResolverFactory();
        ChangeFactory cf = new ChangeFactory( df.getChangeDAO() );
        while ( dto != null ) {
            long startTime = System.currentTimeMillis();
            Transaction tx = session.beginTransaction();
            UUID uuid = dto.getTargetUuid();
            String className = dto.getTargetClassName();

            VersionedObjectEditor e = null;
            if ( className.equals( PhotoFolder.class.getName() ) ) {
                e = getFolderEditor( uuid, folderDao, rf );
                folderCount++;
            } else {
                e = getPhotoEditor( uuid, photoDao, rf );
                photoCount++;
            }
            totalCount++;
            try {
                e.addToHistory( dto, cf );
            } catch ( ClassNotFoundException ex ) {
                log.error( ex );
            }
            session.flush();
            tx.commit();
            session.clear();
            log.debug(  "Imported " + className + " in " + (System.currentTimeMillis()-startTime) + " ms. " +
                    photoCount + " photos, " + folderCount + " folders." );
            // e.apply();
            try {
                dto = (ObjectHistoryDTO) is.readObject();
            } catch ( ClassNotFoundException ex ) {
                log.error( ex );
                return;
            }
        }
    }


    /**
     * Get editor for a folder with given UUID. if the folder is already knwon
     * in current database return editor for it. If it is unknown, create a
     * local instance and return editor for it.
     * @param uuid UUID of the folder
     * @param folderDao DAO for accessing local instances of folders.
     * @param rf Resolver factory for the folders.
     * @return
     */
    private VersionedObjectEditor<PhotoFolder> getFolderEditor(
            UUID uuid, PhotoFolderDAO folderDao, DTOResolverFactory rf ) {
        PhotoFolder target = null;
        log.debug( "getFolderEditor(), uuid " + uuid );
        target = folderDao.findByUUID( uuid );
        VersionedObjectEditor<PhotoFolder> e = null;
        if ( target != null ) {
            log.debug( "getFodlerEditor: folder " + uuid + " found" );
            e = new VersionedObjectEditor( target, rf );
        } else {
            try {
                log.debug( "getFodlerEditor: Creating new folder " + uuid );
                e = new VersionedObjectEditor( PhotoFolder.class, uuid, rf );
                target = e.getTarget();
                folderDao.makePersistent( target );
                folderDao.flush();
            } catch ( InstantiationException ex ) {
                log.error( ex );
            } catch ( IllegalAccessException ex ) {
                log.error( ex );
            }
        }
        return e;
    }

    /**
     * Get editor for a photo with given UUID. If the photo is already knwon
     * in current database return editor for it. If it is unknown, create a
     * local instance and return editor for it.
     * @param uuid UUID of the photo
     * @param photoDao DAO for accessing local instances of photos.
     * @param rf Resolver factory for the photos.
     * @return
     */
    private VersionedObjectEditor<PhotoInfo> getPhotoEditor(
            UUID uuid, PhotoInfoDAO photoDao, DTOResolverFactory rf ) {
        PhotoInfo target = null;
        target = photoDao.findByUUID( uuid );
        VersionedObjectEditor<PhotoInfo> e = null;
        if ( target != null ) {
            e = new VersionedObjectEditor( target, rf );
        } else {
            try {
                e = new VersionedObjectEditor( PhotoInfo.class, uuid, rf );
                target = e.getTarget();
                photoDao.makePersistent( target );
                photoDao.flush();
            } catch ( InstantiationException ex ) {
                log.error( ex );
            } catch ( IllegalAccessException ex ) {
                log.error( ex );
            }
        }
        return e;
    }

    XStreamChangeSerializer ser = new PhotoChangeSerializer();

    public void exportFileInfo( ImageFile f, File sidecar ) throws IOException {
        OutputStream os = new FileOutputStream( sidecar );
        ZipOutputStream zipo = new ZipOutputStream( os );
        for ( Map.Entry<String, ImageDescriptorBase> e : f.getImages().entrySet() ) {
            ImageDescriptorBase img = e.getValue();
            if ( img instanceof OriginalImageDescriptor ) {
                OriginalImageDescriptor orig = (OriginalImageDescriptor) img;
                for ( PhotoInfo p : orig.getPhotos() ) {
                    addPhotoHistory( p, zipo );
                }
            }
        }
        zipo.close();
    }

    public void importChanges( File zipFile, DAOFactory df ) throws FileNotFoundException, IOException {
        FileInputStream is = new FileInputStream( zipFile );
        ZipInputStream zipis = new ZipInputStream( is );

        PhotoInfo p;
        UUID photoUuid = null;
        VersionedObjectEditor<PhotoInfo> pe = null;
        ObjectHistoryDTO<PhotoInfo> h = null;
        PhotoInfoDAO photoDao = df.getPhotoInfoDAO();
        DTOResolverFactory drf = df.getDTOResolverFactory();
        ChangeFactory<PhotoInfo> cf = new ChangeFactory<PhotoInfo>( df.getChangeDAO() );
        for ( ZipEntry e = zipis.getNextEntry(); e != null ; e= zipis.getNextEntry() ) {
            if ( e.isDirectory() ) {
                continue;
            }
            String ename = e.getName();
            log.debug( "start processing entry " + ename );
            String[] path = ename.split( "/" );
            if ( path.length != 2 ) {
                log.warn( "zip directory hierarchy should have 2 levels: " + ename );
            }
            String fname = path[path.length-1];
            if ( !fname.endsWith( ".xml" ) ) {
                log.warn( "Unexpected suffix: " + ename );
            }
            long esize = e.getSize();
            byte[] data = null;
            if ( esize > 0 ) {
                data = new byte[(int) esize];
                zipis.read( data );
            } else {
                byte[] tmp = new byte[65536];
                int offset = 0;
                int bytesRead = 0;
                while ( ( bytesRead = zipis.read( tmp, offset, tmp.length-offset ) ) > 0 ) {
                    offset += bytesRead;
                    if ( offset >= tmp.length ) {
                        tmp = Arrays.copyOf( tmp, tmp.length*2 );
                    }
                }
                data = Arrays.copyOf( tmp, offset );
            }
            ChangeDTO<PhotoInfo> dto = ChangeDTO.createChange( data, PhotoInfo.class );
            if ( dto == null ) {
                log.warn( "Failed to read change " + ename );
                continue;
            }
            UUID changeId = dto.getChangeUuid();
            if ( !fname.startsWith( changeId.toString() ) ) {
                log.warn( "Unexpected changeId " + changeId + " in file " + ename );
            }

            if ( !PhotoInfo.class.getName().equals( dto.getTargetClassName() ) ) {
                log.warn( "Only PhotoInfo class supported, saw " + dto.getTargetClassName() );
                continue;
            }
            UUID changeTargetId = dto.getTargetUuid();
            if ( !changeTargetId.equals( photoUuid ) ) {
                if ( h != null ) {
                    addHistory( h, df );
                }
                photoUuid = changeTargetId;
                h = new ObjectHistoryDTO<PhotoInfo>( PhotoInfo.class, photoUuid );
            }
            h.addChange( dto );
            photoUuid = changeTargetId;
        }
    }
    private void addHistory( ObjectHistoryDTO<PhotoInfo> h, DAOFactory df ) {
        log.debug( "entry: addHistory, " + h.getTargetClassName() + " " + h.getTargetUuid() );
        PhotoInfoDAO photoDao = df.getPhotoInfoDAO();
        DTOResolverFactory drf = df.getDTOResolverFactory();
        Transaction tx = null;
        Session s = null;
        if ( df instanceof HibernateDAOFactory ) {
            s = ((HibernateDAOFactory)df).getSession();
            tx = s.beginTransaction();
        }
        PhotoInfo p = photoDao.findByUUID( h.getTargetUuid() );
        VersionedObjectEditor<PhotoInfo> pe = null;
        if ( p == null ) {
            log.debug(  "  Target object not found." );
            try {
                pe = new VersionedObjectEditor<PhotoInfo>(
                         h, drf );
                pe.apply();
            } catch ( InstantiationException ex ) {
                log.error( ex );
                if ( tx != null ) tx.rollback();
                return;
            } catch ( IllegalAccessException ex ) {
                log.error( ex );
                if ( tx != null ) tx.rollback();
                return;
            } catch ( ClassNotFoundException ex ) {
                log.error( ex );
                if ( tx != null ) tx.rollback();
                return;
            }
            p = pe.getTarget();
            photoDao.makePersistent( p );
        } else {
            log.debug(  "  Target object found" );
            pe = new VersionedObjectEditor<PhotoInfo>(
                    p, df.getDTOResolverFactory() );
            try {
                ChangeFactory<PhotoInfo> cf =
                        new ChangeFactory<PhotoInfo>(  df.getChangeDAO() );
                Change<PhotoInfo> oldVersion = p.getHistory().getVersion();
                boolean wasAtHead = p.getHistory().getHeads().contains( oldVersion );
                pe.addToHistory( h, cf );
                Change<PhotoInfo> newVersion = p.getHistory().getVersion();
                Set<Change<PhotoInfo>> newHeads = new HashSet( p.getHistory().getHeads() );
                if ( newHeads.size() > 1 ) {
                    log.debug( "  merging heads" );
                    boolean conflictsLeft = false;
                    Change<PhotoInfo> currentTip = null;
                    for ( Change<PhotoInfo> head : newHeads ) {
                        if ( currentTip != null && currentTip != head ) {
                            log.debug( "merging " + currentTip.getUuid() +
                                    " with " + head.getUuid() );
                            Change<PhotoInfo> merged = currentTip.merge( head );
                            if ( !merged.hasConflicts() ) {
                                merged.freeze();
                                log.debug( "merge succesfull, " + merged.getUuid() );
                                currentTip = merged;
                            } else {
                                conflictsLeft = true;
                                if ( log.isDebugEnabled() ) {
                                    StringBuffer conflicts = new StringBuffer( " Conflicts unresolved: \n" );
                                    for ( FieldConflictBase conflict: merged.getFieldConficts() ) {
                                        String fieldName = conflict.getFieldName();
                                        conflicts.append( fieldName );
                                        conflicts.append( ": " );
                                        conflicts.append(
                                                currentTip.getField( fieldName) );
                                        conflicts.append( " <-> " );
                                        conflicts.append(
                                                head.getField( fieldName) );
                                    }
                                    log.debug(  conflicts );
                                }
                            }
                        } else {
                            currentTip = head;
                        }
                    }
                    if ( wasAtHead && !conflictsLeft ) {
                        pe.changeToVersion( currentTip );
                    }
                }
                photoDao.flush();
            } catch ( ClassNotFoundException ex ) {
                log.error( ex );
                if ( tx != null ) tx.rollback();
                return;
            } catch ( IOException ex ) {
                log.error( ex );
                if ( tx != null ) tx.rollback();
                return;
            }
        }
        if ( tx != null ) {
            tx.commit();
            s.clear();
        }
    }

    private void addPhotoHistory( PhotoInfo p, ZipOutputStream os ) throws IOException {
        String photoDirName = "photo_" + p.getUuid() + "/";
        ZipEntry photoDir = new ZipEntry( photoDirName );
        os.putNextEntry( photoDir );
        ObjectHistory<PhotoInfo> h = p.getHistory();
        ObjectHistoryDTO<PhotoInfo> hdto = new ObjectHistoryDTO<PhotoInfo>( h );
        for ( ChangeDTO ch : hdto.getChanges() ) {
            UUID chId = ch.getChangeUuid();
            String fname = photoDirName + chId + ".xml";
            ZipEntry chEntry = new ZipEntry( fname );
            os.putNextEntry( chEntry );
            byte[] xml = ch.getXmlData();
            os.write( xml );
        }
    }




}
