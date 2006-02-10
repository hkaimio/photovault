/*
 * ShootingDateComparator.java
 *
 * Created on 29. tammikuuta 2006, 9:32
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.imginfo;

import java.util.Comparator;
import java.util.Date;
    
/**
 * Comparator that orders the photos based on their uid.
 */
public class ShootingDateComparator implements Comparator {
    public int compare( Object o1, Object o2 ) {
        PhotoInfo p1 = (PhotoInfo) o1;
        PhotoInfo p2 = (PhotoInfo) o2;
        Date d1 = p1.getShootTime();
        Date d2 = p2.getShootTime();
        int res = 0;
        if ( d1 != null ) {
            res = d1.compareTo( d2 );
        } else {
            throw new ClassCastException();
        }
        return res;
    }
}
        