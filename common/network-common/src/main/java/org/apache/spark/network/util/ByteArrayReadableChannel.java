/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.network.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;

import io.netty.buffer.ByteBuf;

public class ByteArrayReadableChannel implements ReadableByteChannel {
  private final LinkedList<ByteBuf> buffers = new LinkedList<>();

  public int length() {
    if (!buffers.isEmpty()) {
      return buffers.getFirst().readableBytes();
    } else {
      return 0;
    }
  }

  public ByteArrayReadableChannel() {
  }

  public void feedData(ByteBuf buf) {
    buffers.add(buf);
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (!buffers.isEmpty()) {
      ByteBuf first = buffers.getFirst();
      int bytesToRead = Math.min(first.readableBytes(), dst.remaining());
      ByteBuffer src = first.readSlice(bytesToRead).nioBuffer();
      dst.put(src);

      if (first.readableBytes() == 0) {
        buffers.removeFirst().release();
      }

      return bytesToRead;
    }

    return 0;
  }

  @Override
  public void close() throws IOException {
    while (!buffers.isEmpty()) {
      buffers.removeFirst().release();
    }
  }

  @Override
  public boolean isOpen() {
    return true;
  }

}
