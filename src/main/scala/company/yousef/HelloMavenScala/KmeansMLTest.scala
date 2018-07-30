package company.yousef.HelloMavenScala
import org.apache.spark.mllib.clustering.{ KMeans, KMeansModel }
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.{ Row, SQLContext }
import scala.util.Random

final case class Email(id: Int, text: String)

object KmeansMLTest {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext(new SparkConf()
      .setAppName("Spark Kmeans Demo")
      .setMaster("local[2]"))

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    val emails = Seq(
      "This is an email from your lovely wife. Your mom says...",
      "SPAM SPAM spam",
      "Hello, We'd like to offer you").zipWithIndex.map(_.swap).toDF("id", "text").as[Email]

    println(emails.first())
    println(emails)
    import org.apache.spark.ml.feature._

    val tok = new RegexTokenizer()
      .setInputCol("text")
      .setOutputCol("tokens")
      .setPattern("\\W+")

    val hashTF = new HashingTF()
      .setInputCol("tokens")
      .setOutputCol("features")
      .setNumFeatures(20)

    val res2 = tok.transform _

    val preprocess = (res2).andThen(hashTF.transform)
    println(emails.toDF().first())
    val features = preprocess(emails.toDF())
    println(features.first())
    println(features.show())
    features.select('text, 'features).show(false)

    import org.apache.spark.ml.clustering.KMeans
    val kmeans = new KMeans
    val kmModel = kmeans.fit(features.toDF)

    println(kmModel.clusterCenters.map(_.toSparse).apply(0))

    val email = Seq("hello spam spam").toDF("text")
    val result = kmModel.transform(preprocess(email))
    .show(false)
    sc.stop()
  }

}