package mpbuilder.identity
package impl
package crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import zio.*

import java.security.SecureRandom
import java.util.Base64

/** Argon2id password hashing.
  *
  * Argon2id rather than PBKDF2 or bcrypt because it is memory-hard: the 64 MiB working set makes
  * GPU and ASIC cracking far more expensive per guess, which is the whole point of a password hash.
  *
  * The encoded form carries the algorithm and its cost parameters, so raising the cost later does
  * not invalidate existing hashes — old ones keep verifying against their own parameters.
  */
private[identity] final class Argon2PasswordHasher(
    iterations: Int = 3,
    memoryKb: Int = 65536,
    parallelism: Int = 1,
) extends PasswordHasher:

  private val random = new SecureRandom()
  private val encoder = Base64.getEncoder.withoutPadding()
  private val decoder = Base64.getDecoder

  def hash(plain: String): UIO[PasswordHash] =
    ZIO.succeedBlocking {
      val salt = new Array[Byte](16)
      random.nextBytes(salt)
      val digest = derive(plain, salt, iterations, memoryKb, parallelism)
      PasswordHash(
        s"argon2id$$$iterations$$$memoryKb$$$parallelism$$${encoder.encodeToString(salt)}$$${encoder.encodeToString(digest)}"
      )
    }

  def verify(plain: String, against: PasswordHash): UIO[Boolean] =
    ZIO.succeedBlocking {
      against.encoded.split('$') match
        case Array("argon2id", iter, mem, par, saltB64, hashB64) =>
          val result = scala.util.Try {
            val salt = decoder.decode(saltB64)
            val expected = decoder.decode(hashB64)
            val actual = derive(plain, salt, iter.toInt, mem.toInt, par.toInt)
            constantTimeEquals(expected, actual)
          }
          result.getOrElse(false)
        case _ => false
    }

  private def derive(plain: String, salt: Array[Byte], iter: Int, mem: Int, par: Int): Array[Byte] =
    val params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
      .withVersion(Argon2Parameters.ARGON2_VERSION_13)
      .withIterations(iter)
      .withMemoryAsKB(mem)
      .withParallelism(par)
      .withSalt(salt)
      .build()

    val generator = new Argon2BytesGenerator()
    generator.init(params)
    val out = new Array[Byte](32)
    generator.generateBytes(plain.toCharArray, out)
    out

  /** Compares every byte regardless of where the first difference is, so the comparison time does
    * not reveal how much of a guess was correct.
    */
  private def constantTimeEquals(a: Array[Byte], b: Array[Byte]): Boolean =
    if a.length != b.length then false
    else a.indices.foldLeft(0)((acc, i) => acc | (a(i) ^ b(i))) == 0

private[identity] object Argon2PasswordHasher:
  val layer: ULayer[PasswordHasher] = ZLayer.succeed(new Argon2PasswordHasher())
