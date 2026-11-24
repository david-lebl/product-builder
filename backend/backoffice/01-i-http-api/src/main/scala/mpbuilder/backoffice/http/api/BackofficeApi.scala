package mpbuilder.backoffice
package http.api

import mpbuilder.api.ApiConventions

/** URL layout of the role-based Staff Backoffice BFF (`/api/v1/admin`), shared by the kyo-http routes
  * (`02-i-http`) and the HTTP clients (`modules/ui-http-client`). Every endpoint except login requires
  * `Authorization: Bearer <staff SessionToken>`; role checks are enforced by `BackofficeFacade`.
  */
object BackofficeApi:
  val Base: String                    = s"/api/${ApiConventions.apiVersion}/admin"
  def seg(raw: String): String        = ApiConventions.segment(raw)
  def path(segments: String*): String = (Base +: segments).mkString("/")

  object Tags:
    val Auth      = "Backoffice · Auth"
    val Dashboard = "Backoffice · Dashboard"
    val Orders    = "Backoffice · Order Intake"
    val Artwork   = "Backoffice · Artwork Review"
    val Catalog   = "Backoffice · Catalog Manager"
    val Pricing   = "Backoffice · Pricing Manager"
    val Customers = "Backoffice · Customers"
    val Staff     = "Backoffice · Staff Directory"
    val Settings  = "Backoffice · Store Settings"
