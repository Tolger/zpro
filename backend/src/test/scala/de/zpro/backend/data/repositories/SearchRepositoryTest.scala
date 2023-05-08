package de.zpro.backend.data.repositories

import de.zpro.backend.data.{DatabaseRequester, DbResult}
import org.neo4j.driver.internal.value.{FloatValue, StringValue}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SearchRepositoryTest extends AnyFlatSpec with should.Matchers with MockFactory with ScalaFutures {
  "A SearchRepository" should "perform a sorted quick search" in {
    val requester = mock[DatabaseRequester]
    val repository = SearchRepository(requester)

    val response = List(
      DbResult("SearchResult", Map("score" -> new FloatValue(1.0)), nodes = Map("node" -> List(
        DbResult("Dog", Map("id" -> new StringValue("dog-id"), "fullName" -> new StringValue("Test Dog")))))),
      DbResult("SearchResult", Map("score" -> new FloatValue(2.0)), nodes = Map("node" -> List(
        DbResult("Litter", Map("id" -> new StringValue("litter-id"), "fullName" -> new StringValue("Test Litter")))))),
      DbResult("SearchResult", Map("score" -> new FloatValue(1.5)), nodes = Map("node" -> List(
        DbResult("Kennel", Map("id" -> new StringValue("kennel-id"), "fullName" -> new StringValue("Test Kennel"))))))
    )
    val expectedResult = List(
      QuickSearchResult("litter-id", "Test Litter", 2.0, "Litter"),
      QuickSearchResult("kennel-id", "Test Kennel", 1.5, "Kennel"),
      QuickSearchResult("dog-id", "Test Dog", 1.0, "Dog")
    )

    (requester.quickSearch _).expects("test query query*", 10).returns(Future.successful(response))
    val result = repository.quickSearch("test query")
    whenReady(result) {
      _ should be(expectedResult)
    }
    (requester.quickSearch _).expects("test query*", 10).returns(Future.successful(response))
    val result2 = repository.quickSearch("test query*")
    whenReady(result2) {
      _ should be(expectedResult)
    }
  }

  it should "handle an empty quick search query" in {
    val requester = mock[DatabaseRequester]
    val repository = SearchRepository(requester)

    val result = repository.quickSearch("")
    whenReady(result) {
      _ should be(List.empty)
    }
  }
}
