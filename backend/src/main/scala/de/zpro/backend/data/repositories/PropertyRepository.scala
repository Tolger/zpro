package de.zpro.backend.data.repositories

import de.zpro.backend.data.{DatabaseRequester, DbResult, RequestObject}
import de.zpro.backend.exceptions.UnknownPropertyTypeException
import de.zpro.backend.util.Extensions.OptionIterable
import org.neo4j.driver.Value
import org.neo4j.driver.internal.value.StringValue
import sangria.schema.{BooleanType, EnumType, EnumValue, LongType, OutputType, StringType}

import scala.collection.immutable.HashSet
import scala.concurrent.{ExecutionContext, Future}

private object PropertyRepository {
  val propertyRequest: RequestObject = RequestObject.simple(
    dataType = "Property",
    fields = List("type", "name", "shortDisplayName", "displayName", "description", "section", "options")
  )
}

case class PropertyRepository(storage: DatabaseRequester)(implicit private val context: ExecutionContext) {
  def loadPropertiesForSchema: Future[List[GraphqlProperty[Any, OutputType[Any]]]] =
    loadProperties.map(_.map[Option[GraphqlProperty[Any, OutputType[Any]]]](property => property.valueType match {
      case "String" => Some(GraphqlProperty(property.name, StringType, _.asString))
      case "Long" | "Date" => Some(GraphqlProperty(property.name, LongType, _.asLong))
      case "Boolean" | "Boolean-Tested" => Some(GraphqlProperty(property.name, BooleanType, _.asBoolean))
      case "Enum-Unordered-Int" | "Enum-Ordered-Int" => Some(GraphqlProperty(
        name = property.name,
        valueType = EnumType(property.name, Some(property.description), property.options.get.map(s => EnumValue(name = s"_$s", value = s.toInt))),
        resolve = _.asInt))
      case "Enum-Unordered-String" | "Enum-Ordered-String" => Some(GraphqlProperty(
        name = property.name,
        valueType = EnumType(property.name, Some(property.description), property.options.get.map(s => EnumValue(name = s, value = s))),
        resolve = _.asString))
    }).collectDefined)

  def loadProperties: Future[List[Property]] =
    getProperties.map(_
      .map(parseProperty)
      .filter(checkValidityAndPrintErrors)
    )

  private def getProperties: Future[List[DbResult]] = {
    storage.getNodes(PropertyRepository.propertyRequest)
  }

  private def parseProperty(data: DbResult): Property = Property(
    name = data("name").asString,
    valueType = data("type").asString,
    shortDisplayName = data("shortDisplayName").asString,
    displayName = data("displayName").asString,
    description = data("description").asString,
    section = data("section").asString,
    options = data.getListOption("options").map(_.map {
      case s: StringValue => s.asString
      case v: Value => v.toString
    })
  )

  private val validTypes = HashSet("String", "Long", "Date", "Boolean", "Boolean-Tested",
    "Enum-Unordered-Int", "Enum-Ordered-Int", "Enum-Unordered-String", "Enum-Ordered-String")

  private def checkValidityAndPrintErrors(property: Property): Boolean = {
    if (validTypes.contains(property.valueType))
      return true
    UnknownPropertyTypeException(s"Can't load property of unknown type \"${property.valueType}\"").printStackTrace()
    false
  }
}

case class Property(name: String, valueType: String, shortDisplayName: String, displayName: String, description: String, section: String, options: Option[List[String]]) {
  lazy val transformer: Option[PropertyTransformer] = valueType match {
    case "Enum-Ordered-String" => Some(OrderedStringPropertyTransformer(name, options.get))
    case _ => None
  }
}

trait PropertyTransformer {
  val name: String

  def processDog(toProcess: Map[String, Value]): Map[String, Value] = toProcess.get(name) match {
    case Some(toTransform) => toProcess + (name -> transform(toTransform))
    case None => toProcess
  }

  def transform(value: Value): Value
}

case class OrderedStringPropertyTransformer(name: String, options: List[String]) extends PropertyTransformer {
  override def transform(value: Value): Value = new StringValue(options(value.asInt))
}

case class GraphqlProperty[U, T <: OutputType[U]](name: String, valueType: T, resolve: Value => U)
