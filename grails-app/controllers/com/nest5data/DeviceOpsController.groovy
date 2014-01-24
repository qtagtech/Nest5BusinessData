package com.nest5data

import com.mongodb.BasicDBObject
import grails.converters.JSON
import com.nest5data.Device

class DeviceOpsController {
    def mongo

    def index() {

        def result = [message: "bad query request"]
        render result as JSON
        response.setStatus(400)
        return
    }

    def registerDevice(){
        def received = JSON.parse(params.payload)
        def result
        if(!received){
            response.setStatus(400)
            result = [status: 400, code: 55520,message: 'Invalid Device Registration Parameters']
            render result as JSON
            return
        }
        def company = received?.company
        //check company existance in remote Operational RDBMS database, there should be a local copy of all companies.
        //there should be a Company model for matching the received id to it and getting a company object, for now any id will do it
        if(!company){}//error


        //here we should also receive reported location via gps, wifi or any other location provider. public ip of device, os and many more useful data for trending and statistics
        def registered = Device.findAllByUid(received?.device_id)
        if(registered?.size() > 0){  //there was a device with the same id previously registered

                response.setStatus(400)
                result = [status: 400, code: 55511,message: 'Device is already registered for other company'] //here, the device should ask the user if this device should change company. and the call will the be made to changeDeviceRegistration
                render result as JSON
                return
        }
        //else, save the id and registerit to the current company making the request
        def device = new Device(uid: received?.device_id, company: company)
        if(!device.save()){
            println device.errors.allErrors
            response.setStatus(400)
            result = [status: 400, code: 5550,message: 'Error writing object to file system'] //here, the device should ask the user if this device should change company. and the call will the be made to changeDeviceRegistration
            render result as JSON
            return
        }

        response.setStatus(200)
        result = [status: 200, code: 555,message: 'Device successfully registered to company']
        render result as JSON
        return

    }

    def changeDeviceRegistration(){
        response.setStatus(501)
        def result
        result = [status: 501, code: 00000,message: 'Device Re-Registering not yet available.']
        render result as JSON
        return
    }
    def testNewFields(){
        def db = mongo.getDB('nest5BigData')
        def query = new BasicDBObject("uid", "0A01B18B436A002555AF");
        def value = new BasicDBObject('$set',new BasicDBObject('prueba2','holaaaa'))
        def device = db.device.update(query,value)
        /*try {
            while(cursor.hasNext()) {
                println(cursor.next());
            }
        } finally {
            cursor.close();
        }*/
        //println new BasicDBObject('$match', new BasicDBObject("type", "airfare") );
        println device
    }

    def tryNewField(){
        def device = Device.collection.findOne(uid: "0A01B18B436A002555AF") //Low Level API call
        render device.toString()          //returns JSON string
        return

    }


}
