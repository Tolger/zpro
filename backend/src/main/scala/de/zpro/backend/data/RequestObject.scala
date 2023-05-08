package de.zpro.backend.data

import de.zpro.backend.exceptions.UnknownChildNameException
import de.zpro.backend.util.Extensions.TryIterable
import sangria.schema.ProjectedName

import scala.util.{Failure, Success, Try}

trait RequestObjectParser {
  def fromGraphqlFields(dataType: String, fields: Seq[ProjectedName]): Try[RequestObject]
}

private object RequestObject extends RequestObjectParser {
  def simple(dataType: String, fields: List[String]): RequestObject = RequestObject("simple", None, dataType, fields, List.empty)

  override def fromGraphqlFields(dataType: String, fields: Seq[ProjectedName]): Try[RequestObject] = generateFromFields(dataType, fields)

  private def generateFromFields(dataType: String, fields: Seq[ProjectedName], name: String = "n", fieldName: Option[String] = None): Try[RequestObject] = {
    val (simple, children) = fields.partition(_.children.isEmpty)
    generateChildObjects(children, name).map(children => {
      RequestObject(
        name = name,
        fieldName = fieldName,
        dataType = dataType,
        simpleFields = simple.map(_.name).toList,
        children = children
      )
    })
  }

  private def generateChildObjects(children: Seq[ProjectedName], namePrefix: String): Try[List[RequestObject]] =
    generateChildNames(children, namePrefix).map { case (name, details) =>
      dataTypeFromFieldName(details.name).flatMap(dataType =>
        generateFromFields(
          dataType = dataType,
          fields = details.children,
          name = name,
          fieldName = Some(details.name)
        )
      )
    }.toList.sequence

  private def generateChildNames(children: Seq[ProjectedName], prefix: String): Seq[(String, ProjectedName)] =
    children.zipWithIndex.map { case (details, index) =>
      prefix + (index + 97).toChar -> details
    } // 97 = a; only works for up to 26 children per object (currently impossible)

  private def dataTypeFromFieldName(fieldName: String): Try[String] = fieldName match {
    case "litter" | "litters" => Success("Litter")
    case "kennel" => Success("Kennel")
    case "owner" => Success("Person")
    case "offspring" | "mother" | "father" => Success("Dog")
    case _ => Failure(UnknownChildNameException(s"Can't determine data type for unrecognized field name \"$fieldName\""))
  }
}

case class RequestObject(name: String, fieldName: Option[String], dataType: String,
                         simpleFields: List[String] = List.empty, children: List[RequestObject] = List.empty) {
  lazy val allChildren: List[RequestObject] = children ++ children.flatMap(_.allChildren)
}
