package non_blocking

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
 * Created by michal on 02.08.15.
 */
class BasicSimulation extends Simulation {
  val targetHostPort = System.getProperty("HOST_PORT", "localhost:8080")
  val simulationType = System.getProperty("SIMULATION_TYPE", "sync")
  val numOfUsers = Integer.getInteger("USERS", 48)
  val numOfUsersSlow = Integer.getInteger("USERS_SLOW", 12)
  val rampUpTime = Integer.getInteger("RAMPUP", 15)
  val numOfRepeatsPerUser : Int = Integer.getInteger("REPEATS", 60)

  val httpResource = BasicSimulation.determineResource(simulationType)

  val httpConf = http
    .baseURL(s"http://$targetHostPort")

  val feederFast = csv("product_ids_fast.csv").circular
  val feederSlow = csv("product_ids_slow.csv").circular

  val scnFast = scenario("Get Product details fast").feed(feederFast)
    .repeat(numOfRepeatsPerUser, "i") { exec(http("request_fast")
      .get(httpResource)
      .queryParam("id", "${product_id}"))
      .pause(1)
  }

  val scnSlow = scenario("Get Product details slow").feed(feederSlow)
    .repeat(numOfRepeatsPerUser, "i") { exec(http("request_slow")
    .get(httpResource)
    .queryParam("id", "${product_id}"))
    .pause(1)
  }

  var myScenarios = scnFast.inject(rampUsers(numOfUsers) over rampUpTime)::Nil
  if (numOfUsersSlow > 0) {
    myScenarios ::= scnSlow.inject(rampUsers(numOfUsersSlow) over rampUpTime)
  }

  setUp(myScenarios :_*).protocols(httpConf)

}

object BasicSimulation {
  def determineResource(simulationType: String) = simulationType match {
    case "sync" => "/product"
    case "async_http" => "/productHttpAsync"
    case "async_http_db" => "/productHttpDbAsync"
  }
}