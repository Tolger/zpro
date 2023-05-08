package de.zpro.backend.data

import de.zpro.backend.data.neo4j.Neo4jRequester

import scala.concurrent.{ExecutionContext, Future}

object DatabaseRequester {
  // $COVERAGE-OFF$ requires database connection
  def neo4j(implicit context: ExecutionContext): DatabaseRequester = Neo4jRequester.default
  // $COVERAGE-ON$
}

trait DatabaseRequester {
  def getNodes(request: RequestObject): Future[List[DbResult]]

  def getNodeById(request: RequestObject, id: String): Future[Option[DbResult]]

  def quickSearch(query: String, limit: Int = 10): Future[List[DbResult]]
}
