package controllers

import javax.inject.Inject

import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

import models.AcceptHeaderExtractors

class UsersController @Inject()(cc: ControllerComponents) extends AbstractController(cc)
  with AcceptHeaderExtractors
{

  /** A stub for the /users endpoint which simply echos the body back with a 201 Created status */
  def postw() = Action { implicit request: Request[AnyContent] =>
    println("BODY: " + request.body.toString)
    Created(request.body.toString)
  }

  def post = Action(parse.json) { implicit request =>
    Ok("Got request [" + request + "]")
    render {
      case Accept.JsonApi() => Ok("Got Json")
    }
  }

}


//Created 201 ["003005400529"]
//BODY[
//{"jsonapi":{"version":"1.0"},"data":{"id":"003005400529","type":"users","attributes":{"brands":["SE"],
//"surname":"Radhakrishnan","channel":"PPOT3","title":"Mr","first-name":"Aravindhan",
//"email":"aravindhan.radhakrishnan1@britishgas.co.uk","status":"active"}}
//}
//]
