package de.zpro.backend.util

import scala.collection.IterableOps
import scala.util.{Failure, Success, Try}

object Extensions extends Extensions

trait Extensions {
  implicit class TryIterable[A, CC[B] <: IterableOps[B, CC, CC[B]], T[C] <: Try[C]](val coll: CC[T[A]]) {
    def sequence: Try[CC[A]] =
      coll.iterator.find(_.isFailure) match {
        case Some(Failure(exception)) => Failure(exception)
        case None => Success(coll.map(_.get))
        // $COVERAGE-OFF$ unreachable
        case _ => throw new IllegalStateException()
        // $COVERAGE-ON$
      }
  }

  implicit class OptionIterable[A, CC[B] <: IterableOps[B, CC, CC[B]], O[C] <: Option[C]](val coll: CC[O[A]]) {
    def sequence: Option[CC[A]] =
      if (coll.exists(_.isEmpty)) None
      else Some(coll.map(_.get))

    def collectDefined: CC[A] = coll.collect { case Some(value: A) => value }
  }

  implicit class OptionMap[K, V](val map: Map[K, Option[V]]) {
    def sequence: Option[Map[K, V]] =
      if (map.exists(_._2.isEmpty)) None
      else Some(map.view.mapValues(_.get).toMap)

    def collectDefined: Map[K, V] = map.collect { case (key, Some(value)) => key -> value }
  }

  implicit class OptionString(value: String) {
    def toOption: Option[String] =
      if (value.isEmpty) None
      else Some(value)
  }
}
