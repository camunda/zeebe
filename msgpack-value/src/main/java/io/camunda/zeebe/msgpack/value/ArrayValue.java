/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.msgpack.value;

import io.camunda.zeebe.msgpack.spec.MsgPackReader;
import io.camunda.zeebe.msgpack.spec.MsgPackWriter;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import org.agrona.ExpandableArrayBuffer;

public final class ArrayValue<T extends BaseValue> extends BaseValue implements Iterable<T> {
  private final MsgPackWriter writer = new MsgPackWriter();

  // buffer
  private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(10);
  // inner value
  private final T innerValue;
  private final Supplier<T> innerValueFactory;
  private int elementCount;
  private int bufferLength;
  private int oldInnerValueLength;
  private InnerValueState innerValueState;

  // iterator
  private int cursorOffset;

  public ArrayValue(final Supplier<T> innerValueFactory) {
    this.innerValueFactory = innerValueFactory;
    innerValue = innerValueFactory.get();
    reset();
  }

  @Override
  public void reset() {
    elementCount = 0;
    bufferLength = 0;
    resetInnerValue();
  }

  private void resetInnerValue() {
    innerValue.reset();
    oldInnerValueLength = 0;

    innerValueState = InnerValueState.Uninitialized;
  }

  public boolean isEmpty() {
    return elementCount == 0;
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    flushAndResetInnerValue();

    builder.append("[");

    boolean firstElement = true;

    for (final T element : this) {
      if (!firstElement) {
        builder.append(",");
      } else {
        firstElement = false;
      }

      element.writeJSON(builder);
    }

    builder.append("]");
  }

  @Override
  public void write(final MsgPackWriter writer) {
    flushAndResetInnerValue();

    writer.writeArrayHeader(elementCount);
    writer.writeRaw(buffer, 0, bufferLength);
  }

  @Override
  public void read(final MsgPackReader reader) {
    reset();

    elementCount = reader.readArrayHeader();

    writer.wrap(buffer, 0);

    for (int i = 0; i < elementCount; i++) {
      innerValue.read(reader);
      innerValue.write(writer);
    }

    resetInnerValue();

    bufferLength = writer.getOffset();
  }

  @Override
  public int getEncodedLength() {
    flushAndResetInnerValue();
    return MsgPackWriter.getEncodedArrayHeaderLenght(elementCount) + bufferLength;
  }

  /**
   * Please be aware that doing modifications whiles iterating over an {@link ArrayValue} is not
   * thread-safe. Modification will modify the underlying buffer and will lead to exceptions when
   * done multiple threads are accessing this buffer simultaneously.
   *
   * <p>When modifying during iteration make sure to {@link MutableArrayValueIterator#flush} when
   * done.
   *
   * @return an iterator for this object
   */
  @Override
  public MutableArrayValueIterator<T> iterator() {
    flushAndResetInnerValue();
    return new ArrayValueIterator<>(innerValueFactory.get());
  }

  public T add() {
    final boolean elementUpdated = innerValueState == InnerValueState.Modify;
    final int innerValueLength = getInnerValueLength();

    flushAndResetInnerValue();

    elementCount += 1;

    if (elementUpdated) {
      // if the previous element was return by iterator the new element should be added after it
      cursorOffset += innerValueLength;
    }

    innerValueState = InnerValueState.Insert;

    return innerValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(buffer, elementCount, bufferLength);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof final ArrayValue<?> that)) {
      return false;
    }

    return elementCount == that.elementCount
        && bufferLength == that.bufferLength
        && Objects.equals(buffer, that.buffer);
  }

  private int getInnerValueLength() {
    return switch (innerValueState) {
      case Insert, Modify -> innerValue.getEncodedLength();
      default -> 0;
    };
  }

  private void flushAndResetInnerValue() {
    switch (innerValueState) {
      case Insert -> insertInnerValue();
      case Modify -> updateInnerValue();
      default -> {}
    }

    resetInnerValue();
  }

  private void insertInnerValue() {
    final int innerValueLength = innerValue.getEncodedLength();
    moveValuesRight(cursorOffset, innerValueLength);

    writeInnerValue();

    cursorOffset += innerValueLength;
  }

  private void updateInnerValue() {
    final int innerValueLength = innerValue.getEncodedLength();

    if (oldInnerValueLength < innerValueLength) {
      // the inner value length increased
      // move bytes back to have space for updated value
      final int difference = innerValueLength - oldInnerValueLength;
      moveValuesRight(cursorOffset + oldInnerValueLength, difference);
    } else if (oldInnerValueLength > innerValueLength) {
      // the inner value length decreased
      // move bytes to front to fill gap for smaller updated value
      final int difference = oldInnerValueLength - innerValueLength;
      moveValuesLeft(cursorOffset + oldInnerValueLength, difference);
    }

    writeInnerValue();
  }

  private void writeInnerValue() {
    writer.wrap(buffer, cursorOffset);
    innerValue.write(writer);
  }

  private void moveValuesLeft(final int srcOffset, final int removedLength) {
    if (srcOffset <= bufferLength) {
      final int targetOffset = srcOffset - removedLength;
      final int copyLength = bufferLength - srcOffset;
      buffer.putBytes(targetOffset, buffer, srcOffset, copyLength);
    }

    bufferLength -= removedLength;
  }

  private void moveValuesRight(final int srcOffset, final int requiredLength) {
    if (srcOffset < bufferLength) {
      final int targetOffset = srcOffset + requiredLength;
      final int copyLength = bufferLength - srcOffset;
      buffer.putBytes(targetOffset, buffer, srcOffset, copyLength);
    }

    bufferLength += requiredLength;
  }

  enum InnerValueState {
    Uninitialized,
    Insert,
    Modify,
  }

  private class ArrayValueIterator<V extends BaseValue> implements MutableArrayValueIterator<V> {
    private final V iterationValue;
    private final MsgPackReader iterationReader = new MsgPackReader();
    private int cursorOffset;
    private int cursorIndex;
    private int oldIterationValueLength;

    public ArrayValueIterator(final V iterationValue) {
      this.iterationValue = iterationValue;
      cursorOffset = 0;
      cursorIndex = 0;
    }

    @Override
    public boolean hasNext() {
      return cursorIndex < elementCount;
    }

    @Override
    public V next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more elements left");
      }

      iterationReader.wrap(buffer, cursorOffset, bufferLength - cursorOffset);
      iterationValue.read(iterationReader);

      cursorIndex += 1;
      final int iterationValueLength = iterationValue.getEncodedLength();
      cursorOffset += iterationValueLength;
      oldIterationValueLength = iterationValueLength;

      return iterationValue;
    }

    @Override
    public void remove() {
      if (cursorIndex == 0) {
        throw new IllegalStateException("No element available to remove, call next() before");
      }

      final int iterationValueLength = iterationValue.getEncodedLength();

      elementCount -= 1;
      moveValuesLeft(cursorOffset, iterationValueLength);
      innerValueState = InnerValueState.Uninitialized;

      cursorIndex--;
      cursorOffset -= iterationValueLength;
    }

    @Override
    public void flush() {
      final int iterationValueLength = iterationValue.getEncodedLength();
      cursorOffset -= oldIterationValueLength;

      if (oldIterationValueLength < iterationValueLength) {
        // the iteration value length increased
        // move bytes back to have space for updated value
        final int difference = iterationValueLength - oldIterationValueLength;
        moveValuesRight(cursorOffset + oldIterationValueLength, difference);
      } else if (oldIterationValueLength > iterationValueLength) {
        // the iteration value length decreased
        // move bytes to front to fill gap for smaller updated value
        final int difference = oldIterationValueLength - iterationValueLength;
        moveValuesLeft(cursorOffset + oldIterationValueLength, difference);
      }

      writer.wrap(buffer, cursorOffset);
      iterationValue.write(writer);
      cursorOffset += iterationValue.getEncodedLength();
    }
  }
}
