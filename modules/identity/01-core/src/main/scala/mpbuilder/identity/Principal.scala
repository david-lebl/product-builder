package mpbuilder.identity

/** What a signed-in party is allowed to do.
  *
  * Grouped rather than flat: almost every authorisation check asks "is this staff?", not "is this
  * an operator, or a manager, or an admin?". Adding a fifth staff role later must not require
  * revisiting every check.
  */
sealed trait Role:
  def name: String

object Role:

  /** Anyone who works for the shop. */
  sealed trait Staff extends Role

  case object Customer extends Role:
    val name = "Customer"

  case object Operator extends Staff:
    val name = "Operator"

  case object Manager extends Staff:
    val name = "Manager"

  case object Admin extends Staff:
    val name = "Admin"

  val all: Set[Role] = Set(Customer, Operator, Manager, Admin)

  def parse(raw: String): Option[Role] = all.find(_.name.equalsIgnoreCase(raw))

/** A verified identity, as every other context sees it.
  *
  * `customerId` links to the customers context. It is an `Option` because staff accounts have no
  * customer profile, and a customer profile can exist without ever having a login.
  */
final case class Principal(
    userId: String,
    email: String,
    roles: Set[Role],
    customerId: Option[String],
):
  def isStaff: Boolean = roles.exists {
    case _: Role.Staff => true
    case _             => false
  }

  def has(role: Role): Boolean = roles.contains(role)

/** Issued on a successful sign-in.
  *
  * Two tokens: a short-lived access token sent with every request, and a long-lived refresh token
  * used only to mint new access tokens. A leaked access token stops working in minutes.
  */
final case class AuthTokens(
    accessToken: String,
    refreshToken: String,
    expiresInSeconds: Long,
)

/** A pending one-time-code challenge. The code itself is never returned to the caller — it is
  * delivered out of band (e-mail/SMS). `deliveredTo` is masked for display.
  */
final case class OtpChallenge(
    challengeId: String,
    deliveredTo: String,
    expiresInSeconds: Long,
)
