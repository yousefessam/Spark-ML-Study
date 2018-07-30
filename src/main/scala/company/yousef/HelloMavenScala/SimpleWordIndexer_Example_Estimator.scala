package company.yousef.HelloMavenScala

import org.apache.spark.ml.param.Params
import org.apache.spark.ml.param.Param
import org.apache.spark.ml.Estimator
import org.apache.spark.sql.Dataset
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.types.StructType
import org.apache.spark.ml.Model
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.SQLContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

trait SimpleIndexerParams extends Params {
  final val inputCol = new Param[String](this, "inputCol", "The input column")
  final val outputCol = new Param[String](this, "outputCol", "The output column")

  def setInputCol(value: String) = set(inputCol, value)

  def setOutputCol(value: String) = set(outputCol, value)
}

class SimpleIndexerModel(override val uid: String, words: Array[String])
  extends Model[SimpleIndexerModel]
  with SimpleIndexerParams {

  override def copy(extra: ParamMap): SimpleIndexerModel =
    {
      defaultCopy(extra)
    }

  override def transformSchema(schema: StructType): StructType = {
    // Check that the input type is a string
    val idx = schema.fieldIndex($(inputCol))
    val field = schema.fields(idx)
    if (field.dataType != StringType) {
      throw new Exception(s"Input type ${field.dataType} did not match input type StringType")
    }
    // Add the return field
    schema.add(StructField($(outputCol), IntegerType, false))
  }

  private val labelToIndex: Map[String, Double] = words.zipWithIndex.
    map { case (x, y) => (x, y.toDouble) }.toMap

 
 
    
  override def transform(dataset: Dataset[_]): DataFrame = {
    val indexer = udf { label: String => labelToIndex(label) }
    val oddEven = udf { in: Double => { val in2 = in.toInt;
    if(in % 2 == 0){1} else 0} }
    dataset.select(col("*"),indexer(dataset($(inputCol)).cast(StringType)).as($(outputCol)),oddEven(indexer(dataset($(inputCol)).cast(StringType))).as("OddEven Result"))
  }
}

class SimpleIndexer(override val uid: String)
  extends Estimator[SimpleIndexerModel]
  with SimpleIndexerParams {

  def this() = this(Identifiable.randomUID("simpleindexer"))

  override def copy(extra: ParamMap): SimpleIndexer = {
    defaultCopy(extra)
  }

  override def transformSchema(schema: StructType): StructType = {
    // Check that the input type is a string
    val idx = schema.fieldIndex($(inputCol))
    val field = schema.fields(idx)
    if (field.dataType != StringType) {
      throw new Exception(s"Input type ${field.dataType} did not match input type StringType")
    }
    // Add the return field
    schema.add(StructField($(outputCol), IntegerType, false))
  }

  override def fit(dataset: Dataset[_]): SimpleIndexerModel =
    {
      import dataset.sparkSession.implicits._
      val words = dataset.select(dataset($(inputCol)).as[String]).distinct
        .collect()
      words.foreach(println)
      new SimpleIndexerModel(uid, words);
    }
}

object SimpleWordIndexer_Example_Estimators {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext(new SparkConf()
      .setAppName("Spark Kmeans Demo")
      .setMaster("local[2]"))

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    
 

    val training = Seq(
      (1, "Ahmed"),
      (2, "Saeed"),
      (3, "Yahia"),
      (4, "Nagy"),
      (5, "Gamal"),
      (6, "Sherif"),
      (7, "Gamal"),
      (8, "Ahmed")).toDF("id", "name")

    val myEstimator = new SimpleIndexer("Yousef")
    myEstimator.setInputCol("name")
    myEstimator.setOutputCol("nameDistinct")

    val myModel = myEstimator.fit(training.toDF())
    myModel.setInputCol("name")
    myModel.setOutputCol("nameDistinct")
    myModel.transform(training.toDF()).show()
    println(myEstimator.explainParams())
    println(myModel.explainParams())
    
    
  }
}