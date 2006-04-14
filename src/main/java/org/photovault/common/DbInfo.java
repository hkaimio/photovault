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
  along with Foobar; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.common;

import java.util.Date;
import java.util.List;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.odmg.Transaction;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;

/**
 This class represents the database_info structure for the current database
 
 */
 public class DbInfo {
    
    /** 
     Creates a new instance of DbInfo. THis should <b>not</b> be used by
     any aplication code, it is public only because OJB requires it. Instead,
     use @see getDbInfo method to get the database infor structure of the 
     currently open database.
     */
    public DbInfo() {
    }
    
    static DbInfo info = null;
    
    /**
     Returns the current database infor structure of the currently open 
     database.
     */
    static public DbInfo getDbInfo() {
        if ( info == null ) {
            String oql = "select info from " + DbInfo.class.getName();
            List infos = null;
        
            // Get transaction context
            ODMGXAWrapper txw = new ODMGXAWrapper();
            Implementation odmg = ODMG.getODMGImplementation();
        
            try {
                OQLQuery query = odmg.newOQLQuery();
                query.create( oql );
                infos = (List) query.execute();
                txw.commit();
            } catch (Exception e ) {
                txw.abort();
            }
            if ( infos.size() > 0 ) {
                info = (DbInfo) infos.get(0);
            }
        }
        return info;
    }
    
    /**
     Set the revision of database schema.
     @param version the version number.
     */
    public void setVersion( int version ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.version = version;
        txw.commit();
    }
    
    /**
     Get the current version number.
     */
    public int getVersion() {
        return version;
    }
    
    /**
     Get the time when this database was created.
     */
    public Date getCreateTime() {
        return createTime;
    }
    
    /**
     Get the unique ID of thsi database
     */
    public String getId() {
        return id;
    }
    
    private String id;
    private int version;
    private Date createTime;
}
