package de.zpro.backend.data

import de.zpro.backend.exceptions.UnknownChildNameException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import sangria.schema.ProjectedName

import scala.util.{Failure, Success}

class RequestObjectTest extends AnyFlatSpec with should.Matchers {
  "A RequestObject" should "represent a simple request" in {
    RequestObject.simple("Type", List("f1", "f2")) should be(RequestObject("simple", None, "Type", List("f1", "f2")))
  }

  it should "represent simple graphql fields" in {
    val request = RequestObject.fromGraphqlFields("Type", List(ProjectedName("f1"), ProjectedName("f2")))
    request should be(Success(RequestObject("n", None, "Type", List("f1", "f2"))))
  }

  it should "represent a dogs litter" in {
    val pLitter = ProjectedName("litter", Vector(ProjectedName("l1")))
    val rLitter = RequestObject("na", Some("litter"), "Litter", List("l1"))
    val rDog = RequestObject("n", None, "Dog", children = List(rLitter))
    val request = RequestObject.fromGraphqlFields("Dog", List(pLitter))
    request should be(Success(rDog))
  }

  it should "represent kennels litters" in {
    val pLitter = ProjectedName("litters", Vector(ProjectedName("l1")))
    val rLitter = RequestObject("na", Some("litters"), "Litter", List("l1"))
    val rKennel = RequestObject("n", None, "Kennel", children = List(rLitter))
    val request = RequestObject.fromGraphqlFields("Kennel", List(pLitter))
    request should be(Success(rKennel))
  }

  it should "represent litters kennel" in {
    val pKennel = ProjectedName("kennel", Vector(ProjectedName("l1")))
    val rKennel = RequestObject("na", Some("kennel"), "Kennel", List("l1"))
    val rLitter = RequestObject("n", None, "Litter", children = List(rKennel))
    val request = RequestObject.fromGraphqlFields("Litter", List(pKennel))
    request should be(Success(rLitter))
  }

  it should "represent dogs owner" in {
    val pPerson = ProjectedName("owner", Vector(ProjectedName("l1")))
    val rPerson = RequestObject("na", Some("owner"), "Person", List("l1"))
    val rDog = RequestObject("n", None, "Dog", children = List(rPerson))
    val request = RequestObject.fromGraphqlFields("Dog", List(pPerson))
    request should be(Success(rDog))
  }

  it should "represent a litters parents" in {
    val pMother = ProjectedName("mother", Vector(ProjectedName("m1")))
    val pFather = ProjectedName("father", Vector(ProjectedName("f1")))
    val rMother = RequestObject("na", Some("mother"), "Dog", List("m1"))
    val rFather = RequestObject("nb", Some("father"), "Dog", List("f1"))
    val rLitter = RequestObject("n", None, "Litter", children = List(rMother, rFather))
    val request = RequestObject.fromGraphqlFields("Litter", List(pMother, pFather))
    request should be(Success(rLitter))
  }

  it should "represent litters offspring" in {
    val pDog = ProjectedName("offspring", Vector(ProjectedName("l1")))
    val rDog = RequestObject("na", Some("offspring"), "Dog", List("l1"))
    val rLitter = RequestObject("n", None, "Litter", children = List(rDog))
    val request = RequestObject.fromGraphqlFields("Litter", List(pDog))
    request should be(Success(rLitter))
  }

  it should "represent a Property" in {
    val pDog = ProjectedName("offspring", Vector(ProjectedName("l1")))
    val rDog = RequestObject("na", Some("offspring"), "Dog", List("l1"))
    val rLitter = RequestObject("n", None, "Litter", children = List(rDog))
    val request = RequestObject.fromGraphqlFields("Litter", List(pDog))
    request should be(Success(rLitter))
  }

  it should "reject an unknown field" in {
    val pUnknown = ProjectedName("unknown", Vector(ProjectedName("u1")))
    val request = RequestObject.fromGraphqlFields("Litter", List(pUnknown))
    request should be(Failure(UnknownChildNameException("Can't determine data type for unrecognized field name \"unknown\"")))
  }

  it should "represent complex graphql fields" in {
    val p11 = generateProjected("mother", Vector())
    val p12 = generateProjected("father", Vector())
    val p1 = generateProjected("mother", Vector(p11, p12))
    val p211 = generateProjected("mother", Vector())
    val p212 = generateProjected("father", Vector())
    val p21 = generateProjected("mother", Vector(p211, p212))
    val p22 = generateProjected("father", Vector())
    val p2 = generateProjected("father", Vector(p21, p22))
    val p = ProjectedName("litter", Vector(p1, p2) :+ ProjectedName("l1") :+ ProjectedName("l2"))
    val pRoot = List(p, ProjectedName("s1"), ProjectedName("s2"))

    val r11 = generateRequest("naaa", "mother", List())
    val r12 = generateRequest("naab", "father", List())
    val r1 = generateRequest("naa", "mother", List(r11, r12))
    val r211 = generateRequest("nabaa", "mother", List())
    val r212 = generateRequest("nabab", "father", List())
    val r21 = generateRequest("naba", "mother", List(r211, r212))
    val r22 = generateRequest("nabb", "father", List())
    val r2 = generateRequest("nab", "father", List(r21, r22))
    val r = RequestObject("na", Some("litter"), "Litter", List("l1", "l2"), List(r1, r2))
    val rRoot = RequestObject("n", None, "Type", List("s1", "s2"), List(r))

    val result = RequestObject.fromGraphqlFields("Type", pRoot)
    result should be(Success(rRoot))
  }

  private def generateProjected(name: String, children: Vector[ProjectedName]): ProjectedName =
    ProjectedName(name, children :+ ProjectedName("field1") :+ ProjectedName("field2"))

  private def generateRequest(name: String, fieldName: String, children: List[RequestObject]): RequestObject =
    RequestObject(name, Some(fieldName), "Dog", List("field1", "field2"), children)
}
