import "./Forms.css"

export default function EnumInput<T>(params: { values: T[], default: T, onSelect: (selected: T) => void }) {
  return (
    <select value={params.values.indexOf(params.default)}>
      {params.values.map((value, index) => <option
        value={index}>{value}</option>)}
    </select>
  )
}

export interface EnumInputParams<T> {
  values: T[]
  default: T
  onSelect: (selected: T) => void
}
