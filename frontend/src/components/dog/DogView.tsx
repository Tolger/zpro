import "./DogView.css"
import {gql, useQuery} from "@apollo/client";
import {useParams} from "react-router-dom";
import {PropertyData, PropertyProvider} from "../../model/Property";
import {useContext} from "react";
import {PropertiesContext} from "../../index.js"
import {processDog} from "../../model/Dog.ts";
import {DataFields} from "../../model/Data.ts";

export default function DogView() {
  const properties: PropertyProvider = useContext(PropertiesContext)

  const graphqlRequest = gql`
      query Dog($id: String!) {
          dog(id: $id) {
              fullName
              name
              gender
              litter {
                  ${DataFields.basicLitterFields.join(" ")}
                  kennel {
                      ${DataFields.basicKennelFields.join(" ")}
                  }
              }
              owner {
                  ${DataFields.basicPersonFields.join(" ")}
              }
              ${Array.from(properties.basicProperties.keys()).join(" ")}
          }
      }`

  const {loading, error, data} = useQuery(graphqlRequest, {
    variables: {
      id: useParams().dogId
    }
  })

  if (loading)
    return (
      <div>Loading</div>
    )
  if (error) {
    console.log(error)
    return (
      <div>Error</div>
    )
  }

  const dog = processDog(data.dog, properties.allProperties)

  function renderPropertyValue(value: string, property: PropertyData): string {
    switch (property.valueType) {
      case "Enum-Unordered-Int":
      case "Enum-Ordered-Int":
        return value.substring(1)
      case "Boolean":
      case "Boolean-Tested":
        return value ? "ja" : "nein"
      default:
        return value
    }
  }

  function renderProperty(entry: string[]) {
    const property: PropertyData = properties.allProperties.get(entry[0])
    if (!property)
      throw Error("unknown property name " + entry[0])
    return (
      <div className="property">
        <div className="property-key"><b>{property.displayName}</b></div>
        <div
          className="property-value">{renderPropertyValue(entry[1], property)}</div>
      </div>
    )
  }

  return ([
    <h1>{data.dog.fullName}</h1>,
    <div className="property-container">
      {Object.entries(dog).map(entry => renderProperty(entry))}
    </div>
  ])
}
