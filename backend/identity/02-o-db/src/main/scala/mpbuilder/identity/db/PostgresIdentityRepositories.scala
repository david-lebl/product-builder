package mpbuilder.identity
package db

import core.*
import kyo.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*

/** Schemas & JSONB column codecs for `private[identity]` aggregates (only visible inside the identity context). */
private[identity] object IdentityDbCodecs:
  given Schema[PasswordCredential]           = Schema.derived
  given Schema[OtpChallenge]                 = Schema.derived
  given Schema[StoredSession]                = Schema.derived
  given SqlSchema.Column[PasswordCredential] = Sql.jsonColumn
  given SqlSchema.Column[OtpChallenge]       = Sql.jsonColumn
  given SqlSchema.Column[StoredSession]      = Sql.jsonColumn

  def principalKey(p: Principal): (String, String) = p match
    case c: Principal.Customer => ("Customer", c.customerId.value)
    case e: Principal.Employee => ("Employee", e.employeeId.value)

import IdentityDbCodecs.{given, *}

private[identity] final class PostgresCredentialRepository(db: Database) extends CredentialRepository:

  override def findByEmail(email: EmailAddress): Option[PasswordCredential] < Async =
    db.run(sql"SELECT doc FROM identity.password_credentials WHERE email = ${email.value}".as[PasswordCredential].run.map(_.headOption))

  override def listAll: List[PasswordCredential] < Async =
    db.run(sql"SELECT doc FROM identity.password_credentials ORDER BY email".as[PasswordCredential].run.map(_.toList))

  override def save(c: PasswordCredential): Unit < Async =
    val (kind, ref) = principalKey(c.principal)
    db.tx(
      sql"""INSERT INTO identity.password_credentials (email, principal_kind, principal_ref, is_active, doc)
            VALUES (${c.email.value}, $kind, $ref, ${c.isActive}, $c)
            ON CONFLICT (email) DO UPDATE SET
              principal_kind = EXCLUDED.principal_kind,
              principal_ref  = EXCLUDED.principal_ref,
              is_active      = EXCLUDED.is_active,
              doc            = EXCLUDED.doc,
              updated_at     = now()""".execute
    ).unit

private[identity] final class PostgresOtpRepository(db: Database) extends OtpRepository:

  override def findById(id: OtpChallengeId): Option[OtpChallenge] < Async =
    db.run(sql"SELECT doc FROM identity.otp_challenges WHERE id = ${id.value}".as[OtpChallenge].run.map(_.headOption))

  override def save(ch: OtpChallenge): Unit < Async =
    db.tx(
      sql"""INSERT INTO identity.otp_challenges (id, expires_at, consumed, doc)
            VALUES (${ch.id.value}, ${ch.expiresAt}, ${ch.consumed}, $ch)
            ON CONFLICT (id) DO UPDATE SET
              expires_at = EXCLUDED.expires_at,
              consumed   = EXCLUDED.consumed,
              doc        = EXCLUDED.doc""".execute
    ).unit

private[identity] final class PostgresSessionRepository(db: Database) extends SessionRepository:

  override def findByToken(token: SessionToken): Option[StoredSession] < Async =
    db.run(sql"SELECT doc FROM identity.sessions WHERE token = ${token.value}".as[StoredSession].run.map(_.headOption))

  override def save(s: StoredSession): Unit < Async =
    val (kind, ref) = principalKey(s.principal)
    db.tx(
      sql"""INSERT INTO identity.sessions (token, principal_kind, principal_ref, expires_at, revoked, doc)
            VALUES (${s.token.value}, $kind, $ref, ${s.expiresAt}, ${s.revoked}, $s)
            ON CONFLICT (token) DO UPDATE SET
              expires_at = EXCLUDED.expires_at,
              revoked    = EXCLUDED.revoked,
              doc        = EXCLUDED.doc""".execute
    ).unit

  override def revoke(token: SessionToken): Unit < Async =
    db.tx(
      sql"""UPDATE identity.sessions
            SET revoked = TRUE, doc = jsonb_set(doc, '{revoked}', 'true'::jsonb)
            WHERE token = ${token.value}""".execute
    ).unit

/** PostgreSQL-backed identity repositories. */
private[identity] final case class IdentityRepositories(
    credentials: CredentialRepository,
    otps: OtpRepository,
    sessions: SessionRepository,
)

/** Public entry point used by the composition root (`backend/app`). */
object IdentityDb:
  val migrations: MigrationSet = MigrationSet("identity", "V1__identity.sql")

  private[identity] def repositories(db: Database): IdentityRepositories =
    IdentityRepositories(new PostgresCredentialRepository(db), new PostgresOtpRepository(db), new PostgresSessionRepository(db))
