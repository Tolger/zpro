package de.zpro.backend.data.neo4j

import de.zpro.backend.data.{DbResult, RequestObject}
import de.zpro.backend.util.Extensions.{OptionIterable, OptionMap}
import org.neo4j.driver.internal.value.{ListValue, MapValue, NullValue}
import org.neo4j.driver.{Value, Record => Neo4jRecord}

import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}

private trait RecordParser {
  def parseResult(result: List[Neo4jRecord], details: RequestObject): List[DbResult]
}

private object RecordParser extends RecordParser { // TODO throw errors for unexpected db entries
  override def parseResult(result: List[Neo4jRecord], details: RequestObject): List[DbResult] =
    result.map(record => {
      val fields = record.fields().asScala.map(e => e.key() -> e.value()).toMap
        .filterNot(_._2.isNull)
      processRoot(details, fields)
    }).collectDefined

  private def processRoot(root: RequestObject, fields: Map[String, Value]): Option[DbResult] = {
    fields.get(root.name).map(rootField => processNode(root, valueToNodeMap(rootField)))
  }

  private def processNode(node: RequestObject, fields: Map[String, Value]): DbResult = {
    val (properties, listProperties, childNodes) = separateFields(fields)
    DbResult(
      dataType = node.dataType,
      properties = properties.filter { case (key, _) => node.containsSimpleField(key) },
      listProperties = listProperties.filter { case (key, _) => node.containsSimpleField(key) },
      nodes = processChildNodes(node, childNodes)
    )
  }

  private def processChildNodes(parent: RequestObject, childFields: Map[String, List[Map[String, Value]]]): Map[String, List[DbResult]] =
    childFields
      .map { case (fieldName, childNodes) => fieldName ->
        parent.getChild(fieldName).map(childDetails =>
          childNodes.map(node => processNode(childDetails, node)))
      }.collectDefined

  private def separateFields(fields: Map[String, Value]): (Map[String, Value], Map[String, List[Value]], Map[String, List[Map[String, Value]]]) = {
    val simpleFields = mutable.Map[String, Value]()
    val listFields = mutable.Map[String, List[Value]]()
    val nodeFields = mutable.Map[String, List[Map[String, Value]]]()
    fields.foreach {
      case (name, listValue: ListValue) =>
        val list = listValue.asList[Value](v => v).asScala.toList
        list.headOption match {
          case Some(_: MapValue) => nodeFields += name -> list.map(valueToNodeMap)
          case _ => listFields += name -> list
        }
      case (_, _: NullValue) =>
      case (name, value) =>
        simpleFields += name -> value
    }
    (simpleFields.toMap, listFields.toMap, nodeFields.toMap)
  }

  private def valueToNodeMap(from: Value): Map[String, Value] =
    from.asMap[Value](v => v).asScala.toMap
}
