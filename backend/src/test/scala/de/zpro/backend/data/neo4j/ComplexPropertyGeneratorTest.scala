package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ComplexPropertyGeneratorTest extends AnyFlatSpec with should.Matchers {
  private def pcResponse(name: String): String =
    s"""CALL {
       |  WITH $name
       |  MATCH ($name)-[:BornIn|Mother|Father*..10]->(ancestor:Dog)
       |  WITH collect(ancestor) AS ancestors, collect(DISTINCT ancestor) AS uniqueAncestors
       |  WITH size(uniqueAncestors) AS nUnique, size(ancestors) AS nAll
       |  RETURN toFloat(nUnique) / nAll AS ${name}_pc, nUnique AS ${name}_pcUnique, nAll AS ${name}_pcAll
       |}""".stripMargin

  private def coiResponse(name: String): String =
    s"""CALL {
       |  WITH $name
       |  MATCH ($name)-[:BornIn]->(litter:Litter)-[:Mother]->(mother:Dog) MATCH (litter)-[:Father]->(father:Dog)
       |  MATCH (mother)-[p1:BornIn|Mother|Father*..10]->(common_ancestor:Dog)
       |  MATCH (father)-[p2:BornIn|Mother|Father*..10]->(common_ancestor:Dog)
       |  WITH DISTINCT collect({p1: p1, p2: p2}) AS ancestorPaths
       |  WITH [path IN ancestorPaths WHERE NOT
       |             ANY(otherPath IN ancestorPaths WHERE otherPath <> path AND
       |                  (ALL(relation IN otherPath.p1 WHERE relation IN path.p1) OR
       |                   ALL(relation IN otherPath.p2 WHERE relation IN path.p2)))] AS ancestorPaths
       |  UNWIND ancestorPaths AS dataPoint
       |  WITH dataPoint.p1 AS p1, dataPoint.p2 as p2
       |  WITH 0.5^((size(p1) + size(p2))/2 + 1) AS n
       |  WITH sum(n) AS coi
       |  RETURN coi AS ${name}_coi
       |}""".stripMargin

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

  it should "only generate a single pedigree collapse call per node" in {
    val request = RequestObject("name", None, "Test", List("pc", "pcUnique", "pcAll", "simpleField", "anotherSimpleField"), List())
    val response = ComplexPropertyGenerator.generateComplex(request)
    response should be(pcResponse("name"))
  }

  it should "generate pedigree collapse calls for child nodes" in {
    val grandchild = RequestObject("grandchild", None, "Test", List("pc", "pcUnique", "pcAll", "simpleField", "anotherSimpleField"), List())
    val child = RequestObject("child", None, "Test", List("pc", "pcUnique", "pcAll", "simpleField", "anotherSimpleField"), List(grandchild))
    val request = RequestObject("name", None, "Test", List("pc", "pcUnique", "pcAll", "simpleField", "anotherSimpleField"), List(child))
    val response = ComplexPropertyGenerator.generateComplex(request)
    val expected = s"${pcResponse("name")} ${pcResponse("child")} ${pcResponse("grandchild")}"
    response should be(expected)
  }

  it should "generate a coefficient of inbreeding call for coi fields" in {
    val request = RequestObject("name", None, "Test", List("coi", "simpleField", "anotherSimpleField"), List())
    val response = ComplexPropertyGenerator.generateComplex(request)
    response should be(coiResponse("name"))
  }
}
