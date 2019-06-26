package com.typesafe.tools.mima.core

import scala.collection.mutable
import scala.collection.TraversableOnce

class Members(members: TraversableOnce[MemberInfo]) {
  private val bindings = new mutable.HashMap[String, List[MemberInfo]] {
    override def default(key: String) = Nil
  }

  locally {
    for (m <- members)
      bindings += m.bytecodeName -> (m :: bindings(m.bytecodeName))
  }

  def iterator: Iterator[MemberInfo] = for (ms <- bindings.valuesIterator; m <- ms.iterator) yield m

  def get(name: String): Iterator[MemberInfo] = bindings(name).iterator

  def withoutStatic: Members = new Members(iterator.filterNot(_.isStatic))
}

object NoMembers extends Members(Nil)
