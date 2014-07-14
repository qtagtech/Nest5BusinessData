package com.nest5data

import com.mongodb.BasicDBObject
import org.bson.types.ObjectId

class TransitionController {
    def mongo


    /*
    *
    * PUT ACTIONS TO CONVERT OLD DATAROWS WITH COMPANY IDS INTO NEW FORMAT WITH STORE ID'S
    *
    * */

    def index() {}

    def transformAllDataRows(){
            def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        //get disstinct companies
        def companies = db.dataRow.distinct('device.company')
        companies.each{
            render  "<h1>Convirtiendo Para Empresa: "+it +"</h1><br>"
            render  "<h2>-Empieza Devices</h2><br>"
            def store = db.store.findOne('company':it as Integer)        //¿Cualquier store de la empresa?
            if(store)
                render "--Tiene Tienda con id: "+store._id+"<br>"
            if(!store){
                def obid = new ObjectId()
                def resultado = db.store.insert(_id: obid,name: "Local Por Defecto", latitude: 0, longitude: 0, location: null, company: it as Integer)
                store = db.store.findOne('_id':obid)
            }
            if(!store)
                render "<h3>--Error--</h3> No se pudo crear tienda por defecto para empresa: "+it+"<br>"
            else
                render "--Tienda nueva por defecto creada con id: "+store._id+"<br>"
            render "----Comenzando conversión...<br>"
            BasicDBObject set = new BasicDBObject().append('$set',new BasicDBObject().append('store',store))
            set.append('$unset',new BasicDBObject().append('company',''))
            BasicDBObject query = new BasicDBObject().append("company",it)
            db.device.update(query,set,false,true)
            render "-----¡Devices convertidos con éxito!<br>"
            render  "<h2>-Empieza DataRow</h2><br>"
            BasicDBObject set2 = new BasicDBObject().append('$set',new BasicDBObject().append("device.store",store))
            set2.append('$unset',new BasicDBObject().append("device.company",''))
            BasicDBObject query2 = new BasicDBObject().append("device.company",it)
            db.dataRow.update(query2,set2,false,true)
            render "-----¡DataRows convertidos con éxito!<br>"

        }
        render "¡FIN!"

        }

    def convertToCompany(){
        def from = params.from
        def to = params.to

        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
            render "-Comenzando conversión...<br>"
            BasicDBObject set = new BasicDBObject().append('$set',new BasicDBObject().append('company',to))
            BasicDBObject query = new BasicDBObject().append("company",from)
            db.device.update(query,set,false,true)
            render "-----¡todo convertido con éxito!<br>"
        render "¡FIN!"

    }

}
