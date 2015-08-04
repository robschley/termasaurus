package util

import org.apache.commons.codec.binary.{ Base64 => Base64Codec }

/**
 * Base64 Utility
 */
object Base64 {

  private val codec = new Base64Codec

  // Encode a string to Base64
  def encode(str: String): String = new String(codec.encode(str.getBytes("UTF-8")))

  // Decode a Base64 encoded string.
  def decode(str: String): String = new String((new Base64Codec).decode(str.getBytes("UTF-8")))
}
