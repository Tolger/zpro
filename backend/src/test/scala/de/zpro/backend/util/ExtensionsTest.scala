package de.zpro.backend.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.util.{Failure, Success}

class ExtensionsTest extends AnyFlatSpec with should.Matchers {
  "The Extension" should "sequence a successful List of Trys" in {
    import de.zpro.backend.util.Extensions.TryIterable
    List(Success(1), Success(42), Success(84)).sequence should be(Success(List(1, 42, 84)))
  }

  it should "fail an successful Seq of Trys" in {
    import de.zpro.backend.util.Extensions.TryIterable
    val exception = new Exception("Test")
    Seq(Success(1), Failure(exception), Success(84)).sequence should be(Failure(exception))
  }

  it should "sequence a fully defined Vector of Options" in {
    import de.zpro.backend.util.Extensions.OptionIterable
    Vector(Some(3), Some(42), Some(97)).sequence should be(Some(Vector(3, 42, 97)))
  }

  it should "empty a non fully defined Set of Options" in {
    import de.zpro.backend.util.Extensions.OptionIterable
    Set(Some(3), None, Some(97)).sequence should be(None)
  }

  it should "collect the defined entries in an Iterable of Options" in {
    import de.zpro.backend.util.Extensions.OptionIterable
    Iterable(Some(3), None, Some(97)).collectDefined should be(Iterable(3, 97))
  }

  it should "sequence a fully defined Map of Options" in {
    import de.zpro.backend.util.Extensions.OptionMap
    Map(1 -> Some(3), 2 -> Some(42), 3 -> Some(97)).sequence should be(Some(Map(1 -> 3, 2 -> 42, 3 -> 97)))
  }

  it should "empty a non fully defined Map of Options" in {
    import de.zpro.backend.util.Extensions.OptionMap
    Map(1 -> Some(3), 2 -> None, 3 -> Some(97)).sequence should be(None)
  }

  it should "collect the defined entries in a Map of Options" in {
    import de.zpro.backend.util.Extensions.OptionMap
    Map(1 -> Some(3), 2 -> None, 3 -> Some(97)).collectDefined should be(Map(1 -> 3, 3 -> 97))
  }

  it should "return None for an empty String" in {
    import de.zpro.backend.util.Extensions.OptionString
    "".toOption should be(None)
  }

  it should "return Some for a non empty String" in {
    import de.zpro.backend.util.Extensions.OptionString
    "test".toOption should be(Some("test"))
  }
}
