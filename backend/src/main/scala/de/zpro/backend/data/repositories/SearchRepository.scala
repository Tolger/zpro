package de.zpro.backend.data.repositories

import de.zpro.backend.data.{DatabaseRequester, DbResult}

import scala.concurrent.{ExecutionContext, Future}

case class SearchRepository(connector: DatabaseRequester)(implicit private val context: ExecutionContext) {
  def quickSearch(query: String): Future[List[QuickSearchResult]] =
    if (query.isEmpty)
      Future.successful(List())
    else
      connector.quickSearch(expandQuery(query))
        .map(parseResultList)

  private def expandQuery(baseQuery: String): String =
    if (baseQuery.endsWith("*")) baseQuery
    else s"$baseQuery ${baseQuery.split(" ").lastOption.map(l => s"$l*").getOrElse("")}"

  private def parseResultList(data: List[DbResult]): List[QuickSearchResult] =
    data.map(parseResult).sorted

  private def parseResult(result: DbResult): QuickSearchResult = {
    val score = result("score").asDouble
    val node = result.getNode("node")
    QuickSearchResult(
      id = node("id").asString,
      name = node("fullName").asString,
      score = score,
      nodeType = node.dataType
    )
  }

}

case class QuickSearchResult(id: String, name: String, score: Double, nodeType: String) extends Ordered[QuickSearchResult] {
  override def compare(that: QuickSearchResult): Int = that.score compareTo this.score // reverse order (higher first)
}
