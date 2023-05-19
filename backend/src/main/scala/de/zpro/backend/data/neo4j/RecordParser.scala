package de.zpro.backend.data.neo4j

import de.zpro.backend.data.{DbResult, RequestObject}
import de.zpro.backend.util.Extensions.OptionIterable
import org.neo4j.driver.internal.value.ListValue
import org.neo4j.driver.{Value, Record => Neo4jRecord}

import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}

private trait RecordParser {
  def parseResult(result: List[Neo4jRecord], details: RequestObject): List[DbResult]
}

private object RecordParser extends RecordParser { // TODO error handling for unexpected db entries
  override def parseResult(result: List[Neo4jRecord], details: RequestObject): List[DbResult] =
    result.map(record => {
      val fields = record.fields().asScala.map(e => e.key() -> e.value()).toMap
        .filterNot(_._2.isNull)
      processRoot(details, fields)
    }).collectDefined

  private def processRoot(root: RequestObject, fields: Map[String, Value]): Option[DbResult] =
    arrangeChildrenInTree(root, extractNodes(root, fields))

  private def separateSimpleFields(fields: Map[String, Value]): (Map[String, Value], Map[String, List[Value]]) = {
    val (properties, propertyLists) = fields.partition {
      case (_, l: ListValue) =>
        false
      case _ =>
        true
    }
    (properties, propertyLists.view.mapValues(_.asList[Value](v => v).asScala.toList).toMap)
  }

  private def extractNodes(root: RequestObject, fields: Map[String, Value]): Map[String, List[Map[String, Value]]] =
    root.allChildren
      .filter(child => fields.contains(child.name))
      .map(child => child.name ->
        fields(child.name).asList[Map[String, Value]](_.asMap[Value](v => v).asScala.toMap).asScala.toList)
      .appended(root.name -> List(fields(root.name).asMap[Value](v => v).asScala.toMap))
      .toMap


  private def arrangeChildrenInTree(rootDetails: RequestObject, allNodesByName: Map[String, List[Map[String, Value]]]): Option[DbResult] = {
    def processNode(nodeDetails: RequestObject, fields: Map[String, Value]): DbResult = {
      val (properties, propertyLists) = separateSimpleFields(fields)
      DbResult(
        dataType = nodeDetails.dataType,
        properties = properties.filterNot(_._2.isNull),
        propertyLists = propertyLists,
        nodes = buildChildNodes(nodeDetails)
      )
    }

    def buildChildNodes(nodeDetails: RequestObject): Map[String, List[DbResult]] =
      nodeDetails.children
        .map(details => details.fieldName.get -> buildTree(details))
        .filter(_._2.isDefined)
        .map(tuple => tuple.copy(_2 = tuple._2.get))
        .toMap

    def buildTree(nodeDetails: RequestObject): Option[List[DbResult]] =
      allNodesByName.get(nodeDetails.name)
        .map(_.map(rootNode => processNode(nodeDetails, rootNode)))


    buildTree(rootDetails).flatMap(_.headOption)
  }
}
