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

package org.photovault.command;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.context.ManagedSessionContext;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;

/**
  Basic command handler for Photovault.
 */
public class PhotovaultCommandHandler implements CommandHandler {
    
    Session session;
    
    /** Creates a new instance of PhotovaultCommandHandler */
    public PhotovaultCommandHandler( Session session ) {
        this.session = session;
    }

    public Command executeCommand(Command command) throws CommandException {
        command.execute();
        return command;
    }

    public DataAccessCommand executeCommand(DataAccessCommand command) 
            throws CommandException {
        Session commandSession = session;
        boolean shouldCloseSession = false;
        if ( commandSession == null ) {
            commandSession = HibernateUtil.getSessionFactory().openSession();
            shouldCloseSession = true;
        }
        Session oldSession = ManagedSessionContext.bind( (org.hibernate.classic.Session) commandSession);
        command.setDAOFactory( DAOFactory.instance( HibernateDAOFactory.class ) );

        Transaction tx = commandSession.beginTransaction();        
        command.execute();
        commandSession.flush();
        tx.commit();
        if ( shouldCloseSession ) {
            commandSession.close();
        }
        
        if ( oldSession != null ) {
            ManagedSessionContext.bind( (org.hibernate.classic.Session) oldSession);            
        } else {
            ManagedSessionContext.unbind( HibernateUtil.getSessionFactory() );
        }
        return command;
    }
    
}
