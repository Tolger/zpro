package de.zpro.backend.data.repositories

import de.zpro.backend.data.DatabaseRequester
import de.zpro.backend.util.Extensions.OptionIterable

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object RepositoryProvider {
  // $COVERAGE-OFF$ requires database connection
  def apply(connector: DatabaseRequester)(implicit executionContext: ExecutionContext): RepositoryProvider =
    new RepositoryProvider(connector, new PropertyRepository(connector))
  // $COVERAGE-ON$

  def apply(connector: DatabaseRequester, propertyRepository: PropertyRepository)(implicit executionContext: ExecutionContext): RepositoryProvider =
    new RepositoryProvider(connector, propertyRepository)
}

class RepositoryProvider(connector: DatabaseRequester, val propertyRepository: PropertyRepository)(implicit executionContext: ExecutionContext) {
  val dataRepository: DataRepository = buildDataRepository(propertyRepository)
  val searchRepository: SearchRepository = SearchRepository(connector)

  private def buildDataRepository(propertyRepository: PropertyRepository): DataRepository = {
    val properties: List[Property] = Await.result(propertyRepository.loadProperties, 10 seconds)
    val transformers: List[PropertyTransformer] = properties.map(_.transformer).collectDefined
    DataRepository(connector, transformers)
  }
}
