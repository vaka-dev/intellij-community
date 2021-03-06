// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.dataFlow.types;

import com.intellij.codeInsight.Nullability;
import com.intellij.codeInspection.dataFlow.DfaNullability;
import com.intellij.codeInspection.dataFlow.Mutability;
import com.intellij.codeInspection.dataFlow.SpecialField;
import com.intellij.codeInspection.dataFlow.TypeConstraint;
import com.intellij.codeInspection.dataFlow.rangeSet.LongRangeSet;
import com.intellij.codeInspection.dataFlow.value.DfaPsiType;
import com.intellij.psi.PsiType;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

/**
 * Commonly used types and factory methods
 */
public class DfTypes {
  private DfTypes() {}
  
  /**
   * A type that contains every possible value supported by the type system
   */
  public static final DfType TOP = new DfType() {
    @Override
    public boolean isSuperType(@NotNull DfType other) {
      return true;
    }

    @NotNull
    @Override
    public DfType join(@NotNull DfType other) {
      return this;
    }

    @NotNull
    @Override
    public DfType meet(@NotNull DfType other) {
      return other;
    }

    @NotNull
    @Override
    public DfType tryNegate() {
      return BOTTOM;
    }

    @Override
    public int hashCode() {
      return 1254215;
    }

    @Override
    public String toString() {
      return "TOP";
    }
  };

  /**
   * A type that contains no values
   */
  public static final DfType BOTTOM = new DfType() {
    @Override
    public boolean isSuperType(@NotNull DfType other) {
      return other == this;
    }

    @NotNull
    @Override
    public DfType join(@NotNull DfType other) {
      return other;
    }

    @NotNull
    @Override
    public DfType meet(@NotNull DfType other) {
      return this;
    }

    @NotNull
    @Override
    public DfType tryNegate() {
      return TOP;
    }

    @Override
    public int hashCode() {
      return 67532141;
    }

    @Override
    public String toString() {
      return "BOTTOM";
    }
  };

  /**
   * A special value that represents a contract failure after method return (the control flow should immediately proceed 
   * with exception handling). This value is like a constant but it's type doesn't correspond to any JVM type.
   */
  public static final DfType FAIL = new DfConstantType<Object>(ObjectUtils.sentinel("FAIL")) {
    @NotNull
    @Override
    public PsiType getPsiType() {
      return PsiType.VOID;
    }

    @NotNull
    @Override
    public DfType join(@NotNull DfType other) {
      return other == this ? this : TOP;
    }

    @NotNull
    @Override
    public DfType meet(@NotNull DfType other) {
      return other == this ? this : BOTTOM;
    }

    @Override
    public int hashCode() {
      return 5362412;
    }
  };

  /**
   * A type that corresponds to JVM boolean type. Contains two values: true and false
   */
  public static final DfBooleanType BOOLEAN = new DfBooleanType() {
    @Override
    public boolean isSuperType(@NotNull DfType other) {
      return other == BOTTOM || other instanceof DfBooleanType;
    }

    @NotNull
    @Override
    public DfType join(@NotNull DfType other) {
      if (other instanceof DfBooleanType) return this;
      return TOP;
    }

    @NotNull
    @Override
    public DfType meet(@NotNull DfType other) {
      if (other == TOP) return this;
      if (other instanceof DfBooleanType) return other;
      return BOTTOM;
    }

    @NotNull
    @Override
    public DfType tryNegate() {
      return BOTTOM;
    }

    @Override
    public int hashCode() {
      return 345661;
    }

    @Override
    public String toString() {
      return "boolean";
    }
  };

  /**
   * A true boolean constant
   */
  public static final DfBooleanConstantType TRUE = new DfBooleanConstantType(true);

  /**
   * A false boolean constant
   */
  public static final DfBooleanConstantType FALSE = new DfBooleanConstantType(false);

  /**
   * @param value boolean value
   * @return a boolean constant having given value
   */
  public static DfBooleanConstantType booleanValue(boolean value) {
    return value ? TRUE : FALSE;
  }

  /**
   * A type that corresponds to JVM int type
   */
  public static final DfIntType INT = new DfIntRangeType(LongRangeSet.fromType(PsiType.INT));

  /**
   * Creates a type that represents a subset of int values, clamping values not representable in the JVM int type. 
   * 
   * @param range range of values. Values that cannot be represented in JVM int type are removed from this range upon creation.
   * @return resulting type. Might be {@link #BOTTOM} if range is empty or all its values are out of the int domain.
   */
  @NotNull
  public static DfType intRangeClamped(LongRangeSet range) {
    return intRange(range.intersect(DfIntRangeType.FULL_RANGE));
  }

  /**
   * Creates a type that represents a subset of int values.
   *
   * @param range range of values.
   * @return resulting type. Might be {@link #BOTTOM} if range is empty.
   * @throws IllegalArgumentException if range contains values not representable in the JVM int type.
   */
  @NotNull
  public static DfType intRange(LongRangeSet range) {
    if (range.equals(DfIntRangeType.FULL_RANGE)) return INT;
    if (range.isEmpty()) return BOTTOM;
    Long value = range.getConstantValue();
    if (value != null) {
      return intValue(Math.toIntExact(value));
    }
    return new DfIntRangeType(range);
  }

  /**
   * @param value int value
   * @return a int constant type that contains a given value 
   */
  @NotNull
  public static DfIntConstantType intValue(int value) {
    return new DfIntConstantType(value);
  }

  /**
   * A type that corresponds to JVM long type
   */
  public static final DfLongType LONG = new DfLongRangeType(LongRangeSet.all());

  /**
   * Creates a type that represents a subset of long values.
   *
   * @param range range of values.
   * @return resulting type. Might be {@link #BOTTOM} if range is empty.
   */
  @NotNull
  public static DfType longRange(LongRangeSet range) {
    if (range.equals(LongRangeSet.all())) return LONG;
    if (range.isEmpty()) return BOTTOM;
    Long value = range.getConstantValue();
    if (value != null) {
      return longValue(value);
    }
    return new DfLongRangeType(range);
  }

  /**
   * @param value long value
   * @return a long constant type that contains a given value 
   */
  @NotNull
  public static DfLongConstantType longValue(long value) {
    return new DfLongConstantType(value);
  }

  /**
   * A convenience selector method to call {@link #longRange(LongRangeSet)} or {@link #intRangeClamped(LongRangeSet)}
   * @param range range
   * @param isLong whether int or long type should be created
   * @return resulting type.
   */
  @NotNull
  public static DfType rangeClamped(LongRangeSet range, boolean isLong) {
    return isLong ? longRange(range) : intRangeClamped(range);
  }

  /**
   * A type that corresponds to JVM float type
   */
  public static final DfFloatType FLOAT = new DfFloatType() {
    @Override
    public boolean isSuperType(@NotNull DfType other) {
      return other == BOTTOM || other instanceof DfFloatType;
    }

    @NotNull
    @Override
    public DfType join(@NotNull DfType other) {
      if (other instanceof DfFloatType) return this;
      return TOP;
    }

    @NotNull
    @Override
    public DfType meet(@NotNull DfType other) {
      if (other == TOP) return this;
      if (other instanceof DfFloatType) return other;
      return BOTTOM;
    }

    @NotNull
    @Override
    public DfType tryNegate() {
      return BOTTOM;
    }

    @Override
    public int hashCode() {
      return 521441254;
    }

    @Override
    public String toString() {
      return "float";
    }
  };

  /**
   * @param value float value
   * @return a float constant type that contains a given value 
   */
  public static DfFloatConstantType floatValue(float value) {
    return new DfFloatConstantType(value);
  }

  /**
   * A type that corresponds to JVM double type
   */
  public static final DfDoubleType DOUBLE = new DfDoubleType() {
    @Override
    public boolean isSuperType(@NotNull DfType other) {
      return other == BOTTOM || other instanceof DfDoubleType;
    }

    @NotNull
    @Override
    public DfType join(@NotNull DfType other) {
      if (other instanceof DfDoubleType) return this;
      return TOP;
    }

    @NotNull
    @Override
    public DfType meet(@NotNull DfType other) {
      if (other == TOP) return this;
      if (other instanceof DfDoubleType) return other;
      return BOTTOM;
    }

    @NotNull
    @Override
    public DfType tryNegate() {
      return BOTTOM;
    }

    @Override
    public int hashCode() {
      return 5645123;
    }

    @Override
    public String toString() {
      return "double";
    }
  };

  /**
   * @param value double value
   * @return a double constant type that contains a given value 
   */
  public static DfDoubleConstantType doubleValue(double value) {
    return new DfDoubleConstantType(value);
  }

  /**
   * A reference type that contains only null reference
   */
  public static final DfNullConstantType NULL = new DfNullConstantType();

  /**
   * A reference type that contains any reference except null
   */
  public static final DfReferenceType NOT_NULL_OBJECT = 
    customObject(TypeConstraint.empty(), DfaNullability.NOT_NULL, Mutability.UNKNOWN, null, BOTTOM);

  /**
   * A reference type that contains any reference or null
   */
  public static final DfReferenceType OBJECT_OR_NULL = 
    customObject(TypeConstraint.empty(), DfaNullability.UNKNOWN, Mutability.UNKNOWN, null, BOTTOM);

  /**
   * A reference type that contains any reference to a local object
   */
  public static final DfReferenceType LOCAL_OBJECT =
    new DfGenericObjectType(Collections.emptySet(), TypeConstraint.empty(), DfaNullability.NOT_NULL, Mutability.UNKNOWN,
                            null, BOTTOM, true);

  /**
   * Returns a custom constant type
   * 
   * @param constant constant value
   * @param type value type
   * @return a constant type that contains only given constant
   */
  public static DfConstantType<?> constant(@Nullable Object constant, @NotNull DfaPsiType type) {
    if (constant == null) {
      return NULL;
    }
    if (constant instanceof Boolean) {
      return booleanValue((Boolean)constant);
    }
    if (constant instanceof Integer || constant instanceof Short || constant instanceof Byte) {
      return intValue(((Number)constant).intValue());
    }
    if (constant instanceof Character) {
      return intValue((Character)constant);
    }
    if (constant instanceof Long) {
      return longValue((Long)constant);
    }
    if (constant instanceof Float) {
      return floatValue((Float)constant);
    }
    if (constant instanceof Double) {
      return doubleValue((Double)constant);
    }
    return new DfReferenceConstantType(constant, type.getPsiType(), type.asConstraint());
  }

  /**
   * @param type type of the object
   * @param nullability nullability
   * @return a reference type that references given objects of given type (or it subtypes) and has given nullability 
   */
  public static DfReferenceType typedObject(@NotNull DfaPsiType type, @NotNull Nullability nullability) {
    return new DfGenericObjectType(Collections.emptySet(), Objects.requireNonNull(TypeConstraint.empty().withInstanceofValue(type)),
                                   DfaNullability.fromNullability(nullability), Mutability.UNKNOWN, null, BOTTOM, false);
  }

  /**
   * A low-level method to construct a custom reference type. Should not be normally used. Instead prefer construct a type
   * using a series of {@link DfType#meet(DfType)} calls like 
   * <pre>{@code
   * constraint.asDfType()
   *   .meet(mutability.asDfType())
   *   .meet(LOCAL_OBJECT)
   *   .meet(specialField.asDfType(sfType))
   * }</pre>
   * 
   * 
   * @param constraint type constraint
   * @param nullability nullability, must not be {@link DfaNullability#NULL}
   * @param mutability mutability desired mutability
   * @param specialField special field
   * @param sfType type of special field
   * @return a reference type object
   */
  public static DfReferenceType customObject(@NotNull TypeConstraint constraint,
                                             @NotNull DfaNullability nullability,
                                             @NotNull Mutability mutability,
                                             @Nullable SpecialField specialField,
                                             @NotNull DfType sfType) {
    if (nullability == DfaNullability.NULL) {
      throw new IllegalArgumentException();
    }
    return new DfGenericObjectType(Collections.emptySet(), constraint, nullability, mutability, specialField, sfType, false);
  }
}
