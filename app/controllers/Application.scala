package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def view(view: String) = Action {
    view match {
      case "home.html" => Ok(views.html.home())
      case _ => NotFound
    }
  }
}
