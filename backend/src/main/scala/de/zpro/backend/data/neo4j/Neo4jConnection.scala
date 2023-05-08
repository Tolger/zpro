package de.zpro.backend.data.neo4j

import org.neo4j.driver.{GraphDatabase, Record => Neo4jRecord}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.ListHasAsScala

// $COVERAGE-OFF$ requires database connection
private object Neo4jConnection {
  def default(implicit context: ExecutionContext): Neo4jConnection = new Neo4jConnectionImpl
}

private trait Neo4jConnection {
  def executeRequest(request: String): Future[List[Neo4jRecord]]
}

private class Neo4jConnectionImpl(implicit val context: ExecutionContext) extends Neo4jConnection {
  private val session = GraphDatabase.driver("bolt://localhost:7687").session

  override def executeRequest(request: String): Future[List[Neo4jRecord]] = Future(
    session.run(request).list().asScala.toList
  )
}
// $COVERAGE-ON$
