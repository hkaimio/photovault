/*
  Copyright (c) 2011 Harri Kaimio
  
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

package org.photovault.swingui.taskscheduler;

import org.photovault.swingui.framework.DefaultEvent;
import org.photovault.taskscheduler.BackgroundTask;

/**
 * Event that is sent to indicate that a task has been finished. The task itself
 * is attached as payload.
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class TaskFinishedEvent extends DefaultEvent<BackgroundTask> {
    public TaskFinishedEvent( Object source, BackgroundTask task ) {
        super( source, task );
    }
    
}