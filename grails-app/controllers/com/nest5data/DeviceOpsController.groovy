package com.nest5data
import com.mongodb.BasicDBObject
import com.mongodb.DBCursor
import com.sun.tools.internal.ws.wsdl.document.http.HTTPBinding
import grails.converters.JSON
import com.nest5data.Device
import grails.plugin.springsecurity.annotation.Secured
import groovyx.net.http.*
import org.bson.types.ObjectId

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

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
        //println "aca1"
        def received = null
        try{received = JSON.parse(params?.payload)}catch (Exception e){}
        def result
        if(!received){
            //println "aca2"
            response.setStatus(400)
            result = [status: 400, code: 55520,message: 'Invalid Device Registration Parameters']
            render result as JSON
            return
        }
        //println "aca3"
        def company = received?.company
        //check company existance in remote Operational RDBMS database, there should be a local copy of all companies.
        //there should be a Company model for matching the received id to it and getting a company object, for now any id will do it
        if(!company){
            //println "aca4"
            response.setStatus(400)
            result = [status: 400, code: 55520,message: 'Invalid Device Registration Parameters']
            render result as JSON
            return
        }//error
        //println "aca5"
        def jsonData = companyDetailsRequest(company)
        /*def com = Company.findByGlobal_id(company as Long)
        if(!com){
            com = Company.findByUsername(jsonData?.company?.username?.trim()) //aca es el error, linea 77
        }
        if(!com){

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
                    invoiceMessage: jsonData.company.invoiceMessage,
                    tipMessage: jsonData.company.tipMessage,
                    categoy: category)

            if(!com.save()){
                println com.errors.allErrors
            }
        }else{
            //update values
            if(jsonData?.company){
                com.email =  jsonData?.company?.email
                com.address = jsonData?.company?.address
                com.contactName = jsonData?.company?.contactName
                com.global_id = jsonData?.company?.id
                //println "el id de la compania es "+jsonData?.company?.id
                com.logo = jsonData?.company?.logo
                com.name = jsonData?.company?.name
                com.nit = jsonData?.company?.nit
                com.url = jsonData?.company?.url
                com.telephone = jsonData?.company?.telephone
                com.invoiceMessage = jsonData?.company?.invoiceMessage
                com.tipMessage = jsonData?.company?.tipMessage
            }


            if(!com.save(flush:true))
                println com.errors.allErrors

        }
        def companyRole = SecRole.findByAuthority('ROLE_COMPANY') ?: new SecRole(authority: 'ROLE_COMPANY').save(failOnError: true)
        if (!com.authorities.contains(companyRole)) {
            SecUserSecRole.create com, companyRole
        }*/
        if(!jsonData){
            //println "aca6"
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Error fetching Data.']
            render result as JSON
            return
        }
        //println "aca7"
        if (jsonData.status != 1){
            //println "aca8"
            response.setStatus(400)
            result = [status: 400, code: 55521,message: 'Company does not exist.']
            render result as JSON
        }
        //println "aca9"
            //here we should also receive reported location via gps, wifi or any other location provider. public ip of device, os and many more useful data for trending and statistics
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject().append('uid',received?.device_id)
        def registered = db.device.findOne(query)
//        def registered = Device.findAllByUid(received?.device_id)
        if(registered?.size() > 0){  //there was a device with the same id previously registered
            //println "aca10"

                response.setStatus(200)
                result = [status: 200, code: 55511,message: 'Device is already registered for other company, login continues normally.',minSale: registered.minSale,maxSale: registered.maxSale as Integer, currentSale: registered.currentSale as Integer,prefix: registered.prefix as String,nit: jsonData.nit, tel: jsonData.telephone,address: jsonData.address, name: jsonData.name, email: jsonData.email,url: jsonData.url,invoiceMessage: jsonData.invoiceMessage,tipMessage: jsonData.tipMessage,resolution: jsonData.resolution] //here, the device should ask the user if this device should change company. and the call will the be made to changeDeviceRegistration
                render result as JSON
                return
        }
        //println "aca11"
        //else, save the id and register it to the current company making the request
        //check for stores

        /*def device = new Device(uid: received?.device_id, company: company,registeredOn: new Dateupdate(),minSale: 0, maxSale: 0,currentSale: 0,prefix: " ",resolution: " ")
        if(!device.save()){
            println device.errors.allErrors
            response.setStatus(400)
            result = [status: 400, code: 5550,message: 'Error writing object to file system'] //here, the device should ask the user if this device should change company. and the call will the be made to changeDeviceRegistration
            render result as JSON
            return
        }*/

        def device
        def store = checkStores(received)
        //println "aca12"
        def resultado = db.device.insert('uid':received.device_id, name: "Sin Nombre",'store': store, registeredOn: new Date(), lastUpdated: new Date(), minSale: 0, maxSale: 0, currentSale: 0, prefix: " ", resolution: " " )
        device = db.device.findOne("uid":received?.device_id,"store.company":received.company as Integer)
        if(!device){
            //println "aca13"
            response.setStatus(400)
            result = [status: 400, code: 5550,message: 'Error writing device to file system']
            render result as JSON
            return
        }
        ////println "aca14"
        response.setStatus(200)
        result = [status: 200, code: 555,message: 'Device successfully registered to company',minSale: 0,maxSale: 0, currentSale: 0,prefix: " ",nit: jsonData?.nit, tel: jsonData?.telephone,address: jsonData?.address, name: jsonData?.name, email: jsonData?.email,url: jsonData?.url,invoiceMessage: jsonData?.invoiceMessage,tipMessage: jsonData?.tipMessage,resolution :jsonData?.resolution]
        render result as JSON
        return

    }

    def fetchMaxSale(){
        def received = null
        try{received = JSON.parse(params?.payload)}catch (Exception e){}
        def result
        //println received
        if(!received){
            response.setStatus(400)
            result = [status: 400, code: 55520,message: 'Invalid Device Registration Parameters']
            render result as JSON
            return
        }


        //here we should also receive reported location via gps, wifi or any other location provider. public ip of device, os and many more useful data for trending and statistics
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject().append('uid',received?.device_id)
        def registered = db.device.findOne(query)
//        def registered = Device.findAllByUid(received?.device_id)
        if(registered?.size() == 0){  //there was a device with the same id previously registered
            response.setStatus(400)
            result = [status: 400, code: 55520,message: 'Device does not exist']
            render result as JSON
            return
        }
        //println registered.company
        def com = companyDetailsRequest(registered?.store?.company)
        if(!com){
            response.setStatus(400)
            result = [status: 400, code: 55525,message: 'company does not exist',maxSale: 0, currentSale: 0,prefix: 0,nit: "00000", tel: "00000",address: "NA", name: "NA", email: "NA",url: "NA",invoiceMessage: "NA",tipMessage:"NA"]
            render result as JSON
            return
        }
        response.setStatus(200)
        result = [status: 200, code: 555,message: 'Device exists, find maxSale, currentSale and prefix values attached as payload',maxSale: registered?.maxSale as Integer, currentSale: registered?.currentSale as Integer,prefix: registered?.prefix as String,nit: com.nit, tel: com.telephone,address: com.address, name: com.name, email: com.email,url: com.url,invoiceMessage: com.invoiceMessage,tipMessage: com.tipMessage]
        render result as JSON
        return

    }
    def fetchDevices(){

        def result
        if(!params?.company){
            response.setStatus(400)
            result = [status: 400, code: 55520,message: 'Invalid company']
            render result as JSON
            return
        }

        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("store.company",params?.company as Integer);
        def filas = db.device.find(query)
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

        }//
        render result as JSON
        return



        response.setStatus(200)
        result = [status: 200, code: 555,message: 'Device exists, find maxSale, currentSale and prefix values attached as payload',maxSale: registered.maxSale, currentSale: registered.currentSale,prefix: registered.prefix]
        render result as JSON
        return

    }

    def fetchDevice(){
        //println params
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
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("store.company",company as Integer).
                append("uid", row as String);
        //println query
        def filas = db.device.find(query)
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

    def updateDevice(){
        //println params
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
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("store.company",company as Integer).
                append("uid", row as String);
        BasicDBObject newfields = new BasicDBObject("name":params?.name)
                .append("currentSale",params?.currentSale)
                .append("maxSale",params?.maxSale)
                .append("prefix",params?.prefix)
                .append("resolution",params?.resolution)
        BasicDBObject setObject = new BasicDBObject('$set' : newfields)
        //println query
        //println setObject
        def filas = db.device.update(query,setObject)
        result = [status: 200, code: 555,message: 'Success.']
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
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
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
        //println device
    }

    def tryNewField(){
        def device = Device.collection.findOne(uid: "0A01B18B436A002555AF") //Low Level API call
        render device.toString()          //returns JSON string
        return

    }

    def companyDetailsRequest(cid){
        //check nest5 server sif company exists, has permission and other stuff
        def http = new HTTPBuilder( grailsApplication.config.com.nest5.BusinessData.Nest5APIServerURL )
// perform a GET request, expecting JSON response data
        http.request( GET, TEXT ) {
            uri.path = '/api/companyDetails'
            uri.query = [company_id:cid]
            headers.'User-Agent' = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'
            response.success = { resp, json ->
                return (JSON.parse(json))
            }
            response.failure = { resp ->
                println "Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}"
//                response.setStatus(400)
//                result = [status: 400, code: 55522,message: 'Error fetching Data']
//                render result as JSON
                return null
            }
        }
    }

    def checkStores(received){
        //println"llega a checkstores"
        def company = received.company
        DBCursor stores
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        try{
            BasicDBObject query = new BasicDBObject().append('company',company as Integer)
            stores = db.store.find(query)
        }catch(Exception e){
            e.printStackTrace()
            return null
        }
        //println"stores encontradas:"
        //printlnstores.count()
        if(stores.count() == 0){
            //println"no hay tiendas, crea una nueva"
            def obid = new ObjectId()
            def resultado = db.store.insert(_id: obid,name: "Local Por Defecto", latitude: 0, longitude: 0, location: null, company: company as Integer)
            if(resultado.error)
            {
                //printlnresultado.getLastError()
                return null
            }
            //println"busca en db una tienda con el id que se acaba de crear "+obid
            def store = db.store.findOne('_id':obid)
            //println"dentro de checkStores encontró una vez hecha comprobación y demás esta tienda que devolverá"
            //printlnstore
            return store
        }
        else{
            def store = stores.next()
            //println"existen tiendas entopnces devuleve la primera de la lista"+store
            return store
        }


    }


}
