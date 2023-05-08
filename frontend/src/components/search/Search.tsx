import "./Search.css"
import {gql, useApolloClient} from "@apollo/client";
import {Link} from "react-router-dom";
import {useState} from "react";

export default function Search() {
  const [active, setActive] = useState(false)
  const [results, setResults] = useState([])
  const client = useApolloClient()

  const graphqlRequest = gql`
      query QuickSearch($text: String!) {
          quickSearch(query: $text) {
              id
              name
              nodeType
          }
      }`

  function translateLabel(toTranslate: string): string {
    switch (toTranslate) {
      case "Dog":
        return "Hund"
      case "Litter":
        return "Wurf"
      case "Kennel":
        return "Zwinger"
      case "Person":
        return "Person"
    }
  }

  function updateResults(query: string) {
    client.query({
      query: graphqlRequest,
      variables: {
        text: query
      }
    }).then(result => {
      if (result.error) {
        console.log(error)
        throw Error("Error updating search results")
      }
      setResults(result.data.quickSearch)
    })
  }

  return [
    active ? <div className="search-background"
                  onClick={() => setActive(false)}></div> : null,
    <div className="search-container">
      <input className="search-input"
             onChange={e => updateResults(e.target.value)}
             onClick={() => setActive(true)}/>
      {active ? results.map(result =>
        <Link to={`dog/${result.id}`} className="search-result"
              onClick={() => setActive(false)}>
          <div className="search-result-name">{result.name}</div>
          <div
            className="search-result-type">{translateLabel(result.nodeType)}</div>
        </Link>
      ) : null}
    </div>
  ]
}

export interface SearchState {
  query: string
  results: SearchResult[]
}

export interface SearchResult {
  id: string
  name: string
  nodeType: string
  score: string
}
