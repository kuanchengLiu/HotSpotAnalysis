package cse512

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions._

object HotcellAnalysis {

  def runHotcellAnalysis(spark: SparkSession, pointPath: String): DataFrame = {
    // Load the original data from a data source
    var pickupInfo = spark.read
      .format("com.databricks.spark.csv")
      .option("delimiter", ";")
      .option("header", "false")
      .load(pointPath)
    pickupInfo.createOrReplaceTempView("nyctaxitrips")

    // Assign cell coordinates based on pickup points
    spark.udf.register(
      "CalculateX",
      (pickupPoint: String) => HotcellUtils.CalculateCoordinate(pickupPoint, 0)
    )
    spark.udf.register(
      "CalculateY",
      (pickupPoint: String) => HotcellUtils.CalculateCoordinate(pickupPoint, 1)
    )
    spark.udf.register(
      "CalculateZ",
      (pickupTime: String) => HotcellUtils.CalculateCoordinate(pickupTime, 2)
    )

    // Calculate the cell coordinates for each point
    pickupInfo = spark.sql(
      "select CalculateX(nyctaxitrips._c5) as x, CalculateY(nyctaxitrips._c5) as y, CalculateZ(nyctaxitrips._c1) as z from nyctaxitrips"
    )
    pickupInfo.createOrReplaceTempView("pickupInfo")

    // Define the min and max of x, y, z
    val minX = math.floor(-74.50 / HotcellUtils.coordinateStep).toInt
    val maxX = math.floor(-73.70 / HotcellUtils.coordinateStep).toInt
    val minY = math.floor(40.50 / HotcellUtils.coordinateStep).toInt
    val maxY = math.floor(40.90 / HotcellUtils.coordinateStep).toInt
    val minZ = 1
    val maxZ = 31

    // Calculate the total number of cells
    val numCells = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1)

    // Step 1: Count the number of points in each cell
    val pointCountsDf = spark.sql(
      s"""
    select x, y, z, count(*) as pointCount
    from pickupInfo
    where x >= $minX and x <= $maxX and y >= $minY and y <= $maxY and z >= $minZ and z <= $maxZ
    group by x, y, z
    """
    )
    pointCountsDf.createOrReplaceTempView("pointCounts")

    // Step 2: Calculate the mean and standard deviation for G-Score computation
    val statsDf = spark.sql(
      """
    select avg(pointCount) as mean, stddev_pop(pointCount) as stddev
    from pointCounts
    """
    )
    val stats = statsDf.collect()(0)
    val mean = stats.getDouble(0)
    val stddev = stats.getDouble(1)

    // Step 3: Calculate neighbor sum and neighbor count
    val neighborsDf = spark.sql(
      """
    select 
      pc1.x as x, 
      pc1.y as y, 
      pc1.z as z, 
      sum(pc2.pointCount) as neighborSum, 
      count(pc2.pointCount) as numNeighbors
    from pointCounts pc1, pointCounts pc2
    where 
      abs(pc1.x - pc2.x) <= 1 and 
      abs(pc1.y - pc2.y) <= 1 and 
      abs(pc1.z - pc2.z) <= 1
    group by pc1.x, pc1.y, pc1.z
    """
    )
    neighborsDf.createOrReplaceTempView("neighborsDf")

    // Step 4: Calculate the G-Score for each cell
    spark.udf.register(
      "GScore",
      (neighborSum: Int, numNeighbors: Int) => {
        HotcellUtils.calculateGScore(
          neighborSum,
          numNeighbors,
          mean,
          stddev,
          numCells
        )
      }
    )

    val gScoreDf = spark.sql(
      """
    select x, y, z, GScore(neighborSum, numNeighbors) as gScore
    from neighborsDf
    order by gScore desc
    limit 50
    """
    )

    // Step 5: Return the result sorted by G-score
    gScoreDf.show() // Optional: Show the result for debugging purposes
    return gScoreDf
  }

}
