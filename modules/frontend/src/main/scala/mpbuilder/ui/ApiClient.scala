package mpbuilder.ui

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Thenable.Implicits.*

import mpbuilder.api.*
import org.scalajs.dom
import org.scalajs.dom.{Headers, HttpMethod, RequestInit}
import zio.json.*

object ApiClient:

  def getCatalog: Future[Either[String, CatalogResponse]] =
    for
      response <- dom.fetch("/api/catalog")
      text     <- response.text()
    yield
      if response.ok then text.fromJson[CatalogResponse]
      else Left(s"Catalog request failed: HTTP ${response.status}")

  /** Left = accumulated validation/pricing errors from the server (422),
    * Right = the placed order.
    */
  def submitOrder(request: CreateOrderRequest): Future[Either[ApiError, OrderResponse]] =
    val init = new RequestInit {}
    init.method = HttpMethod.POST
    init.body = request.toJson
    init.headers = new Headers()
    init.headers.asInstanceOf[Headers].append("Content-Type", "application/json")

    for
      response <- dom.fetch("/api/orders", init)
      text     <- response.text()
    yield
      if response.status == 201 then
        text.fromJson[OrderResponse].left.map(msg => ApiError(List(unexpected(msg))))
      else
        Left(text.fromJson[ApiError].getOrElse(ApiError(List(unexpected(s"HTTP ${response.status}")))))

  private def unexpected(detail: String): ErrorDto =
    ErrorDto(
      "Unexpected",
      mpbuilder.domain.LocalizedText(s"Unexpected error: $detail", s"Neočekávaná chyba: $detail"),
    )
