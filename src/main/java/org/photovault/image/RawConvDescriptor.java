/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.photovault.image;

import javax.media.jai.CRIFImpl;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.OperationRegistry;
import javax.media.jai.registry.CRIFRegistry;
import javax.media.jai.registry.RIFRegistry;
import javax.media.jai.util.Range;

/**
 *
 * @author harri
 */
public class RawConvDescriptor extends OperationDescriptorImpl {

    // A map-like array of strings with resources information.
  private static final String[][] resources =
    {
      {"GlobalName",   "RawConv"},
      {"LocalName",    "RawConv"},
      {"Vendor",       "org.photovault"},
      {"Description",  "Converting raw images"},
      {"DocURL",       "http://www.photovault.org"},
      {"Version",      "1.0"}
    };
  // An array of strings with the supported modes for this operator.
  private static final String[] supportedModes = {"rendered", "renderable"};
  // An array of strings with the parameter names for this operator.
  private static final String[] paramNames = {
    "white",
    "black",
    "hlightCompr"
  };
  // An array of Classes with the parameters' classes for this operator.
  private static final Class[] paramClasses = {
    Integer.class,
    Integer.class,
    Double.class
  };
  // An array of Objects with the parameters' default values.
  private static final Object[] paramDefaults =  {
    65535,
    0,
    0.0
  };
  // An array of Ranges with ranges of valid parameter values.
  private static final Range[] validParamValues =
    {
      new Range( Integer.class, 0, 65535 ),
      new Range( Integer.class, 0, 65535 ),
      new Range( Double.class, -5.0, 5.0 )
    };

  // The number of sources required for this operator.
  private static final int numSources = 1;
  // A flag that indicates whether the operator is already registered.
  private static boolean registered = false;

 /**
  * The constructor for this descriptor, which just calls the constructor
  * for its ancestral class (OperationDescriptorImpl).
  */
  public RawConvDescriptor()
    {
    super(resources,supportedModes,numSources,paramNames,
          paramClasses,paramDefaults,validParamValues);
    }

 /**
  * A method to register this operator with the OperationRegistry and
  * RIFRegistry.
  */
  public static void register()
    {
    if (!registered)
      {
      // Get the OperationRegistry.
      OperationRegistry op = JAI.getDefaultInstance().getOperationRegistry();
      // Register the operator's descriptor.
      RawConvDescriptor desc =
        new RawConvDescriptor();
      op.registerDescriptor(desc);
      // Register the operators's RIF.
      RawConvRIF rif = new RawConvRIF();
      RIFRegistry.register(op,"RawConv","org.photovault",rif);
      CRIFRegistry.register( op, "RawConv", rif );
      registered = true;
      }
    }


}
