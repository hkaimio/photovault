/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.photovault.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.tool.ant.HibernateToolTask;
import org.hibernate.usertype.UserType;
import org.photovault.image.ImageOpChain;
import org.photovault.image.ImageOpDto;

/**
 *
 * @author harri
 */
public class ImageOpChainUserType implements UserType {

    static private int[] types = { Types.BLOB };

    public int[] sqlTypes() {
        return types;
    }

    public Class returnedClass() {
        return ImageOpChain.class;
    }

    public boolean equals( Object x, Object y ) throws HibernateException {
        if ( x == y ) {
            return true;
        }
        if ( x == null || y == null ) {
            return false;
        }
        return x.equals( y );
    }

    public int hashCode( Object obj ) throws HibernateException {
        return obj.hashCode();
    }

    public Object nullSafeGet( ResultSet rs, String[] names, Object owner )
            throws HibernateException, SQLException {
        Blob blob = rs.getBlob( names[0] );
        ImageOpDto.ImageOpChain icp = null;
        try {
            icp = ImageOpDto.ImageOpChain.parseFrom( blob.getBinaryStream() );
        } catch ( IOException e ) {
            throw new HibernateException( e );
        }
        return new ImageOpChain( icp );
    }

    public void nullSafeSet( PreparedStatement stmt, Object value, int index )
            throws HibernateException, SQLException {
        ImageOpChain chain = (ImageOpChain) value;
        byte data[] = chain.getBuilder().build().toByteArray();
        stmt.setBytes( index, data );
    }

    public Object deepCopy( Object obj ) throws HibernateException {
        ImageOpChain chain = (ImageOpChain) obj;
        return obj != null ? new ImageOpChain( chain ) : null;
    }

    public boolean isMutable() {
        return true;
    }

    public Serializable disassemble( Object arg0 ) throws HibernateException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Object assemble( Serializable arg0, Object arg1 ) throws HibernateException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Object replace( Object orig, Object target, Object owner ) throws HibernateException {
        return new ImageOpChain( (ImageOpChain) orig);
    }

}
