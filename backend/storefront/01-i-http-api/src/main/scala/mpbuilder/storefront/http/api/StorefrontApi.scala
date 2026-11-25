package mpbuilder.storefront
package http.api

import mpbuilder.api.ApiConventions

/** URL layout of the customer-facing Storefront BFF (`/api/v1/storefront`), shared by the kyo-http
  * routes (`02-i-http`) and the HTTP clients (`modules/ui-http-client`).
  */
object StorefrontApi:
  val Base: String                   = s"/api/${ApiConventions.apiVersion}/storefront"
  def seg(raw: String): String       = ApiConventions.segment(raw)
  def path(segments: String*): String = (Base +: segments).mkString("/")

  object Tags:
    val Catalog  = "Storefront · Catalog & Product Builder"
    val Auth     = "Storefront · Authentication"
    val Cart     = "Storefront · Cart"
    val Checkout = "Storefront · Checkout"
    val Me       = "Storefront · My Account"
    val Artwork  = "Storefront · Artwork"
