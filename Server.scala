package httpserver

import scala.actors.Actor
import scala.collection.parallel.mutable.ParHashMap

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.DataOutputStream
import java.util.NoSuchElementException

class ResponseHandler(uri: String) extends Actor {

  private val header200:String = "HTTP/1.1 200 Ok\r\n"
  private var response:String = ""
  private var HttpResponse:String = "Server:Simple HTTP Server\r\nContent-Type:text/html\r\n"

  def act() {
    while(true) {
      receive {
        case (method:String, uri:String, resp:DataOutputStream) => {
          response += "Hello from scala server!<br>Given method: " + method + "<br>Given uri: " + uri + "<br> \n"
          HttpResponse += "Content-Length: " + response.length() + "\r\nConnection: close \r\n\r\n" + response

          resp.writeBytes(header200 + HttpResponse)
          resp.close()
        }
        case _ => Unit
      }
    }
  }
}


class RequestHandler extends Actor {

  private var respHandlers:ParHashMap[String, ResponseHandler] = new ParHashMap[String, ResponseHandler]
  private val response404:String = "HTTP/1.1 404 Not Found\r\nServer:Simple HTTP Server\r\nContent-Type:text/html\r\nContent-Length: 0\r\nConnection: close \r\n\r\n"

  def addRespHandler(uri:String, handler:ResponseHandler) {
    respHandlers += uri -> handler
    handler.start()
  }

  def act() {
    println("Started Server...")

    while(true) {
      receive {
        case client:Socket => {
          try {
            println("Received message from " + client.getInetAddress() + ":" + client.getPort())
            //Считываем заголовки запроса
            var is:InputStream = client.getInputStream()

            var out:DataOutputStream = new DataOutputStream(client.getOutputStream())

            var buffer:BufferedReader = new BufferedReader(new InputStreamReader(is))

            var header:Array[String] = buffer.readLine().split(" ")

            println("Method: " + header(0))
            println("Uri: " + header(1))

            try {
              var response:ResponseHandler = respHandlers(header(1))
              response ! (header(0), header(1), out)
            } catch {
              case e: NoSuchElementException => {
                out.writeBytes(response404)
                out.close()
              }
            }

          } catch {
            case e: Exception => e.printStackTrace()
          }
        }
        case _ => println("Request error, discarded!")
      }
    }
  }
}

class HttpServer(port: Int, address: InetAddress) extends Actor {
  private var server:ServerSocket = new ServerSocket(port, 10, address)

  private var handler:RequestHandler = new RequestHandler

  handler.start()

  def addRespHandler(uri:String, respHandler:ResponseHandler) {
    handler.addRespHandler(uri, respHandler)
  }

  def act() {
    while(true) {
      try {
        var socket:Socket = server.accept()
        handler ! socket
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }
}


object Server extends App {
  var address:InetAddress = InetAddress.getByName("127.0.0.1")

  var server:HttpServer = new HttpServer(1234, address)

  server.addRespHandler("/", new ResponseHandler("/"))
  server.addRespHandler("/hello", new ResponseHandler("/hello"))

  server.start()
}