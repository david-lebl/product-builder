package mpbuilder.identity
package db

import com.augustnagro.magnum.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import core.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*
import zio.*

/** Codecs for `private[identity]` aggregates (only visible inside the identity context). */
private[identity] object IdentityDbCodecs:
  given JsonValueCodec[PasswordCredential] = JsonCodecMaker.make(config)
  given JsonValueCodec[OtpChallenge]       = JsonCodecMaker.make(config)
  given JsonValueCodec[StoredSession]      = JsonCodecMaker.make(config)

  def principalKey(p: Principal): (String, String) = p match
    case c: Principal.Customer => ("Customer", c.customerId.value)
    case e: Principal.Employee => ("Employee", e.employeeId.value)

import IdentityDbCodecs.{given, *}

private[identity] final class PostgresCredentialRepository(db: Database) extends CredentialRepository:

  override def findByEmail(email: EmailAddress): UIO[Option[PasswordCredential]] =
    db.run(
      sql"SELECT doc::text FROM identity.password_credentials WHERE email = ${email.value}"
        .query[String].run().headOption.map(fromJsonUnsafe[PasswordCredential])
    )

  override def listAll: UIO[List[PasswordCredential]] =
    db.run(
      sql"SELECT doc::text FROM identity.password_credentials ORDER BY email"
        .query[String].run().toList.map(fromJsonUnsafe[PasswordCredential])
    )

  override def save(c: PasswordCredential): UIO[Unit] =
    val (kind, ref) = principalKey(c.principal)
    val json        = toJson(c)
    db.tx(
      sql"""INSERT INTO identity.password_credentials (email, principal_kind, principal_ref, is_active, doc)
            VALUES (${c.email.value}, $kind, $ref, ${c.isActive}, $json::jsonb)
            ON CONFLICT (email) DO UPDATE SET
              principal_kind = EXCLUDED.principal_kind,
              principal_ref  = EXCLUDED.principal_ref,
              is_active      = EXCLUDED.is_active,
              doc            = EXCLUDED.doc,
              updated_at     = now()""".update.run()
    ).unit

private[identity] final class PostgresOtpRepository(db: Database) extends OtpRepository:

  override def findById(id: OtpChallengeId): UIO[Option[OtpChallenge]] =
    db.run(
      sql"SELECT doc::text FROM identity.otp_challenges WHERE id = ${id.value}"
        .query[String].run().headOption.map(fromJsonUnsafe[OtpChallenge])
    )

  override def save(ch: OtpChallenge): UIO[Unit] =
    val json = toJson(ch)
    db.tx(
      sql"""INSERT INTO identity.otp_challenges (id, expires_at, consumed, doc)
            VALUES (${ch.id.value}, ${ch.expiresAt}, ${ch.consumed}, $json::jsonb)
            ON CONFLICT (id) DO UPDATE SET
              expires_at = EXCLUDED.expires_at,
              consumed   = EXCLUDED.consumed,
              doc        = EXCLUDED.doc""".update.run()
    ).unit

private[identity] final class PostgresSessionRepository(db: Database) extends SessionRepository:

  override def findByToken(token: SessionToken): UIO[Option[StoredSession]] =
    db.run(
      sql"SELECT doc::text FROM identity.sessions WHERE token = ${token.value}"
        .query[String].run().headOption.map(fromJsonUnsafe[StoredSession])
    )

  override def save(s: StoredSession): UIO[Unit] =
    val (kind, ref) = principalKey(s.principal)
    val json        = toJson(s)
    db.tx(
      sql"""INSERT INTO identity.sessions (token, principal_kind, principal_ref, expires_at, revoked, doc)
            VALUES (${s.token.value}, $kind, $ref, ${s.expiresAt}, ${s.revoked}, $json::jsonb)
            ON CONFLICT (token) DO UPDATE SET
              expires_at = EXCLUDED.expires_at,
              revoked    = EXCLUDED.revoked,
              doc        = EXCLUDED.doc""".update.run()
    ).unit

  override def revoke(token: SessionToken): UIO[Unit] =
    db.tx(
      sql"""UPDATE identity.sessions
            SET revoked = TRUE, doc = jsonb_set(doc, '{revoked}', 'true'::jsonb)
            WHERE token = ${token.value}""".update.run()
    ).unit

/** Public entry point used by the composition root (`backend/app`). */
object IdentityDb:
  val migrations: MigrationSet = MigrationSet("identity")

  private[identity] val repositories: URLayer[Database, CredentialRepository & OtpRepository & SessionRepository] =
    ZLayer.fromZIOEnvironment(
      ZIO.serviceWith[Database](db =>
        ZEnvironment[CredentialRepository, OtpRepository, SessionRepository](
          new PostgresCredentialRepository(db),
          new PostgresOtpRepository(db),
          new PostgresSessionRepository(db),
        )
      )
    )
