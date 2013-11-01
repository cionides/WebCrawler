import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.RoundRobinRouter
import scala.io._
import scala.collection.mutable.ListBuffer
import org.htmlcleaner.HtmlCleaner
import org.apache.commons.lang3.StringEscapeUtils
import java.net._
import scala.collection.mutable.HashSet
import scala.util.matching.Regex

import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.Restart
import akka.actor.SupervisorStrategy.Resume
import akka.actor.SupervisorStrategy.Stop


class Crawler extends Actor {

  case class StringListHolder(list:List[String])
  
  var URLMatcher = new Regex("href=\"(http://[^\"]+)\"", "url")

  def findURLs(url : String, matcher : Regex) : List[String] = {
    (matcher findAllIn Source.fromURL(url).mkString matchData).
    map(_.group("url")).
    toList.removeDuplicates
    }
    
  def receive = {
  	case url: String => {
      System.out.println(url)
      val URLs = findURLs(url, URLMatcher)
      val slh = StringListHolder(URLs)
      slh.list foreach println
      for(s <- slh.list){
      	sender ! s     	
      }
    }
    
    case _            => throw new IllegalArgumentException("Expect only string URLs")
  }
  
  
}

class Supervisor(host: String) extends Actor {
 
  case class StringListHolder(list:List[String])

  val strategy = OneForOneStrategy() {
    case _: Exception                => Resume
  }
  val crawlers = context.actorOf(Props[Crawler].withRouter(
    RoundRobinRouter(5, supervisorStrategy = strategy)))

  var visited = HashSet[String]()
  
  def receive = {
    case url: String => 
     if (!visited.contains(url)) {
       visited += url
       val urlHost = (new java.net.URL(url)).getHost()
       if (urlHost == host){
          crawlers ! url
      	}
      }
    case listOfLinks: StringListHolder =>
      for(link <- listOfLinks.list){
        crawlers ! link
      }
  }
    
}

object WebCrawler extends App {
  val system = ActorSystem("WebCrawler")
  val url = "http://www.wikipedia.org/"
  val host = (new java.net.URL(url)).getHost()
  val supervisor = system.actorOf(Props(new Supervisor(host)))
  
  
  //first link is sent
  supervisor ! url
  
   Thread.sleep(10000)
   system.shutdown

}
