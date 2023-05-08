package de.zpro.backend.data.repositories

import de.zpro.backend.data.DatabaseRequester
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RepositoryProviderTest extends AnyFlatSpec with should.Matchers with MockFactory {
  "A RepositoryProvider" should "provide all repositories" in {
    val requester = mock[DatabaseRequester]
    val propertyRepository = mock[PropertyRepository]

    val properties = List(
      Property("stringProperty", "String", "s", "string", "a string", "Test", None),
      Property("longProperty", "Long", "l", "long", "a long", "Test", None),
      Property("stringEnumProperty1", "Enum-Ordered-String", "eos", "enum-ordered-string", "an enum-ordered-string", "Test", Some(List("option1", "option2", "option3"))),
      Property("longEnumProperty", "Enum-Ordered-Long", "eol", "enum-ordered-long", "an enum-ordered-long", "Test", Some(List("1", "2", "3"))),
      Property("stringEnumProperty2", "Enum-Ordered-String", "eos2", "enum-ordered-string2", "a second enum-ordered-string", "Test", Some(List("option4", "option5", "option6")))
    )
    val transformers = List(
      OrderedStringPropertyTransformer("stringEnumProperty1", List("option1", "option2", "option3")),
      OrderedStringPropertyTransformer("stringEnumProperty2", List("option4", "option5", "option6"))
    )

    (propertyRepository.loadProperties _).expects().returns(Future.successful(properties)).once()
    val provider = RepositoryProvider(requester, propertyRepository)
    provider.propertyRepository should be(propertyRepository)
    provider.searchRepository should be(SearchRepository(requester))
    provider.dataRepository should be(DataRepository(requester, transformers))
  }
}
