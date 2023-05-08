export class DataFields {
  public static readonly basicLitterFields = ["id", "name", "fullName", "date", "initials"]
  public static readonly basicKennelFields = ["id", "name", "fullName", "link"]
  public static readonly basicPersonFields = ["id", "name", "fullName", "street", "country", "postCode", "city", "phoneNumbers", "emails"]
}

export interface HasIdAndName {
  id: string
  name: string
  fullName: string
}

export interface Litter {
  date: number
  initials: string
}

export interface Kennel {
  litter: string
}

export interface Person {
  street?: string
  country?: string
  postCode?: string
  city?: string
  phoneNumbers?: string[]
  emails?: string[]
}
