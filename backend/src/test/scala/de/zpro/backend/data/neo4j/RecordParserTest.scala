package de.zpro.backend.data.neo4j

import de.zpro.backend.data.{DbResult, RequestObject}
import org.neo4j.driver.internal.InternalRecord
import org.neo4j.driver.internal.value.{IntegerValue, ListValue, MapValue, StringValue}
import org.neo4j.driver.{Value, Record => Neo4jRecord}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}


class RecordParserTest extends AnyFlatSpec with should.Matchers {
  "The Neo4jParser" should "parse a basic Record" in {
    val propertyFields = Map(
      "stringField" -> new StringValue("stringValue"),
      "numberField" -> new IntegerValue(46)
    )
    val listFields = Map(
      "stringListField" -> List(new StringValue("stringListValue1"), new StringValue("stringListValue2")),
      "numberListField" -> List(new IntegerValue(1), new IntegerValue(2))
    )
    val fields = propertyFields ++ toListValue(listFields)
    val request = RequestObject.simple("Test", fields.keys.toList)
    val records = listToRecords(List(fields))

    val result = RecordParser.parseResult(records, request)
    result.size should be(1)
    result.head should be(DbResult("Test", propertyFields, listFields))
  }

  it should "parse a Record with child nodes" in {
    val node1PropertyFields = Map(
      "node1StringField" -> new StringValue("node1StringValue"),
      "node1NumberField" -> new IntegerValue(58)
    )
    val node1ListFields = Map(
      "node1StringListField" -> List(new StringValue("node1StringListValue1"), new StringValue("node1StringListValue2")),
      "node1NumberListField" -> List(new IntegerValue(9), new IntegerValue(7))
    )
    val node1Fields: Map[String, Value] = node1PropertyFields ++ toListValue(node1ListFields)
    val node1Request = RequestObject("node1", Some("node1FieldName"), "Node1", node1Fields.keys.toList, List.empty)
    val node1DbResult = DbResult("Node1", node1PropertyFields, node1ListFields)

    val node2PropertyFields = Map(
      "node2StringField" -> new StringValue("node2StringValue"),
      "node2NumberField" -> new IntegerValue(87)
    )
    val node2ListFields = Map(
      "node2StringListField" -> List(new StringValue("node2StringListValue1"), new StringValue("node2StringListValue2")),
      "node2NumberListField" -> List(new IntegerValue(5), new IntegerValue(3))
    )
    val node2Fields: Map[String, Value] = node2PropertyFields ++ toListValue(node2ListFields)
    val node2Request = RequestObject("node2", Some("node2FieldName"), "Node2", node2Fields.keys.toList, List.empty)
    val node2DbResult = DbResult("Node2", node2PropertyFields, node2ListFields)

    val propertyFields = Map(
      "stringField" -> new StringValue("stringValue"),
      "numberField" -> new IntegerValue(46)
    )
    val listFields = Map(
      "stringListField" -> List(new StringValue("stringListValue1"), new StringValue("stringListValue2")),
      "numberListField" -> List(new IntegerValue(1), new IntegerValue(2))
    )
    val nodeFields = Map(
      "node1" -> new ListValue(new MapValue(node1Fields.asJava)),
      "node2" -> new ListValue(new MapValue(node2Fields.asJava))
    )
    val simpleFields = propertyFields ++ toListValue(listFields)
    val fields = simpleFields ++ nodeFields
    val request = RequestObject("name", None, "Test", simpleFields.keys.toList, List(node1Request, node2Request))
    val dbResult = DbResult("Test", propertyFields, listFields, Map("node1FieldName" -> List(node1DbResult), "node2FieldName" -> List(node2DbResult)))

    val records = listToRecords(List(fields))
    val result = RecordParser.parseResult(records, request)

    result.size should be(1)
    result.head should be(dbResult)
  }

  it should "parse a Record with a complex tree of children" in {
    val node11 = createNode("11", List())
    val node12111 = createNode("12111", List())
    val node1211 = createNode("1211", List(node12111))
    val node121 = createNode("121", List(node1211))
    val node122 = createNode("122", List())
    val node123 = createNode("123", List())
    val node12 = createNode("12", List(node121, node122, node123))
    val node131 = createNode("131", List())
    val node13 = createNode("13", List(node131))
    val node1 = createNode("1", List(node11, node12, node13))
    val node211 = createNode("211", List())
    val node212 = createNode("212", List())
    val node2131 = createNode("2131", List())
    val node2132 = createNode("2132", List())
    val node213 = createNode("213", List(node2131, node2132))
    val node214 = createNode("214", List())
    val node21 = createNode("21", List(node211, node212, node213, node214))
    val node22 = createNode("22", List())
    val node2 = createNode("2", List(node21, node22))
    val node3 = createNode("3", List())
    val node4 = createNode("4", List())

    val allNodes = List(node11, node12111, node1211, node121, node122, node123, node12, node131, node13,
      node1, node211, node212, node2131, node2132, node213, node214, node21, node22, node2, node3, node4)
    val rootNodes = List(node1, node2, node3, node4)

    val propertyFields = Map(
      "stringField" -> new StringValue("stringValue"),
      "numberField" -> new IntegerValue(46)
    )
    val listFields = Map(
      "stringListField" -> List(new StringValue("stringListValue1"), new StringValue("stringListValue2")),
      "numberListField" -> List(new IntegerValue(1), new IntegerValue(2))
    )
    val nodeFields = allNodes.map(n => s"node${n.nameExtension}" -> new ListValue(new MapValue(n.fields.asJava))).toMap

    val simpleFields = propertyFields ++ toListValue(listFields)
    val fields = simpleFields ++ nodeFields
    val request = RequestObject("name", None, "Test", simpleFields.keys.toList, rootNodes.map(_.request))
    val expectedResult = DbResult("Test", propertyFields, listFields, rootNodes.map(n => s"node${n.nameExtension}FieldName" -> List(n.result)).toMap)

    val records = listToRecords(List(fields))
    val result = RecordParser.parseResult(records, request)

    result.size should be(1)
    result.head should be(expectedResult)
  }

  private def toListValue(list: Map[String, List[Value]]): Map[String, ListValue] =
    list.view.mapValues(l => new ListValue(l: _*)).toMap

  private def listToRecords(list: List[Map[String, Value]]) =
    list.map(mapToRecord)

  private def mapToRecord(map: Map[String, Value]): Neo4jRecord = {
    val (keys, values) = map.unzip
    new InternalRecord(keys.toList.asJava, values.toArray)
  }

  private case class NodeDetails(nameExtension: String, fields: Map[String, Value], request: RequestObject, result: DbResult) {
    val name = s"node$nameExtension"
    val fieldName = s"node${nameExtension}FieldName"
    val dataType = s"Node$nameExtension"
    val value = s"node${nameExtension}Value"
  }

  private def createNode(nameExtension: String, children: List[NodeDetails]): NodeDetails = {
    val fields = Map(
      s"node${nameExtension}Field" -> new StringValue(s"node${nameExtension}Value"),
    )
    val request = RequestObject(s"node$nameExtension", Some(s"node${nameExtension}FieldName"), s"Node$nameExtension", fields.keys.toList, children.map(_.request))
    val result = DbResult(s"Node$nameExtension", fields, Map.empty, children.map(c => s"node${c.nameExtension}FieldName" -> List(c.result)).toMap)
    NodeDetails(nameExtension, fields, request, result)
  }
}
