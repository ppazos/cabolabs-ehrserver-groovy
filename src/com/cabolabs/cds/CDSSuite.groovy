package com.cabolabs.cds

class CDSSuite {

   static void main(String[] args)
   {
      // setup the rule
      // in a real scenario, more than one rule can be part of the same suite, we'll keep it simple
      def rule = new CDSRule()
      def params = [ehrUid: 'c35dfe02-4ece-465e-9433-0540c6d95f3f']
      def actions = [new CDSActionPrint()]
      
      // evaluate the rule
      rule.evaluate(params, actions)
      
      // because of the print action, if the rule conditions are met, a result like this will be
      // printed out in the console:
      /*
      [ehrUid:c35dfe02-4ece-465e-9433-0540c6d95f3f, results:[pagination:[max:20, nextOffset:20, offset:0, prevOffset:0], results:[[archetypeId:openEHR-
      EHR-COMPOSITION.encounter.v1, category:event, ehrUid:c35dfe02-4ece-465e-9433-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a
      754-b98f62cdc4a4, parent:fc6da40a-3745-4a1f-9914-4fba03f746d9::EMR::1, startTime:2017-02-11 19:57:19, subjectId:11111111, templateId:Simple Encou
      nter, uid:93b93687-135d-4189-ad01-1b8e1ac13289], [archetypeId:openEHR-EHR-COMPOSITION.encounter.v1, category:event, ehrUid:c35dfe02-4ece-465e-943
      3-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a754-b98f62cdc4a4, parent:bc3a212f-5628-4662-b20a-556064095ea1::EMR::1, star
      tTime:2017-04-19 08:51:09, subjectId:11111111, templateId:Simple Encounter EN, uid:2c4cfdb0-6a1a-4431-b8e6-214e3de367db], [archetypeId:openEHR-EH
      R-COMPOSITION.encounter.v1, category:event, ehrUid:c35dfe02-4ece-465e-9433-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a75
      4-b98f62cdc4a4, parent:e1ff87f4-f5a0-4e1a-99f5-91d4969dd5ba::EMR::1, startTime:2017-04-19 08:51:44, subjectId:11111111, templateId:Simple Encount
      er EN, uid:91c9fe9a-3026-46f3-a9dd-3f6dbcc76286], [archetypeId:openEHR-EHR-COMPOSITION.encounter.v1, category:event, ehrUid:c35dfe02-4ece-465e-94
      33-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a754-b98f62cdc4a4, parent:ac09e87c-2fe8-4d59-b627-88b0afe22b13::EMR::1, sta
      rtTime:2017-04-19 08:57:30, subjectId:11111111, templateId:Simple Encounter EN, uid:33a8f3dc-1e86-4a81-b743-b2fde2a7e745], [archetypeId:openEHR-E
      HR-COMPOSITION.encounter.v1, category:event, ehrUid:c35dfe02-4ece-465e-9433-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a7
      54-b98f62cdc4a4, parent:859010d6-0f05-4158-9601-6b13f8bffb18::EMR::1, startTime:2017-04-19 08:58:46, subjectId:11111111, templateId:Simple Encoun
      ter EN, uid:7850d863-ffe6-47d3-8aa2-32f64aa06826], [archetypeId:openEHR-EHR-COMPOSITION.encounter.v1, category:event, ehrUid:c35dfe02-4ece-465e-9
      433-0540c6d95f3f, lastVersion:true, organizationUid:d8b5bcb6-f26a-4192-a754-b98f62cdc4a4, parent:47ad031f-8344-4448-933f-255fb046f02f::EMR::1, st
      artTime:2017-04-19 08:59:44, subjectId:11111111, templateId:Simple Encounter EN, uid:d692582c-eb6d-4aa1-9ffd-66614dc0a100]], timing:30 ms]]
      */
   }
}

