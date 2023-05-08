package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ReturnGeneratorTest extends AnyFlatSpec with should.Matchers {
  "The ReturnGenerator" should "return a simple Node" in {
    val request = RequestObject("name", None, "Test", List("prop1", "prop2", "prop3"), List())
    val response = ReturnGenerator.generateReturnString(request)
    response should be("WITH name RETURN name.prop1 AS prop1, name.prop2 AS prop2, name.prop3 AS prop3")
  }

  it should "return a dogs litter" in {
    val litter = RequestObject("l", Some("litter"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(litter))
    val response = ReturnGenerator.generateReturnString(request)
    response should be("WITH d, {prop1: l.prop1, prop2: l.prop2} AS l RETURN d.prop3 AS prop3, d.prop4 AS prop4, COLLECT(l) AS l")
  }

  it should "return a dogs parents through its litter" in {
    val mother = RequestObject("m", Some("mother"), "Dog", List("prop1", "prop2"), List())
    val father = RequestObject("f", Some("father"), "Dog", List("prop3", "prop4"), List())
    val litter = RequestObject("l", Some("litter"), "Litter", List("prop5", "prop6"), List(mother, father))
    val request = RequestObject("d", None, "Dog", List("prop7", "prop8"), List(litter))
    val response = ReturnGenerator.generateReturnString(request)
    response should be("WITH d, {prop5: l.prop5, prop6: l.prop6} AS l, {prop1: m.prop1, prop2: m.prop2} AS m, {prop3: f.prop3, prop4: f.prop4} AS f " +
      "RETURN d.prop7 AS prop7, d.prop8 AS prop8, COLLECT(l) AS l, COLLECT(m) AS m, COLLECT(f) AS f")
  }
}
