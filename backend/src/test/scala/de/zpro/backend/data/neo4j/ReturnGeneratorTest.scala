package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ReturnGeneratorTest extends AnyFlatSpec with should.Matchers {
  "The ReturnGenerator" should "return a simple Node" in {
    val request = RequestObject("name", None, "Test", List("prop1", "prop2", "prop3"), List())
    val response = ReturnGenerator.generateReturnString(request)
    response should be(
      """WITH {prop1: name.prop1, prop2: name.prop2, prop3: name.prop3} AS name
        |RETURN name""".stripMargin)
  }

  it should "return a dogs litter" in {
    val litter = RequestObject("l", Some("litter"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(litter))
    val response = ReturnGenerator.generateReturnString(request)
    response should be(
      """WITH {prop3: d.prop3, prop4: d.prop4} AS d, {prop1: l.prop1, prop2: l.prop2} AS l
        |RETURN d, COLLECT(l) AS l""".stripMargin)
  }

  it should "return a dogs parents through its litter" in {
    val mother = RequestObject("m", Some("mother"), "Dog", List("prop1", "prop2"), List())
    val father = RequestObject("f", Some("father"), "Dog", List("prop3", "prop4"), List())
    val litter = RequestObject("l", Some("litter"), "Litter", List("prop5", "prop6"), List(mother, father))
    val request = RequestObject("d", None, "Dog", List("prop7", "prop8"), List(litter))
    val response = ReturnGenerator.generateReturnString(request)
    response should be(
      """WITH {prop7: d.prop7, prop8: d.prop8} AS d, {prop5: l.prop5, prop6: l.prop6} AS l, {prop1: m.prop1, prop2: m.prop2} AS m, {prop3: f.prop3, prop4: f.prop4} AS f
        |RETURN d, COLLECT(l) AS l, COLLECT(m) AS m, COLLECT(f) AS f""".stripMargin)
  }

  it should "return a complex property" in {
    val litter = RequestObject("l", Some("litter"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(litter))
    val response = ReturnGenerator.generateReturnString(request)
    response should be(
      """WITH {prop3: d.prop3, prop4: d.prop4} AS d, {prop1: l.prop1, prop2: l.prop2} AS l
        |RETURN d, COLLECT(l) AS l""".stripMargin)
  }
}
