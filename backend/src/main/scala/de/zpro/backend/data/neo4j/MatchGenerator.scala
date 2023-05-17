package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import de.zpro.backend.exceptions.UnknownRelationException
import de.zpro.backend.util.Extensions.TryIterable

import scala.util.{Failure, Success, Try}

private trait MatchGenerator {
  def generateMatchString(from: RequestObject): Try[String]
}

private object MatchGenerator {
  def default: MatchGenerator = MatchGeneratorImpl(ComplexPropertyGenerator)
}

private case class MatchGeneratorImpl(complexGenerator: ComplexPropertyGenerator) extends MatchGenerator {
  override def generateMatchString(from: RequestObject): Try[String] = {
    val simpleFieldsRequest = complexGenerator.filterOutComplex(from)
    val simpleNodes = processNode(simpleFieldsRequest)
    val propertyCalls = complexGenerator.generateComplex(from)
    simpleNodes.map(nodes => s"MATCH $nodes $propertyCalls")
  }

  private def processNode(node: RequestObject, parent: Option[RequestObject] = None): Try[String] =
    (fromObject(node, parent) +: node.children.map(processNode(_, Some(node))))
      .sequence.map(_.mkString(", "))

  private def fromObject(node: RequestObject, parent: Option[RequestObject]): Try[String] =
    handleParent(parent, node).map(_ + singleNode(node))

  private def handleParent(parent: Option[RequestObject], child: RequestObject): Try[String] =
    parent.map(parent => parentConnection(parent, Relation(parent.dataType, child.dataType, child.fieldName.get)))
      .getOrElse(Success(""))

  private def parentConnection(connectedParent: RequestObject, relation: Relation): Try[String] =
    generate(relation).map(relation => s"(${connectedParent.name})$relation")

  private def singleNode(requestObject: RequestObject): String =
    s"(${requestObject.name}:${requestObject.dataType})"

  private def generate(relation: Relation): Try[String] = relation match {
    case Relation("Dog", "Litter", "litter") => Success("-[:BornIn]->")
    case Relation("Dog", "Person", "owner") => Success("-[:Owner]->")
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
