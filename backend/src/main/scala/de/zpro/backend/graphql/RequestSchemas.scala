package de.zpro.backend.graphql

import de.zpro.backend.data.DbResult
import de.zpro.backend.data.repositories.{GraphqlProperty, RepositoryProvider}
import sangria.schema._

private object RequestSchemas extends BaseSchemas {
  def generateSchema(properties: List[GraphqlProperty[Any, OutputType[Any]]]): Schema[RepositoryProvider, Unit] = {
    lazy val DogType: ObjectType[Unit, DbResult] = ObjectType(
      name = "Dog",
      interfaces = interfaces[Unit, DbResult](BasicDogType),
      fieldsFn = () => properties.map(property => {
        val field: Field[Unit, DbResult] = Field(
          name = property.name,
          fieldType = OptionType(property.valueType),
          resolve = _.value.get(property.name).map(property.resolve)
        )
        field
      }).concat(fields[Unit, DbResult](
        Field(name = "litter",
          fieldType = LitterType,
          resolve = _.value.getNode("litter")),
        Field(name = "owner",
          fieldType = PersonType,
          resolve = _.value.getNode("owner")),
        Field(name = "litters",
          fieldType = ListType(LitterType),
          resolve = _.value.getNodeListOrEmpty("litters"))
      ))
    )

    lazy val LitterType: ObjectType[Unit, DbResult] = ObjectType(
      name = "Litter",
      interfaces = interfaces[Unit, DbResult](BasicLitterType),
      fieldsFn = () => fields(
        Field(name = "offspring",
          fieldType = ListType(DogType),
          resolve = _.value.getNodeListOrEmpty("offspring")),
        Field(name = "kennel",
          fieldType = KennelType,
          resolve = _.value.getNode("kennel")),
        Field(name = "mother",
          fieldType = DogType,
          resolve = _.value.getNode("mother")),
        Field(name = "father",
          fieldType = DogType,
          resolve = _.value.getNode("father"))
      )
    )

    lazy val KennelType: ObjectType[Unit, DbResult] = ObjectType(
      name = "Kennel",
      interfaces = interfaces[Unit, DbResult](BasicKennelType),
      fieldsFn = () => fields(
        Field(name = "litters",
          fieldType = ListType(LitterType),
          resolve = _.value.getNodeListOrEmpty("litters"))
      )
    )

    lazy val PersonType: ObjectType[Unit, DbResult] = ObjectType(
      name = "Person",
      interfaces = interfaces[Unit, DbResult](BasicPersonType),
      fieldsFn = () => fields(
        Field(name = "dogs",
          fieldType = DogType,
          resolve = _.value.getNode("dogs"))
      )
    )

    val QueryType: ObjectType[RepositoryProvider, Unit] = ObjectType("Query", fields[RepositoryProvider, Unit](
      Field("dog", OptionType(DogType),
        arguments = List(Id),
        resolve = Projector((ctx, f) => ctx.ctx.dataRepository.dog(ctx.arg(Id), f))),
      Field("quickSearch", ListType(QuickSearchResultType),
        arguments = List(Query),
        resolve = Projector((ctx, _) => ctx.ctx.searchRepository.quickSearch(ctx.arg(Query)))),
      Field("properties", ListType(PropertyType),
        resolve = Projector((ctx, _) => ctx.ctx.propertyRepository.loadProperties)),
      Field("litter", OptionType(LitterType),
        arguments = List(Id),
        resolve = Projector((ctx, f) => ctx.ctx.dataRepository.litter(ctx.arg(Id), f))),
      Field("kennel", OptionType(KennelType),
        arguments = List(Id),
        resolve = Projector((ctx, f) => ctx.ctx.dataRepository.kennel(ctx.arg(Id), f))),
      Field("person", OptionType(PersonType),
        arguments = List(Id),
        resolve = Projector((ctx, f) => ctx.ctx.dataRepository.person(ctx.arg(Id), f)))
    ))

    Schema(QueryType)
  }
}
