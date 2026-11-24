package mpbuilder.platform
package db

/** A row of `(key, JSONB document)` — select it with `SELECT <col> AS key, doc FROM …`.
  * `A` needs a single-column codec, typically `given SqlSchema.Column[A] = Sql.jsonColumn`.
  */
final case class KeyedDoc[A](key: String, doc: A)
