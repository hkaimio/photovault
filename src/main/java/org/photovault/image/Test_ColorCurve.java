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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 Unit test cases for {@link ColorCurve}
 */
public class Test_ColorCurve extends TestCase {
    
    /** Creates a new instance of Test_ColorCurve */
    public Test_ColorCurve() {
    }
    

    public static Test suite() {
	return new TestSuite( Test_ColorCurve.class );
    }    
    
    final static double DELTA = 0.00001;
    
    /**
     When there are no control points, should return identity transform
     */
    public void testNoPoints() {
        ColorCurve c = new ColorCurve();
        for ( double d = -1.0 ; d < 2.0 ; d += 0.1 ) {
            assertEquals( "Should have identity transform", d, c.getValue( d ), DELTA );
        }
    }
    
    /**
     For 1 control point, should return constant data
     */
    public void testConstant() {
        ColorCurve c = new ColorCurve();
        c.addPoint( 0.4, 0.2 );
        for ( double d = -1.0 ; d < 2.0 ; d += 0.1 ) {
            assertEquals( "Should have constant value", 0.2, c.getValue( d ), DELTA );
        }
    }

    /**
     If 2 control points, should have straight line between them and constant 
     otherwise.
     */
    public void testStraightLine() {
        ColorCurve c = new ColorCurve();
        c.addPoint( 0.1, 0.2 );
        c.addPoint( 0.8, 0.9 );
        assertEquals( 0.2, c.getValue( -100.0 ), DELTA );
        assertEquals( 0.2, c.getValue( 0.0 ), DELTA );
        assertEquals( 0.2, c.getValue( 0.1 ), DELTA );
        for ( double d = 0.1 ; d <= 0.8 ; d += 0.05 ) {
            assertEquals( d+0.1, c.getValue( d ), DELTA );
        }
        assertEquals( 0.9, c.getValue( 1.0 ), DELTA );
        assertEquals( 0.9, c.getValue( 2.0 ), DELTA );
        assertEquals( 0.9, c.getValue( 2.0E13 ), DELTA );
        
    }


    /**
     Test with 4 control points
     */
    public void testInterpolation() {
        ColorCurve c = new ColorCurve();
        c.addPoint( 0.0, 0.0 );
        c.addPoint( 1.0, 1.0 );
        c.addPoint( 0.5, 0.2 );
        c.addPoint( 0.6, 0.3 );
        
        // Check the derivates near control points
        double y = c.getValue( 0.001 );
        double s1 = y / 0.001;
        double s2 = (c.getY(1)-c.getY(0))/(c.getX(1)-c.getX(0));
        assertEquals( s2, s1, 0.001 );

        y = c.getValue( 0.49 );
        s1 = (c.getY(1) - y )/(c.getX(1)-0.49);
        s2 = (c.getY(2)-c.getY(0))/(c.getX(2)-c.getX(0));
        assertEquals( s2, s1, 0.03 );

        y = c.getValue( 0.45 );
        s1 = (c.getY(1) - y )/(c.getX(1)-0.45);
        s2 = (c.getY(2)-c.getY(0))/(c.getX(2)-c.getX(0));
        assertEquals( s2, s1, 0.03 );
    
    }
    

    /**
     Test moving a control point
     */
    public void testMovePoint() {
        ColorCurve c = new ColorCurve();
        c.addPoint( 0.0, 0.0 );
        c.addPoint( 1.0, 1.0 );
        c.addPoint( 0.5, 0.2 );
        c.addPoint( 0.6, 0.3 );
        assertEquals( 4, c.getPointCount() );
        assertEquals( 0.3, c.getValue( 0.6 ), DELTA );
        c.setPoint( 2, 0.7, 0.4 );
        assertEquals( 0.4, c.getValue( 0.7 ), DELTA );
        assertEquals( 4, c.getPointCount(), DELTA );
        assertEquals( 0.7, c.getX( 2 ), DELTA );
        assertEquals( 0.4, c.getY( 2 ), DELTA );
        
        // Exception should be thrown if set to same x coordinate as other control point
        boolean except = false;
        try {
            c.setPoint( 2, 0.5, 0.1 );
        } catch ( IllegalArgumentException e ) {
            except = true;
        }
        assertTrue( except );
        assertEquals( 4, c.getPointCount() );
        assertEquals( 0.7, c.getX( 2 ), DELTA );
        assertEquals( 0.4, c.getY( 2 ), DELTA );
        
        except = false;
        try {
            c.setPoint( 2, 0.4, 0.1 );
        } catch ( IllegalArgumentException e ) {
            except = true;
        }
        assertTrue( except );
        assertEquals( 4, c.getPointCount() );
        assertEquals( 0.7, c.getX( 2 ), DELTA );
        assertEquals( 0.4, c.getY( 2 ), DELTA );
        
        except = false;
        try {
            c.setPoint( 2, 1.01, 0.1 );
        } catch ( IllegalArgumentException e ) {
            except = true;
        }
        assertTrue( except );
        assertEquals( 4, c.getPointCount() );
        assertEquals( 0.7, c.getX( 2 ) , DELTA);
        assertEquals( 0.4, c.getY( 2 ), DELTA );        
    }
    
    public void testOutOfBounds() {
        ColorCurve c = new ColorCurve();
        boolean except = false;
        try {
            c.setPoint( 0, 0.5, 0.1 );
        } catch ( IndexOutOfBoundsException e ) {
            except = true;
        }
        assertTrue( except );
        c.addPoint( 0.0, 0.0 );
        c.addPoint( 1.0, 1.0 );
        c.addPoint( 0.5, 0.2 );
        c.addPoint( 0.6, 0.3 );
        
        except = false;
        try {
            c.setPoint( -1, 0.5, 0.1 );
        } catch ( IndexOutOfBoundsException e ) {
            except = true;
        }
        assertTrue( except );
        
        except = false;
        try {
            c.setPoint( 4, 0.5, 0.1 );
        } catch ( IndexOutOfBoundsException e ) {
            except = true;
        }
        assertTrue( except );
    }
    
    
    public void testEquals() {
        ColorCurve c = new ColorCurve();
        c.addPoint( 0.0, 0.0 );
        c.addPoint( 1.0, 1.0 );
        c.addPoint( 0.5, 0.2 );
        c.addPoint( 0.6, 0.3 );
        
        ColorCurve c2 = new ColorCurve();
        c2.addPoint( 0.0, 0.0 );
        c2.addPoint( 1.0, 1.0 );
        c2.addPoint( 0.5, 0.2 );

        assertFalse( c2.equals( c ) );
        c2.addPoint( 0.6, 0.3 );
        assertTrue( c2.equals( c ) );
    }
        
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }        
}
