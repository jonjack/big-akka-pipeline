package object utility {

  type Structure = List[(Int, Int)]

  object Structure {
    def apply(seq: (Int,Int)*): List[(Int, Int)] = seq.toList
  }

}
