import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import {ApolloClient, ApolloProvider, gql, InMemoryCache} from '@apollo/client';
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import DogView from "./components/dog/DogView.tsx";
import AdvancedSearch from "./components/search/AdvancedSearch.tsx";
import {BasicProperty, ObjectDerivedProperty} from "./model/Property.ts";

const client = new ApolloClient({
  uri: 'http://localhost:3000/graphql',
  cache: new InMemoryCache(),
});

const router = createBrowserRouter([
  {
    path: "/",
    element: <App/>,
    children: [
      {
        path: "/dog/:dogId",
        element: <DogView/>
      },
      {
        path: "/search",
        element: <AdvancedSearch/>
      }
    ]
  }
]);

export const PropertiesContext = React.createContext({})

client.query({
  query: gql`
      query Properties {
          properties {
              name
              shortDisplayName
              displayName
              description
              valueType
              section
              options
          }
      }`
}).then(result => {
  const basicProperties = new Map(result.data.properties.map(p => [p.name, p]))
  // basicProperties.set("id", {name:"id", valueType:"String", shortDisplayName:"ID", displayName: "ID", description:"ID", section: "Intern"})
  basicProperties.set("fullName", {
    name: "fullName",
    valueType: "String",
    shortDisplayName: "GName",
    displayName: "Ganzer Name",
    description: "Ganzer Name",
    section: "Allgemein"
  })
  basicProperties.set("name", {
    name: "name",
    valueType: "String",
    shortDisplayName: "Name",
    displayName: "Name",
    description: "Vorname",
    section: "Allgemein"
  })
  basicProperties.set("gender", {
    name: "gender",
    valueType: "String",
    shortDisplayName: "Geschlecht",
    displayName: "Geschlecht",
    description: "Geschlecht",
    section: "Allgemein"
  })

  const allProperties = new Map(Array.from(basicProperties.entries()).map(([key, property]) => [key, new BasicProperty(property)]))
  // allProperties.set("litterId", new ObjectDerivedProperty("litter", "id",
  //   {name:"litterId", valueType:"String", shortDisplayName:"WId", displayName: "Wurf-ID", description:"ID des Wurfes", section: "Intern"}))
  allProperties.set("litterName", new ObjectDerivedProperty("litter", "fullName",
    {
      name: "litterName",
      valueType: "String",
      shortDisplayName: "WNm",
      displayName: "Wurf",
      description: "Name des Wurfes",
      section: "Allgemein"
    }))
  allProperties.set("litterInitials", new ObjectDerivedProperty("litter", "initials",
    {
      name: "litterInitials",
      valueType: "String",
      shortDisplayName: "WIn",
      displayName: "Wurf-Nummer",
      description: "Startbuchstaben des Wurfes",
      section: "Allgemein"
    }))
  allProperties.set("litterDate", new ObjectDerivedProperty("litter", "litterDate",
    {
      name: "litterDate",
      valueType: "Date",
      shortDisplayName: "Geb",
      displayName: "Geboren",
      description: "Geburtsdatum des Hundes",
      section: "Allgemein"
    }))

  // allProperties.set("kennelId", new ObjectDerivedProperty("kennel", "id",
  //   {name:"kennelId", valueType:"String", shortDisplayName:"ZId", displayName: "Zwinger-ID", description:"ID des Zwingers", section: "Intern"}))
  allProperties.set("kennelFullName", new ObjectDerivedProperty("kennel", "fullName",
    {
      name: "kennelFullName",
      valueType: "String",
      shortDisplayName: "ZNm",
      displayName: "Zwinger",
      description: "Gesamtname des Zwingers",
      section: "Allgemein"
    }))
  allProperties.set("kennelName", new ObjectDerivedProperty("kennel", "name",
    {
      name: "kennelName",
      valueType: "String",
      shortDisplayName: "ZGnm",
      displayName: "Zwinger-Name",
      description: "Name des Zwingers",
      section: "Allgemein"
    }))
  allProperties.set("kennelLink", new ObjectDerivedProperty("kennel", "link",
    {
      name: "kennelLink",
      valueType: "String",
      shortDisplayName: "ZName",
      displayName: "Zwinger-Verbindung",
      description: "Verbindung des Zwingers",
      section: "Allgemein"
    }))

  const properties = {
    basicProperties: basicProperties,
    allProperties: allProperties
  }

  const root = ReactDOM.createRoot(document.getElementById('root'));
  root.render(
    <React.StrictMode>
      <ApolloProvider client={client}>
        <PropertiesContext.Provider value={properties}>
          <RouterProvider router={router}/>
        </PropertiesContext.Provider>
      </ApolloProvider>,
    </React.StrictMode>
  );

  // If you want to start measuring performance in your app, pass a function
  // to log results (for example: reportWebVitals(console.log))
  // or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
  reportWebVitals();
})
