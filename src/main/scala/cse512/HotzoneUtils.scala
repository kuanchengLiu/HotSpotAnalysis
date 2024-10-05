package cse512

object HotzoneUtils {

  def ST_Contains(queryRectangle: String, pointString: String): Boolean = {
    // Parse the rectangle and point
    val rectangleCoords = queryRectangle.split(",")
    val pointCoords = pointString.split(",")

    // Extract the rectangle's bottom-left (x1, y1) and top-right (x2, y2) coordinates
    val x1 = rectangleCoords(0).toDouble
    val y1 = rectangleCoords(1).toDouble
    val x2 = rectangleCoords(2).toDouble
    val y2 = rectangleCoords(3).toDouble

    // Extract the point's (x, y) coordinates
    val x = pointCoords(0).toDouble
    val y = pointCoords(1).toDouble

    // Check if the point is within the bounds of the rectangle
    if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
      return true
    } else {
      return false
    }
  }
}
