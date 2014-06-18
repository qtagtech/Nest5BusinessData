package com.nest5data

import com.mongodb.BasicDBObject
import com.mongodb.DBCursor
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder
import org.bson.types.ObjectId

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

@Secured(["permitAll"])
class RowOpsController {
    def mongo

    def index() {

        def result = [message: "bad query request"]
        render result as JSON
        response.setStatus(400)
        return
    }

    @Transactional
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
            result = [status: 400, code: 55510,message: 'Invalid row or no row at all',syncId: null,syncRow: null]
            render result as JSON
            return
        }
        def syncRow = params?.sync_row_id
        if(!syncRow){
            response.setStatus(400)
            result = [status: 400, code: 55510,message: 'Invalid row or no row at all',syncId: null,syncRow: null]
            render result as JSON
            return
        }
        def received = JSON.parse(params?.row)

        if(!received){
            //return error, and not received
            response.setStatus(400)
            result = [status: 400, code: 55510,message: 'Invalid row or no row at all',syncId: null,syncRow: null]
            render result as JSON
            return
        }

        def timeCreated = received?.time_created  //ATTENTION, timestamp must be in milliseconds and date should be in same format both in server with client's configuration
        if(!timeCreated){
            //return error, and not received
            response.setStatus(400)
            result = [status: 400, code: 55511,message: 'Invalid created_time Parameter',syncId: null,syncRow: null]
            render result as JSON
            return
        }
        timeCreated = new Date(received?.time_created as Long)
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        def device
        if(received.device_id == "DdLrWE6UPLM0uYhSlUO7"){ //web device, check if it is already registered or if not and return it, if null, get it as before
            if(registerDevice(params)) {
                //check using the company in the store
                device = db.device.findOne("uid":received?.device_id,'store.company': params.company as Integer)
                println "device no deberia existir"
                println device
                if(!device){
                    println "deberia entrar aca y llamar a checkStores"
                    def store = checkStores(params)
                    println "regresa de checkStores con tienda"+ store
                    println "guarda device con esa store"
                    def resultado = db.device.insert('uid':received.device_id, name: "Sin Nombre",'store': store, registeredOn: new Date(), lastUpdated: new Date(), minSale: 0, maxSale: 0, currentSale: 0, prefix: " ", resolution: " " )
                    if(resultado.error)
                    {
                        response.setStatus(400)
                        result = [status: 400, code: 55512,message: 'Error saving new device to database: '+resultado?.lastError?.err,syncId: null,syncRow: null]
                        render result as JSON
                        return
                    }
                    println "busca device en db con uid "+received.device_id+" y store.company "+received.company
                        device = db.device.findOne("uid":received?.device_id,"store.company":params.company as Integer)
                    println device
                }
            }
        }
        if(!device) //it is not a web device so try getting the device by id since it has uniue id as it is a mobile device
            device = db.device.findOne('uid':received.device_id)

        if(!device){
            println "aca7"
            response.setStatus(400)
            result = [status: 400, code: 55512,message: 'Invalid device_id Parameter. Either the device id is misspelled or it is not yet registered in the platform',syncId: null,syncRow: null]
            render result as JSON
            return
        }
        if(received.sync_id as Long == 0){//This means it's a new insert, since it doesn't have common id through all devices. nothing else should be done but save it and generate an id that should be returned to the client to update
            //println"aca1"
            def rowHash = received?.fields?.encodeAsMD5()
            def sync_id = randomNumber() //it gets a random number, if the number already exists in the database the function is going to return 0 so it will cycle the while until it gets a number different from 0L
            while(sync_id == 0L){
                sync_id = randomNumber()
            }
          db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': rowHash,'device': device, 'isDeleted': false,'syncId': sync_id)

            //check if it is a sale, if yes, update currentSale value in device
            //update device's lastupdated field everything in try catch to avoid conflicts
            try{
                //println"aca2"
                BasicDBObject searchQuery = new BasicDBObject().append('uid',received.device_id)
                BasicDBObject newDocument = new BasicDBObject()
                newDocument.append('$set',new BasicDBObject().append("lastUpdated",new Date()))
                db.device.update(searchQuery,newDocument)
                if(received.table == "sale"){
                    if(device.currentSale < received.fields.sale_number){
                        BasicDBObject updatedDocument = new BasicDBObject()
                        updatedDocument.append('$set',new BasicDBObject().append("currentSale",received.fields.sale_number))
                        db.device.update(searchQuery,updatedDocument)
                    }
                }
            }catch (Exception e){
                //println"aca3"
                //println"error actualizando device"
                e.printStackTrace()
            }

            response.setStatus(201)//new object created
            result = [status: 201, code: 555,message: 'New document successfully created, please update sync_id value in client\'s DB',syncId: sync_id,syncRow: syncRow]  //success, but status indicates syncId present, and las row should be updated with the sync_id generated in the server
            render result as JSON
            return
        }
        //check the device is registered to the same company as the previous records for this sync_id element
        def deleting = received.is_delete ? true : false
        if(deleting){
            //println"aca4"//the device sent a delete request with all the properties except for the fields (blank values). the server should soft-delete it. The deleted flag is true since the element wont be available to any device from now on
            db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: null,'hashKey': 'N/A','device': device as JSON, 'isDeleted': true,'syncId': received.sync_id as Long,syncRow: syncRow)
            //set isDeleted flag on previous existent element (if any) to true
            def previousResults = DataRow.withCriteria {
                def now = new Date()
                lt 'timeReceived', now
                eq 'isDeleted', false
                eq 'syncId', received.sync_id as Long
            }
            if(previousResults?.size() != 0){ //there were records.
                //println"aca5"
                def lastResult = previousResults?.sort {it?.timeReceived}.get(0)
                //check if current device saving a row, belongs to the same company as the the previous device
                if(lastResult.device.store.company != device.store.company){
                    //println"aca6"
                    response.setStatus(400)//not acceptable
                    result = [status: 400, code: 55514,message: 'This Record doesn\'t belong to your company.',syncId: null,syncRow: null]
                    render result as JSON
                    return
                }
                lastResult.isDeleted = true
                lastResult.save(flush: true)
            }
            //println"aca7"
            response.setStatus(200)//new object created
            result = [status: 200, code: 555,message: 'Document deleted successfully',syncId: received.sync_id,syncRow: syncRow]  //success deleting the current element
            render result as JSON
            //send delete notification to all registered devices.
            return
        }
        def lockedResults = DataRow.withCriteria {
            def now = new Date()
            eq 'syncId',received.sync_id as Long
            ge 'timeCreated', new Date(received?.time_created)
            le 'timeReceived', now
            eq 'isDeleted', false

        }
        if(lockedResults.size() > 0) {   //there were records saved during saved Time and receive time that are functional, that means not deleted, this row must be discarded (softDelete)
            //println "aca8"
            response.setStatus(406)//not acceptable
            result = [status: 406, code: 55513,message: 'CAUTION: Overlap saving attempted. A newer version of this element has been saved from a different client',syncId: null,syncRow: syncRow]  //success, but status indicates syncId present, and las row should be updated with the sync_id generated in the server
            render result as JSON
            return
        }
        //if no record was found, we find previous record prior to now
        def previousResults = DataRow.withCriteria {
            def now = new Date()
            lt 'timeReceived', now
            eq 'isDeleted', false
            eq 'syncId', received.sync_id as Long
            }
        if(previousResults.size() == 0){ //there were no records. it is a new insert, send notification to all devices registered with this one. This shouldn't happen, since if it was a new record it wouldn't have sync_id!= 0
            //println "aca9"
            def rowHash = received?.fields?.encodeAsMD5()
            def sync_id = randomNumber() //it gets a random number, if the number already exists in the database the function is going to return 0 so it will cycle the while until it gets a number different from 0L
            while(sync_id == 0L){
                sync_id = randomNumber()
            }
            db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': rowHash,'device': device, 'isDeleted': false,'syncId': sync_id)
            //check if it is a sale, if yes, update currentSale value in device
            //update device's lastupdated field everything in try catch to avoid conflicts
            try{
                //println "aca10"
                BasicDBObject searchQuery = new BasicDBObject().append('uid',received.device_id)
                BasicDBObject newDocument = new BasicDBObject()
                newDocument.append('$set',new BasicDBObject().append("lastUpdated",new Date()))
               db.device.update(searchQuery,newDocument)
                if(received.table == "sale"){
                    if(device.currentSale < received.fields.sale_number){
                        BasicDBObject updatedDocument = new BasicDBObject()
                        updatedDocument.append('$set',new BasicDBObject().append("currentSale",received.fields.sale_number))
                        db.device.update(searchQuery,updatedDocument)
                    }
                }
            }catch (Exception e){
                //println "aca11"
                println "error actualizando device"
                println e
            }
            //println "aca12"
            response.setStatus(201)//new object created
            result = [status: 201, code: 555,message: 'New document successfully created, please update sync_id value in client\'s DB',syncId: sync_id,syncRow: syncRow]  //success, but status indicates syncId present, and las row should be updated with the sync_id generated in the server
            render result as JSON
            return
        }
        //println "aca13"
        //else, compare md5 hashes but only get the latest record with isDeleted false in case there are more than one (error may exist here)
        def lastResult = previousResults?.sort {it?.timeReceived}?.get(0)
        //println lastResult
        //check if current device saving a row, belongs to the same company as the the previous device
        if(lastResult.device.store.company != device.store.company){
            //

            //println "aca14"
            response.setStatus(400)//not acceptable
            result = [status: 400, code: 55514,message: 'This Record doesn\'t belong to your company.',syncId: null,syncRow: null]
            render result as JSON
            return
        }
        def newHash = received?.fields?.encodeAsMD5()
        if(newHash == lastResult.hashKey){ //hashes are the same, so row hasn't had any update, discard the old one with soft delete, save new one and send ACK
            //save new row
            //println "aca15"
            db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': newHash,'device': device, 'isDeleted': false,'syncId': received.sync_id as Long)
            //discard old row
            lastResult.isDeleted = true
            lastResult.save(flush: true)
            //check if it is a sale, if yes, update currentSale value in device
            //update device's lastupdated field everything in try catch to avoid conflicts
            try{
                //println "aca16"
                BasicDBObject searchQuery = new BasicDBObject().append('uid',received.device_id)
                BasicDBObject newDocument = new BasicDBObject()
                newDocument.append('$set',new BasicDBObject().append("lastUpdated",new Date()))
                db.device.update(searchQuery,newDocument)
                if(received.table == "sale"){
                    if(device.currentSale < received.fields.sale_number){
                        BasicDBObject updatedDocument = new BasicDBObject()
                        updatedDocument.append('$set',new BasicDBObject().append("currentSale",received.fields.sale_number))
                        db.device.update(searchQuery,updatedDocument)
                    }
                }
            }catch (Exception e){
                //println "aca17"
                println "error actualizando device"
                println e
            }
            //println "aca18"
            response.setStatus(200)//new object created
            result = [status: 200, code: 555,message: 'Document successfully updated! There are no changes.',syncId: received.sync_id,syncRow: syncRow]  //success in updating latest row version although ther were no changes.
            render result as JSON
            return

        }
        //println "aca18"
        //else, the new one is an update of the row, save it, send multicast to all devices withe the update to be made on the specific row and table
        db.dataRow.insert('table': received.table, 'rowId': received.row_id,'timeCreated': timeCreated,'timeReceived': new Date(),fields: received.fields,'hashKey': newHash,'device': device, 'isDeleted': false,'syncId': received.sync_id as Long)
        lastResult.isDeleted = true
        lastResult.save(flush: true)
        //check if it is a sale, if yes, update currentSale value in device
        //update device's lastupdated field everything in try catch to avoid conflicts
        try{
            //println "aca19"
            BasicDBObject searchQuery = new BasicDBObject().append('uid',received.device_id)
            BasicDBObject newDocument = new BasicDBObject()
            newDocument.append('$set',new BasicDBObject().append("lastUpdated",new Date()))
            db.device.update(searchQuery,newDocument)
            if(received.table == "sale"){
                if(device.currentSale < received.fields.sale_number){
                    BasicDBObject updatedDocument = new BasicDBObject()
                    updatedDocument.append('$set',new BasicDBObject().append("currentSale",received.fields.sale_number))
                    db.device.update(searchQuery,updatedDocument)
                }
            }
        }catch (Exception e){
            //println "aca20"
            println "error actualizando device"
            println e
        }
        //println "aca21"
        response.setStatus(200)//new object created
        //println "acaaaa"
        result = [status: 200, code: 555,message: 'Document successfully updated to newer version!',syncId: received.sync_id,syncRow: syncRow]  //success in updating to newer version
        render result as JSON
        return
    }

    def viewRow(){
        def dataRow = DataRow.findAll()[0]    //maps the object with GORM thus it doesn´t map field property in it. Fields need to be accessed explicitly with the dot operator.
        //printlndataRow.timeReceived
        //printlndataRow.timeCreated
        //printlndataRow.device
        //printlndataRow.fields.name
        //printlndataRow.syncId
        render dataRow as JSON
        return
    }

    def fetchProperty(){
        def result
        def company = params?.company
        if(!company){

            println "aca1"
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }
        def table = params?.table
        if(!table){
            println "aca2"
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid table',payload: null]
            render result as JSON
            return
        }
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("table",table).
                append("device.store.company",company as Integer).
                append("isDeleted", false);
        //println query
        def filas = db.dataRow.find(query)
        if(filas.size() == 0) {
            response.setStatus(400)
            result = [status: 400, code: 55531,message: 'Empty Result Set',payload: null]
            render result as JSON
            return
        }
        result = [status: 200, code: 555,message: 'Success. See payload.',payload: []]
        while(filas.hasNext()) {
            def element = filas.next() ?: null
            if(element)
                result.payload.add(element.toMap())

        }
        render result as JSON
        return

    }

    def fetchRow(){
        //printlnparams
        def result



        def company = params?.company
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }
        def row = params?.row
        if(!row){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid table',payload: null]
            render result as JSON
            return
        }
        /*
            *
            * **************************************************************************INIT TEMP!!!!
            ********************BRING ALL OLD FORMATTED DATAROWS, WITH COMPANY ID'S CONVERTED TO STORES, THE FIRST STORE IN THE SYSTEM FOR THE COMPANY OR A NEW CREATED ONE IF NONE EXIST
            *
            *
            * */

        //poner una función que traiga todas las datarow con el campo company = 16 o cualquiera que sea, removido y con el primer store que se aparezca $set.

        try{
            transformDataRow(company,row)
        }catch(Exception e){
            e.printStackTrace()
        }



        /*
        *
        *
        *******************************************************************************END TEMP!!!!
        *************************BRING ALL OLD FORMATTED DATAROWS, WITH COMPANY ID'S CONVERTED TO STORES, THE FIRST STORE IN THE SYSTEM FOR THE COMPANY OR A NEW CREATED ONE IF NONE EXIST
        *
        *
        * */
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("syncId",row as Long).
                append("device.store.company",company as Integer).
                append("isDeleted", false);
        //printlnquery
        def filas = db.dataRow.find(query)
        if(filas.size() == 0) {
            response.setStatus(400)
            result = [status: 400, code: 55531,message: 'Empty Result Set',payload: null]
            render result as JSON
            return
        }

        def element = filas.next().toMap()

        result = [status: 200, code: 555,message: 'Success. See payload.',payload: element]

        render result as JSON
        return

    }
    def fetchSpecialRow(){
        //printlnparams
        def result



        def company = params?.company
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }
        def row = params?.row
        if(!row){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid table',payload: null]
            render result as JSON
            return
        }
        /*
            *
            * **************************************************************************INIT TEMP!!!!
            ********************BRING ALL OLD FORMATTED DATAROWS, WITH COMPANY ID'S CONVERTED TO STORES, THE FIRST STORE IN THE SYSTEM FOR THE COMPANY OR A NEW CREATED ONE IF NONE EXIST
            *
            *
            * */

        //poner una función que traiga todas las datarow con el campo company = 16 o cualquiera que sea, removido y con el primer store que se aparezca $set.

        try{
            transformDataRow(company,row)
        }catch(Exception e){
            e.printStackTrace()
        }



        /*
        *
        *
        *******************************************************************************END TEMP!!!!
        *************************BRING ALL OLD FORMATTED DATAROWS, WITH COMPANY ID'S CONVERTED TO STORES, THE FIRST STORE IN THE SYSTEM FOR THE COMPANY OR A NEW CREATED ONE IF NONE EXIST
        *
        *
        * */
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("table","special_product")
                .append("fields.product_id",row as Long).
                append("device.store.company",company as Integer).
                append("isDeleted", false);
        //printlnquery
        def filas = db.dataRow.find(query)
        if(filas.size() == 0) {
            response.setStatus(400)
            result = [status: 400, code: 55531,message: 'Empty Result Set',payload: null]
            render result as JSON
            return
        }
        def element = filas.next().toMap()
        result = [status: 200, code: 555,message: 'Success. See payload.',payload: element]

        render result as JSON
        return

    }


    def checkStores(params){
        println "llega a checkstores"
        def company = params.company
        DBCursor stores
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        try{
            BasicDBObject query = new BasicDBObject().append('company',company as Integer)
            stores = db.store.find(query)
        }catch(Exception e){
            e.printStackTrace()
            return null
        }
        println "stores encontradas:"
        println stores.count()
        if(stores.count() == 0){
            println "no hay tiendas, crea una nueva"
            def obid = new ObjectId()
            def resultado = db.store.insert(_id: obid,name: "Local Por Defecto", latitude: 0, longitude: 0, location: null, company: company as Integer)
            if(resultado.error)
            {
                println resultado.getLastError()
                return null
            }
            println "busca en db una tienda con el id que se acaba de crear "+obid
            def store = db.store.findOne('_id':obid)
            println "dentro de checkStores encontró una vez hecha comprobación y demás esta tienda que devolverá"
            println store
            return store
        }
        else{
            def store = stores.next()
            println "existen tiendas entopnces devuleve la primera de la lista"+store
            return store
        }


    }

    private boolean registerDevice(params){
        def received = null
        try{received = JSON.parse(params?.row)}catch (Exception e){}
        def result
        if(!received){
            return false
        }
        def company = params?.company
        if(!company){
            return false
        }
        def http = new HTTPBuilder( grailsApplication.config.com.nest5.BusinessData.Nest5APIServerURL )
        def jsonData
        http.request( GET, TEXT ) {
            uri.path = '/api/companyDetails'
            uri.query = [company_id:company]
            headers.'User-Agent' = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'
            response.success = { resp, json ->
                jsonData = JSON.parse(json)
            }
            response.failure = { resp ->
                return false
            }
        }
        if(!jsonData){
            return false
        }
        if (jsonData.status != 1){
            return false
        }
        return true

    }

    private Long randomNumber (){
         def number =  (long) Math.floor(Math.random() * 9000000000000000L) + 1000000000000000L
        if(DataRow.findBySyncId(number)){
            return 0L
        }
        return number
    }

    /*
    * /**************************INIT TEMP
    *
    * ******************************TRANSFORM DATAROWS INTO NEW FORMAT WITH STORE
    *
    */
    def transformDataRow(company,row){
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        def store = db.store.findOne('company':company as Integer)        //¿Cualquier store de la empresa?
        if(!store){
            def obid = new ObjectId()
            def resultado = db.store.insert(_id: obid,name: "Local Por Defecto", latitude: 0, longitude: 0, location: null, company: company as Integer)
            store = db.store.findOne('_id':obid)
        }

        if(!store)
            return false

        BasicDBObject set = new BasicDBObject().append('$set',new BasicDBObject().append('store',store))
        set.append('$unset',new BasicDBObject().append('company',''))
        BasicDBObject query = new BasicDBObject().append("company",company as Integer)
        db.device.update(query,set,false,true)
        BasicDBObject set2 = new BasicDBObject().append('$set',new BasicDBObject().append("device.store",store))
        set2.append('$unset',new BasicDBObject().append("device.company",''))
        BasicDBObject query2 = new BasicDBObject().append("syncId",row)
        db.dataRow.update(query2,set2,false,false)
    }
    /*
    *
    *
    *
    * */


}
