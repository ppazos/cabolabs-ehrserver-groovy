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
      def token = client.login('orgman', 'orgman', '1234') // stores token internally
      assert token != null
      //println "login this token " + client.config.token
   }
   
   void test_profile()
   {
     def token = client.login('orgman', 'orgman', '1234')
     def profile = client.getProfile('orgman')
     
     assert profile != null
     assert profile.username == 'orgman'
   }
   
   
   void test_ehrs()
   {
      client.login('orgman', 'orgman', '1234')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      res.ehrs.each {
         println it.uid
      }
   }
   
   
   void test_get_ehr()
   {
      client.login('orgman', 'orgman', '1234')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      String uid = res.ehrs[0].uid
      
      def ehr = client.getEhr(uid)
      
      assert ehr != null
      
      assert ehr.uid == uid
   }
   
   void test_get_ehr_by_subject()
   {
      client.login('orgman', 'orgman', '1234')
      def res = client.getEhrs()
      
      assert res.ehrs.size() > 0
      
      String subjectUid = res.ehrs[0].subjectUid
      
      def ehr = client.getEhrIdByPatientId(subjectUid)
      
      assert ehr != null
      
      assert ehr.subjectUid == subjectUid
   }
   
   /*
   void test_ehruid_for_patient()
   {
      client.login('orgman', 'orgman', '1234')
      def patients = client.getPatients()
      def ehr_uid = client.getEhrIdByPatientId( patients[0].uid )
      println "EHR UID: "+ ehr_uid
   }
   
   void test_ehr_contributions()
   {
      client.login('orgman', 'orgman', '1234')
      def patients = client.getPatients()
      def ehr_uid = client.getEhrIdByPatientId( patients[0].uid )
      def res = client.getContributions( ehr_uid, 10 )
      
      assert res.contributions.size() == 10
      
      //println "contributions: "+ res.contributions
   }
   
   void test_ehr_compositions()
   {
      client.login('orgman', 'orgman', '1234')
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
