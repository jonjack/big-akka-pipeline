package uk.co.britishgas.streams.oam

object Model {

  case class Customer (title: String, `first-name`: String, surname: String, email: String,
                       brands: Set[String], status: String, channel: String)

}