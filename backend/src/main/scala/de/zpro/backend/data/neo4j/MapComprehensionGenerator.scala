package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import de.zpro.backend.exceptions.UnknownRelationException
import de.zpro.backend.util.Extensions.TryIterable

import scala.util.{Failure, Success, Try}

private trait MapComprehensionGenerator {
  def generateReturn(from: RequestObject): Try[String]
}

private object MapComprehensionGenerator extends MapComprehensionGenerator {
  override def generateReturn(from: RequestObject): Try[String] =
    generateNode(from).map(content => s"RETURN $content")

  private def generateNode(node: RequestObject): Try[String] =
    buildChildren(node).map(children =>
      (printSimpleFields(node) ++ children)
        .mkString(s"${node.name} {", ",", "}"))

  private def buildChildren(node: RequestObject): Try[List[String]] =
    node.children.map(child =>
      buildPattern(node, child).flatMap(pattern =>
        generateNode(child).map(childObj =>
          s"${child.fieldName.get}: [$pattern | $childObj]"
        ))).sequence

  private def buildPattern(parent: RequestObject, child: RequestObject): Try[String] =
    relation(Relation(parent.dataType, child.dataType, child.fieldName.get))
      .map(rel => s"(${parent.name})$rel(${child.name}:${child.dataType})")

  private def printSimpleFields(node: RequestObject): List[String] =
    node.simpleFields.map('.' +: _)

  private def relation(relation: Relation): Try[String] = relation match {
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
