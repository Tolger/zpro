import {Property} from "./Property";

export function processDog(dog, properties: Map<String, Property>) {
  const outDog = {}
  Array.from(properties.values())
    .map(property => outDog[property.name] = property.extract(dog))

  return Object.fromEntries(Object.entries(outDog).filter(([_, v]) => v != null))
}
