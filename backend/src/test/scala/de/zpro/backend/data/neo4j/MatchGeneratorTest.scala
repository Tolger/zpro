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

  it should "match a dogs litter" in {
    val generator = MatchGenerator
    val litter = RequestObject("l", Some("litter"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(litter))
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (d:Dog), (d)-[:BornIn]->(l:Litter)")
  }

  it should "match a dogs owner" in {
    val generator = MatchGenerator
    val owner = RequestObject("o", Some("owner"), "Person", List("prop1", "prop2"), List())
    val request = RequestObject("d", None, "Dog", List("prop3", "prop4"), List(owner))
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (d:Dog), (d)-[:Owner]->(o:Person)")
  }

  it should "match a litters kennel" in {
    val generator = MatchGenerator
    val kennel = RequestObject("k", Some("kennel"), "Kennel", List("prop1", "prop2"), List())
    val request = RequestObject("l", None, "Litter", List("prop3", "prop4"), List(kennel))
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (l:Litter), (l)-[:BredBy]->(k:Kennel)")
  }

  it should "match a litters parents" in {
    val generator = MatchGenerator
    val mother = RequestObject("m", Some("mother"), "Dog", List("prop3", "prop4"), List())
    val father = RequestObject("f", Some("father"), "Dog", List("prop3", "prop4"), List())
    val request = RequestObject("l", None, "Litter", List("prop3", "prop4"), List(mother, father))
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (l:Litter), (l)-[:Mother]->(m:Dog), (l)-[:Father]->(f:Dog)")
  }

  it should "match a litters offspring" in {
    val generator = MatchGenerator
    val dog = RequestObject("d", Some("offspring"), "Dog", List("prop3", "prop4"), List())
    val request = RequestObject("l", None, "Litter", List("prop3", "prop4"), List(dog))
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (l:Litter), (l)<-[:BornIn]-(d:Dog)")
  }

  it should "match a kennels litters" in {
    val generator = MatchGenerator
    val litter = RequestObject("l", Some("litters"), "Litter", List("prop1", "prop2"), List())
    val request = RequestObject("k", None, "Kennel", List("prop3", "prop4"), List(litter))
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (k:Kennel), (k)<-[:BredBy]-(l:Litter)")
  }

  it should "match a persons dogs" in {
    val generator = MatchGenerator
    val dog = RequestObject("d", Some("dogs"), "Dog", List("prop3", "prop4"), List())
    val request = RequestObject("p", None, "Person", List("prop3", "prop4"), List(dog))
    val response = generator.generateMatchString(request)
    response.get should be("MATCH (p:Person), (p)<-[:Owner]-(d:Dog)")
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
    response.get should be("MATCH (d:Dog), (d)-[:Owner]->(o:Person), (d)-[:BornIn]->(l:Litter), (l)-[:Mother]->(md:Dog), (md)-[:BornIn]->(ml:Litter), (ml)-[:BredBy]->(mk:Kennel)")
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
