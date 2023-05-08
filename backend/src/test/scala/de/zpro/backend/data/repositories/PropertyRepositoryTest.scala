package de.zpro.backend.data.repositories

import de.zpro.backend.data.{DatabaseRequester, DbResult}
import org.neo4j.driver.internal.value.{BooleanValue, IntegerValue, NullValue, StringValue}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PropertyRepositoryTest extends AnyFlatSpec with should.Matchers with MockFactory with ScalaFutures {
  "A PropertyRepository" should "return properties" in {
    val requester = mock[DatabaseRequester]
    val repository = PropertyRepository(requester)

    val simpleFields = Map(
      "name" -> new StringValue("propertyName"),
      "type" -> new StringValue("Enum-Unordered-String"),
      "shortDisplayName" -> new StringValue("propertyShortDisplayName"),
      "displayName" -> new StringValue("propertyDisplayName"),
      "description" -> new StringValue("propertyDescription"),
      "section" -> new StringValue("propertySection")
    )
    val listFields = Map(
      "options" -> List(new StringValue("option1"), new StringValue("option2"))
    )
    val response = List(DbResult("Property", simpleFields, listFields))
    val expectedResult = List(
      Property(
        "propertyName",
        "Enum-Unordered-String",
        "propertyShortDisplayName",
        "propertyDisplayName",
        "propertyDescription",
        "propertySection",
        Some(List("option1", "option2"))
      )
    )

    (requester.getNodes _).expects(PropertyRepository.propertyRequest).returns(Future.successful(response))
    val result = repository.loadProperties
    whenReady(result) {
      _ should be(expectedResult)
    }
  }

  it should "filter out properties with invalid types" in {
    val requester = mock[DatabaseRequester]
    val repository = PropertyRepository(requester)

    val fields = Map(
      "name" -> new StringValue("propertyName"),
      "type" -> new StringValue("Invalid"),
      "shortDisplayName" -> new StringValue("propertyShortDisplayName"),
      "displayName" -> new StringValue("propertyDisplayName"),
      "description" -> new StringValue("propertyDescription"),
      "section" -> new StringValue("propertySection")
    )
    val response = List(DbResult("Property", fields))

    (requester.getNodes _).expects(PropertyRepository.propertyRequest).returns(Future.successful(response))
    val result = repository.loadProperties
    whenReady(result) {
      _ should be(List.empty[Property])
    }
  }

  it should "return properties for the graphql schema" in {
    val requester = mock[DatabaseRequester]
    val repository = PropertyRepository(requester)

    val properties = List(
      Property(
        "stringProperty",
        "String",
        "stringShortDisplayName",
        "stringDisplayName",
        "stringDescription",
        "stringSection",
        None
      ),
      Property(
        "longProperty",
        "Long",
        "longShortDisplayName",
        "longDisplayName",
        "longDescription",
        "longSection",
        None
      ),
      Property(
        "dateProperty",
        "Date",
        "dateShortDisplayName",
        "dateDisplayName",
        "dateDescription",
        "dateSection",
        None
      ),
      Property(
        "booleanProperty",
        "Boolean",
        "booleanShortDisplayName",
        "booleanDisplayName",
        "booleanDescription",
        "booleanSection",
        None
      ),
      Property(
        "booleanTestedProperty",
        "Boolean-Tested",
        "booleanTestedShortDisplayName",
        "booleanTestedDisplayName",
        "booleanTestedDescription",
        "booleanTestedSection",
        None
      ),
      Property(
        "euiProperty",
        "Enum-Unordered-Int",
        "euiShortDisplayName",
        "euiDisplayName",
        "euiDescription",
        "euiSection",
        Some(List("1", "2", "3"))
      ),
      Property(
        "eoiProperty",
        "Enum-Ordered-Int",
        "eoiShortDisplayName",
        "eoiDisplayName",
        "eoiDescription",
        "eoiSection",
        Some(List("5", "6", "7"))
      ),
      Property(
        "eusProperty",
        "Enum-Unordered-String",
        "eusShortDisplayName",
        "eusDisplayName",
        "eusDescription",
        "eusSection",
        Some(List("A", "B", "C"))
      ),
      Property(
        "eosProperty",
        "Enum-Ordered-String",
        "eosShortDisplayName",
        "eosDisplayName",
        "eosDescription",
        "eosSection",
        Some(List("E", "F", "G"))
      )
    )
    val expectedResult = List(
      GraphqlProperty("stringProperty", StringType, _.asString),
      GraphqlProperty("longProperty", LongType, _.asLong),
      GraphqlProperty("dateProperty", LongType, _.asLong),
      GraphqlProperty("booleanProperty", BooleanType, _.asBoolean),
      GraphqlProperty("booleanTestedProperty", BooleanType, _.asBoolean),
      GraphqlProperty(
        "euiProperty",
        EnumType(
          "euiProperty",
          Some("euiDescription"),
          List(EnumValue("_1", value = 1), EnumValue("_2", value = 2), EnumValue("_3", value = 3))
        ),
        _.asInt
      ),
      GraphqlProperty(
        "eoiProperty",
        EnumType(
          "eoiProperty",
          Some("eoiDescription"),
          List(EnumValue("_5", value = 5), EnumValue("_6", value = 6), EnumValue("_7", value = 7))
        ),
        _.asInt
      ),
      GraphqlProperty(
        "eusProperty",
        EnumType(
          "eusProperty",
          Some("eusDescription"),
          List(EnumValue("A", value = "A"), EnumValue("B", value = "B"), EnumValue("C", value = "C"))
        ),
        _.asString
      ),
      GraphqlProperty(
        "eosProperty",
        EnumType(
          "eosProperty",
          Some("eosDescription"),
          List(EnumValue("E", value = "E"), EnumValue("F", value = "F"), EnumValue("G", value = "G"))
        ),
        _.asString
      )
    )
    val examples = Map(
      "stringProperty" -> new StringValue("A"),
      "longProperty" -> new IntegerValue(42),
      "dateProperty" -> new IntegerValue(75),
      "booleanProperty" -> BooleanValue.TRUE,
      "booleanTestedProperty" -> BooleanValue.FALSE,
      "euiProperty" -> new IntegerValue(2),
      "eoiProperty" -> new IntegerValue(7),
      "eusProperty" -> new StringValue("B"),
      "eosProperty" -> new StringValue("E")
    )
    val response = properties.map(propertyToDb)

    (requester.getNodes _).expects(PropertyRepository.propertyRequest).returns(Future.successful(response))
    val resultFuture = repository.loadPropertiesForSchema
    whenReady(resultFuture) { result =>
      result.map(_.name) should be(expectedResult.map(_.name))
      result.map(_.valueType) should be(expectedResult.map(_.valueType))
      result.map(p => p.resolve(examples(p.name))) should be(expectedResult.map(p => p.resolve(examples(p.name))))
    }
  }

  "A Property" should "generate a transformer for Enum-Ordered-String properties" in {
    val property = Property(
      "test",
      "Enum-Ordered-String",
      "short",
      "name",
      "descr",
      "section",
      Some(List("option1", "option2", "option3"))
    )
    property.transformer.isDefined should be(true)
    val transformer = property.transformer.get
    transformer.name should be("test")
    transformer.transform(new IntegerValue(2)) should be(new StringValue("option3"))
  }

  private def propertyToDb(property: Property): DbResult = {
    val simpleFields = Map(
      "name" -> new StringValue(property.name),
      "type" -> new StringValue(property.valueType),
      "shortDisplayName" -> new StringValue(property.shortDisplayName),
      "displayName" -> new StringValue(property.displayName),
      "description" -> new StringValue(property.description),
      "section" -> new StringValue(property.section)
    )
    val listFields = Map(
      "options" -> property.options.map(_.map(new StringValue(_))).getOrElse(List(NullValue.NULL))
    )
    DbResult("Property", simpleFields, listFields)
  }
}
