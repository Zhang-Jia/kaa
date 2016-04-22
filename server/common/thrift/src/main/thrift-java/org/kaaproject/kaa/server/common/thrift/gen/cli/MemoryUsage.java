/**
 * Autogenerated by Thrift Compiler (0.9.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.kaaproject.kaa.server.common.thrift.gen.cli;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.2)", date = "2016-4-19")
public class MemoryUsage implements org.apache.thrift.TBase<MemoryUsage, MemoryUsage._Fields>, java.io.Serializable, Cloneable, Comparable<MemoryUsage> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("MemoryUsage");

  private static final org.apache.thrift.protocol.TField MAX_FIELD_DESC = new org.apache.thrift.protocol.TField("max", org.apache.thrift.protocol.TType.I64, (short)1);
  private static final org.apache.thrift.protocol.TField TOTAL_FIELD_DESC = new org.apache.thrift.protocol.TField("total", org.apache.thrift.protocol.TType.I64, (short)2);
  private static final org.apache.thrift.protocol.TField FREE_FIELD_DESC = new org.apache.thrift.protocol.TField("free", org.apache.thrift.protocol.TType.I64, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new MemoryUsageStandardSchemeFactory());
    schemes.put(TupleScheme.class, new MemoryUsageTupleSchemeFactory());
  }

  public long max; // required
  public long total; // required
  public long free; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    MAX((short)1, "max"),
    TOTAL((short)2, "total"),
    FREE((short)3, "free");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // MAX
          return MAX;
        case 2: // TOTAL
          return TOTAL;
        case 3: // FREE
          return FREE;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __MAX_ISSET_ID = 0;
  private static final int __TOTAL_ISSET_ID = 1;
  private static final int __FREE_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.MAX, new org.apache.thrift.meta_data.FieldMetaData("max", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64        , "Long")));
    tmpMap.put(_Fields.TOTAL, new org.apache.thrift.meta_data.FieldMetaData("total", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64        , "Long")));
    tmpMap.put(_Fields.FREE, new org.apache.thrift.meta_data.FieldMetaData("free", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64        , "Long")));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(MemoryUsage.class, metaDataMap);
  }

  public MemoryUsage() {
  }

  public MemoryUsage(
    long max,
    long total,
    long free)
  {
    this();
    this.max = max;
    setMaxIsSet(true);
    this.total = total;
    setTotalIsSet(true);
    this.free = free;
    setFreeIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public MemoryUsage(MemoryUsage other) {
    __isset_bitfield = other.__isset_bitfield;
    this.max = other.max;
    this.total = other.total;
    this.free = other.free;
  }

  public MemoryUsage deepCopy() {
    return new MemoryUsage(this);
  }

  @Override
  public void clear() {
    setMaxIsSet(false);
    this.max = 0;
    setTotalIsSet(false);
    this.total = 0;
    setFreeIsSet(false);
    this.free = 0;
  }

  public long getMax() {
    return this.max;
  }

  public MemoryUsage setMax(long max) {
    this.max = max;
    setMaxIsSet(true);
    return this;
  }

  public void unsetMax() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MAX_ISSET_ID);
  }

  /** Returns true if field max is set (has been assigned a value) and false otherwise */
  public boolean isSetMax() {
    return EncodingUtils.testBit(__isset_bitfield, __MAX_ISSET_ID);
  }

  public void setMaxIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MAX_ISSET_ID, value);
  }

  public long getTotal() {
    return this.total;
  }

  public MemoryUsage setTotal(long total) {
    this.total = total;
    setTotalIsSet(true);
    return this;
  }

  public void unsetTotal() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __TOTAL_ISSET_ID);
  }

  /** Returns true if field total is set (has been assigned a value) and false otherwise */
  public boolean isSetTotal() {
    return EncodingUtils.testBit(__isset_bitfield, __TOTAL_ISSET_ID);
  }

  public void setTotalIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __TOTAL_ISSET_ID, value);
  }

  public long getFree() {
    return this.free;
  }

  public MemoryUsage setFree(long free) {
    this.free = free;
    setFreeIsSet(true);
    return this;
  }

  public void unsetFree() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __FREE_ISSET_ID);
  }

  /** Returns true if field free is set (has been assigned a value) and false otherwise */
  public boolean isSetFree() {
    return EncodingUtils.testBit(__isset_bitfield, __FREE_ISSET_ID);
  }

  public void setFreeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __FREE_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case MAX:
      if (value == null) {
        unsetMax();
      } else {
        setMax((Long)value);
      }
      break;

    case TOTAL:
      if (value == null) {
        unsetTotal();
      } else {
        setTotal((Long)value);
      }
      break;

    case FREE:
      if (value == null) {
        unsetFree();
      } else {
        setFree((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case MAX:
      return Long.valueOf(getMax());

    case TOTAL:
      return Long.valueOf(getTotal());

    case FREE:
      return Long.valueOf(getFree());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case MAX:
      return isSetMax();
    case TOTAL:
      return isSetTotal();
    case FREE:
      return isSetFree();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof MemoryUsage)
      return this.equals((MemoryUsage)that);
    return false;
  }

  public boolean equals(MemoryUsage that) {
    if (that == null)
      return false;

    boolean this_present_max = true;
    boolean that_present_max = true;
    if (this_present_max || that_present_max) {
      if (!(this_present_max && that_present_max))
        return false;
      if (this.max != that.max)
        return false;
    }

    boolean this_present_total = true;
    boolean that_present_total = true;
    if (this_present_total || that_present_total) {
      if (!(this_present_total && that_present_total))
        return false;
      if (this.total != that.total)
        return false;
    }

    boolean this_present_free = true;
    boolean that_present_free = true;
    if (this_present_free || that_present_free) {
      if (!(this_present_free && that_present_free))
        return false;
      if (this.free != that.free)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_max = true;
    list.add(present_max);
    if (present_max)
      list.add(max);

    boolean present_total = true;
    list.add(present_total);
    if (present_total)
      list.add(total);

    boolean present_free = true;
    list.add(present_free);
    if (present_free)
      list.add(free);

    return list.hashCode();
  }

  @Override
  public int compareTo(MemoryUsage other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetMax()).compareTo(other.isSetMax());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMax()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.max, other.max);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetTotal()).compareTo(other.isSetTotal());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTotal()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.total, other.total);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetFree()).compareTo(other.isSetFree());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFree()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.free, other.free);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MemoryUsage(");
    boolean first = true;

    sb.append("max:");
    sb.append(this.max);
    first = false;
    if (!first) sb.append(", ");
    sb.append("total:");
    sb.append(this.total);
    first = false;
    if (!first) sb.append(", ");
    sb.append("free:");
    sb.append(this.free);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class MemoryUsageStandardSchemeFactory implements SchemeFactory {
    public MemoryUsageStandardScheme getScheme() {
      return new MemoryUsageStandardScheme();
    }
  }

  private static class MemoryUsageStandardScheme extends StandardScheme<MemoryUsage> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, MemoryUsage struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // MAX
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.max = iprot.readI64();
              struct.setMaxIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // TOTAL
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.total = iprot.readI64();
              struct.setTotalIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // FREE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.free = iprot.readI64();
              struct.setFreeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, MemoryUsage struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(MAX_FIELD_DESC);
      oprot.writeI64(struct.max);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(TOTAL_FIELD_DESC);
      oprot.writeI64(struct.total);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(FREE_FIELD_DESC);
      oprot.writeI64(struct.free);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class MemoryUsageTupleSchemeFactory implements SchemeFactory {
    public MemoryUsageTupleScheme getScheme() {
      return new MemoryUsageTupleScheme();
    }
  }

  private static class MemoryUsageTupleScheme extends TupleScheme<MemoryUsage> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, MemoryUsage struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetMax()) {
        optionals.set(0);
      }
      if (struct.isSetTotal()) {
        optionals.set(1);
      }
      if (struct.isSetFree()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetMax()) {
        oprot.writeI64(struct.max);
      }
      if (struct.isSetTotal()) {
        oprot.writeI64(struct.total);
      }
      if (struct.isSetFree()) {
        oprot.writeI64(struct.free);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, MemoryUsage struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.max = iprot.readI64();
        struct.setMaxIsSet(true);
      }
      if (incoming.get(1)) {
        struct.total = iprot.readI64();
        struct.setTotalIsSet(true);
      }
      if (incoming.get(2)) {
        struct.free = iprot.readI64();
        struct.setFreeIsSet(true);
      }
    }
  }

}

