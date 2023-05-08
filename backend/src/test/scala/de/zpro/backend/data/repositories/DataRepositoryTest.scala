package de.zpro.backend.data.repositories

import de.zpro.backend.data.{DatabaseRequester, DbResult, RequestObject, RequestObjectParser}
import de.zpro.backend.exceptions.{ParsingException, UnknownChildNameException}
import org.neo4j.driver.internal.value.{IntegerValue, StringValue}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import sangria.schema.ProjectedName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

class DataRepositoryTest extends AnyFlatSpec with should.Matchers with MockFactory with ScalaFutures {
  "A DataRepository" should "return a simple dog" in {
    val requester = mock[DatabaseRequester]
    val repository = DataRepository(requester)

    val responseValues = Map(
      "name" -> new StringValue("Test"),
      "fullName" -> new StringValue("Test Dog"),
      "comment" -> new StringValue("Test comment")
    )
    val projectedFields = responseValues.keys.map(ProjectedName(_)).toVector
    val dogId = "dog-id"
    val request = RequestObject("n", None, "Dog", responseValues.keys.toList)
    val response = Some(DbResult("Dog", responseValues))

    (requester.getNodeById _).expects(request, dogId).returns(Future.successful(response))
    val result = repository.dog(dogId, projectedFields)
    whenReady(result) {
      _ should be(response)
    }
  }

  it should "return a simple litter" in {
    val requester = mock[DatabaseRequester]
    val repository = DataRepository(requester)

    val responseValues = Map(
      "name" -> new StringValue("Test"),
      "fullName" -> new StringValue("Test Litter"),
      "date" -> new IntegerValue(42)
    )
    val projectedFields = responseValues.keys.map(ProjectedName(_)).toVector
    val litterId = "litter-id"
    val request = RequestObject("n", None, "Litter", responseValues.keys.toList)
    val response = Some(DbResult("Litter", responseValues))

    (requester.getNodeById _).expects(request, litterId).returns(Future.successful(response))
    val result = repository.litter(litterId, projectedFields)
    whenReady(result) {
      _ should be(response)
    }
  }

  it should "return a simple kennel" in {
    val requester = mock[DatabaseRequester]
    val repository = DataRepository(requester)

    val responseValues = Map(
      "name" -> new StringValue("Test"),
      "id" -> new StringValue("kennel-id"),
      "link" -> new StringValue("test.com")
    )
    val projectedFields = responseValues.keys.map(ProjectedName(_)).toVector
    val litterId = "kennel-id"
    val request = RequestObject("n", None, "Kennel", responseValues.keys.toList)
    val response = Some(DbResult("Kennel", responseValues))

    (requester.getNodeById _).expects(request, litterId).returns(Future.successful(response))
    val result = repository.kennel(litterId, projectedFields)
    whenReady(result) {
      _ should be(response)
    }
  }

  it should "return a simple person" in {
    val requester = mock[DatabaseRequester]
    val repository = DataRepository(requester)

    val responseValues = Map(
      "name" -> new StringValue("Test"),
      "email" -> new StringValue("test@test.com"),
      "zip" -> new IntegerValue(42)
    )
    val projectedFields = responseValues.keys.map(ProjectedName(_)).toVector
    val litterId = "person-id"
    val request = RequestObject("n", None, "Person", responseValues.keys.toList)
    val response = Some(DbResult("Person", responseValues))

    (requester.getNodeById _).expects(request, litterId).returns(Future.successful(response))
    val result = repository.person(litterId, projectedFields)
    whenReady(result) {
      _ should be(response)
    }
  }

  it should "return a dog with transformed properties" in {
    val requester = mock[DatabaseRequester]
    val transformers = List(OrderedStringPropertyTransformer("hd", List("A", "B", "C", "D", "E")))
    val repository = DataRepository(requester, transformers)

    val responseValues = Map(
      "name" -> new StringValue("Test"),
      "fullName" -> new StringValue("Test Dog"),
      "hd" -> new IntegerValue(2)
    )
    val projectedFields = responseValues.keys.map(ProjectedName(_)).toVector
    val dogId = "dog-id"
    val request = RequestObject("n", None, "Dog", responseValues.keys.toList)
    val response = Some(DbResult("Dog", responseValues))
    val resultValues = Map(
      "name" -> new StringValue("Test"),
      "fullName" -> new StringValue("Test Dog"),
      "hd" -> new StringValue("C")
    )
    val expectedResult = Some(DbResult("Dog", resultValues))

    (requester.getNodeById _).expects(request, dogId).returns(Future.successful(response))
    val result = repository.dog(dogId, projectedFields)
    whenReady(result) {
      _ should be(expectedResult)
    }
  }

  it should "return a complex dog" in {
    val requester = mock[DatabaseRequester]
    val transformers = List(OrderedStringPropertyTransformer("hd", List("A", "B", "C", "D", "E")))
    val repository = DataRepository(requester, transformers)


    val motherFields = Map(
      "name" -> new StringValue("Mother"),
      "fullName" -> new StringValue("Mother Dog"),
      "hd" -> new IntegerValue(3)
    )
    val fatherFields = Map(
      "name" -> new StringValue("Father"),
      "comment" -> new StringValue("Father comment"),
      "hd" -> new IntegerValue(0)
    )
    val litterFields = Map(
      "name" -> new StringValue("Test"),
      "fullName" -> new StringValue("Test Litter"),
      "date" -> new IntegerValue(42)
    )
    val dogFields = Map(
      "name" -> new StringValue("Test"),
      "fullName" -> new StringValue("Test Dog"),
      "comment" -> new StringValue("Test comment")
    )
    val motherRequest = RequestObject("naa", Some("mother"), "Dog", motherFields.keys.toList)
    val fatherRequest = RequestObject("nab", Some("father"), "Dog", fatherFields.keys.toList)
    val litterRequest = RequestObject("na", Some("litter"), "Litter", litterFields.keys.toList, List(motherRequest, fatherRequest))
    val request = RequestObject("n", None, "Dog", dogFields.keys.toList, List(litterRequest))

    val projectedFields = dogFields.keys.map(ProjectedName(_)).toVector :+
      ProjectedName("litter", litterFields.keys.map(ProjectedName(_)).toVector :+
        ProjectedName("mother", motherFields.keys.map(ProjectedName(_)).toVector) :+
        ProjectedName("father", fatherFields.keys.map(ProjectedName(_)).toVector))
    val dogId = "dog-id"
    val response = Some(DbResult("Dog", dogFields, nodes = Map(
      "litter" -> List(DbResult("Litter", litterFields, nodes = Map(
        "mother" -> List(DbResult("Dog", motherFields)),
        "father" -> List(DbResult("Dog", fatherFields))
      )))
    )))
    val motherResultFields = Map(
      "name" -> new StringValue("Mother"),
      "fullName" -> new StringValue("Mother Dog"),
      "hd" -> new StringValue("D")
    )
    val fatherResultFields = Map(
      "name" -> new StringValue("Father"),
      "comment" -> new StringValue("Father comment"),
      "hd" -> new StringValue("A")
    )
    val expectedResult = Some(DbResult("Dog", dogFields, nodes = Map(
      "litter" -> List(DbResult("Litter", litterFields, nodes = Map(
        "mother" -> List(DbResult("Dog", motherResultFields)),
        "father" -> List(DbResult("Dog", fatherResultFields))
      )))
    )))

    (requester.getNodeById _).expects(request, dogId).returns(Future.successful(response))
    val result = repository.dog(dogId, projectedFields)
    whenReady(result) {
      _ should be(expectedResult)
    }
  }

  it should "should reject invalid child names" in {
    val requester = mock[DatabaseRequester]
    val repository = DataRepository(requester)

    val litterFields = List("name", "fullName", "date")
    val dogFields = List("name", "fullName", "comment")

    val projectedFields = dogFields.map(ProjectedName(_)).toVector :+
      ProjectedName("invalid", litterFields.map(ProjectedName(_)).toVector)
    val dogId = "dog-id"
    val errorMessage = "Can't determine data type for unrecognized field name \"invalid\""
    val exception = UnknownChildNameException(errorMessage)

    val result = repository.dog(dogId, projectedFields)
    whenReady(result.failed) {
      _ should be(ParsingException(errorMessage, exception))
    }
  }

  it should "should pass along an unknown exception" in {
    val requester = mock[DatabaseRequester]
    val parser = mock[RequestObjectParser]
    val repository = DataRepository(requester, parser = parser)

    val dogFields = List("name", "fullName", "comment")
    val projectedFields = dogFields.map(ProjectedName(_)).toVector
    val dogId = "dog-id"
    case class CustomException() extends Exception
    val exception = CustomException()

    (parser.fromGraphqlFields _).expects("Dog", projectedFields).returns(Failure(exception))
    val result = repository.dog(dogId, projectedFields)
    whenReady(result.failed) {
      _ should be(exception)
    }
  }
}
