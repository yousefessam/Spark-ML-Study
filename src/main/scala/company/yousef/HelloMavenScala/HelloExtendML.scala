package company.yousef.HelloMavenScala

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.{ Row, SQLContext }
import scala.util.Random
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.Transformer
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions.col
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.linalg.{ Vector, Vectors }
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.Row
import org.apache.spark.ml.param.Param

class HardCodedWordCountStage(override val uid: String)
  extends Transformer {
  def this() = this(Identifiable.randomUID("hardcodedwordcount"))

  def copy(extra: ParamMap): HardCodedWordCountStage = {
    defaultCopy(extra)
  }

  override def transformSchema(schema: StructType): StructType = {
    // Check that the input type is a string
    val idx = schema.fieldIndex("happy_pandas")
    val field = schema.fields(idx)
    if (field.dataType != StringType) {
      throw new Exception(s"Input type ${field.dataType} did not match input type StringType")
    }
    // Add the return field
    schema.add(StructField("happy_panda_counts", IntegerType, false))
  }

  def transform(df: Dataset[_]): DataFrame =
    {
      val wordcount = udf { in: String => in.split(" ").size }
      df.select(col("*"), wordcount(df.col("happy_pandas")).as("happy_panda_counts"))

    }

}

object HelloExtendML {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext(new SparkConf()
      .setAppName("Spark Kmeans Demo")
      .setMaster("local[2]"))

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    // Prepare training data from a list of (label, features) tuples.
    val training = Seq(
      (1, "A B C D"),
      (2, "A B C E"),
      (3, "A B C S asd "),
      (4, "A B C hhafs asdasd")).toDF("label", "happy_pandas")

    val add1 = udf { x: Int => x + 1 }
    val repeatColumnByLabel = udf { (x: String, y: Int) => x * y }
    training.select(col("*"), training.col("label").as("New Lable")).show()
    training.select(col("*"), add1(training.col("label")).as("New Lable2")).show()
    training.select(col("*"), repeatColumnByLabel(training.col("happy_pandas"),training.col("label")).as("New Lable3")).show()
    
    val training2 = Seq(
      (1, "A B C X Y"),
      (2, "A B C X X"),
      (3, "A B C W W Z"),
      (4, "A B C Love Cat in May")).toDF("label", "happy_pandas2")

    val training3 = training.join(training2, "label")
    training3.show()

    val model1 = new HardCodedWordCountStage("Yousef")
    val res1 = model1.transform(training)
    res1.show()

    val model2 = new ConfigurableWordCount("Yousef2")
    model2.setInputCol("happy_pandas2")
    model2.setOutputCol("My Defined Output")
    val res2 = model2.transform(training3)
    res2.show()
    sc.stop()
  }
}

class ConfigurableWordCount(override val uid: String)
  extends Transformer {
  final val inputCol = new Param[String](this, "inputCol", "The input column")
  final val outputCol = new Param[String](this, "outputCol", "The output column");

  def setInputCol(value: String): this.type = set(inputCol, value)
  def setOutputCol(value: String): this.type = set(outputCol, value)

  def this() = this(Identifiable.randomUID("configurablewordcount"))

  def copy(extra: ParamMap): HardCodedWordCountStage = {
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

  def transform(df: Dataset[_]): DataFrame = {
    val wordcount = udf { in: String => in.split(" ").size }
    df.select(col("*"), wordcount(df.col($(inputCol))).as($(outputCol)))
  }
}
