package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import de.zpro.backend.exceptions.UnknownRelationException
import de.zpro.backend.util.Extensions.TryIterable

import scala.util.{Failure, Properties, Success, Try}

private trait MatchGenerator {
  def generateMatchString(from: RequestObject, id: Option[String] = None): Try[String]
}

private object MatchGenerator extends MatchGenerator {
  override def generateMatchString(from: RequestObject, id: Option[String]): Try[String] =
    handleChildren(from).map(children =>
      (singleNode(from) +: id.map(id => s"WHERE ${from.name}.id = \"$id\"") ++: children).mkString("MATCH ", Properties.lineSeparator, "")
    )

  private def processNode(node: RequestObject, parent: RequestObject): Try[List[String]] =
    fromObject(node, parent)
      .flatMap(root => handleChildren(node)
        .map(children => root +: children))


  private def handleChildren(parent: RequestObject): Try[List[String]] =
    parent.children.map(processNode(_, parent))
      .sequence.map(_.flatten)

  private def fromObject(node: RequestObject, parent: RequestObject): Try[String] =
    handleParent(parent, node).map(relation => s"OPTIONAL MATCH $relation${singleNode(node)}")

  private def handleParent(parent: RequestObject, child: RequestObject): Try[String] =
    parentConnection(parent, Relation(parent.dataType, child.dataType, child.fieldName.get))

  private def parentConnection(connectedParent: RequestObject, relation: Relation): Try[String] =
    generate(relation).map(relation => s"(${connectedParent.name})$relation")

  private def singleNode(requestObject: RequestObject): String =
    s"(${requestObject.name}:${requestObject.dataType})"

  private def generate(relation: Relation): Try[String] = relation match {
    case Relation("Dog", "Litter", "litter") => Success("-[:BornIn]->")
    case Relation("Dog", "Person", "owner") => Success("-[:Owner]->")
    case Relation("Dog", "Litter", "litters") => Success("<-[:Mother|Father]-")
    case Relation("Litter", "Kennel", "kennel") => Success("-[:BredBy]->")
    case Relation("Litter", "Dog", "offspring") => Success("<-[:BornIn]-")
    case Relation("Litter", "Dog", "mother") => Success("-[:Mother]->")
    case Relation("Litter", "Dog", "father") => Success("-[:Father]->")
    case Relation("Kennel", "Litter", "litters") => Success("<-[:BredBy]-")
    case Relation("Person", "Dog", "dogs") => Success("<-[:Owner]-")
    case _ => Failure(UnknownRelationException(s"Can't determine relation type for unrecognized relation \"$relation\""))
  }

  private case class Relation(parentType: String, childType: String, fieldName: String) {
    override def toString: String = s"$parentType-$fieldName->$childType"
  }
}
