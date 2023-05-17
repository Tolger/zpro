package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ComplexPropertyGeneratorTest extends AnyFlatSpec with should.Matchers {
  private def pcResponse(name: String): String =
    s"""
       | CALL {
       |   WITH $name
       |   MATCH ($name)-[:BornIn|Mother|Father*..10]->(ancestor:Dog)
       |   WITH collect(ancestor) AS ancestors, collect(DISTINCT ancestor) AS uniqueAncestors
       |   WITH size(uniqueAncestors) AS nUnique, size(ancestors) AS nAll
       |   RETURN toFloat(nUnique) / nAll AS pc_$name, nUnique AS pcUnique_$name, nAll AS pcAll_$name
       | }
       |""".stripMargin

  "The ComplexPropertyGenerator" should "generate a pedigree collapse call for pc fields" in {
    val request = RequestObject("name", None, "Test", List("pc", "simpleField", "anotherSimpleField"), List())
    val response = ComplexPropertyGenerator.generateComplex(request)
    response should be(pcResponse("name"))
  }

  it should "generate a pedigree collapse call for pcUnique fields" in {
    val request = RequestObject("name", None, "Test", List("pcUnique", "simpleField", "anotherSimpleField"), List())
    val response = ComplexPropertyGenerator.generateComplex(request)
    response should be(pcResponse("name"))
  }

  it should "generate a pedigree collapse call for pcAll fields" in {
    val request = RequestObject("name", None, "Test", List("pcAll", "simpleField", "anotherSimpleField"), List())
    val response = ComplexPropertyGenerator.generateComplex(request)
    response should be(pcResponse("name"))
  }

  it should "generate pedigree collapse calls for child nodes" in {
    val grandchild = RequestObject("grandchild", None, "Test", List("pc", "pcUnique", "pcAll", "simpleField", "anotherSimpleField"), List())
    val child = RequestObject("child", None, "Test", List("pc", "pcUnique", "pcAll", "simpleField", "anotherSimpleField"), List(grandchild))
    val request = RequestObject("name", None, "Test", List("pc", "pcUnique", "pcAll", "simpleField", "anotherSimpleField"), List(child))
    val response = ComplexPropertyGenerator.generateComplex(request)
    val expected = s"${pcResponse("name")}, ${pcResponse("child")}, ${pcResponse("grandchild")}"
    response should be(expected)
  }
}
