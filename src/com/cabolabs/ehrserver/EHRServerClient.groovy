package com.cabolabs.ehrserver

import groovyx.net.http.*
import org.apache.log4j.Logger
import java.security.KeyStore
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import static groovyx.net.http.ContentType.XML

/**
 * TODO: show status code on exceptions: e.response.status.toString() + e.message << status=400 message=Bad Request
 * on case of 4xx or 5xx errors, the body of the response with the error message is on e.response.data
 */
 

class EhrServerClient {

   // TODO: operation here and job to free the cache
   def cache = [:]
   
   def server
   
   private Logger log = Logger.getLogger(EhrServerClient.class) 

   def config = [
      server: [
         protocol: 'http://',
         ip: '',
         port: '',
         path: ''
      ],
      token: '' // set from login
   ]
   
   def EhrServerClient(String protocol, String ip, int port, String path)
   {
      config.server.protocol = protocol
      config.server.ip = ip
      config.server.port = port

      if (!path.endsWith('/')) path += '/'
      config.server.path = path
      
      server = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)
      
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
   
   /**
    * Get token from login endpoint.
    */
   def login(String username, String password, String orgnumber)
   {
      // service login
      // set token on session
      //def ehr = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)
      def res
      try
      {
         // Sin URLENC da error null pointer exception sin mas datos... no se porque es. PREGUNTAR!
         res = server.post(
            path:'api/v1/login',
            requestContentType: ContentType.URLENC,
            body: [username: username, password: password, organization: orgnumber]
         )
         
         config.token = res.responseData.token
         
         println res.responseData
         //println "token: "+ config.token
         
         return [
            status: res.status,
            token: res.responseData.token,
            message: 'ehrserver.login.success'
         ] //res.responseData.token
      }
      catch (Exception e)
      {
         /*
         println e.response
         println e.response.status
         println e.response.responseData
         */
         //println e.getClass() // groovyx.net.http.HttpResponseException
         //println e.message
         //e.printStackTrace(System.out)
         if (e.message == "Unauthorized")
         {
            assert e.response.responseData.type == "AR"
            /*
            {
                "result": {
                    "type": "AR",
                    "message": "No matching account",
                    "code": "EHRSERVER::API::RESPONSE_CODES::e01.0001"
                }
            }
            */
            return [
               status: e.response.status,
               message: 'ehrserver.login.fail'
            ]
         }
         else
         {
            return [
               status: e.response.status,
               message: e.message
            ]
         }
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

         println res
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
         
         // TODO: log a disco
         // no debe serguir si falla el lookup
         //render "Ocurrio un error al obtener el ehr del paciente "+ e.message
         log.error( e.response.data.message.text() )
         return
      }
      
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
   
   
   def getEhrs()
   {
      def res
      def ehrs

      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         server.get( path: 'api/v1/ehrs',
                     query: [format:'json'],
                     headers: ['Authorization': 'Bearer '+ config.token] )
         { resp, json ->
         
            //println resp // groovyx.net.http.HttpResponseDecorator@1ac3d0c
            ehrs = json
            //println "JSON "+ ehrs +" "+ ehrs.getClass()
         }
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         println "A1"
         log.error( e.message )
         return
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         println "A2 "+ e.message +" "+ e.cause
         log.error( e.message )
         //log.error( e.response.data.message.text() )
         return
      }
      
      return ehrs
      
   } // getEhrs
   
   
   // TODO: add commit and test
   def commit(String ehrUid, String xml, String committer, String systemId)
   {
      def res
      
      try
      {
         server.post( path: 'api/v1/ehrs/'+ehrUid+'/compositions',
                     requestContentType: XML,
                     query: [
                        auditCommitter: committer,
                        auditSystemId: systemId,
                        format: 'xml'
                     ],
                     body: xml,
                     headers: ['Authorization': 'Bearer '+ config.token] )
         { resp, data ->
         
            //println resp // groovyx.net.http.HttpResponseDecorator@1ac3d0c
            res = data
            //println res.data // null
            //println res.data.type // null
            println res.code +' '+res.message
            
            if (resp.status in 200..299)
            {
               println "Status OK: "+ resp.statusLine.statusCode +' '+ resp.statusLine.reasonPhrase
            }
            else
            {
               println "Status ERROR: "+ resp.statusLine.statusCode +' '+ resp.statusLine.reasonPhrase
            }
         }
      }
      catch (org.apache.http.conn.HttpHostConnectException e) // no hay conectividad
      {
         println "XXX"
         log.error( e.message )
         return
      }
      catch (groovyx.net.http.HttpResponseException e)
      {
         println "YYY "+ e.response.status.toString() +' "'+ e.message +'" '+ e.response.data.toString() +' '+ e.response.data.getClass() // XML nodechild
         log.error( e.message )
         //log.error( e.response.data.message.text() )
         return
      }
      
      return res
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
   def getCompositions(String ehrUid, int max = 20, int offset = 0)
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
         server.get( path: 'api/v1/compositions',
                        query: [ehrUid: ehrUid, format:'json', max: max, offset: offset],
                        headers: ['Authorization': 'Bearer '+ config.token] )
         { resp, json ->
         
            //println resp // groovyx.net.http.HttpResponseDecorator@1ac3d0c
            res = json
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
      //def server = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)
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
   
}
