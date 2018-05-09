package com.cabolabs.ehrserver

import groovyx.net.http.*
import org.apache.log4j.Logger
import java.security.KeyStore
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.ContentType.JSON

/**
 * TODO: show status code on exceptions: e.response.status.toString() + e.message << status=400 message=Bad Request
 * on case of 4xx or 5xx errors, the body of the response with the error message is on e.response.data
 */


class EhrServerAsyncClient {

   // TODO: operation here and job to free the cache
   def cache = [:]

   def server

   private Logger log = Logger.getLogger(EhrServerAsyncClient.class)

   def config = [
      server: [
         protocol: 'http://',
         ip: '',
         port: '',
         path: ''
      ],
      token: '' // set from login
   ]

   def EhrServerAsyncClient(String protocol, String ip, int port, String path, int poolSize = 10)
   {
      config.server.protocol = protocol
      config.server.ip = ip
      config.server.port = port

      if (!path.endsWith('/')) path += '/'
      config.server.path = path


      server = new AsyncHTTPBuilder(
             poolSize : poolSize,
             uri : config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)

      server.encoderRegistry = new EncoderRegistry( charset: 'utf-8' ) // avoids issues sending spanish accentuated words

      server.handler.failure = { response, data ->

         response.setData(data)
         //String headers = ""
         //response.headers.each {
         //   headers = headers+"${it.name} : ${it.value}\n"
         //}
         //throw new HttpResponseException(response.getStatus(),"HTTP call failed. Status code: ${response.getStatus()}\n${headers}\n"+
         //                                "Response: "+(response as HttpResponseDecorator).getData())
         throw new HttpResponseException(response as HttpResponseDecorator)
         return response
      }

      if (protocol.toLowerCase().startsWith('https'))
      {
         println """Extra config needs to be done for HTTPS connections, see https://github.com/jgritman/httpbuilder/wiki/SSL
Use the cabolabs2.crt certificate and the store.jks keystore from this project to run the tests
cabolabs-ehrserver-groovy>keytool -importcert -alias "cabo2-ca" -file cabolabs2.crt -keystore store.jks -storepass test1234"""

         /*
          * change these lines to use your keystore.
          */
         def keyStore = KeyStore.getInstance( KeyStore.defaultType )
         //println getClass().getResource( "/" ) // /bin
         getClass().getResource( "/store.jks" ).withInputStream {
            keyStore.load( it, "test1234".toCharArray() )
         }

         server.client.connectionManager.schemeRegistry.register( new Scheme("https", 443, new SSLSocketFactory(keyStore)) )
      }

      println 'Base URL: '+ config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path
   }

   /**
    * Set token directly from organization API key, no need to call login on this case.
    */
   def setAPIKey(String token)
   {
      config.token = token
      return token
   }

   def handleExceptions(Exception e, String action, Exception owner = null)
   {
      switch (e)
      {
         case java.util.concurrent.ExecutionException: // owner of the real cause

            return handleExceptions(e.getCause(), action, e)

         break
         case java.net.UnknownHostException: // server down

            return [
               status: 0,
               message: 'noconnection'
            ]

         break
         case org.apache.http.conn.HttpHostConnectException: // nwtwork problem, e.g. timeout?

            return [
               status: -1,
               message: e.message
            ]

         break
         case groovyx.net.http.HttpResponseException: // 40X, e.g. 401 Unauthorized

            // This is a bug, data is null even the service returns a JSON body, get data is empty,
            // but when data is set from the custom error handler failure{} we get the response body!
            //println e.response.getData() // null
            //println e.response.getContentType() // application/json
            //println e.response.getClass() // groovyx.net.http.HttpResponseDecorator
            //println e.response.status // int HTTP Response Status

            //if (owner) println owner.response.getData() // groovy.lang.MissingPropertyException: No such property: response for class: java.util.concurrent.ExecutionException

            //println e.response.data.result.message // path for login errors
            def message
            if (action == 'login') message = e.response.data.result.message
            else message =  e.response.data.message

            return [
               status: e.response.status,
               message: message ?: e.message
            ]

         break
         default:
            println 'client '+ action +' '+ e.getClass()
            e.printStackTrace()
            return [
               status: -2,
               message: e.message
            ]
      }

      /*
      return [
         status: ((e.response) ? e.response.status : 0),
         message: e.message
      ]

      catch (java.lang.IllegalStateException e)
      {
         println e
         // When there is no connection to the server, therr is no response in the exception
         return [
            status: -1,
            message: e.message
         ]
      }
      */
   }

   /**
    * Get token from login endpoint.
    */
   def login(String username, String password, String orgnumber)
   {
      def res, resp
      try
      {
         // Sin URLENC da error null pointer exception sin mas datos... no se porque es. PREGUNTAR!
         res = server.post(
            path:'api/v1/login',
            requestContentType: ContentType.TEXT, //ContentType.URLENC,
            //query: [format:'json'],
            //body: [username: username, password: password, organization: orgnumber]
            query: [format:'json', username: username, password: password, organization: orgnumber],
         ) { response, json ->

            [
               status: response.status,
               token: json.token,
               message: 'ehrserver.login.success'
            ]
         }

         resp = res.get()
         //println resp
         config.token = resp.token

         return resp
      }
      catch (Exception e)
      {
         return handleExceptions(e, 'login')
      }
   }

   def Object getProfile(String username)
   {
      def res
      def profile

      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         res = server.get( path: 'api/v1/users/'+ username,
                          query: [format:'json'],
                          headers: ['Authorization': 'Bearer '+ config.token] )

         profile = res.data
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         log.error( e.message )
         return
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
      /*
      Unauthorized
      401
      [result:Unauthorized to access user info]
      Unauthorized to access user info
      println e.message
      println e.response.status
      println e.response.data
      println e.response.data.result
      */
         log.error( e.response.data ) //.message.text() )
         return
      }

      return profile

   } // getProfile

   def Object getEhr(String uid)
   {
      def res
      def ehr

      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         res = server.get( path: 'api/v1/ehrs/ehrUid/'+ uid,
                        query: [format:'json'],
                        headers: ['Authorization': 'Bearer '+ config.token] )

         ehr = res.data
      }
      catch (Exception e) // no hay conectividad
      {
         return handleExceptions(e, 'getEhr')
      }

      /*
      catch (groovyx.net.http.HttpResponseException e)
      {
         // puedo acceder al response usando la excepci?n!
         // 500 class groovyx.net.http.HttpResponseDecorator
         println e.response.status.toString() +" "+ e.response.class.toString()

         // errorEHR no encontrado para el paciente $subjectId, se debe crear un EHR para el paciente
         println e.response.data

         // WARNING: es el XML parseado, no el texto en bruto!
         // class groovy.util.slurpersupport.NodeChild
         println e.response.data.getClass()

         // Procesando el XML
         println e.response.data.code.text() // error
         println e.response.data.message.text() // el texto

         // text/xml
         println e.response.contentType

         // TODO: log a disco
         // no debe serguir si falla el lookup
         //render "Ocurrio un error al obtener el ehr del paciente "+ e.message
         log.error( e.response.data.message.text() )
         return
      }
      */

      return ehr

   } // getEhr


   def Object getEhrIdByPatientId(String patientUid)
   {
      def res
      def ehr

      try
      {
         res = server.get( path: 'api/v1/ehrs/subjectUid/'+patientUid,
                        query: [format:'json'],
                        headers: ['Authorization': 'Bearer '+ config.token] )

         ehr = res.data
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         log.error( e.message )
         return
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         // puedo acceder al response usando la excepci?n!
         // 500 class groovyx.net.http.HttpResponseDecorator
         println e.response.status.toString() +" "+ e.response.class.toString()

         // errorEHR no encontrado para el paciente $subjectId, se debe crear un EHR para el paciente
         println e.response.data

         // WARNING: es el XML parseado, no el texto en bruto!
         // class groovy.util.slurpersupport.NodeChild
         println e.response.data.getClass()

         // Procesando el XML
         println e.response.data.code.text() // error
         println e.response.data.message.text() // el texto

         // text/xml
         println e.response.contentType

         log.error( e.response.data.message.text() )
         return
      }

      return ehr

   } // getEhrIdByPatientId


   def getEhrs(int max = 10, int offset = 0)
   {
      def res, resp
      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         res = server.get( path: 'api/v1/ehrs',
                     query: [format:'json', max: max, offset: offset],
                     headers: ['Authorization': 'Bearer '+ config.token] )
         { response, json ->

            [
               status: response.status,
               result: json,
               message: 'ehrserver.ehrs.success'
            ]
         }

         resp = res.get()
         return resp
      }
      catch (java.net.UnknownHostException e)
      {
         res = [
            status: 0,
            message: 'noconnection'
         ]
      }
      catch (org.apache.http.conn.HttpHostConnectException e)
      {
         // When there is no connection to the server, therr is no response in the exception
         return [
            status: -1,
            message: e.message
         ]
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         println "A2 "+ e.message +" "+ e.cause
         log.error( e.message )
         //log.error( e.response.data.message.text() )
         res = [
            status: e.response.status,
            message: e.response.data.message
         ]
      }

      return res

   } // getEhrs

   /*
   def createEhr(String subjectUid)
   {
      def ehr
      server.post( path: 'api/v1/ehrs/'+ehrUid+'/compositions',
                     requestContentType: URLENC,
                     query: [
                        subjectUid: subjectUid,
                        format: 'json'
                     ],
                     body: [],
                     headers: ['Authorization': 'Bearer '+ config.token] )
      { resp, data ->

         //println data

         if (resp.status in 200..299)
         {
            println "Status OK: "+ resp.statusLine.statusCode +' '+ resp.statusLine.reasonPhrase
            ehr = data
         }
         else // on this case an exception is thrown
         {
            println "Status ERROR: "+ resp.statusLine.statusCode +' '+ resp.statusLine.reasonPhrase
         }
      }

      return ehr
   }
   */

   def createEhr(String subjectUid)
   {
      def res, ehrUid, resp
      try
      {
         // Sin URLENC da error null pointer exception sin mas datos... no se porque es. PREGUNTAR!
         res = server.post(
            path:'api/v1/ehrs',
            requestContentType: ContentType.URLENC,
            query: [format:'json'],
            body: [subjectUid: subjectUid],
            headers: ['Authorization': 'Bearer '+ config.token])
         { response, json ->

            [
               status: response.status,
               ehrUid: json.uid,
               organizationUid: json.organizationUid,
               message: 'ehrserver.createEHR.success'
            ]
         }

         resp = res.get() // [status: , ehrUid: , organizationUid: , message: ]

         return resp
      }
      catch (Exception e)
      {
         return handleExceptions(e, 'createEhr')
      }

      /*
      catch (Exception e)
      {
         return [
            status: ((e.hasProperty('response')) ? e.response.status : 0),
            message: e.message,
            description: e.response.data.result.message
         ]
      }
      */
   }


   def commit(String ehrUid, String compo, String committer, String systemId)
   {
      def res, resp

      try
      {
         res = server.post( path: 'api/v1/ehrs/'+ehrUid+'/compositions',
                     requestContentType: XML,
                     query: [
                        auditCommitter: committer,
                        auditSystemId: systemId,
                        format: 'xml'
                     ],
                     body: compo,
                     headers: ['Authorization': 'Bearer '+ config.token] )
         { response, xml ->

            //println "commit data result "+ xml

            if (response.status in 200..299)
            {
               println "Status OK: "+ response.statusLine.statusCode +' '+ response.statusLine.reasonPhrase
            }
            else // on this case an exception is thrown
            {
               println "Status ERROR: "+ response.statusLine.statusCode +' '+ response.statusLine.reasonPhrase
            }

            return [
               status: response.status,
               message: xml.message
            ]
         }

         resp = res.get()

         //println resp // groovyx.net.http.HttpResponseDecorator@1ac3d0c
         //println res.data // null
         //println res.data.type // null
         //println 'cccc> '+ res.code +' '+res.message // code is null for commit success

         return resp
      }
      catch (Exception e) // no hay conectividad
      {
         return handleExceptions(e, 'commit')
      }
   }

   def getContributions(String ehrUid, int max = 20, int offset = 0)
   {
      def res

      // Pide datos al EHR Server
      //def server = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)

      // Lookup de ehrId por subjectId
      // FIXME: esto se puede evitar si viene el dato con el paciente
      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         server.get( path: 'api/v1/ehrs/'+ehrUid+'/contributions',
                        query: [format:'json', max: max, offset: offset],
                        headers: ['Authorization': 'Bearer '+ config.token] )
         { resp, json ->

            //println resp // groovyx.net.http.HttpResponseDecorator@1ac3d0c
            res = json
            //println json
            //println json.contributions.uid
         }
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         log.error( e.message )
         return
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         log.error( e.response.data )
         println "ERROR: "+ e.response.data +" "+ e. message
         return
      }

      return res

   } // getContributions

   /**
    * Retrieves clinical document indexes from an EHR, to get the content, use a composition uid on getComposition()
    */
   def getCompositions(String ehrUid, int max = 20, int offset = 0, String archetypeId)
   {
      def res

      // Lookup de ehrId por subjectId
      // FIXME: esto se puede evitar si viene el dato con el paciente
      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         res = server.get( path: 'api/v1/compositions',
                        query: [ehrUid: ehrUid, format:'json', max: max, offset: offset, archetypeId: archetypeId],
                        headers: ['Authorization': 'Bearer '+ config.token] )
         { response, json ->

            //println resp // groovyx.net.http.HttpResponseDecorator@1ac3d0c

            //res = json
            //println "JSON "+ ehrs +" "+ ehrs.getClass()

            [
               status: response.status,
               result: json,
               message: 'ehrserver.compositions.success'
            ]
         }

         return res.get()
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         return [
            status: -1,
            message: e.message
         ]
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         log.error( e.response.data )
         println "ERROR: "+ e.response.data +" "+ e. message
         return [
            status: e.response.status,
            message: e.response.data.message
         ]
      }

   } // getConmpositions


   def getQueries(String format = 'json', int max = 20, int offset = 0)
   {
      def res
      //def server = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)
      try
      {
         server.get( path: 'api/v1/queries',
                        query: [format: format, max: max, offset: offset],
                        headers: ['Authorization': 'Bearer '+ config.token] )
         { resp, data ->

            res = data // json (class groovy.json.internal.LazyMap) or xml
         }
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         log.error( e.message )
         return
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         log.error( e.response.data )
         println "ERROR: "+ e.response.data +" "+ e. message
         return
      }

      return res
   }

   /**
    * Runs an existing query and retrieves the result sets.
    */
   def query(String queryUid, String ehrUid, String fromDate, String format = 'json')
   {
      def res
      try
      {
         server.get( path: 'api/v1/queries/'+ queryUid +'/execute',
                        query: [ehrUid: ehrUid, format: format, fromDate: fromDate],
                        headers: ['Authorization': 'Bearer '+ config.token] )
         { resp, data ->

            //println resp // groovyx.net.http.HttpResponseDecorator@1ac3d0c
            //println resp.data // nyll

            res = data // json (class groovy.json.internal.LazyMap) or xml
            //println "JSON "+ ehrs +" "+ ehrs.getClass()
         }
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         log.error( e.message )
         return
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         log.error( e.response.data )
         println "ERROR: "+ e.response.data +" "+ e. message
         return
      }

      return res

   } // query

   def executeGivenQuery(String query, String ehrUid)
   {
      def res, resp

      try
      {
         res = server.post( path: 'api/v1/query/composition/execute',
                     requestContentType: JSON,
                     query: [
                        format: 'json'
                     ],
                     body: query,
                     headers: ['Authorization': 'Bearer '+ config.token] )
         { response, json ->

            [
               status: response.status,
               data: json
            ]
         }

         resp = res.get()

         return resp
      }
      catch (Exception e)
      {
         println e.message
      }
   }


   def getTemplates()
   {
      def res, resp
      try
      {
         res = server.get( path: 'api/v1/templates',
                     query: [format:'json'],
                     headers: ['Authorization': 'Bearer '+ config.token] )
         { response, json ->

            [
               status: response.status,
               result: json,
               message: 'ehrserver.templates.success'
            ]
         }

         resp = res.get()
         //println resp

         return resp
      }
      catch (java.net.UnknownHostException e)
      {
         res = [
            status: 0,
            message: 'noconnection'
         ]
      }
      catch (org.apache.http.conn.HttpHostConnectException e)
      {
         // When there is no connection to the server, therr is no response in the exception
         return [
            status: -1,
            message: e.message
         ]
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         println "A2 "+ e.message +" "+ e.cause
         log.error( e.message )
         //log.error( e.response.data.message.text() )
         res = [
            status: e.response.status,
            message: e.response.data.message
         ]
      }

      return res

   }
}
