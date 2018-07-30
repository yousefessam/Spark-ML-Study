package company.yousef.HelloMavenScala
import scala.math.random
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

object WordCounter {

  def main(args: Array[String]) {

    val sc = new SparkContext(new SparkConf()
      .setAppName("Spark WordCount")
      .setMaster("local[2]"))
      
    println(args.length)
    var path = "src/main/resources/README.md"
    if(args.length > 0)
      path = args(0)
    val textFile = sc.textFile(path)
    val tfData = textFile.flatMap(line => line.split(" "))
    val countPrep = tfData.map(w => (w, 1))
    val counts = countPrep.reduceByKey(_+_)
    val sortedCounts = counts.sortBy(kv => kv._2, false)
    counts.foreach(println)
    sortedCounts.saveAsTextFile("C:/Users/lenovo/res5")
    sc.stop()
  }
}