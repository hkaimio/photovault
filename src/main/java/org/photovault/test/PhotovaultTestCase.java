/*
 * PhotovaultTestCase.java
 *
 * Created on 30. lokakuuta 2005, 9:28
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.test;

import junit.framework.TestCase;
import org.photovault.common.JUnitOJBManager;

/**
 * This class extends junit TestCase class so that it sets up the OJB environment
 * 
 * @author Harri Kaimio
 */
public class PhotovaultTestCase extends TestCase {
    
    /** Creates a new instance of PhotovaultTestCase */
    public PhotovaultTestCase() {
        JUnitOJBManager.getOJBManager();
    }

    /**
     *       Sets ut the test environment
     */
    public void setUp() {
    }
    
}
