// Follower DF
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

object FollowerDF {

  /**
    * This function should first read the graph located at the input path, it should compute the
    * follower count, and save the top 100 users to the output path in parquet format.
    *
    * It must be done using the DataFrame/Dataset API.
    *
    * It is NOT valid to do it with the RDD API, and convert the result to a DataFrame, nor to read
    * the graph as an RDD and convert it to a DataFrame.
    *
    * @param inputPath the path to the graph.
    * @param outputPath the output path.
    * @param spark the spark session.
    */
  def computeFollowerCountDF(inputPath: String, outputPath: String, spark: SparkSession): Unit = {
    // TODO: Calculate the follower count for each user
      // get dataset
    var schema = StructType(Array(
        StructField("follower",   StringType, true),
        StructField("followee",   StringType, true))
    )   
    val df = spark.read.format("csv")
        .option("sep","\t")
        .option("header", "true")
        .schema(schema)
        .load(inputPath)

    // TODO: Write the top 100 users to the above outputPath in parquet format
    val count_df = df.groupBy("followee")
                    .agg(
                      countDistinct("follower").as("count")
                    ).sort(desc("count")).limit(100)
    // Write output
    count_df.show()
    count_df.write.parquet(outputPath)
  }

  /**
    * @param args it should be called with two arguments, the input path, and the output path.
    */
  def main(args: Array[String]): Unit = {
    val spark = SparkUtils.sparkSession()

    val inputGraph = args(0)
    val followerDFOutputPath = args(1)

    computeFollowerCountDF(inputGraph, followerDFOutputPath, spark)
  }

}