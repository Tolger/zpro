export interface PropertyData {
  name: string
  valueType: string
  shortDisplayName: string
  displayName: string
  description: string
  section: string
  options?: string[]
}

export abstract class Property implements PropertyData {
  public readonly name: string;
  public readonly valueType: string;
  public readonly shortDisplayName: string;
  public readonly displayName: string;
  public readonly description: string;
  public readonly section: string;
  public readonly options?: string[];

  protected constructor(props: PropertyData) {
    this.name = props.name
    this.valueType = props.valueType
    this.shortDisplayName = props.shortDisplayName
    this.displayName = props.displayName
    this.description = props.description
    this.section = props.section
    this.options = props.options
  }

  public abstract extract(response: object)
}

export class BasicProperty extends Property {
  extract(response: object) {
    if (response)
      return response[this.name]
    else
      return null
  }
}

export class ObjectDerivedProperty extends Property {
  private readonly objectName: string
  private readonly propertyName: string

  constructor(objectName: string, propertyName: string, props: PropertyData) {
    super(props)
    this.objectName = objectName
    this.propertyName = propertyName
  }

  extract(response: object) {
    if (response) {
      if (response[this.objectName])
        return response[this.objectName][this.propertyName]
      else
        return null
    } else
      return null
  }
}

export interface PropertyProvider {
  basicProperties: Map<String, Property>
  allProperties: Map<String, Property>
}
