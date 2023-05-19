package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject

private trait ComplexPropertyGenerator {
  def generateComplex(request: RequestObject): String

  def filterOutComplex(request: RequestObject): RequestObject

  val pcFields: List[String] = List("pc", "pcUnique", "pcAll")
  val coiFields: List[String] = List("coi")
  val complexFields: List[String] = pcFields ++ coiFields
}

private object ComplexPropertyGenerator extends ComplexPropertyGenerator {

  override def filterOutComplex(request: RequestObject): RequestObject =
    request.copy(simpleFields = request.simpleFields.diff(complexFields), children = request.children.map(filterOutComplex))

  override def generateComplex(request: RequestObject): String =
    (request +: request.allChildren).flatMap(generateNode).mkString(" ")

  private def generateNode(property: RequestObject): List[String] =
    addPcIfNeeded(property).toList ++ addCoiIfNeeded(property)

  private def addPcIfNeeded(node: RequestObject): Option[String] =
    if (node.simpleFields.intersect(pcFields).isEmpty) None
    else Some(generatePedigreeCollapse(node.name))

  private def addCoiIfNeeded(node: RequestObject): Option[String] =
    if (node.simpleFields.intersect(coiFields).isEmpty) None
    else Some(generateCoefficientOfInbreeding(node.name))

  private def generatePedigreeCollapse(nodeName: String, generations: Int = 5): String =
    s"""CALL {
       |  WITH $nodeName
       |  MATCH ($nodeName)-[:BornIn|Mother|Father*..${generations * 2}]->(ancestor:Dog)
       |  WITH collect(ancestor) AS ancestors, collect(DISTINCT ancestor) AS uniqueAncestors
       |  WITH size(uniqueAncestors) AS nUnique, size(ancestors) AS nAll
       |  RETURN toFloat(nUnique) / nAll AS ${nodeName}_pc, nUnique AS ${nodeName}_pcUnique, nAll AS ${nodeName}_pcAll
       |}""".stripMargin

  private def generateCoefficientOfInbreeding(nodeName: String, generations: Int = 5): String =
    s"""CALL {
       |  WITH $nodeName
       |  MATCH ($nodeName)-[:BornIn]->(litter:Litter)-[:Mother]->(mother:Dog) MATCH (litter)-[:Father]->(father:Dog)
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
       |  RETURN coi AS ${nodeName}_coi
       |}""".stripMargin
}
