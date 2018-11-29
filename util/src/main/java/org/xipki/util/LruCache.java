// #THIRDPARTY#

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.util;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LruCache<K, V> {

  private final LinkedHashMap<K, V> map;

  /** Size of this cache in units. Not necessarily the number of elements. */
  private int size;

  private int maxSize;

  private int putCount;

  private int createCount;

  private int evictionCount;

  private int hitCount;

  private int missCount;

  /**
   * TODO.
   * @param maxSize for caches that do not override {@link #sizeOf}, this is
   *     the maximum number of entries in the cache. For all other caches,
   *     this is the maximum sum of the sizes of the entries in this cache.
   */
  public LruCache(int maxSize) {
    this.maxSize = Args.positive(maxSize, "maxSize");
    this.map = new LinkedHashMap<>(0, 0.75f, true);
  }

  /**
   * Sets the size of the cache.
   *
   * @param maxSize the new maximum size.
   */
  public void resize(int maxSize) {
    synchronized (this) {
      this.maxSize = Args.positive(maxSize, "maxSize");
    }
    trimToSize(maxSize);
  }

  /**
   * Returns the value for {@code key} if it exists in the cache or can be
   * created by {@code #create}. If a value was returned, it is moved to the
   * head of the queue. This returns null if a value is not cached and could not
   * be created.
   *
   * @param key the key the value for {@code key} if it exists in the cache or can be
   *      created by {@code #create}.
   * @return the
   */
  public final V get(K key) {
    if (key == null) {
      throw new NullPointerException("key == null");
    }

    V mapValue;
    synchronized (this) {
      mapValue = map.get(key);
      if (mapValue != null) {
        hitCount++;
        return mapValue;
      }
      missCount++;
    }

    /*
     * Attempt to create a value. This may take a long time, and the map
     * may be different when create() returns. If a conflicting value was
     * added to the map while create() was working, we leave that value in
     * the map and release the created value.
     */

    V createdValue = create(key);
    if (createdValue == null) {
      return null;
    }

    synchronized (this) {
      createCount++;
      mapValue = map.put(key, createdValue);

      if (mapValue != null) {
        // There was a conflict so undo that last put
        map.put(key, mapValue);
      } else {
        size += safeSizeOf(key, createdValue);
      }
    }

    if (mapValue != null) {
      entryRemoved(false, key, createdValue, mapValue);
      return mapValue;
    } else {
      trimToSize(maxSize);
      return createdValue;
    }
  }

  /**
   * Caches {@code value} for {@code key}. The value is moved to the head of
   * the queue.
   * @param key the key.
   * @param value the value.
   * @return the previous value mapped by {@code key}.
   */
  public final V put(K key, V value) {
    if (key == null || value == null) {
      throw new NullPointerException("key == null || value == null");
    }

    V previous;
    synchronized (this) {
      putCount++;
      size += safeSizeOf(key, value);
      previous = map.put(key, value);
      if (previous != null) {
        size -= safeSizeOf(key, previous);
      }
    }

    if (previous != null) {
      entryRemoved(false, key, previous, value);
    }

    trimToSize(maxSize);
    return previous;
  }

  /**
   * Remove the eldest entries until the total of remaining entries is at or
   * below the requested size.
   *
   * @param maxSize the maximum size of the cache before returning. Could be -1
   *            to evict even 0-sized elements.
   */
  public void trimToSize(int maxSize) {
    while (true) {
      K key;
      V value;
      synchronized (this) {
        if (size < 0 || (map.isEmpty() && size != 0)) {
          throw new IllegalStateException(getClass().getName()
              + ".sizeOf() is reporting inconsistent results!");
        }

        if (size <= maxSize || map.isEmpty()) {
          break;
        }

        Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
        key = toEvict.getKey();
        value = toEvict.getValue();
        map.remove(key);
        size -= safeSizeOf(key, value);
        evictionCount++;
      }

      entryRemoved(true, key, value, null);
    }
  }

  /**
   * Removes the entry for {@code key} if it exists.
   * @param key the key
   * @return the previous value mapped by {@code key}.
   */
  public final V remove(K key) {
    if (key == null) {
      throw new NullPointerException("key == null");
    }

    V previous;
    synchronized (this) {
      previous = map.remove(key);
      if (previous != null) {
        size -= safeSizeOf(key, previous);
      }
    }

    if (previous != null) {
      entryRemoved(false, key, previous, null);
    }

    return previous;
  }

  /**
   * Called for entries that have been evicted or removed. This method is
   * invoked when a value is evicted to make space, removed by a call to
   * {@link #remove}, or replaced by a call to {@link #put}. The default
   * implementation does nothing.
   *
   * <p>The method is called without synchronization: other threads may
   * access the cache while this method is executing.
   *
   * @param evicted true if the entry is being removed to make space, false
   *     if the removal was caused by a {@link #put} or {@link #remove}.
   * @param key the key
   * @param oldValue the old value of the {@code key}
   * @param newValue the new value for {@code key}, if it exists. If non-null,
   *     this removal was caused by a {@link #put}. Otherwise it was caused by
   *     an eviction or a {@link #remove}.
   */
  protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {
  }

  /**
   * Called after a cache miss to compute a value for the corresponding key.
   * Returns the computed value or null if no value can be computed. The
   * default implementation returns null.
   *
   * <p>The method is called without synchronization: other threads may
   * access the cache while this method is executing.
   *
   * <p>If a value for {@code key} exists in the cache when this method
   * returns, the created value will be released with {@link #entryRemoved}
   * and discarded. This can occur when multiple threads request the same key
   * at the same time (causing multiple values to be created), or when one
   * thread calls {@link #put} while another is creating a value for the same
   * key.
   *
   * @param key the key
   * @return the created computed value or null if no value can be computed
   */
  protected V create(K key) {
    return null;
  }

  private int safeSizeOf(K key, V value) {
    int result = sizeOf(key, value);
    if (result < 0) {
      throw new IllegalStateException("Negative size: " + key + "=" + value);
    }
    return result;
  }

  /**
   * Returns the size of the entry for {@code key} and {@code value} in
   * user-defined units. The default implementation returns 1 so that size
   * is the number of entries and max size is the maximum number of entries.
   *
   * <p>An entry's size may not change while it is in the cache.
   * @param key the key
   * @param value the value
   * @return he size of the entry for {@code key} and {@code value} in
   *      user-defined units
   */
  protected int sizeOf(K key, V value) {
    return 1;
  }

  /**
   * Clear the cache, calling {@link #entryRemoved} on each removed entry.
   */
  public final void evictAll() {
    trimToSize(-1); // -1 will evict 0-sized elements
  }

  /**
   * For caches that do not override {@link #sizeOf}, this returns the number
   * of entries in the cache. For all other caches, this returns the sum of
   * the sizes of the entries in this cache.
   * @return the size
   */
  public final synchronized int size() {
    return size;
  }

  /**
   * For caches that do not override {@link #sizeOf}, this returns the maximum
   * number of entries in the cache. For all other caches, this returns the
   * maximum sum of the sizes of the entries in this cache.
   * @return the maximal size
   */
  public final synchronized int maxSize() {
    return maxSize;
  }

  /**
   * Returns the number of times {@link #get} returned a value that was
   * already present in the cache.
   * @return the number of times
   */
  public final synchronized int hitCount() {
    return hitCount;
  }

  /**
   * Returns the number of times {@link #get} returned null or required a new
   * value to be created.
   * @return the number of times returned null or required a new value to be
   *         created
   */
  public final synchronized int missCount() {
    return missCount;
  }

  /**
   * Returns the number of times {@link #create(Object)} returned a value.
   * @return the number of times {@link #create(Object)} returned a value.
   */
  public final synchronized int createCount() {
    return createCount;
  }

  /**
   * Returns the number of times {@link #put} was called.
   * @return the number of times {@link #put} was called.
   */
  public final synchronized int putCount() {
    return putCount;
  }

  /**
   * Returns the number of values that have been evicted.
   * @return the number of values that have been evicted.
   */
  public final synchronized int evictionCount() {
    return evictionCount;
  }

  /**
   * TODO.
   * @return a copy of the current contents of the cache, ordered from least
   *      recently accessed to most recently accessed.
   */
  public final synchronized Map<K, V> snapshot() {
    return new LinkedHashMap<>(map);
  }

  /**
   * TODO.
   * @return a copy of the keys of the current contents of the cache.
   */
  public final synchronized Set<K> keySnapshot() {
    return new HashSet<>(map.keySet());
  }

  @Override
  public final synchronized String toString() {
    int accesses = hitCount + missCount;
    int hitPercent = (accesses == 0) ? 0 : (100 * hitCount / accesses);
    return String.format("LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]",
        maxSize, hitCount, missCount, hitPercent);
  }

}
