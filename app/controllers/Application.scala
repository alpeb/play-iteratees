package controllers

import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global

import models._

object Application extends Controller {

  /** Enumeratee that transforms a stream of Array[Byte] into a stream of Byte */
  val toBytes: Enumeratee[Array[Byte], Byte] = Enumeratee.mapInputFlatten[Array[Byte]] {
    case Input.El(arr) => Enumerator[Byte](arr: _*)
    case Input.Empty => Enumerator.empty[Byte]
    case Input.EOF => Enumerator.eof[Byte]
  }

  val mp3MetadataParser = BodyParser { requestHeader =>
    toBytes &>> Mp3File.tagParser map (Right(_))
  }

  def index = Action {
    Ok(views.html.index())
  }

  def upload = Action(mp3MetadataParser) { request =>
    Ok(views.html.index(Some(request.body)))
  }
}
