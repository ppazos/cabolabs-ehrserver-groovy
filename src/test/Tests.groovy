package test

import groovy.util.GroovyTestCase

import com.cabolabs.ehrserver.*

class Tests extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   static EhrServerClient client

   protected void setUp()
   {
      println "setUp"
      
      // port 80 for http, port 443 for https
      //client = new EhrServerClient('https://', 'ehrserver-cabolabs2.rhcloud.com', 443, '/')
      
      //local
      client = new EhrServerClient('http://', 'localhost', 8090, '/ehr')
   }
   protected void tearDown()
   {
      println "tearDown\n"
   }
   

   void test_login()
   {
      println "test_login"
      def token = client.login('orgman', 'orgman', '123456') // stores token internally
      assert token != null
      //println "login this token " + client.config.token
   }
   
/*
   void test_profile()
   {
      println "test_profile"
   
      def token = client.login('orgman', 'orgman', '123456')
      def profile = client.getProfile('orgman')
     
      assert profile != null
      assert profile.username == 'orgman'
   }

   void test_ehrs()
   {
      println "test_ehrs"
      
      client.login('orgman', 'orgman', '123456')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      res.ehrs.each {
         println it.uid
      }
   }
   

   void test_get_ehr()
   {
      println "test_get_ehr"
      
      client.login('orgman', 'orgman', '123456')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      String uid = res.ehrs[0].uid
      
      def ehr = client.getEhr(uid)
      
      assert ehr != null
      
      assert ehr.uid == uid
   }
   */
   
   void test_commit()
   {
      println "test_commit"
      
      client.login('orgman', 'orgman', '123456')
      
      println new File('.').canonicalPath
      
      def res = client.getEhrs()
      assert res.ehrs.size() > 0
      String uid = res.ehrs[0].uid
      
      
      def PS = File.separator
      def xml = new File('.'+ PS +'resources'+ PS +'2ebfee9d-4f1b-4529-8947-6a62bed502db.xml').text
      res = client.commit(uid, xml, 'orgman', 'TestEMR') // is xml for now
      /*
      <result>
        <type>AA</type>
        <message>Se han recibido correctamente todas las versiones para el EHR 11111111-1111-1111-1111-111111111111</message>
      </result>
      */
      
      
      println "res commit "+ res
      /*
      res commit ARVersions were committed successfully, but warnings are returned.EHR_SERVER::API::ERRORS::1324The OPT LabResults1 referenced by the c
omposition 06f971f4-39a4-471e-90e4-6b93318e2e90 is not loaded. Please load the OPT to allow data indexing.

*/

      def csresult = client.getContributions(uid)
      assert csresult.contributions.size() == 1
      
      def coresult = client.getCompositions(uid)
      assert coresult.result.size() == 1
   }
   
/*
   void test_queries()
   {
      client.login('orgman', 'orgman', '123456')
      def res = client.getQueries()
      
      assert res.queries.size() > 0
      
      res.queries.each {
         println it.uid +" "+ it.type
      }
   }
   

   void test_get_ehr_by_subject()
   {
      client.login('orgman', 'orgman', '123456')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      String subjectUid = res.ehrs[0].subjectUid
      
      def ehr = client.getEhrIdByPatientId(subjectUid)
      
      assert ehr != null
      
      assert ehr.subjectUid == subjectUid
   }
*/

   def api_key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InR4YWltbXphc2ZweXhudmR2bHV2ZGNqeGR4ZmZjcXFtZHlsa2lvaWZpaGR2dXlqa2t1IiwiZXh0cmFkYXRhIjp7Im9yZ2FuaXphdGlvbiI6IjEyMzQ1NiJ9LCJpc3N1ZWRfYXQiOiIyMDE3LTA0LTE5VDAzOjEwOjQxLjU5NS0wNDowMCJ9.HpEmGDoc9bwAY0CMU72cBuMnrmuP5Y9S7PbQO6opxkk="
   
   /**
    * Same tests using Organization API Key.
    * Get an API Key under your organization section from the EHRServer's Web Console.
    */
   
   // Profile endpoint can't be accessed when using an API Token, since it has the lowest permissions on the system.
   // The proile can be accessed by a user to it's own profile, or by an admin on any org, and org managers for
   // users on their organizations. 401 Unauthorized will be received otherwise.

/*
   void test_ehrs_api_key()
   {
      client.setAPIKey(api_key)
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      res.ehrs.each {
         println it.uid
      }
   }
   
   void test_get_ehr_api_key()
   {
      client.setAPIKey(api_key)
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      String uid = res.ehrs[0].uid
      
      def ehr = client.getEhr(uid)
      
      assert ehr != null
      
      assert ehr.uid == uid
   }
   
   void test_get_ehr_by_subject_api_key()
   {
      client.setAPIKey(api_key)
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      String subjectUid = res.ehrs[0].subjectUid
      
      def ehr = client.getEhrIdByPatientId(subjectUid)
      
      assert ehr != null
      
      assert ehr.subjectUid == subjectUid
   }
   
   void test_query_api_key()
   {
      client.setAPIKey(api_key)
      
      // existing queryUid, ehrUid
      def res = client.query('7d125791-9f16-4b9b-a121-8b1238b59cb5',
                             'c35dfe02-4ece-465e-9433-0540c6d95f3f',
                             (new Date() - 365).format('yyyyMMdd'),
                             'json')
      
      println res.results.size()
      
      //[pagination:[max:20, nextOffset:20, offset:0, prevOffset:0], results:[[archetypeId:openEHR-EHR-COMPOSITION.encounter.v1, category:event,
      //ehrUid:c35dfe02-4ece-465e-9433-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a754-b98f62cdc4a4,
      //parent:fc6da40a-3745-4a1f-9914-4fba03f746d9::EMR::1, startTime:2017-02-11 19:57:19, subjectId:11111111, templateId:Simple Encounter,
      //uid:93b93687-135d-4189-ad01-1b8e1ac13289]], timing:20 ms]
      

      assert res.results.size() > 0
      assert res.results[0].uid == '93b93687-135d-4189-ad01-1b8e1ac13289' // composition uid
      assert res.results[0].ehrUid == 'c35dfe02-4ece-465e-9433-0540c6d95f3f' // ehrUid, used as a query parameter
   }
*/
   
   
   /*
   void test_ehruid_for_patient()
   {
      client.login('orgman', 'orgman', '123456')
      def patients = client.getPatients()
      def ehr_uid = client.getEhrIdByPatientId( patients[0].uid )
      println "EHR UID: "+ ehr_uid
   }
   
   void test_ehr_contributions()
   {
      client.login('orgman', 'orgman', '123456')
      def patients = client.getPatients()
      def ehr_uid = client.getEhrIdByPatientId( patients[0].uid )
      def res = client.getContributions( ehr_uid, 10 )
      
      assert res.contributions.size() == 10
      
      //println "contributions: "+ res.contributions
   }
   
   void test_ehr_compositions()
   {
      client.login('orgman', 'orgman', '123456')
      def patients = client.getPatients()
      def ehr_uid = client.getEhrIdByPatientId( patients[0].uid )
      def res = client.getCompositions( ehr_uid, 10 )
      
      assert res.result.size() == 10
      
      //println "compositions: "+ res.result
      
      res.result.each {
         println it.archetypeId
      }
   }
   */
}
