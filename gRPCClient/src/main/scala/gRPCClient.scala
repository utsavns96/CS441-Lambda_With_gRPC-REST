import HelperUtils.CreateLogger

import java.util.concurrent.TimeUnit
import com.typesafe.config.{Config, ConfigFactory}
import com.example.protos.hw2grpc.{Reply, Request, logfindGrpc}
import com.example.protos.hw2grpc.logfindGrpc.logfindBlockingStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}

//This program creates our gRPC Client and sends requests to our gRPC Server


object gRPCClient {

  val logger = CreateLogger(classOf[gRPCClient.type])
  //loading our configs
  val config: Config = ConfigFactory.load("application.conf").getConfig("gRPCClient")
  val requestconfig: Config = ConfigFactory.load("application.conf").getConfig("request")
  logger.info(s"Configs loaded")
  def apply(host: String, port: Int): gRPCClient = {
    //setting up our channel
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    //blocking call to server
    new gRPCClient(channel, logfindGrpc.blockingStub(channel))
  }
  def main(args: Array[String]): Unit = {
    //getting the URL and port that the server is configured on
    var URL: String = ""
    if(config.getInt("RemoteServer")==1){
     URL = config.getString("URLRemote")
      logger.debug(s"Connecting to gRPCServer on remote EC2 instance")
    }
    else
      {
        URL = config.getString("URLLocal")
        logger.debug(s"Connecting to gRPCServer on localhost")
      }
    val client = gRPCClient(URL, config.getInt("Port"))
    //sending our request
    try {
      val T = requestconfig.getString("T")
      val dT = requestconfig.getString("dT")
      logger.debug(s"Sending request")
      client.findlogmessages(T, dT)
    } finally {
      client.shutdown()
    }
  }
}

class gRPCClient private(private val channel: ManagedChannel, private val blockingStub: logfindBlockingStub) {

  val logger = CreateLogger(classOf[gRPCClient.type])
  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(10, TimeUnit.SECONDS)
  }
  //This send the request to the server and prints the response that we get from the Lambda function through the server
  def findlogmessages(T: String, dT: String): Unit = {
    logger.debug(s"Sending request")
    val request = Request(T, dT)
    try {
      val response = blockingStub.logFinder(request)
      logger.info(s"Response: " + response.message)
      println("***********\n\nResponse:\n"+response.message + "\n\n***********")
    }
    catch {
      case e: StatusRuntimeException =>
        logger.error("RPC failed: {0}" + e.getStatus)
    }
  }
}
