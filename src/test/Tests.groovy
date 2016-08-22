package test

import groovy.util.GroovyTestCase

import com.cabolabs.ehrserver.*

class Tests extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   static EhrServerClient client

   protected void setUp()
   {
      println ""
      client = new EhrServerClient('http://', 'cabolabs-ehrserver.rhcloud.com', 80, '/ehr/')
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
   
   void test_patients()
   {
      client.login('orgman', 'orgman', '1234')

      def patients = client.getPatients()
      
      assert patients.size() > 0
      
      patients.each {
         println it.firstName
      }
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
}
