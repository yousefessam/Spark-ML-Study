package company.yousef.HelloMavenScala
import org.apache.spark.mllib.clustering.{ KMeans, KMeansModel }
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import scala.util.Random

object KmeansDemo {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext(new SparkConf()
      .setAppName("Spark Kmeans Demo")
      .setMaster("local[2]"))
  
    // $example on$
    // Load and parse the data
    val data = sc.textFile("src/main/resources/kmeans_data.txt")
    val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble))).cache()

    // Cluster the data into two classes using KMeans
    val numClusters = 2
    val numIterations = 20
    val clusters = KMeans.train(parsedData, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = clusters.computeCost(parsedData)
    println(s"Within Set Sum of Squared Errors = $WSSSE")

    val r = new Random()
    // Save and load model
    clusters.save(sc, "src/main/resources/KMeansModel"+r.nextInt().toString())
    val sameModel = KMeansModel.load(sc, "src/main/resources/KMeansModel")
    // $example off$
    val myVec = Vectors.dense(1,2,3)
   // println(myVec)
    println(sameModel.predict(myVec))
    println(sameModel.clusterCenters.foreach(println))
    sc.stop()

  }
}