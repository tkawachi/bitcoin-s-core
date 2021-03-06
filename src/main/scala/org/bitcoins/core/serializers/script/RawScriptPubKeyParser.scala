package org.bitcoins.core.serializers.script

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.protocol.script.{EmptyScriptPubKey, ScriptPubKey}
import org.bitcoins.core.script.constant.ScriptToken
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * Created by chris on 1/12/16.
 */
trait RawScriptPubKeyParser extends RawBitcoinSerializer[ScriptPubKey] {

  override def read(bytes : List[Byte]) : ScriptPubKey = {
    if (bytes.isEmpty) EmptyScriptPubKey
    else {
      val compactSizeUInt = CompactSizeUInt.parseCompactSizeUInt(bytes)
      //TODO: Figure out a better way to do this, we can theoretically have numbers larger than Int.MaxValue,
      //but scala collections don't allow you to use 'slice' with longs
      val len = Try(compactSizeUInt.num.toInt).getOrElse(Int.MaxValue)
      val scriptPubKeyBytes = bytes.slice(compactSizeUInt.size.toInt, len + compactSizeUInt.size.toInt)
      val script : List[ScriptToken] = ScriptParser.fromBytes(scriptPubKeyBytes)
      ScriptPubKey.fromAsm(script)
    }
  }

  override def write(scriptPubKey : ScriptPubKey) : String = scriptPubKey.hex
}

object RawScriptPubKeyParser extends RawScriptPubKeyParser
