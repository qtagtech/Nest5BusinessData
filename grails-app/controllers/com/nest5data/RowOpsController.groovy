package com.nest5data

import grails.converters.JSON

class RowOpsController {
    def mongo

    def index() {

        def result = [message: "bad query request"]
        render result as JSON
        response.setStatus(400)
        return
    }

    def rowReceived(){
        /*
        We have received a row of data, it may be an Insert, update or delete of any table and in any client.
        First, the system looks for the same element (table + rowid) between timeReceived and timeSaved which comes from directly from the client.
        If there is a record between those two times, the response must be an orther to discard the row and update new value set from different client in the meanwhile.
        Then, a search for equal items before timeReceived must be made.
        If no equal record exists, the an Insert is being generated and all devices must be notified.
        If there is a record, the md5 hashes must be compared (md5 made on data only)
        In case hashes are different, Update notification must be sent to all devices.
        If hashes are equal, old record is discarded leaving as current the new received record.
        EVERY DISCARD ACTION IS A SOFT-DELETE WITH FIELD isDeleted.
        */
        def result
        if(!params?.row){
            response.setStatus(400)
            result = [status: 400, code: 55510,message: 'Invalid row or no row at all',payload: null]
            render result as JSON
            return
        }
        def received = JSON.parse(params?.row)

        if(!received){
            //return error, and not received
            response.setStatus(400)
            result = [status: 400, code: 55510,message: 'Invalid row or no row at all',payload: null]
            render result as JSON
            return
        }

        def timeCreated = received?.time_created  //ATTENTION, timestamp must be in milliseconds and date should be in same format both in server with client's configuration
        if(!timeCreated){
            //return error, and not received
            response.setStatus(400)
            result = [status: 400, code: 55511,message: 'Invalid created_time Parameter',payload: null]
            render result as JSON
            return
        }
        timeCreated = new Date(received?.time_created as Long)
        if(received.sync_id == 0){//This means it's a new insert, since it doesn't have common id through all devices. nothing else should be done but save it and generate an id that should be returned to the client to update
            def rowHash = received?.fields?.encodeAsMD5()
            def sync_id = 234293874298374203 // generate unique inter-table id for this row
            def db = mongo.getDB('nest5BigData')
            def device = Device.collection.findOne('uid':received.device_id)
            if(!device){
                response.setStatus(400)
                result = [status: 400, code: 55512,message: 'Invalid device_id Parameter. Either the device id is misspelled or it is not yet registered in the platform',payload: null]
                render result as JSON
                return
            }
            db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': rowHash,'device': device, 'isDeleted': false,'syncId': sync_id)
            response.setStatus(201)//new object created
            result = [status: 201, code: 555,message: 'New document successfully created, please update sync_id value in client\'s DB',payload: sync_id]  //success, but status indicates payload present, and las row should be updated with the sync_id generated in the server
            render result as JSON
            return
        }
        def lockedResults = DataRow.withCriteria {
            def now = new Date()
            eq 'syncId',received.sync_id
            gt 'timeCreated', new Date(received.time_created)
            lt 'timeReceived', now
            eq 'isDeleted', false

        }
        if(lockedResults.size() > 0) {   //there were records saved during saved Time and receive time that are funcitonal, that means not deleted, this row must be discarded (softDelete)
            response.setStatus(406)//not acceptable
            result = [status: 406, code: 55513,message: 'CAUTION: Overlap saving attempted. A newer version of this element has been saved from a different client',payload: null]  //success, but status indicates payload present, and las row should be updated with the sync_id generated in the server
            render result as JSON
            return
        }
        //if no record was found, we find previous record prior to now
        def previousResults = DataRow.withCriteria {
            def now = new Date()
            lt 'timeReceived', now
            eq 'isDeleted', false
            }
        if(previousResults.size() == 0){ //there were no records. it is a new insert, send notification to all devices registered with this one. This shouldn't happen, since if it was a new record it wouldn't have sync_id!= 0
            def rowHash = received?.fields?.encodeAsMD5()
            def sync_id = 234293874298374203 // generate unique inter-table id for this row
            def db = mongo.getDB('nest5BigData')
            def device = Device.collection.findOne('uid':received.device_id)
            if(!device){
                response.setStatus(400)
                result = [status: 400, code: 55512,message: 'Invalid device_id Parameter. Either the device id is misspelled or it is not yet registered in the platform',payload: null]
                render result as JSON
                return
            }
            db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': rowHash,'device': device, 'isDeleted': false,'syncId': sync_id)
            response.setStatus(201)//new object created
            result = [status: 201, code: 555,message: 'New document successfully created, please update sync_id value in client\'s DB',payload: sync_id]  //success, but status indicates payload present, and las row should be updated with the sync_id generated in the server
            render result as JSON
            return
        }
        //else, compare md5 hashes but only get the latest record with isDeleted false in case there are more than one (error may exist here)
        def lastResult = previousResults?.sort {it?.timeReceived}.get(0)
        def newHash = received?.fields?.encodeAsMD5()
        if(newHash == lastResult.hashKey){ //hashes are the same, so row hasn't had any update, discard the old one with soft delete, save new one and send ACK
            //save new row
            def db = mongo.getDB('nest5BigData')
            def device = Device.collection.findOne('uid':received.device_id)
            if(!device){
                response.setStatus(400)
                result = [status: 400, code: 55512,message: 'Invalid device_id Parameter. Either the device id is misspelled or it is not yet registered in the platform',payload: null]
                render result as JSON
                return
            }
            db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': newHash,'device': device, 'isDeleted': false,'syncId': received.sync_id)
            //discard old row
            lastResult.isDeleted = true
            lastResult.save(flush: true)
            response.setStatus(200)//new object created
            result = [status: 200, code: 555,message: 'Document successfully updated! There are no changes.',payload: received.sync_id]  //success in updating latest row version although ther were no changes.
            render result as JSON
            return

        }
        //else, the new one is an update of the row, save it, send multicast to all devices withe the update to be made on the specific row and table
        def db = mongo.getDB('nest5BigData')
        def device = Device.collection.findOne('uid':received.device_id)
        if(!device){
            response.setStatus(400)
            result = [status: 400, code: 55512,message: 'Invalid device_id Parameter. Either the device id is misspelled or it is not yet registered in the platform',payload: null]
            render result as JSON
            return
        }
        db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': newHash,'device': device, 'isDeleted': false,'syncId': received.sync_id)

        lastResult.isDeleted = true
        lastResult.save(flush: true)
        response.setStatus(200)//new object created
        result = [status: 200, code: 555,message: 'Document successfully updated to newer version!',payload: received.sync_id]  //success in updating to newer version
        render result as JSON
        return


    }

    def viewRow(){
        def dataRow = DataRow.findAll()[0]    //maps the object with GORM thus it doesn´t map field property in it. Fields need to be accessed explicitly with the dot operator.
        println dataRow.timeReceived
        println dataRow.timeCreated
        println dataRow.device
        println dataRow.fields.name
        println dataRow.syncId
        render dataRow as JSON
        return
    }


}
