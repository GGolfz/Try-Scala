object Main extends App {
  // Variable initialize
  val x = 2
  val y = 3
  // x = 5 // val is final, we cannot reassign the value
  println(x+y)
  var z = 6
  z = 4 // var is not final, we can reassign value to var
  println(z)
  // z = "test" // However, the reassigned value need to be same type
  var number: Int = 100 // variable initialize with specific type
  println(number)

  def add(x: Int, y: Int): Int = {
    return x + y
  }

  def minus(x: Int, y:Int) = x - y // Shorthand with implicit return type

  println(add(x,y))
  println(minus(z,y))
  // Array with map
  var defaultName = "Hello"
  var arr: Array[String] = Array.empty
  arr = arr :+ defaultName + "1"
  arr = arr :+ defaultName + "2"
  arr.foreach(i => {
    println(i)
  })

  if(arr.length == 2) {
    println("Array length is 2")
  }
  println("Array length is not 2: " + (if(arr.length == 2) "YEP!" else "NOPE"))
}