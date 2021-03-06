/**
 * Copyright The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.hbase.util.Bytes;
/**
 * Tags are part of cells and helps to add metadata about the KVs.
 * Metadata could be ACLs per cells, visibility labels, etc.
 */
@InterfaceAudience.Private
@InterfaceStability.Evolving
public class Tag {
  public final static int TYPE_LENGTH_SIZE = Bytes.SIZEOF_BYTE;
  public final static int TAG_LENGTH_SIZE = Bytes.SIZEOF_SHORT;
  public final static int INFRASTRUCTURE_SIZE = TYPE_LENGTH_SIZE + TAG_LENGTH_SIZE;

  private final byte type;
  private final byte[] bytes;
  private int offset = 0;
  private short length = 0;

  // The special tag will write the length of each tag and that will be
  // followed by the type and then the actual tag.
  // So every time the length part is parsed we need to add + 1 byte to it to
  // get the type and then get the actual tag.
  public Tag(byte tagType, String tag) {
    this(tagType, Bytes.toBytes(tag));
  }

  /**
   * @param tagType
   * @param tag
   */
  public Tag(byte tagType, byte[] tag) {
    /** <length of tag - 2 bytes><type code - 1 byte><tag>
     * taglength maximum is Short.MAX_SIZE.  It includes 1 byte type length and actual tag bytes length.
     */
    short tagLength = (short) ((tag.length & 0x0000ffff) + TYPE_LENGTH_SIZE);
    length = (short) (TAG_LENGTH_SIZE + tagLength);
    bytes = new byte[length];
    int pos = Bytes.putShort(bytes, 0, tagLength);
    pos = Bytes.putByte(bytes, pos, tagType);
    Bytes.putBytes(bytes, pos, tag, 0, tag.length);
    this.type = tagType;
  }

  /**
   * Creates a Tag from the specified byte array and offset. Presumes
   * <code>bytes</code> content starting at <code>offset</code> is formatted as
   * a Tag blob.
   * The bytes to include the tag type, tag length and actual tag bytes.
   * @param bytes
   *          byte array
   * @param offset
   *          offset to start of Tag
   */
  public Tag(byte[] bytes, int offset) {
    this(bytes, offset, getLength(bytes, offset));
  }

  private static short getLength(byte[] bytes, int offset) {
    return (short) (TAG_LENGTH_SIZE + Bytes.toShort(bytes, offset));
  }

  /**
   * Creates a Tag from the specified byte array, starting at offset, and for
   * length <code>length</code>. Presumes <code>bytes</code> content starting at
   * <code>offset</code> is formatted as a Tag blob.
   * @param bytes
   *          byte array
   * @param offset
   *          offset to start of the Tag
   * @param length
   *          length of the Tag
   */
  public Tag(byte[] bytes, int offset, short length) {
    this.bytes = bytes;
    this.offset = offset;
    this.length = length;
    this.type = bytes[offset + TAG_LENGTH_SIZE];
  }

  /**
   * @return The byte array backing this Tag.
   */
  public byte[] getBuffer() {
    return this.bytes;
  }

  /**
   * @return the tag type
   */
  public byte getType() {
    return this.type;
  }

  /**
   * @return Length of actual tag bytes within the backed buffer
   */
  public int getTagLength() {
    return this.length - INFRASTRUCTURE_SIZE;
  }

  /**
   * @return Offset of actual tag bytes within the backed buffer
   */
  public int getTagOffset() {
    return this.offset + INFRASTRUCTURE_SIZE;
  }

  public byte[] getValue() {
    int tagLength = getTagLength();
    byte[] tag = new byte[tagLength];
    Bytes.putBytes(tag, 0, bytes, getTagOffset(), tagLength);
    return tag;
  }

  /**
   * Creates the list of tags from the byte array b. Expected that b is in the
   * expected tag format
   * @param b
   * @param offset
   * @param length
   * @return List of tags
   */
  public static List<Tag> asList(byte[] b, int offset, int length) {
    List<Tag> tags = new ArrayList<Tag>();
    int pos = offset;
    while (pos < offset + length) {
      short tagLen = Bytes.toShort(b, pos);
      tags.add(new Tag(b, pos, (short) (tagLen + TAG_LENGTH_SIZE)));
      pos += TAG_LENGTH_SIZE + tagLen;
    }
    return tags;
  }

  /**
   * Returns the total length of the entire tag entity
   */
  short getLength() {
    return this.length;
  }

  /**
   * Returns the offset of the entire tag entity
   */
  int getOffset() {
    return this.offset;
  }
}
