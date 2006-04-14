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

/**
 Event object used to notify @see SchemaUpdateListener objects about update 
 progress.
 */
public class SchemaUpdateEvent {
    
    /** 
     Creates a new instance of SchemaUpdateEvent 
     @param phase The current phase of schema update
     @param percentComplete How fas we are in the current phase (0-100)
     */
    public SchemaUpdateEvent( int phase, int percentComplete ) {
        this.phase = phase;
        this.percentComplete = percentComplete;
    }
    
    int phase = -1;
    int percentComplete = -1;
    
    /**
     Get the current phase of schema update - either 
     @see SchemaUpdateAction.PHASE_ALTER_SCHEMA, 
     @see SchemaUpdateAction.PHASE_CREATING_HASHES or
     @see SchemaUpdateAction.PHASE_COMPLETE.
     */
    public int getPhase() {
        return phase;
    }
    
    /**
     Get how far the currently running phas is (0 - just started, 100 - complete)
     */
    public int getPercentComplete() {
        return percentComplete;
    }
}

    