package companyTestNBC

import org.apache.spark.sql.Dataset
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.linalg.DenseMatrix
import org.apache.spark.ml.classification.Classifier
import org.apache.spark.sql.types.DoubleType
import org.apache.spark.ml.classification.ClassificationModel
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions.count
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.ml.util.MetadataUtils
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.mllib.classification.{ NaiveBayes, NaiveBayesModel }
import org.apache.spark.mllib.util.MLUtils

case class LabeledToken(label: Double, index: Integer)

class SimpleNaiveBayes(val uid: String)
  extends Classifier[Vector, SimpleNaiveBayes, SimpleNaiveBayesModel] {

  def this() = this(Identifiable.randomUID("simple-naive-bayes"))

  override def copy(extra: ParamMap) = {
    defaultCopy(extra)
  }
  override def train(ds: Dataset[_]): SimpleNaiveBayesModel =
    {
      import ds.sparkSession.implicits._

      ds.cache()

      // Compute the number of documents
      val numDocs = ds.count

      // Get the number of classes.
      // Note this estimator assumes they start at 0 and go to numClasses
      val numClasses = getNumClasses(ds)

      // Get the number of features by peaking at the first row
      val numFeatures: Integer = ds.select(col($(featuresCol))).head
        .get(0).asInstanceOf[Vector].size

      // Determine the number of records for each class
      val groupedByLabel = ds.select(col($(labelCol)).as[Double]).groupByKey(x => x)

      val classCounts = groupedByLabel.agg(count("*").as[Long])
        .sort(col("value"))
        .collect()
        .toMap

      // Select the labels and features so we can more easily map over them.
      // Note: we do this as a DataFrame using the untyped API because the Vector
      // UDT is no longer public.
      val df = ds.select(col($(labelCol)).cast(DoubleType), col($(featuresCol)))

      // Figure out the non-zero frequency of each feature for each label and
      // output label index pairs using a case clas to make it easier to work with.
      val labelCounts: Dataset[LabeledToken] = df.flatMap {
        case Row(label: Double, features: Vector) =>
          features.toArray.zip(Stream from 1)
            .filter { vIdx => vIdx._2 == 1.0 }
            .map { case (v, idx) => LabeledToken(label, idx) }
      }

      // Use the typed Dataset aggregation API to count the number of non-zero
      // features for each label-feature index.
      val aggregatedCounts: Array[((Double, Integer), Long)] = labelCounts
        .groupByKey(x => (x.label, x.index))
        .agg(count("*").as[Long]).collect()

      val theta = Array.fill(numClasses)(new Array[Double](numFeatures))

      // Compute the denominator for the general prioirs
      val piLogDenom = math.log(numDocs + numClasses)

      // Compute the priors for each class
      val pi = classCounts.map {
        case (_, cc) => math.log(cc.toDouble) - piLogDenom
      }.toArray

      // For each label/feature update the probabilities
      aggregatedCounts.foreach {
        case ((label, featureIndex), count) =>
          // log of number of documents for this label + 2.0 (smoothing)
          val thetaLogDenom = math.log(
            classCounts.get(label).map(_.toDouble).getOrElse(0.0) + 2.0)
          theta(label.toInt)(featureIndex) = math.log(count + 1.0) - thetaLogDenom
      }
      // Unpersist now that we are done computing everything
      ds.unpersist()
      // Construct a model
      new SimpleNaiveBayesModel(uid, numClasses, numFeatures, Vectors.dense(pi),
        new DenseMatrix(numClasses, theta(0).length, theta.flatten, true))
    }

}

object NBC_estimator_example {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext(new SparkConf()
      .setAppName("Spark Kmeans Demo")
      .setMaster("local[2]"))

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    /*


    val data = MLUtils.loadLibSVMFile(sc, "src/main/resources/sample_libsvm_data.txt")


    val nData= data.map{ x=>
      x.features.toDense.toArray
    }

   val nn = sc.parallelize(nData.collect()).toDF()
   nn.show()


    val Array(training, test) = data.randomSplit(Array(0.6, 0.4))

    val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

    val predictionAndLabel = test.map(p => (model.predict(p.features), p.label))
    val accuracy = 1.0 * predictionAndLabel.filter(x => x._1 == x._2).count() / test.count()
    predictionAndLabel.foreach(println)
    println(accuracy)

    val df = mySpark.read.option("header", "false").option("delimiter", " ").csv("src/main/resources/sample_svm_data.txt")
    df.show()


    df.printSchema()
    val assembler = new VectorAssembler()
      .setInputCols(Array("_c1", "_c2", "_c3"))
      .setOutputCol("features")

    val res = df.withColumn("_c0", df("_c0").cast(DoubleType))
      .withColumn("_c1", df("_c1").cast(IntegerType))
      .withColumn("_c2", df("_c2").cast(IntegerType))
      .withColumn("_c3", df("_c3").cast(IntegerType))

    val transformed = assembler.transform(res)
    transformed.show()
    transformed.select("_c0").groupBy("_c0").count().show()

    val myClassifierTest = new SimpleNaiveBayes("Yousef")
    myClassifierTest.setFeaturesCol("features")
    myClassifierTest.setLabelCol("_c0")
    println(myClassifierTest.getFeaturesCol)
    println(myClassifierTest.getLabelCol)

    val myModel = myClassifierTest.train(transformed)

    val testData = transformed.select(col("features"))

    val test = Array(
       Vectors.dense(0.0,2.0,0.0),
       Vectors.dense(0.0,0.0,2.0),
       Vectors.dense(2.0,0.0,0.0)
    )
    println(myModel.predictRaw(test.apply(0)))
    myModel.transform(testData).show()

    //val model = NaiveBayes.train(transformed, lambda = 1.0, modelType = "multinomial")

    return
  */

    val training = Seq(
      (1, "Ahmed", 100, "M"),
      (2, "Sara", 55, "F"),
      (3, "Noha", 59, "F"),
      (4, "Nagy", 95, "M"),
      (5, "Gamal", 86, "M"),
      (6, "Sherif", 69, "M"),
      (7, "Gamela", 70, "F"),
      (8, "Nora", 45, "F")).toDF("id", "name", "Weight", "Gender")

    val ConvertMyData = udf { x: String => if (x == "M") 1.toDouble else 0.toDouble }
    val training2 = training.select(col("id"), col("name"), col("Weight"), ConvertMyData(col("Gender")).as("Gender"))
    training2.show()

    val assembler = new VectorAssembler()
      .setInputCols(Array("Weight", "Weight"))
      .setOutputCol("features")

    val transformed = assembler.transform(training2)
    transformed.show()
    val myClassifierTest = new SimpleNaiveBayes("Yousef")
    myClassifierTest.setFeaturesCol("features")
    myClassifierTest.setLabelCol("Gender")
    println(myClassifierTest.getFeaturesCol)
    println(myClassifierTest.getLabelCol)

    val myModel = myClassifierTest.train(transformed)

    val testData = transformed.select(col("features"))

    myModel.transform(testData).show()

    sc.stop()
  }
}

// Simplified Naive Bayes Model
case class SimpleNaiveBayesModel(
  override val uid:         String,
  override val numClasses:  Int,
  override val numFeatures: Int,
  val pi:                   Vector,
  val theta:                DenseMatrix) extends ClassificationModel[Vector, SimpleNaiveBayesModel] {

  override def copy(extra: ParamMap) = {
    defaultCopy(extra)
  }

  // We have to do some tricks here because we are using Spark's
  // Vector/DenseMatrix calculations - but for your own model don't feel
  // limited to Spark's native ones.
  val negThetaArray = theta.values.map(v => math.log(1.0 - math.exp(v)))
  val negTheta = new DenseMatrix(numClasses, numFeatures, negThetaArray, true)
  val thetaMinusNegThetaArray = theta.values.zip(negThetaArray)
    .map { case (v, nv) => v - nv }
  val thetaMinusNegTheta = new DenseMatrix(
    numClasses, numFeatures, thetaMinusNegThetaArray, true)
  val onesVec = Vectors.dense(Array.fill(theta.numCols)(1.0))
  val negThetaSum: Array[Double] = negTheta.multiply(onesVec).toArray

  // Here is the prediciton functionality you need to implement - for ClassificationModels
  // transform automatically wraps this - but if you might benefit from broadcasting your model or
  // other optimizations you can also override transform.
  def predictRaw(features: Vector): Vector = {
    // Toy implementation - use BLAS or similar instead
    // the summing of the three vectors but the functionality isn't exposed.
    Vectors.dense(thetaMinusNegTheta.multiply(features).toArray.zip(pi.toArray)
      .map { case (x, y) => x + y }.zip(negThetaSum).map { case (x, y) => x + y })
  }
}