package company.yousef.HelloMavenScala
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.linalg.{ Vector, Vectors }
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

object LogisticRegressionPiplineDemo {

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf()
      .setAppName("Spark Kmeans Demo")
      .setMaster("local[2]"))
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Prepare training data from a list of (label, features) tuples.
    val training = Seq(
      (1.0, Vectors.dense(0.0, 1.1, 0.1)),
      (0.0, Vectors.dense(2.0, 1.0, -1.0)),
      (0.0, Vectors.dense(2.0, 1.3, 1.0)),
      (1.0, Vectors.dense(0.0, 1.2, -0.5))).toDF("label", "features")

    // Create a LogisticRegression instance. This instance is an Estimator.
    val lr = new LogisticRegression()
    lr.setMaxIter(10).setRegParam(0.01)

    // Print out the parameters, documentation, and any default values.
    println("LogisticRegression parameters:\n" + lr.explainParams() + "\n")

    // Learn a LogisticRegression model.
    //This uses the parameters stored in lr.
    val model1 = lr.fit(training)
    println("Model 1 was fit using parameters: " + model1.parent.extractParamMap)

    model1.transform(training).show()

    // We may alternatively specify parameters using a ParamMap,
    // which supports several methods for specifying parameters.
    val paramMap = ParamMap(lr.maxIter -> 20)
      .put(lr.maxIter, 30) // Specify 1 Param. This overwrites the original maxIter.
      .put(lr.regParam -> 0.1, lr.threshold -> 0.55) // Specify multiple Params.

    // One can also combine ParamMaps.
    val paramMap2 = ParamMap(lr.probabilityCol -> "myProbability") // Change output column name.
    val paramMapCombined = paramMap ++ paramMap2

    // Now learn a new model using the paramMapCombined parameters.
    // paramMapCombined overrides all parameters set earlier via lr.set* methods.
    val model2 = lr.fit(training, paramMapCombined)

    println("Model 2 was fit using parameters: " + model2.parent.extractParamMap)
    model2.transform(training).show()
    println(paramMapCombined.toString())

    model2.transform(training)
      .select("features", "label", "myProbability", "prediction")
      .collect()
      .foreach {
        case Row(features: Vector, label: Double, prob: Vector, prediction: Double) =>
          println(s"($features, $label) -> prob=$prob, prediction=$prediction")
      }
    sc.stop()
    

  }

}