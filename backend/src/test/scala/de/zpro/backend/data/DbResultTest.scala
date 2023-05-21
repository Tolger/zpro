package de.zpro.backend.data

import org.neo4j.driver.Value
import org.neo4j.driver.internal.value.{IntegerValue, StringValue}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class DbResultTest extends AnyFlatSpec with should.Matchers {
  "A DbResult" should "return a property" in {
    val stringValue = new StringValue("stringValue")
    val intValue = new IntegerValue(42)
    val dbResult = DbResult(dataType = "Test", properties = Map("stringKey" -> stringValue, "intKey" -> intValue))
    dbResult("stringKey") should be(stringValue)
    dbResult.get("intKey") should be(Some(intValue))
    dbResult.get("inexistentNode") should be(None)
  }

  it should "return a property list" in {
    val stringListValue = List(new StringValue("e1"), new StringValue("e2"))
    val intListValue = List(new IntegerValue(1), new IntegerValue(42))
    val dbResult = DbResult(dataType = "Test", listProperties = Map(
      "stringListKey" -> stringListValue,
      "intListKey" -> intListValue))
    dbResult.getList("stringListKey") should be(stringListValue)
    dbResult.getListOption("stringListKey") should be(Some(stringListValue))
    dbResult.getListOrEmpty("intListKey") should be(intListValue)
    dbResult.getListOption("inexistentListKey") should be(None)
    dbResult.getListOrEmpty("inexistentListKey") should be(List.empty[Value])
  }

  it should "return a node" in {
    val nodeValue = DbResult("Single")
    val dbResult = DbResult(dataType = "Test", nodes = Map("singleNode" -> List(nodeValue)))
    dbResult.getNode("singleNode") should be(nodeValue)
    dbResult.getNodeOption("singleNode") should be(Some(nodeValue))
    dbResult.getNodeOption("inexistentNode") should be(None)
  }

  it should "return a node list" in {
    val nodeListValue = List(DbResult("Node1"), DbResult("Node2"))
    val dbResult = DbResult(dataType = "Test", nodes = Map("nodeList" -> nodeListValue))
    dbResult.getNodeList("nodeList") should be(nodeListValue)
    dbResult.getNodeListOption("nodeList") should be(Some(nodeListValue))
    dbResult.getNodeListOrEmpty("nodeList") should be(nodeListValue)
    dbResult.getNodeListOption("inexistentList") should be(None)
    dbResult.getNodeListOrEmpty("inexistentList") should be(List.empty[Value])
  }
}
