// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: types.proto

package org.photovault.common;

public final class Types {
  private Types() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public static final class UUID extends
      com.google.protobuf.GeneratedMessage {
    // Use UUID.newBuilder() to construct.
    private UUID() {
      initFields();
    }
    private UUID(boolean noInit) {}
    
    private static final UUID defaultInstance;
    public static UUID getDefaultInstance() {
      return defaultInstance;
    }
    
    public UUID getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.photovault.common.Types.internal_static_UUID_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.photovault.common.Types.internal_static_UUID_fieldAccessorTable;
    }
    
    // required fixed64 least_sig_bits = 1;
    public static final int LEAST_SIG_BITS_FIELD_NUMBER = 1;
    private boolean hasLeastSigBits;
    private long leastSigBits_ = 0L;
    public boolean hasLeastSigBits() { return hasLeastSigBits; }
    public long getLeastSigBits() { return leastSigBits_; }
    
    // required fixed64 most_sig_bits = 2;
    public static final int MOST_SIG_BITS_FIELD_NUMBER = 2;
    private boolean hasMostSigBits;
    private long mostSigBits_ = 0L;
    public boolean hasMostSigBits() { return hasMostSigBits; }
    public long getMostSigBits() { return mostSigBits_; }
    
    private void initFields() {
    }
    public final boolean isInitialized() {
      if (!hasLeastSigBits) return false;
      if (!hasMostSigBits) return false;
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasLeastSigBits()) {
        output.writeFixed64(1, getLeastSigBits());
      }
      if (hasMostSigBits()) {
        output.writeFixed64(2, getMostSigBits());
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasLeastSigBits()) {
        size += com.google.protobuf.CodedOutputStream
          .computeFixed64Size(1, getLeastSigBits());
      }
      if (hasMostSigBits()) {
        size += com.google.protobuf.CodedOutputStream
          .computeFixed64Size(2, getMostSigBits());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    public static org.photovault.common.Types.UUID parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static org.photovault.common.Types.UUID parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static org.photovault.common.Types.UUID parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static org.photovault.common.Types.UUID parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static org.photovault.common.Types.UUID parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static org.photovault.common.Types.UUID parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static org.photovault.common.Types.UUID parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static org.photovault.common.Types.UUID parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static org.photovault.common.Types.UUID parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static org.photovault.common.Types.UUID parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(org.photovault.common.Types.UUID prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> {
      private org.photovault.common.Types.UUID result;
      
      // Construct using org.photovault.common.Types.UUID.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new org.photovault.common.Types.UUID();
        return builder;
      }
      
      protected org.photovault.common.Types.UUID internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new org.photovault.common.Types.UUID();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return org.photovault.common.Types.UUID.getDescriptor();
      }
      
      public org.photovault.common.Types.UUID getDefaultInstanceForType() {
        return org.photovault.common.Types.UUID.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public org.photovault.common.Types.UUID build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private org.photovault.common.Types.UUID buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public org.photovault.common.Types.UUID buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        org.photovault.common.Types.UUID returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof org.photovault.common.Types.UUID) {
          return mergeFrom((org.photovault.common.Types.UUID)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(org.photovault.common.Types.UUID other) {
        if (other == org.photovault.common.Types.UUID.getDefaultInstance()) return this;
        if (other.hasLeastSigBits()) {
          setLeastSigBits(other.getLeastSigBits());
        }
        if (other.hasMostSigBits()) {
          setMostSigBits(other.getMostSigBits());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                return this;
              }
              break;
            }
            case 9: {
              setLeastSigBits(input.readFixed64());
              break;
            }
            case 17: {
              setMostSigBits(input.readFixed64());
              break;
            }
          }
        }
      }
      
      
      // required fixed64 least_sig_bits = 1;
      public boolean hasLeastSigBits() {
        return result.hasLeastSigBits();
      }
      public long getLeastSigBits() {
        return result.getLeastSigBits();
      }
      public Builder setLeastSigBits(long value) {
        result.hasLeastSigBits = true;
        result.leastSigBits_ = value;
        return this;
      }
      public Builder clearLeastSigBits() {
        result.hasLeastSigBits = false;
        result.leastSigBits_ = 0L;
        return this;
      }
      
      // required fixed64 most_sig_bits = 2;
      public boolean hasMostSigBits() {
        return result.hasMostSigBits();
      }
      public long getMostSigBits() {
        return result.getMostSigBits();
      }
      public Builder setMostSigBits(long value) {
        result.hasMostSigBits = true;
        result.mostSigBits_ = value;
        return this;
      }
      public Builder clearMostSigBits() {
        result.hasMostSigBits = false;
        result.mostSigBits_ = 0L;
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:UUID)
    }
    
    static {
      defaultInstance = new UUID(true);
      org.photovault.common.Types.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:UUID)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_UUID_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_UUID_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\013types.proto\"5\n\004UUID\022\026\n\016least_sig_bits\030" +
      "\001 \002(\006\022\025\n\rmost_sig_bits\030\002 \002(\006B\032\n\025org.phot" +
      "ovault.common\210\001\000"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_UUID_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_UUID_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_UUID_descriptor,
              new java.lang.String[] { "LeastSigBits", "MostSigBits", },
              org.photovault.common.Types.UUID.class,
              org.photovault.common.Types.UUID.Builder.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}
