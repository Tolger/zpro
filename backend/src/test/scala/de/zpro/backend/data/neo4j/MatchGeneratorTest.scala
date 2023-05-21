package de.zpro.backend.data.neo4j

import de.zpro.backend.data.RequestObject
import de.zpro.backend.exceptions.UnknownRelationException
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class MatchGeneratorTest extends AnyFlatSpec with should.Matchers with MockFactory {
  "The MatchGenerator" should "generate a simple match" in {
    val generator = MatchGenerator
    val request = RequestObject("name", None, "Test", List("prop1", "prop2", "prop3"), List())
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (name:Test)")
  }

  it should "generate a simple match with ID" in {
    val generator = MatchGenerator
    val request = RequestObject("name", None, "Test", List("prop1", "prop2", "prop3"), List())
    val id = "dog-id"
    val response = generator.generateMatchString(request, Some(id))
    response.get should be(
      s"""MATCH (name:Test)
         |WHERE name.id = \"$id\"""".stripMargin)
  }

  it should "match a dogs litter" in {
    val generator = MatchGenerator
    val litter = RequestObject("l", Some("litter"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(litter))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (d:Dog)
         |OPTIONAL MATCH (d)-[:BornIn]->(l:Litter)""".stripMargin)
  }

  it should "match a dogs litters" in {
    val generator = MatchGenerator
    val litter = RequestObject("l", Some("litters"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(litter))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (d:Dog)
         |OPTIONAL MATCH (d)<-[:Mother|Father]-(l:Litter)""".stripMargin)
  }

  it should "match a dogs owner" in {
    val generator = MatchGenerator
    val owner = RequestObject("o", Some("owner"), "Person", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(owner))
    val response = generator.generateMatchString(request)
    response.get should be(
      """MATCH (d:Dog)
        |OPTIONAL MATCH (d)-[:Owner]->(o:Person)""".stripMargin)
  }

  it should "match a litters kennel" in {
    val generator = MatchGenerator
    val kennel = RequestObject("k", Some("kennel"), "Kennel", List("prop1", "prop2"), List())
    val request = RequestObject("l", None, "Litter", List("prop3", "prop4"), List(kennel))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (l:Litter)
         |OPTIONAL MATCH (l)-[:BredBy]->(k:Kennel)""".stripMargin)
  }

  it should "match a litters parents" in {
    val generator = MatchGenerator
    val mother = RequestObject("m", Some("mother"), "Dog", List("prop3", "prop4"), List())
    val father = RequestObject("f", Some("father"), "Dog", List("prop3", "prop4"), List())
    val request = RequestObject("l", None, "Litter", List("prop3", "prop4"), List(mother, father))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (l:Litter)
         |OPTIONAL MATCH (l)-[:Mother]->(m:Dog)
         |OPTIONAL MATCH (l)-[:Father]->(f:Dog)""".stripMargin)
  }

  it should "match a litters offspring" in {
    val generator = MatchGenerator
    val dog = RequestObject("d", Some("offspring"), "Dog", List("prop3", "prop4"), List())
    val request = RequestObject("l", None, "Litter", List("prop3", "prop4"), List(dog))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (l:Litter)
         |OPTIONAL MATCH (l)<-[:BornIn]-(d:Dog)""".stripMargin)
  }

  it should "match a kennels litters" in {
    val generator = MatchGenerator
    val litter = RequestObject("l", Some("litters"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("k", None, "Kennel", List("prop3", "prop4"), List(litter))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (k:Kennel)
         |OPTIONAL MATCH (k)<-[:BredBy]-(l:Litter)""".stripMargin)
  }

  it should "match a persons dogs" in {
    val generator = MatchGenerator
    val dog = RequestObject("d", Some("dogs"), "Dog", List("prop3", "prop4"), List())
    val request = RequestObject("p", None, "Person", List("prop3", "prop4"), List(dog))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (p:Person)
         |OPTIONAL MATCH (p)<-[:Owner]-(d:Dog)""".stripMargin)
  }

  it should "perform a complex match" in {
    val generator = MatchGenerator
    val mothersKennel = RequestObject("mk", Some("kennel"), "Kennel", List("mk1, mk2"), List())
    val mothersLitter = RequestObject("ml", Some("litter"), "Litter", List("ml1, ml2"), List(mothersKennel))
    val mother = RequestObject("md", Some("mother"), "Dog", List("md1, md2"), List(mothersLitter))
    val litter = RequestObject("l", Some("litter"), "Litter", List("l1, l2"), List(mother))
    val owner = RequestObject("o", Some("owner"), "Person", List("o1, o2"), List())
    val request = RequestObject("d", None, "Dog", List("d1, d2"), List(owner, litter))
    val response = generator.generateMatchString(request)
    response.get should be(
      s"""MATCH (d:Dog)
         |OPTIONAL MATCH (d)-[:Owner]->(o:Person)
         |OPTIONAL MATCH (d)-[:BornIn]->(l:Litter)
         |OPTIONAL MATCH (l)-[:Mother]->(md:Dog)
         |OPTIONAL MATCH (md)-[:BornIn]->(ml:Litter)
         |OPTIONAL MATCH (ml)-[:BredBy]->(mk:Kennel)""".stripMargin)
  }

  //  it should "handle complex properties" in {
  //    val complexGenerator = mock[ComplexPropertyGenerator]
  //    val generator = MatchGeneratorImpl(complexGenerator)
  //    val request = RequestObject("l", None, "Litter", List("complex", "simple"), List())
  //    val filteredRequest = request.copy(simpleFields = List("complex"))
  //
  //    (complexGenerator.filterOutComplex _).expects(request).returns(filteredRequest)
  //    (complexGenerator.generateComplex _).expects(request).returns("COMPLEX")
  //    val response = generator.generateMatchString(request)
  //    response.get should be("MATCH (l:Litter) COMPLEX")
  //  }

  it should "reject an invalid relation" in {
    val generator = MatchGenerator
    val owner = RequestObject("o", Some("owner"), "Person", List("prop1", "prop2"), List())
    val request = RequestObject("l", None, "Litter", List("prop3", "prop4"), List(owner))
    val response = generator.generateMatchString(request)
    response.failed.get should be(UnknownRelationException("Can't determine relation type for unrecognized relation \"Litter-owner->Person\""))
  }
}
