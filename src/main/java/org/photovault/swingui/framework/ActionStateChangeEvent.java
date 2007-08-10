/*
 * ActionStateChangeEvent.java
 *
 * Created on July 31, 2007, 4:51 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.swingui.framework;

/**
 *
 * @author harri
 */
public class ActionStateChangeEvent extends DefaultEvent<String> {

    private boolean isEnabled;
    
    /** Creates a new instance of ActionStateChangeEvent */
    public ActionStateChangeEvent( AbstractController src, String actionCmd, boolean isEnabled ) {
        super( src, actionCmd );
        this.isEnabled = isEnabled;
    }
    
    String getActionCmd() {
        return getPayload();
    }
    
    boolean isEnabled() {
        return isEnabled;
    }
}
