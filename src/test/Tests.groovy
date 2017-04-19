package test

import groovy.util.GroovyTestCase

import com.cabolabs.ehrserver.*

class Tests extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   static EhrServerClient client

   protected void setUp()
   {
      println ""
      // port 80 for http, port 443 for https
      client = new EhrServerClient('https://', 'ehrserver-cabolabs2.rhcloud.com', 443, '/')
   }
   protected void tearDown()
   {
   }
   
   void test_login()
   {
      def token = client.login('orgman', 'orgman', '123456') // stores token internally
      assert token != null
      //println "login this token " + client.config.token
   }
   
   void test_profile()
   {
     def token = client.login('orgman', 'orgman', '123456')
     def profile = client.getProfile('orgman')
     
     assert profile != null
     assert profile.username == 'orgman'
   }
   
   void test_ehrs()
   {
      client.login('orgman', 'orgman', '123456')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      res.ehrs.each {
         println it.uid
      }
   }
   
   void test_get_ehr()
   {
      client.login('orgman', 'orgman', '123456')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      String uid = res.ehrs[0].uid
      
      def ehr = client.getEhr(uid)
      
      assert ehr != null
      
      assert ehr.uid == uid
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
   
   def api_key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InR4YWltbXphc2ZweXhudmR2bHV2ZGNqeGR4ZmZjcXFtZHlsa2lvaWZpaGR2dXlqa2t1IiwiZXh0cmFkYXRhIjp7Im9yZ2FuaXphdGlvbiI6IjEyMzQ1NiJ9LCJpc3N1ZWRfYXQiOiIyMDE3LTA0LTE5VDAzOjEwOjQxLjU5NS0wNDowMCJ9.HpEmGDoc9bwAY0CMU72cBuMnrmuP5Y9S7PbQO6opxkk="
   
   /**
    * Same tests using Organization API Key.
    * Get an API Key under your organization section from the EHRServer's Web Console.
    */
   
   // Profile endpoint can't be accessed when using an API Token, since it has the lowest permissions on the system.
   // The proile can be accessed by a user to it's own profile, or by an admin on any org, and org managers for
   // users on their organizations. 401 Unauthorized will be received otherwise.
   
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
      
      /*
       * existing queryUid, ehrUid
       */
      def res = client.query('7d125791-9f16-4b9b-a121-8b1238b59cb5',
                             'c35dfe02-4ece-465e-9433-0540c6d95f3f',
                             (new Date() - 365).format('yyyyMMdd'),
                             'json')
      
      println res.results.size()
      /*
      [pagination:[max:20, nextOffset:20, offset:0, prevOffset:0], results:[[archetypeId:openEHR-EHR-COMPOSITION.encounter.v1, category:event,
      ehrUid:c35dfe02-4ece-465e-9433-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a754-b98f62cdc4a4,
      parent:fc6da40a-3745-4a1f-9914-4fba03f746d9::EMR::1, startTime:2017-02-11 19:57:19, subjectId:11111111, templateId:Simple Encounter,
      uid:93b93687-135d-4189-ad01-1b8e1ac13289]], timing:20 ms]
      */

      assert res.results.size() > 0
      assert res.results[0].uid == '93b93687-135d-4189-ad01-1b8e1ac13289' // composition uid
      assert res.results[0].ehrUid == 'c35dfe02-4ece-465e-9433-0540c6d95f3f' // ehrUid, used as a query parameter
   }
   
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
