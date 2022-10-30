import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

import java.util.regex.Pattern
import scala.reflect.io.File

object TestCases

  class TestCases extends AnyFunSuite{
    test("Check if application.conf file is present") {
      //Check if the application.conf file is present or not.
      val file = File("src/main/resources/application.conf")
      assert(file.exists)
    }
    test("Unit test for request configuration existing") {
      //test if the config file has the request configuration
      val requestconfig: Config = ConfigFactory.load("application.conf").getConfig("request")
      assert((!requestconfig.isEmpty))
    }
    test("Unit test for Lambda configuration existing") {
      //test if the config file has the lambda configuration
      val requestconfig: Config = ConfigFactory.load("application.conf").getConfig("Lambda")
      assert((!requestconfig.isEmpty))
    }
    test("Unit test for request type existing and being of either GET or POST type") {
      //test for request type existing and being of either GET or POST type
      val requestconfig: String = ConfigFactory.load("application.conf").getString("request.Type")
      assert((!requestconfig.isEmpty) && (requestconfig.equals("GET") || requestconfig.equals("POST")))
    }
    test("Unit test for the time input being set") {
      //test if the time input is set
      val requestconfig: String = ConfigFactory.load("application.conf").getString("request.T")
      assert(!requestconfig.isEmpty)
    }
    test("Unit test for the time differential being set") {
      //test if the time differential is set
      val requestconfig: String = ConfigFactory.load("application.conf").getString("request.dT")
      assert(!requestconfig.isEmpty)
    }
    test("Unit test for the API Gateway URL being set") {
      //test if the invokeURL is configured
      val requestconfig: String = ConfigFactory.load("application.conf").getString("Lambda.invokeURL")
      assert(!requestconfig.isEmpty)
    }
    test("Unit test for injected regex negative") {
      //check if we are matching our injected pattern correctly. It should not match in this case.
      val injectedstring = "([a-c][e-g][0-3]|[A-Z][5-9][f-w]){5,15}"
      val value = "14:35:50.652 [scala-execution-context-global-21] ERROR HelperUtils.Parameters$ - P#~\"PoX@Oc+f!&Q4h3TM:ioE(+B(\"\"`*3U2y;2~[hQL1Js{Iez<(A&CP"
      val injectedpattern = Pattern.compile(injectedstring).matcher(value)
      assert(!injectedpattern.find())
    }
    test("Unit test for injected regex positive") {
      //check if we are matching our injected pattern correctly. It should match in this case.
      val injectedstring = "([a-c][e-g][0-3]|[A-Z][5-9][f-w]){5,15}"
      val value = "14:35:49.958 [scala-execution-context-global-21] INFO  HelperUtils.Parameters$ - hxgQ_i:JDGT7hN7wbg3ae0cg0ag2NG-xk\\Bcb."
      val injectedpattern = Pattern.compile(injectedstring).matcher(value)
      assert(injectedpattern.find())
    }
  }