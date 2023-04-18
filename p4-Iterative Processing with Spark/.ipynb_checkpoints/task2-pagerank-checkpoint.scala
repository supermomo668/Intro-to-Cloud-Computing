object PageRank {

    // Do not modify
    val PageRankIterations = 10

    def calculatePageRank(
      inputGraphPath: String,
      outputPath: String,
      iterations: Int,
      spark: SparkSession
  ): Unit = {
    val sc = spark.sparkContext

    // TODO - Your code here
    // params
    val damping = 0.85
    // map followee as key
    val edgelist = sc.textFile(inputGraphPath)
    val followers_list = edgelist.map { // id(0)   // or use row(1)
      _.split("\\s+")(0)
    }
    // val n_followers = followers.count()
    // obtain all followee
    val followees_list = edgelist
      .map {
        _.split("\\s+")(1)
      }.distinct()
    // val n_followees = followees_list.count()
    // all body
    val all_ids = followers_list.union(followees_list).distinct()
    // val n_users = 4
    val n_users = if (inputGraphPath.startsWith("wasb://")) {
        1006458
    } else {
        all_ids.distinct().count() // already have
    }
    // dangnling ids
    var dangling_edges = all_ids.subtract(followers_list).map(id => (id, "e"))
    // Form adj graph
    // graph adj_list (no dangling)
    val glinks_all = edgelist.map { row =>
      { // graph edges
        val edge = row.split("\\s+")
        (edge(0), edge(1))
      }
    }.union(dangling_edges).groupByKey().cache()

    ////////////////////////////////////////////////////
    // Initialization
    // full adjacent list
    // ranks
    var ranks = glinks_all.mapValues { v => 1.0 / n_users }
    var followers_only_rank = all_ids.subtract(followees_list).map(id => (id, 0.0)).cache()
    var is_dangling: Boolean = false
    if (iterations > 0) {
      // only allow if iterations is not 0
      for (i <- 1 to iterations) {
        var dangling_ranks = sc.doubleAccumulator("sumAccumulator")
        val ranking =glinks_all.join(ranks).values // (CompactBuffer(2),0.25) (CompactBuffer(0, 2, 3),0.25) (CompactBuffer(3),0.25) (CompactBuffer(e),0.25)
        val scores = ranking.flatMap {
          case (followee, rank) => // contrib (url, rank)
            //
            var is_dangling: Boolean = followee.count { v => v == "e" } == 1
            if (!is_dangling) {
              // this is not a dangling edge
              followee.map(id => { // 0, 2, 3... 2, 3... e
                (id, rank / followee.size) // number of followee
              })
            } else {
              // summing up the dangling ranks
              followee.map(id => {
                dangling_ranks.add(rank)
              })
              List() // set as list
              // dangling_ranks.add(rank)  instead of +=
            }
        }
        scores.count()
        /// compute updated rank scores here
        // scores.count()//
        val dangling_sum = dangling_ranks.value
        ranks = scores.union(followers_only_rank)
          .reduceByKey(_ + _)
          .mapValues(scores =>
            // (1-damping) * dangling_ranks.value / n_users + damping * scores  // vert algo formula
            (1 - damping) / n_users + damping * (scores + dangling_sum / n_users)
          ) // (0,0.25) (1,0.25) (2,0.25) (3,0.25)
      }
    }
    // normalize
    val rank_sum = ranks.map(e => e._2).sum()
    // write to file
    //ranks.map{tup => s"${tup._1}\t${tup._2}"}.saveAsTextFile(outputPath)
    ranks.mapValues(s => s / rank_sum).foreach(tup => println(s"${tup._1} has rank:  ${tup._2} ."))
  }

  /** @param args
    *   it should be called with two arguments, the input path, and the output
    *   path.
    */
  def main(args: Array[String]): Unit = {
    val spark = SparkUtils.sparkSession()

    val inputGraph = args(0)
    val pageRankOutputPath = args(1)
    val PageRankIterations = 10
    calculatePageRank(inputGraph, pageRankOutputPath, PageRankIterations, spark)
  }
}
