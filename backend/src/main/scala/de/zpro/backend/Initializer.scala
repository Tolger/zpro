package de.zpro.backend

import de.zpro.backend.data.DatabaseRequester
import de.zpro.backend.data.repositories.RepositoryProvider
import de.zpro.backend.graphql.{GraphQlEndpoint, SchemaBuilder}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class Initializer {
  private val executor = Executors.newCachedThreadPool()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)

  val db: DatabaseRequester = DatabaseRequester.neo4j
  val provider: RepositoryProvider = RepositoryProvider(db)

  val schemaBuilder: SchemaBuilder = new SchemaBuilder(provider)
  val endpoint: GraphQlEndpoint = new GraphQlEndpoint(provider, schemaBuilder)
}
