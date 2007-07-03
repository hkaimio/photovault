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

package org.photovault.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.hibernate.Session;
import org.photovault.common.JUnitHibernateManager;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.ImageInstanceDAO;
import org.photovault.imginfo.ImageInstanceDAOHibernate;
import org.photovault.imginfo.PhotoInfo;

/**
 * This class extends junit TestCase class so that it sets up the OJB environment
 * 
 * @author Harri Kaimio
 */
public class PhotovaultTestCase extends TestCase {
    
    /** Creates a new instance of PhotovaultTestCase */
    public PhotovaultTestCase() {
        JUnitHibernateManager.getHibernateManager();
    }

    /**
     *       Sets ut the test environment
     */
    public void setUp() {
    }

    /**
     Utility to check that the object in memory matches the database. Checks
     that a record with same id exists, that all fields match and that the 
     collections (instances & folders) are saved correctly
     @param p The photo to verify
     @param session Hibernate persistence context used to query the database
     */
    public static void assertMatchesDb( PhotoInfo p, Session session ) {
        String sql = "select * from photos where photo_id = " + p.getId();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = session.connection().createStatement();
            rs = stmt.executeQuery( sql );
            if ( !rs.next() ) {
                fail( "record not found" );
            }
            // TODO: there is no pointer back from instance to photo so this cannot be checked
            //	    assertEquals( "photo doesn't match", i.getPhoto .getUid(), rs.getInt( "photo_id" ) );
            assertEquals( p.getCamera(), rs.getString( "camera" ) );
            assertEquals( p.getDescription(), rs.getString( "description" ) );
            assertEquals( p.getFStop(), rs.getDouble( "f_stop" ) );
            assertEquals( p.getFilm(), rs.getString( "film" ) );
            assertEquals( p.getFilmSpeed(), rs.getInt( "film_speed") );
            assertEquals( p.getFocalLength(), rs.getDouble( "focal_length" ) );
            assertEquals( p.getLens(), rs.getString( "lens" ) );
            assertEquals( p.getOrigFname(), rs.getString( "orig_fname" ) );
            // assertEquals( p.getOrigInstanceHash(), rs.getByte( "hash") );
            assertEquals( p.getPhotographer(), rs.getString( "photographer" ) );
            assertEquals( p.getPrefRotation(), rs.getDouble( "pref_rotation" ) );
            assertEquals( p.getQuality(), rs.getInt( "photo_quality" ) );
            assertEquals( p.getShootTime(), rs.getTimestamp( "shoot_time" ) );
            assertEquals( p.getShootingPlace(), rs.getString( "shooting_place" ) );
            assertEquals( p.getShutterSpeed(), rs.getDouble( "shutter_speed" ) );
            assertEquals( p.getTechNotes(), rs.getString( "tech_notes" ) );
            assertEquals( p.getTimeAccuracy(), rs.getDouble( "time_accuracy" ) );
            RawConversionSettings s = p.getRawSettings();
            if ( s != null ) {
                assertEquals( s.getRawSettingId(), rs.getInt( "rawconv_id" ) );
            }
            
            assertTrue( "Photo not correct", p.getUid() == rs.getInt( "photo_id" ) );
        } catch ( SQLException e ) {
            fail( e.getMessage() );
        } finally {
            if ( rs != null ) {
                try {
                    rs.close();
                } catch ( Exception e ) {
                    fail( e.getMessage() );
                }
            }
            if ( stmt != null ) {
                try {
                    stmt.close();
                } catch ( Exception e ) {
                    fail( e.getMessage() );
                }
            }
        }
        // Check that instance collection matches
        Set<ImageInstance> instances = p.getInstances();
        Map<ImageInstance,Boolean> instFound = new HashMap();
        for ( ImageInstance i : instances ) {
            instFound.put( i, Boolean.FALSE );
        }
        ImageInstanceDAO instDAO = new ImageInstanceDAOHibernate();
        List dbInstances = instDAO.findPhotoInstances( p );
        for ( Object o : dbInstances ) {
            if ( !instFound.containsKey( (ImageInstance) o ) ) {
                fail( "ImageInstance " + o.toString() + " in database but not in instances collection" );
            }
            instFound.put( (ImageInstance) o, Boolean.TRUE );
        }
        if ( instFound.containsValue( Boolean.FALSE ) ) {
            fail( "Not all instances found in database" );
        }
        
    }
    
    public static void assertMatchesDb( PhotoFolder folder, Session session ) {
	int id = folder.getFolderId();
	String sql = "select * from photo_collections where collection_id = " + id;
	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = session.connection().createStatement();
	    rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		fail( "rrecord not found" );
	    }
	    assertEquals( "name doesn't match", folder.getName(), rs.getString( "collection_name" ) );
	    assertEquals( "description doesn't match", folder.getDescription(), rs.getString( "collection_desc" ) );
            int parentId = rs.getInt( "parent" );
            if ( rs.wasNull() ) {
                assertNull( folder.getParentFolder() );
            } else {
                assertEquals( parentId, folder.getParentFolder().getFolderId() );
            }
            rs.close();
            
            // Check that subfolders collection matches database
            rs = stmt.executeQuery( "select * from photo_collections where parent = " + id );
            Set<Integer> folderIds = new HashSet<Integer>();
            for ( PhotoFolder f : folder.getSubfolders() ) {
                folderIds.add( f.getFolderId() );
            }
            while ( rs.next() ) {
                int subId = rs.getInt( "collection_id" );
                assertTrue( "folder " + subId + " not in memory copy", 
                        folderIds.remove( subId ) );
                
            }            
            assertTrue( folderIds.size() == 0 );
            rs.close();
            
            // Check that photos collection matches database
            rs = stmt.executeQuery( "select * from collection_photos where collection_id = " + id );
            Set<Integer> photoIds = new HashSet<Integer>();
            for ( PhotoInfo p : folder.getPhotos() ) {
                photoIds.add( p.getId() );
            }
            while ( rs.next() ) {
                int photoId = rs.getInt( "photo_id" );
                assertTrue( "photo " + photoId + " not in memory copy", 
                        photoIds.remove( photoId ) );                
            }            
            assertTrue( photoIds.size() == 0 );
	} catch ( SQLException e ) {
	    fail( e.getMessage() );
	} finally {
	    if ( rs != null ) {
		try {
		    rs.close();
		} catch ( Exception e ) {
		    fail( e.getMessage() );
		}
	    }
	    if ( stmt != null ) {
		try {
		    stmt.close();
		} catch ( Exception e ) {
		    fail( e.getMessage() );
		}
	    }
	}
    }
	    
}
