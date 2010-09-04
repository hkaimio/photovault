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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.replication.ChangeFactory;
import org.photovault.replication.DTOResolverFactory;
import org.photovault.replication.ObjectHistory;
import org.photovault.replication.ObjectHistoryDTO;
import org.photovault.replication.VersionedObjectEditor;

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
}
