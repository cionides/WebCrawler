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
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.Restart
import akka.actor.SupervisorStrategy.Resume
import akka.actor.SupervisorStrategy.Stop


class Crawler extends Actor {

  case class LinksHolder(list:List[String])
  
  def receive = {
    case link: String => 
      val links: LinksHolder = getLinks(link)   
      links.list foreach println
      sender ! links
    
    case _ => throw new Exception("bad url")
  }

  def getLinks(url: String): LinksHolder = {
    var links = new ListBuffer[String]
    val cleaner = new HtmlCleaner
    val rootNode = cleaner.clean(new URL(url))
    val elements = rootNode.getElementsByName("a", true)
    
    for (elem <- elements) {
      var hrefContent = elem.getAttributeByName("href")
      
      if(hrefContent.contains("//")){
        hrefContent = hrefContent.replace("//", "")
        hrefContent = "http://" + hrefContent
      }
      links += hrefContent
    }
    LinksHolder(links.toList)   
  }
}

class Supervisor extends Actor {
 
  case class LinksHolder(list:List[String])
  var visited = HashSet[String]()
  val system = ActorSystem("WebCrawler")
  var instances = Runtime.getRuntime().availableProcessors()
  val crawler = context.actorOf(Props[Crawler].withRouter(RoundRobinRouter(instances)))
 
  def receive = {
    
  case firstLink: String => 
      crawler ! firstLink
  
  case listOfLinks: LinksHolder =>
      for(link <- listOfLinks.list){
        if(!visited.contains(link)){
          visited.add(link)        
          crawler ! link
        }         
      }
  }
    
}

//Entry point to the application
object WebCrawler extends App {
  val system = ActorSystem("WebCrawler")
  val supervisor = system.actorOf(Props[Supervisor])
  
  supervisor ! "http://www.wikipedia.com"
  Thread.sleep(100)
  system.shutdown

}
