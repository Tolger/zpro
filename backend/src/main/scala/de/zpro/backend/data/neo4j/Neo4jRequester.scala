package de.zpro.backend.data.neo4j

import de.zpro.backend.data._
import de.zpro.backend.exceptions.{ParsingException, UnknownRelationException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Neo4jRequester {
  // $COVERAGE-OFF$ requires database connection
  def default(implicit context: ExecutionContext): DatabaseRequester = default(Neo4jConnection.default)
  // $COVERAGE-ON$

  private[neo4j] def default(connection: Neo4jConnection)(implicit context: ExecutionContext): DatabaseRequester =
    new Neo4jRequester(MatchGenerator, ComplexPropertyGenerator, ReturnGenerator, RecordParser, connection)
}

class Neo4jRequester(matchGenerator: MatchGenerator, complexGenerator: ComplexPropertyGenerator,
                     returnGenerator: ReturnGenerator, parser: RecordParser,
                     connection: Neo4jConnection)(implicit private val context: ExecutionContext) extends DatabaseRequester {
  override def getNodes(request: RequestObject): Future[List[DbResult]] =
    run(request, "")

  override def getNodeById(request: RequestObject, id: String): Future[Option[DbResult]] = // TODO Log warning on multiple
    run(request, s"WHERE ${request.name}.id = \"$id\"")
      .map(_.headOption)

  override def quickSearch(query: String, limit: Int): Future[List[DbResult]] =
    runQuickSearch(s"CALL db.index.fulltext.queryNodes(\"quickSearchIndex\", \"$query\") YIELD node, score RETURN * LIMIT $limit")

  private def run(details: RequestObject, whereString: String): Future[List[DbResult]] =
    buildRequest(details, whereString).flatMap(request =>
      connection.executeRequest(request)
        .map(records => parser.parseResult(records, details))
    )

  private def buildRequest(details: RequestObject, whereString: String): Future[String] = {
    val simplePropertiesRequest = complexGenerator.filterOutComplex(details)
    matchGenerator.generateMatchString(simplePropertiesRequest) match {
      case Success(matchString) =>
        val propertyCalls = complexGenerator.generateComplex(details)
        val returnString = returnGenerator.generateReturnString(details)
        Future.successful(
          s"""$matchString
             |$whereString
             |$propertyCalls
             |$returnString""".stripMargin)
      case Failure(e: UnknownRelationException) => Future.failed(ParsingException(e.getMessage, e))
      case Failure(e) => Future.failed(e)
    }
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
