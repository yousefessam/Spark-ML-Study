package company.yousef.HelloMavenScala

import org.apache.spark.sql.Dataset
import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.types.StructType
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.ml.Transformer
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions.col

class CustomTransformer(override val uid: String) extends Transformer {

  def this() = this(Identifiable.randomUID("My Transformer"))

  def copy(extra: ParamMap): CustomTransformer =
    {
      defaultCopy(extra)
    }

  override def transformSchema(schema: StructType): StructType =
    {
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
   //   val wordAgg = udf { in: Stt 
      df.select(col("*"), wordcount(df.col("happy_pandas")).as("happy_panda_counts"))

    }

}