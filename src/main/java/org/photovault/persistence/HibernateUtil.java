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

package org.photovault.persistence;
import java.io.File;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultException;

/**
 Hibernate startup utility class
 */ 
public class HibernateUtil {
    private static SessionFactory sessionFactory;
    
    static public void init( String user, String passwd, PVDatabase dbDesc )
    throws PhotovaultException {
        try {
            AnnotationConfiguration cfg = new AnnotationConfiguration();
            cfg.configure();
            if ( dbDesc.getInstanceType() == PVDatabase.TYPE_EMBEDDED ) {
                cfg.setProperty( "hibernate.connection.driver_class", 
                        "org.apache.derby.jdbc.EmbeddedDriver" );
                cfg.setProperty( "hibernate.dialect", 
                        "org.hibernate.dialect.DerbyDialect" );
                cfg.setProperty( "hibernate.connection.url", 
                        "jdbc:derby:photovault;create=true" );
                File derbyDir = new File( dbDesc.getEmbeddedDirectory(), "derby" );
                System.setProperty( "derby.system.home", derbyDir.getAbsolutePath()  );
                if ( ( user != null && user.length() != 0 ) ||
                        ( passwd != null && passwd.length() != 0 ) )  {
                    throw new PhotovaultException( "No username or password allowed for Derby database" );
                }
            } else {
                // This is a MySQL database
                String dbhost = dbDesc.getHost();
                String dbname = dbDesc.getDbName();
                cfg.setProperty( "hibernate.connection.driver_class", 
                        "com.mysql.jdbc.Driver" );
                cfg.setProperty( "hibernate.dialect", 
                        "org.hibernate.dialect.MysqlDialect" );
                cfg.setProperty( "hibernate.connection.url", 
                        "jdbc:mysql://" + dbhost + "/" + dbname );
                cfg.setProperty( "hibernate.connection.username", user );
                cfg.setProperty( "hibernate.connection.password", passwd );
            }
            
            sessionFactory = cfg.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    public static SessionFactory getSessionFactory() {
// Alternatively, you could look up in JNDI here
        return sessionFactory;
    }
    public static void shutdown() {
// Close caches and connection pools
        getSessionFactory().close();
    }
}
