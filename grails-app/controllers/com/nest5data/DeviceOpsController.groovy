package com.nest5data
import com.mongodb.BasicDBObject
import com.sun.tools.internal.ws.wsdl.document.http.HTTPBinding
import grails.converters.JSON
import com.nest5data.Device
import grails.plugin.springsecurity.annotation.Secured
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

@Secured(["permitAll"])
class DeviceOpsController {
    def mongo
    def springSecurityService

    def index() {

        def result = [message: "bad query request"]
        render result as JSON
        response.setStatus(400)
        return
    }

    def registerDevice(){
        def received = null
        try{received = JSON.parse(params?.payload)}catch (Exception e){}
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
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55520,message: 'Invalid Device Registration Parameters']
            render result as JSON
            return
        }//error

        def com = Company.findByGlobal_id(company as Long)
        if(!com){
            //check nest5 server since it hasn't synced
            def http = new HTTPBuilder( 'http://nest5api.aws.af.cm' )
            def jsonData
// perform a GET request, expecting JSON response data
            http.request( GET, TEXT ) {

                uri.path = '/company/companyDetails'
                uri.query = [company_id:company]

                headers.'User-Agent' = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'

                // response handler for a success response code:
                response.success = { resp, json ->
                    println resp.statusLine
                    println resp.contentType

                    // parse the JSON response object:
                    jsonData = JSON.parse(json)
                    println jsonData
                }

                // handler for any failure status code:
                response.failure = { resp ->
                    println "Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}"
                    response.setStatus(400)
                    result = [status: 400, code: 55522,message: 'Error fetching Data']
                    render result as JSON
                    return
                }
            }
            if(!jsonData){
                response.setStatus(400)
                result = [status: 400, code: 55522,message: 'Error fetching Data.']
                render result as JSON
                return
            }
            if (jsonData.status != 1){
                response.setStatus(400)
                result = [status: 400, code: 55521,message: 'Company does not exist.']
                render result as JSON
                return
            }
            def category = Category.findByName(jsonData.category.name)
            if(!category){
                category = new Category(
                        name: jsonData.category.category.name,
                        description: jsonData.category.category.description,
                        icon: new Icon(
                                name:jsonData.category.icon.name,
                                tipo:jsonData.category.icon.tipo,
                                description: jsonData.category.icon.description,
                                ruta:jsonData.category.icon.ruta).save()).save(flush: true)
            }

            com = new Company(
                    accountExpired: jsonData.company.accountExpired,
                    accountLocked:  jsonData.company.accountLocked,
                    active: jsonData.company.active,
                    email:  jsonData.company.email,
                    address: jsonData.company.address,
                    contactName: jsonData.company.contactName,
                    enabled: jsonData.company.enabled,
                    global_id: jsonData.company.id,
                    logo: jsonData.company.log,
                    name: jsonData.company.name,
                    nit: jsonData.company.nit,
                    password: jsonData.company.password,
                    passwordExpired: jsonData.company.passwordExpired,
                    registerDate: jsonData.company.registerDate,
                    username: jsonData.company.username,
                    url: jsonData.company.url,
                    telephone: jsonData.company.telephone,
                    categoy: category).save(flush: true)

            def companyRole = SecRole.findByAuthority('ROLE_COMPANY') ?: new SecRole(authority: 'ROLE_COMPANY').save(failOnError: true)
            if (!com.authorities.contains(companyRole)) {
                SecUserSecRole.create com, companyRole
            }

        }






        //here we should also receive reported location via gps, wifi or any other location provider. public ip of device, os and many more useful data for trending and statistics
        def registered = Device.findAllByUid(received?.device_id)
        if(registered?.size() > 0){  //there was a device with the same id previously registered

                response.setStatus(200)
                result = [status: 200, code: 55511,message: 'Device is already registered for other company, login continues normally.'] //here, the device should ask the user if this device should change company. and the call will the be made to changeDeviceRegistration
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
        def db = mongo.getDB('pruebamongo')
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
