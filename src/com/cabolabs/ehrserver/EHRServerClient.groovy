package com.cabolabs.ehrserver

import groovyx.net.http.*
import org.apache.log4j.Logger
import java.security.KeyStore
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory

class EhrServerClient {

   // TODO: operation here and job to free the cache
   def cache = [:]
   
   def server
   
   private Logger log = Logger.getLogger(getClass()) 

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
                    Use the cabolabs2.crt certificate and the store.jks keystore from this project to run the tests"""
                    
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
    * TODO: HTTPS requires extra config
    * https://github.com/jgritman/httpbuilder/wiki/SSL
    * .\cabolabs-ehrserver-groovy>keytool -importcert -alias "cabo2-ca" -file cabolabs2.crt -keystore store.jks -storepass test1234
    */
   def login(String username, String password, String orgnumber)
   {
      // service login
      // set token on session
      //def ehr = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)
      try
      {
         // Sin URLENC da error null pointer exception sin mas datos... no se porque es. PREGUNTAR!
         def res = server.post(
            path:'rest/login',
            requestContentType: ContentType.URLENC,
            body: [username: username, password: password, organization: orgnumber]
         )
         
         config.token = res.responseData.token
         
         //println "token: "+ config.token
         
         // token
         return res.responseData.token
      }
      catch (Exception e)
      {
         e.printStackTrace(System.out)
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
         res = server.get( path: 'rest/profile/'+ username,
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
         log.error( e.response.data ) //.message.text() )
         return
      }
      
      return profile
      
   } // getProfile
   
   def String getEhrIdByPatientId(String patientUid)
   {
      def res
      def ehrUid
      
      // Pide datos al EHR Server
      def ehr = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)
      
      
      // Lookup de ehrId por subjectId
      // FIXME: esto se puede evitar si viene el dato con el paciente
      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         res = ehr.get( path: 'rest/ehrForSubject',
                        query: [subjectUid:patientUid, format:'json'],
                        headers: ['Authorization': 'Bearer '+ config.token] )
         
         // FIXME: el paciente puede existir y no tener EHR, verificar si devuelve el EHR u otro error, ej. paciente no existe...
         // WONTFIX: siempre tira una excepcion en cada caso de error porque el servidor tira error 500 not found en esos casos.
         ehrUid = res.data.uid
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
      
      return ehrUid
      
   } // getEhrIdByPatientId
   
   
   def getEhrs()
   {
      def res
      def ehrs
      
      // Pide datos al EHR Server
      def ehr = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)
      
      
      // Lookup de ehrId por subjectId
      // FIXME: esto se puede evitar si viene el dato con el paciente
      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         ehr.get( path: 'rest/ehrs',
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
      
      return ehrs
      
   } // getEhrs
   
   def getContributions(String ehrUid, int max = 20)
   {
      def res
      
      // Pide datos al EHR Server
      def ehr = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)

      // Lookup de ehrId por subjectId
      // FIXME: esto se puede evitar si viene el dato con el paciente
      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         ehr.get( path: 'rest/contributions',
                        query: [ehrUid: ehrUid, format:'json', max: max],
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
      
   } // getContributions
   
   def getCompositions(String ehrUid, int max = 20)
   {
      def res
      
      // Pide datos al EHR Server
      def ehr = new RESTClient(config.server.protocol + config.server.ip +':'+ config.server.port + config.server.path)

      // Lookup de ehrId por subjectId
      // FIXME: esto se puede evitar si viene el dato con el paciente
      try
      {
         // Si ocurre un error (status >399), tira una exception porque el defaultFailureHandler asi lo hace.
         // Para obtener la respuesta del XML que devuelve el servidor, se accede al campo "response" en la exception.
         ehr.get( path: 'rest/compositions',
                        query: [ehrUid: ehrUid, format:'json', max: max],
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
}
