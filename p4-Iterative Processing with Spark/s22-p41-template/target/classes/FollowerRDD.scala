import org.apache.spark.SparkContext

// FollowerRDD
object FollowerRDD {

  /** This function should first read the graph located at the input path, it
    * should compute the follower count, and save the top 100 users to the
    * output path with userID and count **tab separated**.
    *
    * It must be done using the RDD API.
    *
    * @param inputPath
    *   the path to the graph.
    * @param outputPath
    *   the output path.
    * @param sc
    *   the SparkContext.
    */
    
  def computeFollowerCountRDD(
      inputPath: String,
      outputPath: String,
      sc: SparkContext
  ): Unit = {
    // TODO: Calculate the follower count for each user
    //  read file with rdd
    val file_rdd = sc.textFile(input_path)
    val graph = file_rdd
      .map(row => row.split("\t"))
      .map(x => (x(1), x(0))) // map followee as key
    // TODO: Write the top 100 users to the outputPath with userID and count **tab separated**
    val popular_followee = graph
      //.distinct() // remove duplicate
      .map(x => (x._1, 1)) // count
      .reduceByKey(_ + _) // sum counts
      .sortBy(x => x._2, ascending=false) // sort by number of followers
      .take(100)      // .keys.take(100)
      // take top 100
      
    val result = sc.parallelize(popular_followee).map(f => f._1 +"\t"+f._2)
    println(result.take(5).mkString(" "))
    // popular_followee.map(r => (r._1+"\t"r._2.toString)).saveAsTextFile("outputCSV2")
    result.saveAsTextFile(outputPath)
  }

  /** @param args
    *   it should be called with two arguments, the input path, and the output
    *   path.
    */
  def main(args: Array[String]): Unit = {
    val spark = SparkUtils.sparkSession()
    val sc = spark.sparkContext

    val inputGraph = args(0)
    val followerRDDOutputPath = args(1)

    computeFollowerCountRDD(inputGraph, followerRDDOutputPath, sc)
  }
}
