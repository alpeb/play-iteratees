import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import java.io._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  val Dashes = "--".getBytes
  val CRLF = "\r\n".getBytes
  val WebkitBoundary = "----WebKitFormBoundaryD3OXAVAyBuRzJGDi".getBytes
  val ContentType = "Content-Type: audio/mp3".getBytes
  val fileWithoutId3= Seq[Byte](1, 2, 3, 4)

  def contentDisposition(filename: String) =
    s"""Content-Disposition: form-data; name="file"; filename="$filename"""".getBytes

  def framedBody(bytes: Seq[Byte], filename: String = "whateva.mp3") = 
    Dashes ++ WebkitBoundary ++ CRLF ++
    contentDisposition(filename) ++ CRLF ++
    ContentType ++ CRLF ++
    CRLF ++
    bytes ++ CRLF ++
    CRLF ++ CRLF ++
    WebkitBoundary ++ Dashes

  def requestHeaders(body: Seq[Byte]) = FakeHeaders(Seq(
    CONTENT_LENGTH -> Seq(body.size.toString),
    CONTENT_TYPE -> Seq(s"multipart/form-data; boundary=$WebkitBoundary")
  ))

  def file(filename: String): Seq[Byte] = {
    var file = None: Option[FileInputStream]
    var bytes = Seq[Byte]()
    try {
      file = Some(new FileInputStream(filename))
      var c = 0
      while ({c = file.get.read; c != -1}) {
        bytes = bytes :+ c.toByte
      }
    } finally {
      if (file.isDefined) file.get.close
    }

    bytes
  }

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Please submit your MP3 file:")
    }

    "Throw an exception for files that are not mp3" in new WithApplication {
      val body = framedBody(fileWithoutId3, "somedoc.doc")
      val headers = requestHeaders(body)

      val result = route(FakeRequest(POST, "/", headers, body)).get

      status(result) must throwA[RuntimeException](message = "File is not an mp3")
    }

    "Throw an exception for files that don't have an ID3 tag" in new WithApplication {
      val body = framedBody(fileWithoutId3)
      val headers = requestHeaders(body)

      val result = route(FakeRequest(POST, "/", headers, body)).get

      status(result) must throwA[RuntimeException](message = "File is not formated using the id3v2 standard")
    }

    "parse ID3v2.2 files correctly" in new WithApplication {
      val body = framedBody(file("test/id3v2.2.mp3"))
      val headers = requestHeaders(body)

      val result = route(FakeRequest(POST, "/", headers, body)).get

      status(result) must equalTo(OK)
      contentAsString(result) must contain ("charset=ISO-8859-1")
      contentAsString(result) must contain ("<b>id3 version:</b> 2.2<br>")
      contentAsString(result) must contain ("<b>title:</b> Types are Cool<br>")
      contentAsString(result) must contain ("<b>artist:</b> The Scala Dudes<br>")
      contentAsString(result) must contain ("<b>album:</b> Scala Hits<br>")
    }

    "parse ID3v2.3 files correctly" in new WithApplication {
      val body = framedBody(file("test/id3v2.3.mp3"))
      val headers = requestHeaders(body)

      val result = route(FakeRequest(POST, "/", headers, body)).get

      status(result) must equalTo(OK)
      contentAsString(result) must contain ("charset=UTF-8")
      contentAsString(result) must contain ("<b>id3 version:</b> 2.3<br>")
      contentAsString(result) must
        contain ("<b>title:</b> P\u0000l\u0000a\u0000y\u0000t\u0000a\u0000s\u0000t\u0000i\u0000c\u0000<br>")
      contentAsString(result) must
        contain ("<b>artist:</b> T\u0000h\u0000e\u0000 \u0000P\u0000l\u0000a\u0000y\u0000e\u0000r\u0000s\u0000<br>")
      contentAsString(result) must
        contain ("<b>album:</b> P\u0000l\u0000a\u0000y\u0000i\u0000n\u0000g\u0000 \u0000a\u0000r\u0000o\u0000u\u0000n\u0000d\u0000<br>")
      contentAsString(result) must
        contain ("title:</b> P\u0000l")
    }
  }
}
