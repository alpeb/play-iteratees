package iteratees

import play.api.libs.iteratee._

object HelperIteratees {

  /** Advances the stream until the target string is reached */
  def forwardAfter(target: String, acum: String = ""): Iteratee[Byte, Unit] = Cont {
    case Input.Empty => forwardAfter(target)
    case Input.EOF => Done(Unit)
    case Input.El(byte) =>
      val expectedChar = target charAt acum.length
      if (byte == expectedChar.toByte) {
        if (acum.length < target.length - 1)
          forwardAfter(target, acum + expectedChar)
        else
          Done(Unit)
      } else {
        forwardAfter(target)
      }
  }

  /** Produces a string, until the target character is reached */
  def getUntil(target: Char, acum: String = ""): Iteratee[Byte, String] = Cont {
    case Input.Empty => getUntil(target, acum)
    case Input.EOF => Done(acum)
    case in @ Input.El(byte) =>
      if (byte == target.toByte) Done(acum, in) else getUntil(target, acum + byte.toChar)
  }

  /** Produces the next n bytes in a stream */
  def take(n: Int, acum: Seq[Byte] = Seq[Byte]()): Iteratee[Byte, Seq[Byte]] = Cont {
    case Input.Empty => take(n, acum)
    case Input.EOF => Done(acum)
    case in @ Input.El(byte) =>
      if (n == 0) Done(acum, in)
      else {
        val newAcum = acum :+ byte
        if (newAcum.length < n) take(n, newAcum) else Done(newAcum)
      }
  }
}
