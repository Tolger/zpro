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

    def collectDefined: CC[A] = coll.filter(_.isDefined).map(_.get)
  }
}
