package de.zpro.backend.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import de.zpro.backend.data.repositories.RepositoryProvider
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json.{DefaultJsonProtocol, JsObject, JsString}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.util.{Failure, Success}

class GraphQlEndpoint(provider: RepositoryProvider, schemaBuilder: SchemaBuilder)(implicit val context: ExecutionContext)
  extends SprayJsonSupport with DefaultJsonProtocol {
  private val schema = schemaBuilder.buildSchema

  def graphQLEndpoint(requestJson: JsObject): StandardRoute = {
    val JsObject(fields) = requestJson
    processQuery(
      query = fields("query").asInstanceOf[JsString].value,
      operation = fields.get("operationName").collect { case JsString(op) => op },
      vars = fields.get("variables") match {
        case Some(obj: JsObject) => obj
        case _ => JsObject.empty
      })
  }

  def processQuery(query: String, operation: Option[String], vars: JsObject): StandardRoute =
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        complete(executeGraphQLQuery(queryAst, operation, vars))
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
    }

  def executeGraphQLQuery(query: Document, op: Option[String], vars: JsObject): ToResponseMarshallable =
    Executor.execute(schema, query, provider, variables = vars, operationName = op)
      .map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
}
