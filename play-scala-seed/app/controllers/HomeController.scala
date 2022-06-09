package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import spray.json._
import play.api.libs.json.{JsValue => PJsValue}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

case class Book(id: Int, name: String, author: String, price: Double)
trait BookJSONProtocol extends DefaultJsonProtocol {
  implicit val bookFormat = jsonFormat4(Book)
}
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with BookJSONProtocol {
  var books: Map[Int,Book] = Map.empty

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("{'success': true}").as("application/json")
  }

  def getBooks(): Action[AnyContent] = Action { implicit  request: Request[AnyContent] =>
    Ok(books.values.toJson.toString).as("application/json")
  }
  def getBook(id: Int): Action[AnyContent] = Action {implicit request: Request[AnyContent] =>
    Ok(books.get(id).toJson.toString).as("application/json")
  }
  def addBook(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    var body = request.body
    var jsonValue: Option[PJsValue] = body.asJson
    jsonValue.map{ value =>
      val id: Int = (value \ "id").as[Int]
      val name: String = (value \ "name").as[String]
      val author: String = (value \ "author").as[String]
      val price: Double = (value \ "price").as[Double]

      books = books + ( id -> Book(id,name,author, price))
    }
    Ok("{'success':true}").as("application/json")
  }
  def removeBook(id: Int): Action[AnyContent] = Action {implicit request: Request[AnyContent] =>
    books = books.removed(id)
    Ok("{'success':true}").as("application/json")

  }
}
