package non_blocking

import io.gatling.core.config.Protocol

import scala.concurrent.duration._
import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.core.structure.PopulatedScenarioBuilder
import io.gatling.http.Predef._

import scalaj.http.Http

/**
 * Created by michal on 02.08.15.
 */
class BasicSimulation extends Simulation {
  val targetHostPorts = getTargetHostPorts
  val simulationType = System.getProperty("SIMULATION_TYPE", "sync")
  val numOfUsers = getNumOfUsersFast(targetHostPorts.length)
  val numOfUsersSlow = getNumOfUsersSlow(targetHostPorts.length)
  val rampUpTime = Integer.getInteger("RAMPUP", 10)
  val numOfRepeatsPerUser : Int = Integer.getInteger("REPEATS", 60)
  val scenarioName = System.getProperty("SCENARIO_NAME", "")
  val httpResource = determineResource(simulationType)

  before {
    if (! simulationType.contains("node")) {
      for (hostPort <- targetHostPorts) {
        println(s"Starting monitoring for host $hostPort ...")
        callMonitoringOperation("start", hostPort)
      }
      println("Started monitoring.")
    }
  }

  after {
    if (! simulationType.contains("node")) {
      for (hostPort <- targetHostPorts) {
        println(s"Stopping monitoring for host $hostPort ...")
        callMonitoringOperation("stop", hostPort)
      }
      println("Stopped monitoring.")
    }
  }

  var myScenarios : List[PopulatedScenarioBuilder] = Nil
  var myProtocols : List[Protocol] = Nil
  for (hostPort <- targetHostPorts) {
    myProtocols ::= http
      .baseURL(s"http://$hostPort")

    val feederFast = csv("product_ids_fast.csv").circular
    val feederSlow = csv("product_ids_slow.csv").circular

    val scnFast = scenario(s"Get Product details fast $hostPort").feed(feederFast)
      .repeat(numOfRepeatsPerUser, "i") { exec(http("request_fast")
        .get(httpResource)
        .queryParam("id", "${product_id}"))
        .pause(1)
      }

    val scnSlow = scenario(s"Get Product details slow $hostPort").feed(feederSlow)
      .repeat(numOfRepeatsPerUser, "i") { exec(http("request_slow")
        .get(httpResource)
        .queryParam("id", "${product_id}"))
        .pause(1)
      }

    if (numOfUsers > 0) {
      myScenarios ::= scnFast.inject(rampUsers(numOfUsers) over rampUpTime)
    }
    if (numOfUsersSlow > 0) {
      myScenarios ::= scnSlow.inject(rampUsers(numOfUsersSlow) over rampUpTime)
    }

  }
  setUp(myScenarios).protocols(myProtocols)

  def determineResource(simulationType: String) = simulationType match {
    case "sync" => "/product"
    case "async_http" => "/productHttpAsync"
    case "async_http_db" => "/productHttpDbAsync"
    case "sync_withdesc" => "/productWithDescription"
    case "async_http_db_withdesc" => "/productWithDescriptionHttpDbAsync"
    case "nodejs" => "/productNodeJs"
  }

  def callMonitoringOperation(operation : String, hostPort : String) {
    val monitoringName = s"${simulationType}_${numOfUsers}_fast_${numOfUsersSlow}_slow_$scenarioName"
    val timeoutMillis = (10, SECONDS).toMillis
    val response = Http(s"http://$hostPort/monitoring").timeout(timeoutMillis.toInt, timeoutMillis.toInt).postForm(Seq("operation" -> operation, "name" -> monitoringName)).asString
    if (response.isError) {
      println(response)
      throw new IllegalStateException("Monitoring operation failed")
    }
  }

  def getTargetHostPorts: Array[String] = {
    System.getProperty("HOST_PORT", "localhost:8080").split(",")
  }

  def getNumOfUsersFast(numberOfHosts : Int): Int = {
    getNumOfUsersPerHost(Integer.getInteger("USERS", 48), numberOfHosts)
  }

  def getNumOfUsersSlow(numberOfHosts : Int): Int = {
    getNumOfUsersPerHost(Integer.getInteger("USERS_SLOW", 12), numberOfHosts)
  }

  def getNumOfUsersPerHost(totalNumberOfUsers : Int, numberOfHosts: Int): Int = {
    val userPerHost = totalNumberOfUsers.toFloat / numberOfHosts
    userPerHost.toInt
  }
}

