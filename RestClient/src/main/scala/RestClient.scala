import HelperUtils.CreateLogger
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

final case class Requestdetails(T: String, dT: String)

//This program is the REST client implementation to search our log messages using the Lambda function deployed in AWS
object RestClient extends App {
  //Setting up the logger
  val logger = CreateLogger(classOf[RestClient.type])
  //Creating Akka HTTP Actors
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  logger.info(s"Akka Actors created")
  //Loading our configs from application.conf
  val requestconfig: Config = ConfigFactory.load("application.conf").getConfig("request")
  val lambdaconfig: Config = ConfigFactory.load("application.conf").getConfig("Lambda")
  logger.info(s"Configs loaded")
  import system.dispatcher
  //creating our JSON for the POST request
  val input: Requestdetails = Requestdetails(requestconfig.getString("T"), requestconfig.getString("dT"))
  private implicit val formats = DefaultFormats
  val json = write(input)
  logger.debug(s"Request JSON created")
  val lambda = lambdaconfig.getString("invokeURL")
  //Creating the GET request
  val getrequest = HttpRequest(
    method = HttpMethods.GET,
    uri = s"${lambda}?T=${input.T}&dT=${input.dT}"
  )
  logger.debug(s"GET request created")
  //Creating the POST request
  val postrequest = HttpRequest(
    method = HttpMethods.POST,
    uri = s"${lambda}",
    entity = HttpEntity(json)
  )
  logger.debug(s"POST request created")
  //Checking what request type the user has configured and calling that request
  if(requestconfig.getString("Type") == "GET")
    {
      logger.info(s"Sending request")
      logger.debug(s"Sending GET request")
      val responseFuture = Http().singleRequest(getrequest)
      responseFuture.flatMap(_.entity.toStrict(5 seconds)).map(_.data.utf8String).foreach(println)
    }
  else if(requestconfig.getString("Type") == "POST")
    {
      logger.info(s"Sending request")
      logger.debug(s"Sending POST request")
      val responseFuture = Http().singleRequest(postrequest)
      responseFuture.flatMap(_.entity.toStrict(5 seconds)).map(_.data.utf8String).foreach(println)
    }
  else{
      logger.error("Invalid Request type")
      println("***\nInvalid Request type\n***")
  }
  //This auto-terminates the program. Without this, the program would continue to run until the user terminates it.
 Thread.sleep(10000)
  logger.info(s"Shutting down client")
  system.terminate()
}