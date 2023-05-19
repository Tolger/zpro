package de.zpro.backend.data.neo4j

import de.zpro.backend.data.{DbResult, RequestObject}
import de.zpro.backend.exceptions.{ParsingException, UnknownRelationException}
import org.neo4j.driver.Value
import org.neo4j.driver.internal.value._
import org.neo4j.driver.internal.{InternalNode, InternalRecord}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.util.{List => JavaList}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}
import scala.util.{Failure, Success}

class Neo4jRequesterTest extends AnyFlatSpec with should.Matchers with MockFactory with ScalaFutures {
  "A Neo4jRequester" should "request a list of nodes" in {
    val matchGenerator = mock[MatchGenerator]
    val complexGenerator = mock[ComplexPropertyGenerator]
    val returnGenerator = mock[ReturnGenerator]
    val connection = mock[Neo4jConnection]
    val parser = mock[RecordParser]
    val requester = new Neo4jRequester(matchGenerator, complexGenerator, returnGenerator, parser, connection)

    val fields = Map(
      "prop1" -> new StringValue("stringValue"),
      "prop2" -> new IntegerValue(42)
    )
    val request = RequestObject("name", None, "Test", fields.keys.toList, List())
    val response = List(new InternalRecord(fields.keys.toList.asJava, fields.values.toArray))
    val result = List(DbResult("Test", fields))
    val expectedRequest =
      """MATCH
        |
        |CALLS
        |RETURN""".stripMargin

    (complexGenerator.filterOutComplex _).expects(request).returns(request).once()
    (matchGenerator.generateMatchString _).expects(request).returns(Success("MATCH")).once()
    (complexGenerator.generateComplex _).expects(request).returns("CALLS").once()
    (returnGenerator.generateReturnString _).expects(request).returns("RETURN").once()
    (connection.executeRequest _).expects(expectedRequest).returns(Future.successful(response)).once()
    (parser.parseResult _).expects(response, request).returns(result).once()

    val requestFuture = requester.getNodes(request)
    whenReady(requestFuture) {
      _ should be(result)
    }
  }

  it should "request a single node" in {
    val matchGenerator = mock[MatchGenerator]
    val complexGenerator = mock[ComplexPropertyGenerator]
    val returnGenerator = mock[ReturnGenerator]
    val connection = mock[Neo4jConnection]
    val parser = mock[RecordParser]
    val requester = new Neo4jRequester(matchGenerator, complexGenerator, returnGenerator, parser, connection)

    val fields1 = Map(
      "prop1" -> new StringValue("value1"),
      "prop2" -> new IntegerValue(42)
    )
    val fields2 = Map(
      "prop1" -> new StringValue("value2"),
      "prop2" -> new IntegerValue(97)
    )
    val fields = List(fields1, fields2)
    val request = RequestObject("name", None, "Test", fields.head.keys.toList, List())
    val response = fields.map(f => new InternalRecord(f.keys.toList.asJava, f.values.toArray))
    val result = fields.map(f => DbResult("Test", f))
    val id = "node-id"
    val expectedRequest =
      s"""MATCH
         |WHERE name.id = \"$id\"
         |CALLS
         |RETURN""".stripMargin

    (complexGenerator.filterOutComplex _).expects(request).returns(request).once()
    (matchGenerator.generateMatchString _).expects(request).returns(Success("MATCH")).once()
    (complexGenerator.generateComplex _).expects(request).returns("CALLS").once()
    (returnGenerator.generateReturnString _).expects(request).returns("RETURN").once()
    (connection.executeRequest _).expects(expectedRequest).returns(Future.successful(response)).once()
    (parser.parseResult _).expects(response, request).returns(result).once()

    val requestFuture = requester.getNodeById(request, id)
    whenReady(requestFuture) {
      _ should be(result.headOption)
    }
  }

  it should "deal with an invalid relation" in {
    val matchGenerator = mock[MatchGenerator]
    val complexGenerator = mock[ComplexPropertyGenerator]
    val returnGenerator = mock[ReturnGenerator]
    val connection = mock[Neo4jConnection]
    val parser = mock[RecordParser]
    val requester = new Neo4jRequester(matchGenerator, complexGenerator, returnGenerator, parser, connection)

    val request = RequestObject("name", None, "Test", List("prop1", "prop2"), List())
    val errorMessage = "Test Message"
    val exception = UnknownRelationException(errorMessage)

    (complexGenerator.filterOutComplex _).expects(request).returns(request).once()
    (matchGenerator.generateMatchString _).expects(request).returns(Failure(exception)).once()

    val requestFuture = requester.getNodes(request)
    whenReady(requestFuture.failed) {
      _ should be(ParsingException(errorMessage, exception))
    }
  }

  it should "pass along an unknown error" in {
    val matchGenerator = mock[MatchGenerator]
    val complexGenerator = mock[ComplexPropertyGenerator]
    val returnGenerator = mock[ReturnGenerator]
    val connection = mock[Neo4jConnection]
    val parser = mock[RecordParser]
    val requester = new Neo4jRequester(matchGenerator, complexGenerator, returnGenerator, parser, connection)

    val request = RequestObject("name", None, "Test", List("prop1", "prop2"), List())
    case object CustomException extends Exception

    (complexGenerator.filterOutComplex _).expects(request).returns(request).once()
    (matchGenerator.generateMatchString _).expects(request).returns(Failure(CustomException)).once()

    val requestFuture = requester.getNodes(request)
    whenReady(requestFuture.failed) {
      _ should be(CustomException)
    }
  }

  it should "perform a quick search" in {
    val matchGenerator = mock[MatchGenerator]
    val complexGenerator = mock[ComplexPropertyGenerator]
    val returnGenerator = mock[ReturnGenerator]
    val connection = mock[Neo4jConnection]
    val parser = mock[RecordParser]
    val requester = new Neo4jRequester(matchGenerator, complexGenerator, returnGenerator, parser, connection)

    val fields1: Map[String, Value] = Map(
      "id" -> new StringValue("id1"),
      "fullName" -> new StringValue("name1")
    )
    val node1Score = Map("score" -> new FloatValue(1.0))
    val node1Node = Map("node" -> new NodeValue(new InternalNode(1, JavaList.of("Node1"), fields1.asJava)))
    val node1: Map[String, Value] = node1Score ++ node1Node
    val node1NodeResult = DbResult("Node1", fields1)
    val result1 = DbResult("SearchResult", properties = node1Score, nodes = Map("node" -> List(node1NodeResult)))

    val fields2: Map[String, Value] = Map(
      "id" -> new StringValue("id2"),
      "fullName" -> new StringValue("name2")
    )
    val node2Score = Map("score" -> new FloatValue(2.0))
    val node2Node = Map("node" -> new NodeValue(new InternalNode(2, JavaList.of("Node2"), fields2.asJava)))
    val node2: Map[String, Value] = node2Score ++ node2Node
    val node2NodeResult = DbResult("Node2", fields2)
    val result2 = DbResult("SearchResult", properties = node2Score, nodes = Map("node" -> List(node2NodeResult)))

    val nodes = List(node1, node2)
    val response = nodes.map(f => new InternalRecord(f.keys.toList.asJava, f.values.toArray))
    val results = List(result1, result2)

    val limit = 25
    val query = "Test"

    (connection.executeRequest _).expects(s"CALL db.index.fulltext.queryNodes(\"quickSearchIndex\", \"$query\") YIELD node, score RETURN * LIMIT $limit").returns(Future.successful(response)).once()

    val requestFuture = requester.quickSearch(query, limit)
    whenReady(requestFuture) {
      _ should be(results)
    }
  }

  it should "integrate with its components" in {
    val connection = mock[Neo4jConnection]
    val requester = Neo4jRequester.default(connection)

    val motherSimpleProperties = Map(
      "pc" -> new StringValue("motherString"))
    val motherListProperties = Map(
      "prop2" -> List(new IntegerValue(5), new IntegerValue(1)))
    val motherProperties: Map[String, Value] = motherSimpleProperties ++ motherListProperties.view.mapValues(l => new ListValue(l: _*)).toMap
    val motherRequest = RequestObject("m", Some("mother"), "Dog", motherProperties.keys.toList)
    val motherDbResult = DbResult("Dog", motherSimpleProperties, motherListProperties)

    val fatherSimpleProperties = Map(
      "prop3" -> new StringValue("fatherString"))
    val fatherListProperties = Map(
      "prop4" -> List(new IntegerValue(7), new IntegerValue(3)))
    val fatherProperties: Map[String, Value] = fatherSimpleProperties ++ fatherListProperties.view.mapValues(l => new ListValue(l: _*)).toMap
    val fatherRequest = RequestObject("f", Some("father"), "Dog", fatherProperties.keys.toList)
    val fatherDbResult = DbResult("Dog", fatherSimpleProperties, fatherListProperties)

    val litterSimpleProperties = Map(
      "prop5" -> new StringValue("litterString"))
    val litterListProperties = Map(
      "prop6" -> List(new IntegerValue(4), new IntegerValue(3)))
    val litterProperties: Map[String, Value] = litterSimpleProperties ++ litterListProperties.view.mapValues(l => new ListValue(l: _*)).toMap
    val litterRequest = RequestObject("l", Some("litter"), "Litter", litterProperties.keys.toList, List(motherRequest, fatherRequest))
    val litterDbResult = DbResult("Litter", litterSimpleProperties, litterListProperties, Map("mother" -> List(motherDbResult), "father" -> List(fatherDbResult)))

    val simpleProperties = Map(
      "prop7" -> new StringValue("litterString"))
    val listProperties = Map(
      "prop8" -> List(new IntegerValue(4), new IntegerValue(3)))
    val basicProperties: Map[String, Value] = simpleProperties ++ listProperties.view.mapValues(l => new ListValue(l: _*)).toMap
    val properties = Map(
      "l" -> new ListValue(new MapValue(litterProperties.asJava)),
      "m" -> new ListValue(new MapValue(motherProperties.asJava)),
      "f" -> new ListValue(new MapValue(fatherProperties.asJava)),
      "d" -> new MapValue(basicProperties.asJava))
    val (keys, values) = properties.unzip
    val response = List(new InternalRecord(keys.toList.asJava, values.toArray))
    val request = RequestObject("d", None, "Dog", basicProperties.keys.toList, List(litterRequest))
    val dbResult = DbResult("Dog", simpleProperties, listProperties, Map("litter" -> List(litterDbResult)))

    val id = "dog-id"
    val databaseRequest =
      s"""MATCH (d:Dog), (d)-[:BornIn]->(l:Litter), (l)-[:Mother]->(m:Dog), (l)-[:Father]->(f:Dog)
         |WHERE d.id = \"$id\"
         |CALL {
         |  WITH m
         |  MATCH (m)-[:BornIn|Mother|Father*..10]->(ancestor:Dog)
         |  WITH collect(ancestor) AS ancestors, collect(DISTINCT ancestor) AS uniqueAncestors
         |  WITH size(uniqueAncestors) AS nUnique, size(ancestors) AS nAll
         |  RETURN toFloat(nUnique) / nAll AS m_pc, nUnique AS m_pcUnique, nAll AS m_pcAll
         |}
         |WITH {prop7: d.prop7, prop8: d.prop8} AS d, {prop5: l.prop5, prop6: l.prop6} AS l, {pc: m_pc, prop2: m.prop2} AS m, {prop3: f.prop3, prop4: f.prop4} AS f
         |RETURN d, COLLECT(l) AS l, COLLECT(m) AS m, COLLECT(f) AS f""".stripMargin
    (connection.executeRequest _).expects(databaseRequest).returns(Future.successful(response)).once()

    val requestFuture = requester.getNodeById(request, id)
    whenReady(requestFuture) {
      _ should be(Some(dbResult))
    }
  }
}
