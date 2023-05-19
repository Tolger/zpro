package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject

private trait ReturnGenerator {
  def generateReturnString(from: RequestObject): String
}

private object ReturnGenerator extends ReturnGenerator {
  private val complexProperties = ComplexPropertyGenerator.complexFields

  override def generateReturnString(from: RequestObject): String =
    s"WITH ${generateWithFilter(from)}\r\nRETURN ${generateReturnPhrase(from)}"

  private def generateWithFilter(node: RequestObject): String =
    (node.name +: node.allChildren.map(childObject)).mkString(", ")

  private def childObject(child: RequestObject): String =
    s"{${childProperties(child)}} AS ${child.name}"

  private def childProperties(child: RequestObject): String =
    child.simpleFields.map(generateMapProperty(child.name, _)).mkString(", ")

  private def generateMapProperty(nodeName: String, propertyName: String): String = {
    val link = if (complexProperties.contains(propertyName)) '_' else '.'
    s"$propertyName: $nodeName$link$propertyName"
  }

  private def generateReturnPhrase(node: RequestObject): String =
    (rootNodeProperties(node) +: node.allChildren.map(child => s"COLLECT(${child.name}) AS ${child.name}")).mkString(", ")

  private def rootNodeProperties(node: RequestObject): String =
    node.simpleFields.map(field => s"${node.name}.$field AS $field").mkString(", ")
}
