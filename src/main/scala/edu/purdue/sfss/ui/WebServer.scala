/*
   Copyright 2014 - Thamir Qadah

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package edu.purdue.sfss.ui

import java.io.File
import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import spray.http.ContentType.apply
import spray.http.HttpEntity
import spray.http.HttpResponse
import spray.http.MediaTypes
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.json.DefaultJsonProtocol
import spray.json.JsArray
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsString
import spray.routing.Directive.pimpApply
import spray.routing.SimpleRoutingApp
import spray.routing.directives.FieldDefMagnet.apply
import spray.routing.directives.ParamDefMagnet.apply
import edu.purdue.sfss.utils.Helper
import edu.purdue.sfss.NetworkSocketControllerServer
import edu.purdue.sfss.ChangeCount

object Webserver extends App with SimpleRoutingApp {

  // Loading configuration
  val conf = Helper.getConfig()
  implicit val system = ActorSystem("webserver")
  implicit val ec = system.dispatcher
  implicit val log = system.log

  val host = conf.getString("webserver.hostname")
  val port = conf.getInt("webserver.port")
  
  // case class for holding infomation about streams
  case class StreamSourceMetaData(name: String, filepath: String, port: Int, var count: Int, aref: ActorRef) {    
    def rate = count 
    def getActorRef = aref
    def getJsonMap = Map("name" -> JsString(name),
      "file" -> JsString(filepath),
      "host" -> JsString(host),
      "port" -> JsNumber(port),
      "rate" -> JsNumber(rate))
    override def toString = Array(name, filepath, port, count).mkString(",")
  }
  
  val activeStreams = scala.collection.mutable.Map[String, StreamSourceMetaData]()

  // Routes
  val getAvailableDataFiles = path("datafiles") {
    get {
      val datafiles = new File(conf.getString("webserver.data.dir"))
      complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
        JsArray(datafiles.listFiles().filter {
          f => !f.isDirectory && """.*\.csv""".r.findFirstIn(f.getName()).isDefined
        }.map(f => JsString(f.getName()))
          .toVector).toString)))
    }
  }

  val getAllStreamSourcesRoute = path("streamsources") {
    get {
      complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
        JsArray(activeStreams.values.map {
          s => JsObject(s.getJsonMap)
        }.toVector).toString)))
    }
  }
  
  val streamSourcesControlRoute =
    sealRoute {
      path("stream-source-control") {
        post {
            formFields('name, 'filename, 'sport.as[Int], 'scount.as[Int]) { (name, fname, port, count) =>
              {                
               // create source stream if not exist
                if (activeStreams.keys.toSet[String].contains(name)){
                  complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
				        JsObject("streamName"-> JsString(name),
				        		"status" -> JsString("already exists")).toString)))   	                
                }               
                else{
                  val portSet = activeStreams.values.flatMap(p => {
                    if (p.port == port) Some(port) else None
                  })
                  if (portSet.size > 0){
                    log.info("port already in use")
                	  complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
				        JsObject("streamName"-> JsString(name),
				        		"status" -> JsString("port already in use")).toString)))  
                  }
                  else{
                    val c = system.actorOf(Props(classOf[NetworkSocketControllerServer], conf.getString("webserver.data.dir")+"/"+fname, host, port, count, 1), name = name)                
	                val ss = StreamSourceMetaData(name, fname, port.toInt, count.toInt, c)
	                log.info("created a stream source with: "+ss.toString)
	                activeStreams += (name -> ss)
	                complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
				        JsObject("streamName"-> JsString(name),
				        		"status" -> JsString("created")).toString)))   
                  }                  
	            }                 
              }
            }~
            formFields('name, 'scount.as[Int]) { (name, count) =>{
              // update rate 
              if (activeStreams.keys.toSet[String].contains(name)){
                
            	  val s = activeStreams.get(name)
            	  
            	  s.foreach(ss => {
            	    ss.count = count
            	    ss.aref ! ChangeCount(count)
            	  })
                  complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
				        JsObject("streamName"-> JsString(name),
				        		"status" -> JsString("updated")).toString)))   	                
                }
                else{                  
	                complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
				        JsObject("streamName"-> JsString(name),
				        		"status" -> JsString("does not exist")).toString)))   
	            }
            }
            }
          }~
          delete{
            parameters('name){ 
              (name) => {
                activeStreams -= name
                complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,
			        JsObject("streamName"-> JsString(name),
			        		"status" -> JsString("deleted")).toString)))
              }
              
            }                
          }
      }
    }

  // starting http server 
  startServer(interface = host, port = port) {
    getFromDirectory("ui/public") ~
      getAvailableDataFiles ~
      getAllStreamSourcesRoute ~
      streamSourcesControlRoute      
  }
}