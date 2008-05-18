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

package org.photovault.imginfo.xml;

import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;
import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hsqldb.lib.StringInputStream;
import org.photovault.change.ChangeDescParserRuleSet;
import org.photovault.command.CommandException;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.folder.CreatePhotoFolderCommand;
import org.photovault.image.ColorCurve;
import org.photovault.imginfo.ChangePhotoInfoCommand;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.imginfo.PhotoInfoFields;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 Test cases for PhotoInfoChangeDesc
 @author Harri Kaimio
 @since 0.6.0
 */
public class Test_PhotoInfoChangeDesc extends PhotovaultTestCase {
    
    private static Log log 
            = LogFactory.getLog( Test_PhotoInfoChangeDesc.class.getName() );

    /**
     Simple test of change record creation and persistence.
     */
    @Test
    public void testChangeRecordCreation() throws IOException {
        PhotovaultCommandHandler cmdHandler = new PhotovaultCommandHandler( null );
        
        CreatePhotoFolderCommand fcmd = new CreatePhotoFolderCommand( null, "Koe", "Koe" );
        try {
            cmdHandler.executeCommand( fcmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        
        
	ChangePhotoInfoCommand photoCreateCmd = new ChangePhotoInfoCommand( );
        try {
            cmdHandler.executeCommand( photoCreateCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        
        PhotoInfo p = photoCreateCmd.getChangedPhotos().iterator().next();
        
	ChangePhotoInfoCommand photoChangeCmd = 
                new ChangePhotoInfoCommand( p.getUuid() );
        photoChangeCmd.setPhotographer("Harri" );
        photoChangeCmd.setFStop( 5.6 );
        ColorCurve c = new ColorCurve();
        c.addPoint( 0.0, 0.3 );
        c.addPoint( 1.0, 0.7 );
        photoChangeCmd.setField( PhotoInfoFields.COLOR_CURVE_RED, c );
        photoChangeCmd.setCropBounds( new Rectangle2D.Double( 0.1, 0.2, 0.6, 0.5 ) );
        photoChangeCmd.addToFolder( fcmd.getCreatedFolder() );
        try {
            cmdHandler.executeCommand( photoChangeCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        
        Session session = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory hdf = (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        hdf.setSession( session );
        
        PhotoInfoDAO photoDAO = hdf.getPhotoInfoDAO();
        p = photoDAO.findByUUID( p.getUuid() );
        ChangeDesc v = p.getVersion();
        StringWriter strw = new StringWriter();
        BufferedWriter w = new BufferedWriter( strw );
        ((PhotoInfoChangeDesc)v).writeXml(w, 0);
        w.flush();
        System.out.println( strw.toString() );
        assert ((PhotoInfoChangeDesc)v).changedFields.size() == 4;
        ColorCurve c1 = 
                (ColorCurve) ((PhotoInfoChangeDesc)v).changedFields.
                get( PhotoInfoFields.COLOR_CURVE_RED );
        assert c1.getY( 1 ) == 0.7;
        assert v.verify();
        assert v.getTargetUuid().equals(p.getUuid());
        Set<ChangeDesc> vp = v.getPrevChanges();
        assert vp.size() == 1;
        
        ChangeDesc v2 = vp.iterator().next();
        assert v2.verify();
        assert v2.getTargetUuid().equals( p.getUuid() );
        assert v2.getPrevChanges().size() == 0;
    }
    
    static final String xmlProlog = "<?xml version='1.0' ?>";
        static final String invalidChange = xmlProlog +
            "<change uuid=\"37916313-9480-39fc-8a8e-c83bfc296766\" class=\"org.photovault.imginfo.xml.PhotoInfoChangeDesc\">" +
            "<prev-change uuid=\"3ed68413-f708-3015-82b8-2854caabdde4\"/>" +
            "<photographer><![CDATA[Harri]]></photographer>" +
            "<FStop>5.6</FStop>" +
            "<filmSpeed>200</filmSpeed>" +
            "<cropBounds xmin=\"0.0\" ymin=\"0.1\" xmax=\"0.3\"  ymax=\"0.5\"/>" +
            "<redColorCurve><point x=\"0.2\" y=\"0.1\"/><point x=\"0.8\" y=\"0.9\"/></redColorCurve>" +
            "<folder-change uuid=\"c144e258-3741-42fe-881b-1be1b0d911d9\" operation=\"add\"/>" +
            "</change>";
    @Test
    public void testChangeDescParsing() throws IOException, SAXException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory hdf = (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        hdf.setSession( session );

        StringInputStream in = new StringInputStream( invalidChange );
        Digester dg = new Digester();
        dg.addRuleSet( new ChangeDescParserRuleSet());
        dg.push( "daofactorystack", hdf );
        PhotoInfoChangeDesc cd = (PhotoInfoChangeDesc) dg.parse(in);
        assert cd.verify();
    }
}
