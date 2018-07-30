package company.yousef.HelloMavenScala

import org.nd4j.linalg.factory.Nd4j
import org.nd4s.Implicits._
object TestMath {

  def methodA(s: String) = println(s)
  def methodB(f: () => String) = println(f())

  def f = "foo"
  def f2() = "foo2"
  val sqr = (x: Int) => x * x
  val sqr2 = (x: Int) => sqr(x)
  val sqr3 = (x: Int) => sqr2(x)
  val sqr3_expandedVersion = (x: Int) =>
    (
      (x: Int) => sqr(x))(x)

  def f3() = "foo33"
  val f2fun1 = f3 // f2fun is a string, not a function
  val f2fun2: () => String = f3 // however, this works!
  val f2fun3 = f3 _ // this too!

  def main(args: Array[String]): Unit = {

    println(f2fun1)
    println(f2fun2())
    println(f2fun3())
    
    println(f)
    methodA(f)
    //methodB(f)// error!
    //methodA(f()) // error!
    //methodB(f()) // error!

    println(f2)
    methodA(f2)
    methodB(f2)
    methodA(f2())

    println(sqr(4))
    println(sqr2(4))
    println(sqr3(4))
    println(sqr3_expandedVersion(4))
    //methodB(f2()) // error!
  }

}