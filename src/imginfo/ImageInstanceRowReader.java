// $Id: ImageInstanceRowReader.java,v 1.1 2003/03/01 19:43:17 kaimio Exp $
package imginfo;


import org.apache.ojb.broker.accesslayer.*;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import java.util.Map;

/**
   This class extends the standard OJB object loading so that it initializes the instanceFile field correctly
   as it does not map directly to any database field
*/

public class ImageInstanceRowReader extends RowReaderDefaultImpl {

    public ImageInstanceRowReader( ClassDescriptor cld ) {
	super( cld );
    }
    
    public Object readObjectFrom(Map row ) {
	Object result = super.readObjectFrom(row);
	if ( result instanceof ImageInstance ) {
	    ImageInstance inst = (ImageInstance) result;
	    inst.initFileAttrs();
	}
	return result;
    }
}
