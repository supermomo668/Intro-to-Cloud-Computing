import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.udf


object PageRank {

    // Do not modify
    val PageRankIterations = 10

  /**
    * Input graph is a plain text file of the following format:
    *   follower  followee
    
    * After calculating the page ranks of all the nodes in the graph,
    * the output should be written to `outputPath` in the following format:
    *   node  rank
    *
    * where node and rank are separated by `\t`.
    */
    
    /* recitaiton example :
    val links = spark.textFile(...).map(...).cache()
    var ranks = links.RDD// RDD
    for (i <- 1 to iter)
    {
      // build rdd (url, foat)
      val contribs = links.join(ranks).flatMap
      {
        case (url, (links, rank)) =>
        links.map(dest => dest, rank/links.size)
      }
      // sum contributions
      ranks = contribs.reduceByKey(+)
                      .mapValues(sum=> a/N+(1-a)*sum)
    }
    */
    def calculatePageRank(
        inputGraphPath: String,
        outputPath: String,
        iterations: Int,
        spark: SparkSession): Unit = {
        val sc = spark.sparkContext
        
        // TODO - Your code here
        // params
        val damp = 0.85 
        var sk_contrib = 0.0
        val iter = iterations
        val delim = "\t"
        // read schmea 
        val schemaString = "follower followeee"
        val graph_schema = StructType(schemaString.split(" ").map(
             name => StructField(name, StringType, true)))
                
        // map followee as key 
        val file_ = sc.textFile(inputGraphPath)
        val all_vert =  file_.map(row => row.split("\t")).map(x => (x(1), 1))   // map all vertice
        val n_verts = all_vert.count()   // total number of vertices
        
        // cached graph links
        val url_links = file_.map(row => row.split("\t")).map(x => (x(1), x(2))).distinct().groupByKey().cache()
        val graph_rdd = file_.map(_.split(delim)).map(e => (e(0).toInt, e(1).toInt))
        val ranking_now = url_links.map(row=> (row._1, 1.0/n_verts))
        val default_rank = 1/n_verts                 
        //        // number of followee per ID

        
        var ranks = url_links.map(e=> (e._1, e._2, default_rank))  //  map existing edge as 1
        
        /*  example
        val contribs = links.join(ranks).flatMap
          {
            case (url, (links, rank)) =>
            links.map(dest => dest, rank/links.size)
          }
          
        */
        val sink_rank = 0 
        
        for (i <- 1 to iter) {  // looop over iterations n_verts
            // compute contirbution

            val contributions = ranks.flatMap
              {
                case (url, (links, rank)) =>
                    links.map(dest => dest, rank/links.size)
              }
            
            val rank_sum = contributions.map(e => e._2).sum()
            val inv_sum =  1- rank_sum

            sink = inv_sum / n_verts
            //compute ranking
            val vert_rank = udf((x: Int) => (1-damp)/n_verts + damp*x + (1-damp) * sink)  // vert algo formula
            ranking_now =  contributions.reduceByKey(+).mapValues(vert_rank)
            //dampening factor // or use .reduceByKey(_ + )
            // now compute the sink contributios
            val sink_rank = 0.85* sink +  0.15/n_verts 
            ranks = url_links.leftOuterJoin(
                ranking_now).map{ 
                case (urls, (links, rank)) => (urls, links, rank.getOrElse(sink_rank))}.cache()
            
            }

        all_vert.leftOuterJoin(ranking_now).map{ case (urls, (links, rank)) => (urls, rank.getOrElse(sink_rank) )}.map(
            f => f._1 +"\t"+f._2).saveAsTextFile(outputPath)
    }


  /**
    * @param args it should be called with two arguments, the input path, and the output path.
    */
    def main(args: Array[String]): Unit = {
        val spark = SparkUtils.sparkSession()

        val inputGraph = args(0)
        val pageRankOutputPath = args(1)

        calculatePageRank(inputGraph, pageRankOutputPath, PageRankIterations, spark)
  }
}