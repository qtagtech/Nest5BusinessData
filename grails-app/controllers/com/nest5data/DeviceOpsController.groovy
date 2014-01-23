package com.nest5data

import grails.converters.JSON

class DeviceOpsController {

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
        println registered
        if(registered?.size() > 0){  //there was a device with the same id previously registered

                response.setStatus(400)
                result = [status: 400, code: 55511,message: 'Device is already registered for other company'] //here, the device should ask the user if this device should change company. and the call will the be made to changeDeviceRegistration
                render result as JSON
                return
        }
        println received
        println received.device_id
        println received.company
        //else, save the id and registerit to the current company making the request
        def device = new Device(uid: received?.device_id, company: company)
        device.save(flush: true)
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
}
