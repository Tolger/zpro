package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject

private trait ComplexPropertyGenerator {
  def generateComplex(request: RequestObject): String

  def filterOutComplex(request: RequestObject): RequestObject
}

private object ComplexPropertyGenerator extends ComplexPropertyGenerator {
  private val pcFields = List("pc", "pcUnique", "pcAll")
  private val complexFields = pcFields

  override def filterOutComplex(request: RequestObject): RequestObject =
    request.copy(simpleFields = request.simpleFields.diff(complexFields), children = request.children.map(filterOutComplex))

  override def generateComplex(request: RequestObject): String =
    (request +: request.allChildren).flatMap(generateNode).mkString(", ")

  private def generateNode(property: RequestObject): List[String] =
    addPcIfNeeded(property).toList

  private def addPcIfNeeded(node: RequestObject): Option[String] =
    if (node.simpleFields.intersect(pcFields).isEmpty) None
    else Some(generatePedigreeCollapse(node.name))

  private def generatePedigreeCollapse(nodeName: String, generations: Int = 5): String =
    s"""
       | CALL {
       |   WITH $nodeName
       |   MATCH ($nodeName)-[:BornIn|Mother|Father*..${generations * 2}]->(ancestor:Dog)
       |   WITH collect(ancestor) AS ancestors, collect(DISTINCT ancestor) AS uniqueAncestors
       |   WITH size(uniqueAncestors) AS nUnique, size(ancestors) AS nAll
       |   RETURN toFloat(nUnique) / nAll AS pc_$nodeName, nUnique AS pcUnique_$nodeName, nAll AS pcAll_$nodeName
       | }
       |""".stripMargin
}
