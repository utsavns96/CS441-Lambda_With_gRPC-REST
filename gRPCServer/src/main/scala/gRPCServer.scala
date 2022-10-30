package com.grpcserver

import HelperUtils.CreateLogger
import com.example.protos.hw2grpc.{Reply, Request, logfindGrpc}
import akka.actor.ActorSystem

import scala.concurrent.Await
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import com.grpcserver.gRPCServer.{lambdaconfig, logger}
import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.concurrent.{ExecutionContext, Future}

//This program creates our gRPC Server to take requests from our gRPC client, and connects to the Lambda function using Akka HTTP Actors
object gRPCServer {
  val logger = CreateLogger(classOf[gRPCServer.type])
  //Loading our configs
  val config: Config = ConfigFactory.load("application.conf").getConfig("gRPCServer")
  val lambdaconfig: Config = ConfigFactory.load("application.conf").getConfig("Lambda")
  private val port = config.getInt("Port")
  logger.info(s"Loaded configurations")
  //main class for the Server
  def main(args: Array[String]): Unit = {
    val server = new gRPCServer(ExecutionContext.global)
    server.start()
    logger.info("Starting Server")
    server.blockUntilShutdown()
  }
}

class gRPCServer(executionContext: ExecutionContext) {
  self =>
  //bootstrapping our server
  private[this] val server: Server = ServerBuilder.forPort(gRPCServer.port).addService(logfindGrpc.bindService(new LogFinderImpl, executionContext)).build
  logger.info(s"Server bootstrapped")
  //The implementation is also bound in the above line
  //The below code starts the server
  private def start(): Unit = {
    server.start
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      logger.error(s"*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
      logger.error(s"*** server shut down")
    }
  }
  //This stops the server
  private def stop(): Unit = {
    if (server != null) {
      logger.info(s"Shutting down server")
      server.shutdown()
    }
  }

  //This keeps the server running until it receives an instruction to shutdown
  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }
  //This is our implementation of the service
  private class LogFinderImpl extends logfindGrpc.logfind {
    //Setting up Akka HTTP Actors
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    logger.debug(s"Akka Actors created")
    import system.dispatcher
    //The below code creates our request, sends it to the Lambda function and waits for the response.
    //Once the response is received, it send that to our gRPC Client
    override def logFinder(request: Request): Future[Reply] = {
      //getting the invokeURL of the API Gateway
      val lambda = lambdaconfig.getString("invokeURL")
      val getrequest = HttpRequest(
        method = HttpMethods.GET,
        uri = s"${lambda}?T=${request.t}&dT=${request.dT}"
      )
      logger.debug(s"Request for Lambda function created")
      logger.debug(s"**************Trying to contact Lambda**************")
      val responseFuture = Http().singleRequest(getrequest)
      val response = Await.result(responseFuture.flatMap(_.entity.toStrict(10 seconds)).map(_.data.utf8String), 11 seconds)
      Future.successful(Reply(response))
    }
  }
}