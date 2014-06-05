package models

import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global

import iteratees.HelperIteratees._

object Mp3File {

  case class Metadata(
      id3Version: String,
      textEncoding: String = "",
      title: String = "",
      artist: String = "",
      album: String = "",
      tagSize: Int,
      parsedBytes: Int = 0) {

    def completed =
      parsedBytes >= tagSize ||
      (id3Version.nonEmpty && title.nonEmpty && artist.nonEmpty && album.nonEmpty)
  }

  /**
    * Parses the file tag (first bytes) and produces a completed metadata object.
    * 
    * The process is as follows:
    *   - Skip until header containing "filename"
    *   - Grab file name and check it's an mp3 file
    *   - Skip past the header to the beginning of the file payload
    *   - Check the file starts with the "ID3" identifier
    *   - Grab the spec version number
    *   - Grab the tag size
    *   - Initialize the metadata object
    *   - Parse the frames according to the spec version
    */
  def tagParser: Iteratee[Byte, Metadata] = for {
    _             <- forwardAfter("filename=\"")
    fileName      <- getUntil('"')
    _             <- if (isMp3File(fileName)) Done[Byte, Unit](Unit)
                     else Error[Byte]("File is not an mp3", Input.Empty)
    _             <- forwardAfter("\r\n\r\n")
    identifier    <- take(3) map bytes2PlainString
    _             <- if (identifier  == "ID3") Done[Byte, Unit](Unit)
                     else Error[Byte]("File is not formated using the id3v2 standard", Input.Empty)
    majorVersion  <- take(1) map (bytes => if (bytes.head == 0x02) "2.2" else "2.3")
    _             <- take(2) // ignore minor version byte and flags byte
    size          <- take(4) map to7BitPadded32UInt
    initMetadata  <- Done[Byte, Metadata](Metadata(id3Version = majorVersion, tagSize = size))
    fullMetadata  <- if (majorVersion == "2.2") ID3v2_2framesParser(initMetadata)
                     else ID3v2_3framesParser(initMetadata)
  } yield fullMetadata

  /**
    * Parses the ID3v2.2 frames and produces the the completed metadata object
    *
    * @see http://id3.org/id3v2-00
    */
  private def ID3v2_2framesParser(metadata: Metadata): Iteratee[Byte, Metadata] = {
    if (metadata.completed)
      Done[Byte, Metadata](metadata)
    else for {
      frameId         <- take(3) map bytes2PlainString
      frameSize       <- take(3) map to24BitUInt
      textEncoding    <- take(1) map byte2Encoding
      frameContent    <- take(frameSize - 1) map (bytes => new String(bytes.toArray, textEncoding))
      tempMetadata    <- {
        val counter = metadata.parsedBytes + frameSize + 10
        frameId match {
          case "TT2" => ID3v2_2framesParser(metadata.copy(title = frameContent, parsedBytes = counter))
          case "TP1" => ID3v2_2framesParser(metadata.copy(artist = frameContent, parsedBytes = counter))
          case "TAL" => ID3v2_2framesParser(metadata.copy(album = frameContent, parsedBytes = counter))
          case _ => ID3v2_2framesParser(metadata.copy(parsedBytes = counter))
        }
      }
    } yield tempMetadata.copy(textEncoding = textEncoding)
  }

  /**
    * Parses the ID3v2.3 frames and produces the the completed metadata object
    *
    * @see http://id3.org/id3v2.3.0
    */
  private def ID3v2_3framesParser(metadata: Metadata): Iteratee[Byte, Metadata] = {
    if (metadata.completed)
      Done[Byte, Metadata](metadata)
    else for {
      frameId         <- take(4) map bytes2PlainString
      frameSize       <- take(4) map to32BitUInt
      _               <- take(2) // discard flags bytes
      textEncoding    <- take(1) map byte2Encoding
      frameContent    <- take(frameSize - 1) map skipBom map (bytes => new String(bytes.toArray, textEncoding))
      tempMetadata    <- {
        val counter = metadata.parsedBytes + frameSize + 10
        frameId match {
          case "TIT2" => ID3v2_3framesParser(metadata.copy(title = frameContent, parsedBytes = counter))
          case "TPE1" => ID3v2_3framesParser(metadata.copy(artist = frameContent, parsedBytes = counter))
          case "TALB" => ID3v2_3framesParser(metadata.copy(album = frameContent, parsedBytes = counter))
          case _ => ID3v2_3framesParser(metadata.copy(parsedBytes = counter))
        }
      }
    } yield tempMetadata.copy(textEncoding = textEncoding)
  }

  /** True if file has an "mp3" extension */
  private def isMp3File(fileName: String): Boolean =
    """(?i)^.*\.mp3$""".r.findFirstIn(fileName).nonEmpty

  private def bytes2PlainString(bytes: Seq[Byte]) = new String(bytes.toArray, "ISO-8859-1")

  private def to7BitPadded32UInt(bytes: Seq[Byte]): Int =
    ((bytes(0) & 0xFF) << 21) + ((bytes(1) & 0xFF) << 14) + ((bytes(2) & 0xFF) << 7) + (bytes(3) & 0xFF).toInt

  private def to24BitUInt(bytes: Seq[Byte]): Int =
    ((bytes(0) & 0xFF) << 16) + ((bytes(1) & 0xFF) << 8) + (bytes(2) & 0xFF).toInt

  private def to32BitUInt(bytes: Seq[Byte]): Int =
    ((bytes(0) & 0xFF)<< 24) + ((bytes(1) & 0xFF) << 16) + ((bytes(2) & 0xFF) << 8) + (bytes(3) & 0xFF).toInt

  private def byte2Encoding(bytes: Seq[Byte]): String = if (bytes.head == 0x01) "UTF-8" else "ISO-8859-1"

  /** Sometimes when strings are in UTF-8, there's a Unicode BOM we need to skip */
  private def skipBom(bytes: Seq[Byte]): Seq[Byte] =
    if (bytes.length > 1 && (bytes(0) & 0xFF) == 0xFF && (bytes(1) & 0xFF) == 0xFE) {
      bytes.drop(2)
    } else bytes
}

