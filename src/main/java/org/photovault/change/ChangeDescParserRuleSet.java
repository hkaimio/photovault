/*
  Copyright (c) 2008 Harri Kaimio
  
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

package org.photovault.change;

import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import org.apache.commons.digester.CallMethodRule;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;
import org.photovault.image.ColorCurve;
import org.photovault.imginfo.PhotoInfoFields;
import org.photovault.imginfo.xml.RectangleFactory;

/**
 Apache Digester rule set for parsing ChangeDesc XML presentation   
 @author Harri Kaimio
 @since 0.5.0
 */
public class ChangeDescParserRuleSet extends RuleSetBase {

    /**
     Prefix path for the tag for which we are constructing a rule set.
     */     
    String prefix = "";
    
    /**
     Default constructor.
     */
    public ChangeDescParserRuleSet() {
        this("");
    } 
    
    /**
     Construct a rule set for given path
     @param prefix Path prefix for the tag we are constructing a rule set.
     */
    public ChangeDescParserRuleSet( String prefix ) {
        super();
        this.prefix = prefix;
    }

    /**
     Add the rules that consist this rule set to a digester
     @param dg The digester object.
     */
    public void addRuleInstances( Digester dg ) {
        dg.addRule( prefix + "change", new ChangeDescParserRule() );
        dg.addRule( prefix + "change/prev-change", new PrevChangeParserRule() );
        for ( PhotoInfoFields f : EnumSet.allOf( PhotoInfoFields.class ) ) {
            String path = prefix + "change/" + f.getName();
            if ( f.getType() == Rectangle2D.class ) {
                dg.addFactoryCreate(path, new RectangleFactory() );
                dg.addRule( path, new CallMethodRule( 1, "setField", 
                        2, new Class[]{ PhotoInfoFields.class, Rectangle2D.class}));
                dg.addObjectParam(path, 0, f );
                dg.addCallParam( path, 1, true );
            } else if ( f.getType() == ColorCurve.class ) {
                dg.addObjectCreate(path , ColorCurve.class );
                dg.addCallMethod( path + "/point", "addPoint", 
                        2, new Class[]{Double.class, Double.class} );
                dg.addCallParam(path + "/point", 0, "x" );
                dg.addCallParam(path + "/point", 1, "y" );
                dg.addRule( path, new CallMethodRule( 1, "setField", 
                        2, new Class[]{ PhotoInfoFields.class, ColorCurve.class}));
                dg.addObjectParam(path, 0, f );
                dg.addCallParam( path, 1, true );
            } else {
                dg.addCallMethod( path, "setField", 2, new Class[]{ PhotoInfoFields.class, f.getType() });
                dg.addObjectParam(path, 0, f );
                dg.addCallParam( path, 1 );
            }
        }
        
        // Folder association
        
        dg.addCallMethod( prefix + "change/folder-change", "addFolderChange", 
                2, new Class[]{String.class, String.class} );
        dg.addCallParam( prefix + "change/folder-change", 0, "uuid" );
        dg.addCallParam( prefix + "change/folder-change", 1, "operation" );

    }

}
