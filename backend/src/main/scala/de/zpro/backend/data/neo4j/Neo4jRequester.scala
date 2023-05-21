package de.zpro.backend.data.neo4j

import de.zpro.backend.data._
import de.zpro.backend.exceptions.{ParsingException, UnknownRelationException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Neo4jRequester {
  // $COVERAGE-OFF$ requires database connection
  def default(implicit context: ExecutionContext): DatabaseRequester = default(Neo4jConnection.default)
  // $COVERAGE-ON$

  private[neo4j] def default(connection: Neo4jConnection)(implicit context: ExecutionContext): DatabaseRequester =
    new Neo4jRequester(MapComprehensionGenerator, RecordParser, connection)
}

class Neo4jRequester(generator: MapComprehensionGenerator, parser: RecordParser,
                     connection: Neo4jConnection)(implicit private val context: ExecutionContext) extends DatabaseRequester {
  override def getNodes(request: RequestObject): Future[List[DbResult]] =
    run(request)

  override def getNodeById(request: RequestObject, id: String): Future[Option[DbResult]] = // TODO Log warning on multiple
    run(request, Some(id))
      .map(_.headOption)

  override def quickSearch(query: String, limit: Int): Future[List[DbResult]] =
    runQuickSearch(s"CALL db.index.fulltext.queryNodes(\"quickSearchIndex\", \"$query\") YIELD node, score RETURN * LIMIT $limit")

  private def run(details: RequestObject, id: Option[String] = None): Future[List[DbResult]] =
    buildRequest(details, id) match {
      case Success(request) =>
        connection.executeRequest(request)
          .map(records => parser.parseResult(records, details))
      case Failure(e) => Future.failed(e)
    }

  private def buildRequest(details: RequestObject, id: Option[String]): Try[String] =
    generator.generateReturn(details) match {
      case Success(returnString) => Success(
        s"""MATCH (${details.name}:${details.dataType})
           |${id.map(id => s"WHERE ${details.name}.id = \"$id\"").getOrElse("")}
           |$returnString""".stripMargin)
      case Failure(e: UnknownRelationException) => Failure(ParsingException(e.getMessage, e))
      case f: Failure[_] => f
    }

  private def runQuickSearch(request: String): Future[List[DbResult]] = {
    val response = connection.executeRequest(request)
    response.map(records =>
      records.map(record => {
        val result = record.get("node").asNode()
        val node = DbResult(
          dataType = result.labels().iterator().next(),
          properties = Map(
            "id" -> result.get("id"),
            "fullName" -> result.get("fullName")
          )
        )
        DbResult(
          dataType = "SearchResult",
          properties = Map("score" -> record.get("score")),
          nodes = Map("node" -> List(node))
        )
      })
    )
  }
}
