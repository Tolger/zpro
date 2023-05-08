import {useContext, useState} from "react";
import {PropertyData, PropertyProvider} from "../../model/Property";
import {PropertiesContext} from "../../index.js"
import "./AdvancedSearch.css"
import Select from "react-select";
import makeAnimated from 'react-select/animated';

export default function AdvancedSearch() {
  const properties: PropertyProvider = useContext(PropertiesContext)


  const [filterValues, setFilterValues] = useState(new Map())

  function updateFilterValues(propertyName: string, value: string) {
    if (value)
      filterValues.set(propertyName, value)
    else
      filterValues.delete(propertyName)
    setFilterValues(new Map(filterValues))
  }

  const [comparators, setComparators] = useState(new Map())

  function updateComparators(propertyName: string, comparator: object) {
    if (comparator)
      comparators.set(propertyName, comparator.value)
    else {
      comparators.delete(propertyName)
      updateFilterValues(propertyName, null)
    }
    setComparators(new Map(comparators))
  }


  const customComponents = makeAnimated()
  customComponents["DropdownIndicator"] = () => null
  customComponents["IndicatorSeparator"] = () => null

  function comparatorSelect(propertyName: string, options: object[]): JSX.Element {
    return <Select options={options} isClearable placeholder={"Filter"}
                   components={customComponents}
                   className="advanced-search-filter-value-component advanced-search-filter-input"
                   onChange={comparator => updateComparators(propertyName, comparator)}/>
  }

  function optionsSelect(property: PropertyData, second: boolean = false, isMulti: boolean = false, clearable: boolean = true,
                         options: object[] = property.options.map(option => {
                           return {value: option, label: option}
                         })): JSX.Element {
    return <Select options={options} isMulti={isMulti} isClearable={clearable}
                   placeholder={property.displayName}
                   closeMenuOnSelect={!isMulti} components={customComponents}
                   className="advanced-search-filter-value-component advanced-search-filter-input"
                   onChange={(value: string) => updateFilterValues(property.name, value)}/>
  }

  function input(property: PropertyData, second: boolean = false, type: string = "") {
    return <input
      className="advanced-search-filter-value-component advanced-search-filter-input"
      onChange={e => updateFilterValues(property.name, e.target.value)}
      type={type}/>
  }

  function isActive(property: PropertyData): boolean {
    return comparators.get(property) || filterValues.get(property)
  }

  function renderInput(property: PropertyData) {
    switch (property.valueType) {
      case "Boolean":
        return optionsSelect(property, false, false, true, [
          {value: "true", label: "Ja"},
          {value: "false", label: "Nein"}
        ])
      case "Boolean-Tested":
        return optionsSelect(property, false, false, true, [
          {value: "true", label: "Ja"},
          {value: "false", label: "Nein"},
          {value: "tested", label: "Getestet"},
          {value: "notTested", label: "Nicht Getestet"}
        ])
      case "Enum-Unordered-Int":
      case "Enum-Unordered-String":
        return optionsSelect(property, false, true)
      case "Enum-Ordered-Int":
      case "Enum-Ordered-String":
        return [
          comparatorSelect(property.name, [
            {value: "equals", label: "gleich"},
            {value: "unequal", label: "nicht"},
            {value: "lessThan", label: "weniger als"},
            {value: "moreThan", label: "mehr als"},
            {value: "between", label: "zwischen"}
          ]),
          comparators.get(property.name) ? optionsSelect(property) : null,
          comparators.get(property.name) === "between" ? [
            <span className="advanced-search-filter-value-component">und</span>,
            optionsSelect(property, true)
          ] : null
        ]
      case "Long":
        return [
          comparatorSelect(property.name, [
            {value: "equals", label: "gleich"},
            {value: "unequal", label: "nicht"},
            {value: "lessThan", label: "weniger als"},
            {value: "moreThan", label: "mehr als"},
            {value: "between", label: "zwischen"}
          ]),
          comparators.get(property.name) ? input(property) : null,
          comparators.get(property.name) === "between" ? [
            <span className="advanced-search-filter-value-component">und</span>,
            input(property, true)
          ] : null
        ]
      case "Date":
        return [
          comparatorSelect(property.name, [
            {value: "equals", label: "gleich"},
            {value: "unequal", label: "nicht"},
            {value: "lessThan", label: "vor"},
            {value: "moreThan", label: "nach"},
            {value: "between", label: "zwischen"}
          ]),
          comparators.get(property.name) ? input(property, false, "date") : null,
          comparators.get(property.name) === "between" ? [
            <span className="advanced-search-filter-value-component">und</span>,
            input(property, true, "date")
          ] : null
        ]
      case "String":
        return [
          comparatorSelect(property.name, [
            {value: "contains", label: "beinhaltet"},
            {value: "equals", label: "gleich"},
            {value: "containsNot", label: "nicht"},
            {value: "startsWith", label: "beginnt mit"},
            {value: "endsWith", label: "endet mit"}
          ]),
          comparators.get(property.name) ? input(property) : null
        ]
      default:
        return <input/>
    }
  }


  const initialOutputs: Map<String, Boolean> = new Map(Array.from(properties.allProperties.keys()).map(p => [p, false]))
  const [activeOutputs, setActiveOutputs] = useState(initialOutputs)
  const [activePreset, setActivePreset] = useState(-1)

  function switchOutput(name: String) {
    activeOutputs.set(name, !activeOutputs.get(name))
    setActivePreset(-1)
    setActiveOutputs(new Map(activeOutputs))
  }

  function applyPreset(id: number, toActivate: string[]) {
    const newOutputs = Array.from(properties.allProperties.keys()).map(p => [p, toActivate.includes(p)])
    setActivePreset(id)
    setActiveOutputs(new Map(newOutputs))
  }

  function renderOutputButton(property: PropertyData): JSX.Element {
    return <div
      className={`advanced-search-output-button advanced-search-output-button-property ${activeOutputs.get(property.name) ? "advanced-search-output-button-property-active" : ""}`}
      onClick={() => switchOutput(property.name)} title={property.description}>
      {property.displayName}
    </div>
  }


  const groupedProperties: Map<String, PropertyData[]> =
    Array.from(properties.allProperties.values()).reduce((grouped, property) => {
      const section = property.section
      const oldGroup = grouped.get(section)
      if (!oldGroup)
        grouped.set(section, [property])
      else
        grouped.set(section, [property, ...oldGroup])
      return grouped
    }, new Map<String, PropertyData[]>())

  return (
    <div className="advanced-search-container">
      <h1>Filter</h1>
      <div className="advanced-search-filter-container">
        {Array.from(groupedProperties.entries()).map(([key, sectionProperties]) => [
          <h2>{key}</h2>,
          <div className="advanced-search-filter-section">
            {Array.from(sectionProperties.values()).sort((p1: PropertyData, p2: PropertyData) => p1.displayName > p2.displayName ? 1 : p1.displayName === p2.displayName ? 0 : -1).map(property =>
              <div
                className={`advanced-search-filter ${isActive(property.name) ? "advanced-search-filter-active" : ""}`}>
                <div className="advanced-search-filter-label"
                     title={property.description}>
                  {property.displayName}
                </div>
                <div className="advanced-search-filter-value">
                  {renderInput(property)}
                  {isActive(property.name) ? [
                    <span>in</span>,
                    <input type="number" defaultValue="1"
                           className="advanced-search-filter-value-component advanced-search-filter-input advanced-search-filter-generations"/>
                  ] : null}
                </div>
              </div>)}
          </div>
        ])}
      </div>
      <h1>Ausgabe</h1>
      <div className="advanced-search-preset-container">
        <div
          className={`advanced-search-output-button advanced-search-output-button-preset ${activePreset === 0 ? "advanced-search-output-button-preset-active" : ""}`}
          onClick={() => applyPreset(0, [])}>
          Keine
        </div>
        <div
          className={`advanced-search-output-button advanced-search-output-button-preset ${activePreset === 2 ? "advanced-search-output-button-preset-active" : ""}`}
          onClick={() => applyPreset(2, ["fullName", "hd", "pl", "d", "dw", "zzu", "zge", "gender", "organisationId"])}>
          Standard
        </div>
        <div
          className={`advanced-search-output-button advanced-search-output-button-preset ${activePreset === 1 ? "advanced-search-output-button-preset-active" : ""}`}
          onClick={() => applyPreset(1, ["fullName"])}>
          Name
        </div>
        <div
          className={`advanced-search-output-button advanced-search-output-button-preset ${activePreset === 3 ? "advanced-search-output-button-preset-active" : ""}`}
          onClick={() => applyPreset(3, ["fullName", "color"])}>
          Farbe
        </div>
      </div>
      <br/>
      <div className="advanced-search-output-container">
        {Array.from(groupedProperties.entries()).map(([key, sectionProperties]) => [
          <h3>{key}</h3>,
          <div className="advanced-search-output-section">
            {Array.from(sectionProperties.values()).sort((p1: PropertyData, p2: PropertyData) => p1.displayName > p2.displayName ? 1 : p1.displayName === p2.displayName ? 0 : -1)
              .map(property => renderOutputButton(property))}
          </div>

        ])}
      </div>
      <div className="advanced-search-submit">Suchen</div>
    </div>
  )
}
