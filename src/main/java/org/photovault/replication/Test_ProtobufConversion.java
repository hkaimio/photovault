/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.photovault.replication;

import org.photovault.imginfo.PvProtobufChangeSerializer;
import org.photovault.image.ImageOp.Source;
import org.photovault.image.CropOp;
import org.photovault.image.ColorCurve;
import org.photovault.image.ChanMapOp;
import org.photovault.image.ImageOp.Sink;
import org.photovault.image.DCRawMapOp;
import org.photovault.image.DCRawOp;
import org.photovault.image.ImageOpChain;
import java.util.UUID;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/**
 *
 * @author harri
 */
public class Test_ProtobufConversion {

    @Test
    public void testEmptyConversion() {
        ChangeDTO dto1 = new ChangeDTO();
        dto1.targetUuid = UUID.randomUUID();
        dto1.targetClassName = "org.photovault.imginfo.PhotoInfo";
        dto1.parentIds.add( UUID.randomUUID() );
        dto1.parentIds.add( UUID.randomUUID() );
        ProtobufChangeSerializer s = new ProtobufChangeSerializer();
        byte[] serialized = s.serializeChange( dto1 );
        ChangeDTO dto2 = s.deserializeChange( serialized );
        assertEquals( dto1.targetUuid, dto2.targetUuid );
        assertEquals( dto1.targetClassName, dto2.targetClassName );
        assertEquals( dto1.parentIds.size(), dto2.parentIds.size() );

    }

    @Test
    public void testChangeSerialization() {
        ChangeDTO dto1 = new ChangeDTO();
        dto1.targetUuid = UUID.randomUUID();
        dto1.targetClassName = "org.photovault.imginfo.PhotoInfo";
        dto1.parentIds.add( UUID.randomUUID() );
        dto1.parentIds.add( UUID.randomUUID() );

        ImageOpChain chain = new ImageOpChain();
        DCRawOp op1 = new DCRawOp( chain, "dcraw" );
        op1.setWhite( 32000 );
        op1.setBlack( 30 );
        Source op1out = (Source) op1.getOutputPort( "out" );
        DCRawMapOp op2 = new DCRawMapOp( chain, "op2" );
        op2.setBlack( 25 );
        op2.setWhite( 10000 );
        op2.setEvCorr( -1.0  );
        op2.setHlightCompr( 0.1 );
        Sink op2in = op2.getInputPort( "in" );
        op2in.setSource( op1out );
        Source op2out = op2.getOutputPort( "out" );
        ChanMapOp op3 = new ChanMapOp( chain, "map" );
        ColorCurve sat = new ColorCurve();
        sat.addPoint( 0.0, 0.0 );
        sat.addPoint( 1.0, 0.5 );
        op3.setChannel( "sat", sat );
        Sink op3in = op3.getInputPort( "in" );
        op3in.setSource( op2out );
        Source op3out = op3.getOutputPort( "out" );
        CropOp op4 = new CropOp( chain, "crop" );
        op4.setRot( 2.0 );
        op4.setMaxX( 0.9 );
        op4.setMaxY( 0.8 );
        op4.setMinX( 0.1 );
        op4.setMinY( 0.2 );
        Sink op4in = op4.getInputPort( "in" );
        op4in.setSource( op3out );
        Source op4out = op4.getOutputPort( "out" );
        chain.addOperation( op1 );
        chain.addOperation( op2 );
        chain.addOperation( op3 );
        chain.addOperation( op4 );
        chain.setHead( "out");

        FieldChange fc = new ValueChange( "processing", chain );
        dto1.changedFields.put( "processing", fc );
        ProtobufChangeSerializer s = new PvProtobufChangeSerializer();
        byte[] serialized = s.serializeChange( dto1 );
        ChangeDTO dto2 = s.deserializeChange( serialized );
        assertEquals( dto1.targetUuid, dto2.targetUuid );
        assertEquals( dto1.targetClassName, dto2.targetClassName );
        assertEquals( dto1.parentIds.size(), dto2.parentIds.size() );

    }

}
