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

package org.photovault.dcraw;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.photovault.dbhelper.ImageDb;
import org.photovault.imginfo.FileUtils;
import org.photovault.imginfo.VolumeBase;
import org.photovault.test.PhotovaultTestCase;

/**
 * Test cases for color profile handling.
 * @author Harri Kaimio
 * @since 0.4.0
 */
public class Test_ColorProfile extends PhotovaultTestCase {
    
    static final String testProfilePath = "/usr/share/color/icc/Canon EOS-30D generic.icm";
    /** Creates a new instance of Test_ColorProfile */
    public Test_ColorProfile() {
    }
    
    public void testCreateProfile() {
        ColorProfileDesc.CreateProfile creator = 
                new ColorProfileDesc.CreateProfile( new File( testProfilePath ),
                "Test", "Test profile" );
        ColorProfileDesc p = creator.execute();
        
        assertEquals( "Test", p.getName() );
        assertEquals( "Test profile", p.getDescription() );
        
        File pf = p.getInstanceFile();
        this.assertNotNull( pf );
         
        ColorProfileDesc p2 = ColorProfileDesc.getProfileById( p.id );
        assertTrue( p == p2 );
    }
    
    public void testProfilePersistence() {
        ColorProfileDesc.CreateProfile creator = 
                new ColorProfileDesc.CreateProfile( new File( testProfilePath ),
                "Test", "Test profile" );
        ColorProfileDesc p = creator.execute();
	
        // Check that the profile is saved correctly
        Connection conn = ImageDb.getConnection();
	String sql = "SELECT * FROM icc_profiles WHERE profile_id = " + p.id;
	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = conn.createStatement();
	    rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		fail( "Matching DB record not found" );
	    }
            String name = rs.getString( "PROFILE_NAME" );
            assertEquals( "Test", name );
	} catch ( SQLException e ) {
	    fail( "DB error:; " + e.getMessage() );
	} finally {
	    if ( rs != null ) {
		try {
		    rs.close();
		} catch ( Exception e ) {}
	    }
	    if ( stmt != null ) {
		try {
		    stmt.close();
		} catch ( Exception e ) {}
	    }
	}        

        sql = "SELECT * FROM icc_instances WHERE profile_id = " + p.id;
	try {
	    stmt = conn.createStatement();
	    rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		fail( "Matching DB record not found" );
	    }
            String volume = rs.getString( "VOLUME_ID" );
            String fname = rs.getString( "FNAME" );
            VolumeBase vol = VolumeBase.getVolume( volume );
            File f = null;
            try {
                f = vol.mapFileName(fname);
            } catch (FileNotFoundException ex) {
                fail( "Cannot map " + fname + " in volume " + vol.getName() );
            }
            assertTrue( f.exists() );
	} catch ( SQLException e ) {
	    fail( "DB error:; " + e.getMessage() );
	} finally {
	    if ( rs != null ) {
		try {
		    rs.close();
		} catch ( Exception e ) {}
	    }
	    if ( stmt != null ) {
		try {
		    stmt.close();
		} catch ( Exception e ) {}
	    }
	}        
        
        
        Collection profiles = ColorProfileDesc.getAllProfiles();
        assertTrue( profiles.size() > 0 );
    }
    
    public void testRetrieveProfile() {
        ColorProfileDesc p = ColorProfileDesc.getProfileById( 1 );
        assertEquals( "Test 1", p.getName() );
        assertEquals( "Seed profile", p.getDescription() );
        
        Collection profiles = ColorProfileDesc.getAllProfiles();
        Iterator iter = profiles.iterator();
        boolean found = false;
        while ( iter.hasNext() ) {
            ColorProfileDesc p2 = (ColorProfileDesc) iter.next();
            if ( p == p2 ) {
                found = true;
                break;
            }
        }
        assertTrue( found );
    }
    
    
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_ColorProfile.class );
    }
}
