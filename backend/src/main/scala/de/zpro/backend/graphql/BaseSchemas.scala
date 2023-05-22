package de.zpro.backend.graphql

import de.zpro.backend.data.DbResult
import de.zpro.backend.data.repositories.{Property, QuickSearchResult}
import sangria.macros.derive.deriveObjectType
import sangria.schema._

private object BaseSchemas extends BaseSchemas

private trait BaseSchemas {
  val Id: Argument[String] = Argument("id", StringType)

  val QuickSearchResultType: ObjectType[Unit, QuickSearchResult] = deriveObjectType[Unit, QuickSearchResult]()
  val Query: Argument[String] = Argument("query", StringType)

  val PropertyType: ObjectType[Unit, Property] = deriveObjectType[Unit, Property]()

  val HasIdAndNameType: InterfaceType[Unit, DbResult] = InterfaceType(
    name = "HasIdAndName",
    fields = fields(
      Field(name = "id",
        fieldType = StringType,
        resolve = _.value("id").asString),
      Field(name = "name",
        fieldType = StringType,
        resolve = _.value("name").asString),
      Field(name = "fullName",
        fieldType = StringType,
        resolve = _.value("fullName").asString)
    )
  )

  val BasicKennelType: InterfaceType[Unit, DbResult] = InterfaceType(
    name = "BasicKennel",
    interfaces = interfaces[Unit, DbResult](HasIdAndNameType),
    fields = fields(
      Field(name = "link",
        fieldType = StringType,
        resolve = _.value("link").asString)
    )
  )

  val BasicLitterType: InterfaceType[Unit, DbResult] = InterfaceType(
    name = "BasicLitter",
    interfaces = interfaces[Unit, DbResult](HasIdAndNameType),
    fields = fields(
      Field(name = "date",
        fieldType = OptionType(LongType),
        resolve = _.value.get("date").map(_.asLong)),
      Field(name = "initials",
        fieldType = StringType,
        resolve = _.value("initials").asString)
    )
  )

  val BasicDogType: InterfaceType[Unit, DbResult] = InterfaceType(
    name = "BasicDog",
    interfaces = interfaces[Unit, DbResult](HasIdAndNameType),
    fields = fields(
      Field(name = "gender",
        fieldType = StringType,
        resolve = _.value("gender").asString),
      Field(name = "pc",
        fieldType = OptionType(FloatType),
        resolve = _.value.get("pc").map(_.asDouble)),
      Field(name = "pcUnique",
        fieldType = OptionType(IntType),
        resolve = _.value.get("pcUnique").map(_.asInt)),
      Field(name = "pcAll",
        fieldType = OptionType(IntType),
        resolve = _.value.get("pcAll").map(_.asInt)),
      Field(name = "coi",
        fieldType = OptionType(FloatType),
        resolve = _.value.get("coi").map(_.asDouble))
    )
  )

  val BasicPersonType: InterfaceType[Unit, DbResult] = InterfaceType(
    name = "BasicPerson",
    interfaces = interfaces[Unit, DbResult](HasIdAndNameType),
    fields = fields(
      Field(name = "street",
        fieldType = OptionType(StringType),
        resolve = _.value.get("street").map(_.asString)),
      Field(name = "country",
        fieldType = OptionType(StringType),
        resolve = _.value.get("country").map(_.asString)),
      Field(name = "postCode",
        fieldType = OptionType(StringType),
        resolve = _.value.get("postCode").map(_.asString)),
      Field(name = "city",
        fieldType = OptionType(StringType),
        resolve = _.value.get("city").map(_.asString)),
      Field(name = "phoneNumbers",
        fieldType = ListType(StringType),
        resolve = _.value.getListOrEmpty("street").map(_.asString)),
      Field(name = "emails",
        fieldType = ListType(StringType),
        resolve = _.value.getListOrEmpty("emails").map(_.asString))
    )
  )
}
