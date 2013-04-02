/*
 * Copyright 2013 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.storehaus.cache

import scala.collection.mutable.{ Map => MutableMap }

object MutableCache {
  def fromMap[K, V](m: MutableMap[K, V]) = new MutableMapCache[K, V](m)
  def filter[K, V](cache: MutableCache[K, V])(pred: ((K, V)) => Boolean) =
    new FilteredMutableCache(cache)(pred)
}

trait MutableCache[K, V] {
  /**
    * Returns an option containing the value stored for the supplied
    * key or None if the key is not present in the cache.
    */
  def get(k: K): Option[V]
  def +=(kv: (K, V)): this.type
  def hit(k: K): Option[V]

  /* Returns an option of the (potentially) evicted value. */
  def evict(k: K): Option[V]

    /**
   * Returns an iterator of all key-value pairs inside of this
   * cache.
   */
  def iterator: Iterator[(K, V)]

  /** Returns an empty version of this specific cache implementation. */
  def empty: MutableCache[K, V]

  def clear: this.type

  /**
   * Returns true if the cache contains a value for the supplied key,
   * false otherwise.
   */
  def contains(k: K): Boolean = get(k).isDefined

  /* Returns the cache with the supplied key evicted. */
  def -=(k: K): this.type = { evict(k); this }

  def getOrElseUpdate(k: K, v: => V): V =
    hit(k).getOrElse {
      val realizedV = v
      this += (k -> realizedV)
      realizedV
    }

  def filter(pred: ((K, V)) => Boolean): MutableCache[K, V] = MutableCache.filter(this)(pred)
}
