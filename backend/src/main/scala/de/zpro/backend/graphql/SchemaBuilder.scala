package de.zpro.backend.graphql

import de.zpro.backend.data.repositories.RepositoryProvider
import sangria.schema.Schema

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class SchemaBuilder(provider: RepositoryProvider) {
  def buildSchema: Schema[RepositoryProvider, Unit] = {
    val properties = Await.result(provider.propertyRepository.loadPropertiesForSchema, 10 seconds)
    RequestSchemas.generateSchema(properties)
  }
}
