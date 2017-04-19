package com.cabolabs.cds

import com.cabolabs.ehrserver.EhrServerClient

class CDSRule {

   // data extractor
   // the rule needs to extract data from the clinical data repository,
   // on this case it knows the specific datasource, this can be further
   // abstracted to be more generic, but it is an EHRServer client example :D
   def ehrserver = new EhrServerClient('https://', 'ehrserver-cabolabs2.rhcloud.com', 443, '/')
   
   /**
    * implements the rule logic, on this case check if a patient has more than 5 records
    * of high BP in the last 12 months, if it does, the actions will be executed.
    */
   def evaluate(Map params, List<CDSAction> actions)
   {
      // patient's ehrUid is present
      assert params.ehrUid
      
      // the query we want to execute has this queryUid (created in the EHRServer Web Console)
      // this query is for clinical documents that contains records of high blood pressure
      def query_uid = '7d125791-9f16-4b9b-a121-8b1238b59cb5'
      
      // we have an organization API Key to use the EHRServer's REST API
      def api_key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InR4YWltbXphc2ZweXhudmR2bHV2ZGNqeGR4ZmZjcXFtZHlsa2lvaWZpaGR2dXlqa2t1IiwiZXh0cmFkYXRhIjp7Im9yZ2FuaXphdGlvbiI6IjEyMzQ1NiJ9LCJpc3N1ZWRfYXQiOiIyMDE3LTA0LTE5VDAzOjEwOjQxLjU5NS0wNDowMCJ9.HpEmGDoc9bwAY0CMU72cBuMnrmuP5Y9S7PbQO6opxkk="
   
      // setup the call
      ehrserver.setAPIKey(api_key)
      
      // make the call and get results from the last 12 months
      def res = ehrserver.query(query_uid, params.ehrUid, (new Date() - 365).format('yyyyMMdd'), 'json')
      
      /**
       * the result looks like this:
       *
       * [
       *   pagination: [max:20, nextOffset:20, offset:0, prevOffset:0],
       *   results: [
       *     [
       *       archetypeId:openEHR-EHR-COMPOSITION.encounter.v1,
       *       category:event,
       *       ehrUid:c35dfe02-4ece-465e-9433-0540c6d95f3f,
       *       lastVersion:true,
       *       organizationUid:d8b5bcb6-f26a-4192-a754-b98f62cdc4a4,
       *       parent:fc6da40a-3745-4a1f-9914-4fba03f746d9::EMR::1,
       *       startTime:2017-02-11 19:57:19,
       *       subjectId:11111111,
       *       templateId:Simple Encounter,
       *       uid:93b93687-135d-4189-ad01-1b8e1ac13289
       *     ]
       *   ],
       *   timing:20 ms
       * ]
       */
      
      // execute the rule logic
      if ( res.results.size() > 5 )
      {
         // add data from data source in the action params
         params.results = res
         
         // execute all the actions if any
         // this can be anything, invoking an external service, sending an email or SMS,
         // storing data in a database, creating a log in a file, etc.
         // actions might return results, the results can be merged and returned from
         // the rule evaluation, this is off scope for this example.
         actions*.execute(params)
      }
   }
}
