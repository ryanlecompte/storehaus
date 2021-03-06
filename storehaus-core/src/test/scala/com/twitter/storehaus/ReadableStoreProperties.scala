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

package com.twitter.storehaus

import com.twitter.util.{ Await, Future }

import org.scalacheck.{ Arbitrary, Properties }
import org.scalacheck.Gen.choose
import org.scalacheck.Prop._

object ReadableStoreProperties extends Properties("ReadableStore") {
    /**
    * multiGet returns Some(Future(None)) for missing keys.
    */
  def multiGetLaw[K: Arbitrary, V: Arbitrary](fn: Map[K, V] => ReadableStore[K, V]) =
    forAll { (m: Map[K, V], others: Set[K]) =>
      val keys = m.keySet
      val expanded: Set[K] = keys ++ others
      val store = fn(m)
      val multiGetReturn = store.multiGet(expanded)
      expanded.forall { k: K => Await.result(store.get(k)) == Await.result(multiGetReturn(k)) }
    }

  /**
    * get and multiGet properly return items from the backing map.
    */
  def properReturnLaws[K: Arbitrary, V: Arbitrary](fn: Map[K, V] => ReadableStore[K, V]) =
    forAll { m: Map[K, V] =>
      val store = fn(m)
      val retM = store.multiGet(m.keySet)
      retM.forall { case (k, v) =>
          (Await.result(store.get(k)) == Await.result(v)) && (m.get(k) == Await.result(v))
      } && m.keySet.forall { k => (Await.result(store.get(k)) == m.get(k)) }
    }

  /**
    * A multiGet's returned map must have the same keyset as the input
    * map.
    */
  def multiGetReturnSizeLaw[K: Arbitrary, V: Arbitrary](fn: Map[K, V] => ReadableStore[K, V]) =
    forAll { m: Map[K, V] =>
      val store = fn(m)
      val keys = m.keySet
      store.multiGet(keys).keySet == keys
    }

  def readableStoreLaws[K: Arbitrary, V: Arbitrary](fn: Map[K, V] => ReadableStore[K, V]) =
    properReturnLaws(fn) && multiGetLaw(fn) && multiGetReturnSizeLaw(fn)

  property("MapStore obeys the ReadableStore laws") =
    readableStoreLaws[Int, String](ReadableStore.fromMap(_))
  property("FunctionStore obeys the ReadableStore laws") =
    readableStoreLaws[Int, String](m => ReadableStore.fromFn(m.get(_)))
}
