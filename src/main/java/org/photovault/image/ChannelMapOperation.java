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

package org.photovault.image;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 This class is the database representation of mapping from input color channels 
 to output cahnnels. It does not contain logic for the actual mapping; this is done
 in {@link ColorCurve}
 <p>
 This class is immutable, new objects should be created using {@link 
 ChannelMapOperationFactory}.
 */
public class ChannelMapOperation {
    
    /**
     * Creates a new instance of ChannelMapOperation. Should not be used by 
     application code, this is purely for OJB.
     */
    public ChannelMapOperation() {
    }
    
    /**
     Map from channel name to an array of control points (Point2D objects)
     */
    Map channelPoints = new HashMap();
    
    /**
     Get names of defined channels
     @return Array of all channel names
     */
    public String[] getChannelNames() {
        return (String[]) channelPoints.keySet().toArray( new String[0] );
    }
    
    /**
     Get the mapping curve for given channel. Note that the curve is a copy, 
     changes to it are not applied to this object.
     @param channel Name of the channel
     @return Mapping curve for the channel.
     */
    public ColorCurve getChannelCurve( String channel ) {
        ColorCurve c = null;
        if ( channelPoints.containsKey( channel ) ) {
            c = new ColorCurve();
            Point2D[] points = (Point2D[]) channelPoints.get( channel );
            for ( int n = 0; n < points.length ; n++ ) {
                Point2D p = points[n];
                c.addPoint( p.getX(), p.getY() );
            }
        }
        return c;
    }
}
