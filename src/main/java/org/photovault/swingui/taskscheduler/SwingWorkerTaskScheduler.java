/*
  Copyright (c) 2007 Harri Kaimio
 
  This file is part of Photovault.
 
  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even therrore implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */


package org.photovault.swingui.taskscheduler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import javax.swing.SwingUtilities;
import org.hibernate.Session;
import org.hibernate.context.ManagedSessionContext;
import org.jdesktop.swingworker.SwingWorker;
import org.photovault.command.CommandExecutedEvent;
import org.photovault.command.CommandListener;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.persistence.HibernateUtil;
import org.photovault.swingui.framework.AbstractController;
import org.photovault.taskscheduler.BackgroundTask;
import org.photovault.taskscheduler.TaskProducer;
import org.photovault.taskscheduler.TaskScheduler;

/**
 TaskScheduler implementation that uses SwingWorker to execute the tasks. The 
 tasks are executed in order set by producer priorities - if several TaskProducers 
 share the same priority they are scheduler by round robin algorithm.
 
 <p>
 SwingWorkerTaskScheduler can be associated with an {@link AbstractController}. If
 this is the case the scheduler send a {@link CommandEvent} to it after every 
 command execution.
 */
public class SwingWorkerTaskScheduler implements CommandListener, TaskScheduler {

    /**
     Create new SwingWorkerTaskScheduler
     @parent The controller that owns this scheduler
     */
    public SwingWorkerTaskScheduler( AbstractController parent ) {
        this.parent = parent;
    }    
    /**
     Lowest priority for tasks
     */
    public static int MIN_PRIORITY = 31;
    
    /**
     Parent controller
     */
    AbstractController parent;
    
    /**
     Priorities of each currently registered producer
     */
    Map<TaskProducer, Integer> customerPriorities = new HashMap<TaskProducer, Integer>(  );
    
    /**
     Registered producers by priority
     */
    @SuppressWarnings( value = "unchecked" )
    Queue<TaskProducer>[] waitList = new LinkedList[MIN_PRIORITY + 1];
    
    /**
     Currently active task or <code>null</code> if no task is active
     */
    BackgroundTask activeTask = null;

    /**
     See {@linkTaskScheduler#registerTaskProducer()} for details.
     */
    public void registerTaskProducer( TaskProducer c, int priority  ) {
        if ( priority < 0 || priority > MIN_PRIORITY ) {
            throw new IllegalArgumentException( "Priority must be between 0 and " + MIN_PRIORITY );
        }
        if ( waitList[priority] == null ) {
            waitList[priority] = new LinkedList<TaskProducer>(  );
        }
        Integer oldPriority = null;
        if ( (oldPriority = customerPriorities.put( c, priority )) != null ) {
            waitList[oldPriority].remove( c );
        }
        waitList[priority].add( c );
        if ( activeTask == null ) {
            scheduleNext(  );
        }
    }

    /**
     Called by schduleNext in AWT thread to schedule the selected task for 
     execution.
     @param nextTask The task to execute
     */
    private void runTask( final BackgroundTask nextTask  ) {
        final SwingWorkerTaskScheduler tthis = this;
        SwingWorker worker = new SwingWorker(  ) {

            protected Object doInBackground( ) throws Exception {
                tthis.doRunTask( nextTask );
                return null;
            }

            @Override
            protected void done( ) {
                scheduleNext(  );
            }
        };
        worker.execute(  );
    }

    /**
     Called in worker thread by runTask to actually execute the task. Sets up
     Hibernate environment and runs the task.
     @param task The task to execute
     @throws Exception if the task.run() nethod throws one.
     */
    protected void doRunTask( BackgroundTask task ) throws Exception {
        Session session = HibernateUtil.getSessionFactory(  ).openSession(  );
        Session oldSession = ManagedSessionContext.bind( (org.hibernate.classic.Session) session );
        PhotovaultCommandHandler cmdHandler = new PhotovaultCommandHandler( session );
        cmdHandler.addCommandListener( this );
        task.setSession( session );
        task.setCommandHandler( cmdHandler );
        try {
            task.run(  );
        } catch ( Exception e ) {
            throw e;
        } finally {
            ManagedSessionContext.bind( (org.hibernate.classic.Session) oldSession );
            session.close(  );
        }
    }

    /**
     Select the next task to execute.
     */
    protected synchronized void scheduleNext( ) {
        activeTask = null;
        for ( int n = 0 ; n <= MIN_PRIORITY && activeTask == null ; n++ ) {
            if ( waitList[n] != null ) {
                TaskProducer c;
                while ( (c = waitList[n].poll(  )) != null ) {
                    activeTask = c.requestTask(  );
                    if ( activeTask != null ) {
                        runTask( activeTask );
                        return;
                    }
                }
            }
        }
    }

    /**
    This method is called by command handler in wirker thread when a command
    is executed. It asks the parent controller to fire the event in
    java AWT thread.
    @param e The command event
     */
    public void commandExecuted( final CommandExecutedEvent e ) {
        if ( parent == null ) {
            return;
        }
        SwingUtilities.invokeLater( new Runnable(  ) {

            public void run( ) {
                parent.fireEventGlobal( e );
            }
        } );
    }
}