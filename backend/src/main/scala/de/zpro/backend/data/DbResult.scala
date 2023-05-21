package de.zpro.backend.data

import org.neo4j.driver.Value

case class DbResult(dataType: String, properties: Map[String, Value] = Map.empty, listProperties: Map[String, List[Value]] = Map.empty,
                    nodes: Map[String, List[DbResult]] = Map.empty) {
  def apply(key: String): Value = properties(key)

  def get(key: String): Option[Value] = properties.get(key)

  def getList(key: String): List[Value] = listProperties(key)

  def getListOption(key: String): Option[List[Value]] = listProperties.get(key)

  def getListOrEmpty(key: String): List[Value] = listProperties.getOrElse(key, List.empty)

  def getNode(key: String): DbResult = nodes(key).head

  def getNodeOption(key: String): Option[DbResult] = nodes.get(key).flatMap(_.headOption)

  def getNodeList(key: String): List[DbResult] = nodes(key)

  def getNodeListOption(key: String): Option[List[DbResult]] = nodes.get(key)

  def getNodeListOrEmpty(key: String): List[DbResult] = nodes.getOrElse(key, List.empty)
}
