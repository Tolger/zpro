package de.zpro.backend.data.repositories

import de.zpro.backend.data.{DatabaseRequester, DbResult, RequestObject, RequestObjectParser}
import de.zpro.backend.exceptions.{ParsingException, UnknownChildNameException}
import sangria.schema.ProjectedName

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class DataRepository(requester: DatabaseRequester, transformers: List[PropertyTransformer] = List.empty, parser: RequestObjectParser = RequestObject)(implicit val context: ExecutionContext) {
  def dog(id: String, args: Vector[ProjectedName]): Future[Option[DbResult]] =
    dbRequest("Dog", id, args)

  def litter(id: String, args: Vector[ProjectedName]): Future[Option[DbResult]] =
    dbRequest("Litter", id, args)

  def kennel(id: String, args: Vector[ProjectedName]): Future[Option[DbResult]] =
    dbRequest("Kennel", id, args)

  def person(id: String, args: Vector[ProjectedName]): Future[Option[DbResult]] =
    dbRequest("Person", id, args)

  private def dbRequest(dataType: String, id: String, fields: Vector[ProjectedName]): Future[Option[DbResult]] =
    parser.fromGraphqlFields(dataType, fields) match {
      case Success(request) => performRequest(request, id)
      case Failure(e: UnknownChildNameException) => Future.failed(ParsingException(e.message, e))
      case Failure(e) => Future.failed(e)
    }

  private def performRequest(request: RequestObject, id: String): Future[Option[DbResult]] =
    requester.getNodeById(request, id)
      .map(_.map(processDogs))

  private def processDogs(result: DbResult): DbResult = {
    if (result.dataType == "Dog")
      result.copy(
        properties = transformers.foldLeft(result.properties)((toProcess, processor) => processor.processDog(toProcess)),
        nodes = result.nodes.view.mapValues(_.map(processDogs)).toMap
      )
    else
      result.copy(
        nodes = result.nodes.view.mapValues(_.map(processDogs)).toMap
      )
  }
}
