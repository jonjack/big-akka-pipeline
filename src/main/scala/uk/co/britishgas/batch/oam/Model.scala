package uk.co.britishgas.batch.oam

object Model {

  object Brands {
    sealed trait Brand
    case object BG extends Brand
    case object SE extends Brand
    val brands = Seq(BG, SE)
  }

  case class Customer (
                       title: String,
                       `first-name`: String,
                       surname: String,
                       email: String,
                       brands: Set[String],
                       status: String,
                       channel: String)

}