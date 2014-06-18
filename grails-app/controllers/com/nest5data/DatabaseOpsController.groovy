package com.nest5data

import com.mongodb.BasicDBObject
import com.mongodb.DBCursor
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.io.FileUtils
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.format.datetime.joda.DateTimeFormatterFactory

import java.awt.Cursor
import java.text.SimpleDateFormat

@Secured(["permitAll"])
class DatabaseOpsController {

    def mongo

    def index() {
        def result = [message: "bad query request"]
        render result as JSON
        response.setStatus(400)
        return
    }

    def importDatabase(){
        def result
        //println params.payload
        if(!params?.payload){
            response.setStatus(400)
            result = [status: 400, code: 55510,message: 'Inavlid request structure',payload: null]
            render result as JSON
            return
        }
        def received = JSON.parse(params?.payload)

        if(!received){
            //return error, and not received
            response.setStatus(400)
            result = [status: 400, code: 55521,message: 'Invalid parameters or no parameters at all all',payload: null]
            render result as JSON
            return
        }
        def company = received.company
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }

        //get tax that means no tax, that's 0%, and if it doesn't exist, create it.
    //hacer JSON.parse("{'name': 'Sin Impuesto','percentage': 0.0}") borrar el que ya hay en db
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        def notax = null
        def device = db.device.findOne('store.company':company)        //¿Cualquier Device?
        try{
            BasicDBObject query = new BasicDBObject("table",'tax').
                    append("device.store.company",company).
                    append("isDeleted", false)
                    .append("fields.percentage", 0);
             def resultados = db.dataRow.find(query)
            if(!resultados?.hasNext()){
                def fields = JSON.parse('{"name":"Sin Impuesto","percentage":0}')
                def hash = fields.encodeAsMD5()
                def sync_id = randomNumber()
                while(sync_id == 0L){
                    sync_id = randomNumber()
                }
                def resultado = db.dataRow.insert('table': 'tax', 'rowId': 0,'timeCreated': new Date(),'timeReceived': new Date(),fields: fields,'hashKey': hash,'device': device, 'isDeleted': false,'syncId': sync_id)
                if(resultado)
                    notax = sync_id
            }
            else{
                notax = resultados.next().syncId
            }

        }catch (Exception e){
            e.printStackTrace()
        }


        //


        //generate all tables
        def tables = '/*---------------------------------NEST5 BIG DATA SERVER-----------------------------------------------\n\n' +
                'THE INFORMATION CONTAINED IN THIS FILE IS PROPERTY OF \n' +
                'QTAG TECHNOLOGIES S.A.S\n' +
                'CALLE 129 #8 - 08\n' +
                'BOGOTA\n' +
                'COLOMBIA\n' +
                'TEL (COLOMBIA): +57 - 01800 700 19 91\n' +
                '(USA)           +1 - (786) 515 25 86\n' +
                'THE REPRODUCTION DISTRIBUTION OR UNAUTHORIZED USE OF ANY DATA CONTAINED IN THIS FILE MAY END IN LEGAL CONSEQUENCES\n' +
                'IF YOU DOWNLOADED THIS FILE BY MISTAKE PLEASE DELETE IMMEDIATELY.\n' +
                'OTHERWISE CONTACT OUR SUPPORT TEAM AT:\n' +
                'CONTACTO@QTAGTECH.COM,\n' +
                'SOPORTE@NEST5.COM\n' +
                'OR OUR MAIN LINE IN COLOMBIA OR THE UNITED STATES OF AMERICA.*/' +
                '\n\n\n\n' +
                'CREATE TABLE ingredient_category (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text not null,\n' +
                'sync_id          real\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE product_category (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text not null,\n' +
                'sync_id          real\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE ingredient (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text,\n' +
                'category_id     integer,\n' +
                'tax_id          integer,\n' +
                'unit_id\t\t    integer,\n' +
                'cost_per_unit   real,\n' +
                'price_per_unit    real,\n' +
                'price_measure    real,\n' +
                'quantity         real,\n' +
                'date             real,\n' +
                'sync_id          real,\n' +
                'FOREIGN KEY (category_id) REFERENCES ingredient_category (_id),\n' +
                'FOREIGN KEY (tax_id) REFERENCES tax (_id),\n' +
                'FOREIGN KEY (unit_id) REFERENCES measurement_unit (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE product (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text,\n' +
                'category_id     integer,\n' +
                'automatic_cost  integer,\n' +
                'cost \t\t\treal,\n' +
                'price\t\t\treal,\n' +
                'tax_id\t\t\t\tinteger,\n' +
                'sync_id          real,\n' +
                'FOREIGN KEY (category_id) REFERENCES product_category (_id),\n' +
                'FOREIGN KEY (tax_id) REFERENCES tax (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE combo (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text,\n' +
                'automatic_cost  integer,\n' +
                'cost \t\t\treal,\n' +
                'price\t\t\treal,\n' +
                'tax_id\t\t\t\tinteger,\n' +
                'sync_id          real,\n' +
                'FOREIGN KEY (tax_id) REFERENCES tax (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE productingredient (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'product_id\t\tinteger,\n' +
                'ingredient_id\tinteger,\n' +
                'qty             real,\n' +
                'FOREIGN KEY (product_id) REFERENCES product (_id),\n' +
                'FOREIGN KEY (ingredient_id) REFERENCES ingredient (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE comboingredient (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'combo_id\t\tinteger,\n' +
                'ingredient_id\tinteger,\n' +
                'qty             real,\n' +
                'FOREIGN KEY (combo_id) REFERENCES combo (_id),\n' +
                'FOREIGN KEY (ingredient_id) REFERENCES ingredient (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE comboproduct (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'combo_id\t\tinteger,\n' +
                'product_id\tinteger,\n' +
                'qty             real,\n' +
                'FOREIGN KEY (combo_id) REFERENCES combo (_id),\n' +
                'FOREIGN KEY (product_id) REFERENCES product (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE tax (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text,\n' +
                'percentage     real,\n' +
                'sync_id          real\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE measurement_unit (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text,\n' +
                'initials        text not null,\n' +
                'multipliers     text,\n' +
                'sync_id          real\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE sale (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'date            integer,\n' +
                'received        real,\n' +
                'payment_method  text,\n' +
                'delivery          integer,\n' +
                'togo          integer,\n' +
                'discount          real,\n' +
                'tip          integer,\n' +
                'sale_number          integer,\n' +
                'sync_id          real\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE sale_item (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'sale_id \t\tinteger,\n' +
                'sale_item_id\t\t\t integer,\n' +
                'sale_item_qty   real,\n' +
                'sale_item_type  integer,\n' +
                'FOREIGN KEY (sale_id) REFERENCES sale (_id)\n' +
                ');\n'+
                '\n' +
                'CREATE TABLE sync_row (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'device_id \t\ttext,\n' +
                'reference \t\ttext,\n' +
                'row_id\t\t\t real,\n' +
                'time_created   real,\n' +
                'sync_id  real,\n' +
                'fields  text\n' +
                ');\n';





        def table_list = ["ingredient_category","product_category","tax","measurement_unit","ingredient","product","combo"/*,"sale"*/,"sync_row"]  //the order matters since for inserting an ingredient, tax, measuremet_unit and categories must be present in the database
        def str = new StringBuilder()
        str.append("\n")
        def sales = []
        def products = []
        def combos = []
        /*
            *
            * **************************************************************************INIT TEMP!!!!
            ********************BRING ALL OLD FORMATTED DATAROWS, WITH COMPANY ID'S CONVERTED TO STORES, THE FIRST STORE IN THE SYSTEM FOR THE COMPANY OR A NEW CREATED ONE IF NONE EXIST
            *
            *
            * */

        //poner una función que traiga todas las datarow con el campo company = 16 o cualquiera que sea, removido y con el primer store que se aparezca $set.

            try{
                transformDataRows(company)
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
        table_list.each {      //nunca llamara que traiga tablas productingredient, comboingredient, compboproduct ni sale_item, solo llama product, combo, sale y con eso al final agrega las relaciones mucho a mucho
            BasicDBObject query = new BasicDBObject("table",it).
                    append("device.store.company",company).
                    append("isDeleted", false);

            def filas = db.dataRow.find(query)
            def currentTable = it
            try {
                if((it == "ingredient_category") || (it == "product_category") || (it == "tax") || (it == "measurement_unit") || (it == "sync_row") /*|| (it == "sale")*/) {
                    while(filas.hasNext()) {
                        //println it
                        def actual = filas.next()
                        def values = "null,"
                        def keys= "_id,"
                        def agregar = false
                        //println actual.fields
                        actual.fields.sort{it.getKey()}.each{
                            if((it.getKey() != "_id") && (it.getKey() != "ingredients")&& (it.getKey() != "products")&& (it.getKey() != "combos")){
                                if(it.getValue() != null & it.getValue() != "") {
                                    agregar = true
                                    keys += it.getKey()+","
                                    if(it.getKey() == "multipliers"){

                                        if(it.getValue() == [:]){
                                            //no puso submedidas, y al menos debe tener la basica de la misma igual a 1, coge iniciales
                                            def iniciales = (((actual.fields.name).toString()).replaceAll("//s","")).substring(0,2)
                                            def vec = [:]
                                            vec[iniciales] = 1
                                            def real = vec as JSON
                                            values += "'"+real+"',"
                                        }
                                        else{
                                            values +="'"+(it.getValue()).toString()+"',"
                                        }
                                    }
                                    else{

                                        if( isNumeric(it.getValue() as String)){
                                            if((it.getValue() != Double.NaN) && (it.getValue()!= "NaN"))
                                                values +=it.getValue()+","
                                            else
                                                values += "0,"
                                        }

                                        else
                                            values +="'"+it.getValue()+"',"

                                    }

                                    /*if(actual.fields.next?.getValue() != null)
                                        values+=","*/
                                }
                                else{ //en el caso que haya error con las medidas pasaria aca porque no se ponen initials y el valor seria "" en vez de initials, asi que revisamos si es measurament y lo forzamos
                                   if (it.getKey() == "initials"){
                                       keys += it.getKey()+","
                                       def iniciales = (((actual.fields.name).toString()).replaceAll("//s","")).substring(0,2)
                                       values += "'"+iniciales+"',"
                                   }
                                }
                            }

                        }

                        if (agregar){
                            keys += "sync_id"
                            values += actual.syncId
                            str.append (" \nINSERT INTO ")
                            str.append (currentTable)
                            str.append ("\n (")
                            str.append(keys)
                            str.append(")\n")
                            str.append (" VALUES (")
                            str.append ("\n")
                            str.append (values)
                            str.append(");")
                            str.append("\n")
                            // println str.toString()
                        }

                         if(it == "sale"){
                             sales.push([sync_id: actual.syncId,ingredients: actual.fields.ingredients,products: actual.fields.products,combos:actual.fields.combos])
                         }

                    }
                }
                else{
                    /*   estructura de query para ingredientes que depende de tax, measurement_unit y ingredient_category. Se hace un insert seleccionando los valores con los sync_id de esas tres tablas, luego
                 *los valores fijos se meten con un update diciendole que lo haga sobre el ultimo row actualizado en esa tabla
                 *
                    INSERT into ingredient (category_id, tax_id, unit_id)***
                    "SELECT ingredient_category._id as category_id, tax._id as tax_id,
                    measurement_unit._id as unit_id
                    FROM ingredient_category, tax, measurement_unit
                    WHERE
                    ingredient_category.sync_id = 7331970326568846
                    and
                    tax.sync_id = 4679973691934272
                    and
                    measurement_unit.sync_id = 9408808377253652;"
                    update ingredient set name='primer ingrediente',cost_per_unit=10.0,price_per_unit=69.44444444,quantity=3000.0,price_measure=20.0,sync_id=9624909824776420 where _id=last_insert_rowid();

                 * */

                    while(filas.hasNext()) {
                        //println it
                        def actual = filas.next()
                        def keys= "_id,"
                        def values = "\n\n"
                        if(it != 'combo' && it != 'product'){  //si es ingredient
                            def taxidval = actual.fields.tax_id != 0 ? actual.fields.tax_id : notax
                            values += "INSERT into "+it+" (category_id, tax_id, unit_id)\n"
                            values += "SELECT ingredient_category._id as category_id, tax._id as tax_id, "+
                                        "measurement_unit._id as unit_id\n"+
                                        "FROM ingredient_category, tax, measurement_unit\n"+
                                        "WHERE\n"+
                                        "ingredient_category.sync_id = "+actual.fields.category_id+"\n"+
                                        "AND\n"+
                                        "tax.sync_id = "+taxidval+"\n"+
                                        "AND\n"+
                                        "measurement_unit.sync_id = "+actual.fields.unit_id+";\n"
                        }
                        else{
                            if( it != 'combo') {//product
                                def taxidval = actual.fields.tax_id != 0 ? actual.fields.tax_id : notax
                                values += "INSERT into "+it+" (category_id, tax_id)\n"
                                values += "SELECT product_category._id as category_id, tax._id as tax_id "+
                                        "FROM product_category, tax\n"+
                                        "WHERE\n"+
                                        "product_category.sync_id = "+actual.fields.category_id+"\n"+
                                        "AND\n"+
                                        "tax.sync_id = "+taxidval+";\n"
                            }else{ //combo
                                def taxidval = actual.fields.tax_id != 0 ? actual.fields.tax_id : notax
                                values += "INSERT into "+it+" (tax_id)"
                                values += "SELECT tax._id as tax_id \n"+
                                        "FROM tax\n"+
                                        "WHERE\n"+
                                        "tax.sync_id = "+taxidval+";\n"
                            }

                        }
                        str.append(values);
                        str.append("\n")
                        //update ingredient set name='primer ingrediente',cost_per_unit=10.0,price_per_unit=69.44444444,quantity=3000.0,price_measure=20.0,sync_id=9624909824776420 where _id=last_insert_rowid();
                        str.append("update "+it+" set \n");
                        def i = 0
                        actual.fields.sort{it.getKey()}.each{
                            if((it.getKey() != "_id") && (it.getKey() != "category_id") && (it.getKey() != "unit_id") && (it.getKey() != "tax_id") && (it.getKey() != 'ingredients')&& (it.getKey() != 'products')&& (it.getKey() != 'combos')){
                                if(it.getValue() != null & it.getValue() != "") {
                                    if(i != 0 )
                                        str.append(",\n")
                                    def val
                                    if( isNumeric(it.getValue() as String)){
                                        if((it.getValue() != Double.NaN) && (it.getValue()!= "NaN"))
                                            val = it.getValue()
                                        else
                                            val = 0
                                    }

                                    else
                                        val ="'"+it.getValue()+"'"
                                    str.append(it.getKey()+" = "+val)

                                }
                                i++
                            }

                        }
                        str.append(", sync_id = "+actual.syncId)
                        str.append("\n WHERE _id = last_insert_rowid();\n\n")
                        if(it == "combo"){

                                combos.push([sync_id: actual.syncId,ingredients: actual.fields.ingredients,products: actual.fields.products])


                        }else{
                            if(it == "product"){

                                    products.push([sync_id: actual.syncId,ingredients: actual.fields.ingredients])

                            }
                        }

                    }
                }


            } finally {
                filas.close();
            }

        }
        //println "sales: "
//        println sales
//        println "products: "
//        println products
//        println "combos: "
//        println combos
        def TYPE_INGREDIENT = 0;
        def TYPE_PRODUCT = 1;
        def TYPE_COMBO = 3;
        def TYPE_CUSTOM = 2;
        /*sales.each {
            //println it.ingredients
            def elementId = it.sync_id
            def values = "\n"
            it.ingredients.each{
                  if(it != [] && it != [:] ){
                      values += "INSERT into sale_item (sale_id,sale_item_id)\n"
                      values += "SELECT sale._id as sale_id, ingredient._id as sale_item_id\n"+
                              "FROM sale,ingredient\n"+
                              "WHERE\n"+
                              "sale.sync_id = "+elementId+"\n"+
                                "AND\n"+
                              "ingredient.sync_id = "+it.sync_id+";\n"
                      values += "UPDATE sale_item set sale_item_qty = "+it.quantity+",sale_item_type = "+TYPE_INGREDIENT+" WHERE _id = last_insert_rowid();\n"
                  }

            }
            it.products.each{
                if(it != [] && it != [:] ){
                    values += "INSERT into sale_item (sale_id,sale_item_id)\n"
                    values += "SELECT sale._id as sale_id, product._id as sale_item_id\n"+
                            "FROM sale,product\n"+
                            "WHERE\n"+
                            "sale.sync_id = "+elementId+"\n"+
                            "AND\n"+
                            "product.sync_id = "+it.sync_id+";\n"
                    values += "UPDATE sale_item set sale_item_qty = "+it.quantity+",sale_item_type = "+TYPE_PRODUCT+" WHERE _id = last_insert_rowid();\n"
                }

            }
            it.combos.each{
                if(it != [] && it != [:] ){
                    values += "INSERT into sale_item (sale_id,sale_item_id)\n"
                    values += "SELECT sale._id as sale_id, combo._id as sale_item_id\n"+
                            "FROM sale,combo\n"+
                            "WHERE\n"+
                            "sale.sync_id = "+elementId+"\n"+
                            "AND\n"+
                            "combo.sync_id = "+it.sync_id+";\n"
                    values += "UPDATE sale_item set sale_item_qty = "+it.quantity+",sale_item_type = "+TYPE_COMBO+" WHERE _id = last_insert_rowid();\n"
                }

            }
            str.append(values)
        }*/

        /*combos.each {
            //println it.ingredients
            def elementId = it.sync_id
            def values = "\n"
            it.ingredients.each{
                if(it != [] && it != [:] ){
                    values += "INSERT into comboingredient (combo_id,ingredient_id)\n"
                    values += "SELECT combo._id as combo_id, ingredient._id as ingredient_id\n"+
                            "FROM combo,ingredient\n"+
                            "WHERE\n"+
                            "combo.sync_id = "+elementId+"\n"+
                            "AND\n"+
                            "ingredient.sync_id = "+it.sync_id+";\n"
                    values += "UPDATE comboingredient set qty = "+it.qty+" WHERE _id = last_insert_rowid();\n"
                }

            }
            it.products.each{
                if(it != [] && it != [:] ){
                    values += "INSERT into comboproduct (combo_id,product_id)\n"
                    values += "SELECT combo._id as combo_id, product._id as product_id\n"+
                            "FROM combo,product\n"+
                            "WHERE\n"+
                            "combo.sync_id = "+elementId+"\n"+
                            "AND\n"+
                            "product.sync_id = "+it.sync_id+";\n"
                    values += "UPDATE comboproduct set qty = "+it.qty+" WHERE _id = last_insert_rowid();\n"
                }

            }
            str.append(values)

        }*/
       /* products.each {
            //println it.ingredients
            def elementId = it.sync_id
            def values = "\n"
            it.ingredients.each{
                if(it != [] && it != [:] ){
                    values += "INSERT into productingredient (product_id,ingredient_id)\n"
                    values += "SELECT product._id as product_id, ingredient._id as ingredient_id\n"+
                            "FROM product,ingredient\n"+
                            "WHERE\n"+
                            "product.sync_id = "+elementId+"\n"+
                            "AND\n"+
                            "ingredient.sync_id = "+it.sync_id+";\n"
                    values += "UPDATE productingredient set qty = "+it.qty+" WHERE _id = last_insert_rowid();\n"
                }

            }
            str.append(values)

        }*/
        def directory = System.getProperty("java.io.tmpdir")
        def today = new Date()
        def fileStore = new File(directory+"dbExport_${company}_${today[Calendar.DATE]}_${today[Calendar.MONTH]+1}_${today[Calendar.YEAR]}_${today[Calendar.HOUR_OF_DAY]}_${today[Calendar.MINUTE]}_${today[Calendar.SECOND]}_${today[Calendar.MILLISECOND]}.sql");
        fileStore.createNewFile();
        FileUtils.writeStringToFile(fileStore, tables,'UTF-8');
        FileUtils.writeStringToFile(fileStore, str.toString(),'UTF-8',true);
//        println fileStore.name
        response.setHeader "Content-disposition", "attachment; filename="+fileStore.name.lastIndexOf('.').with {it != -1 ? fileStore.name[0..<it] : fileStore.name}+".sql"
        response.contentType = 'application/octet-stream'
        response.outputStream << fileStore.text
        response.outputStream.flush()


    }

    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    /*
    * CALLS MADE FROM ANY PLATFORM REQUIRING INFO OM SALES, PRODUCTS, COMBOS, INGREDIENTS, CATEGORIES ETC
    *
    * */

    def allSales(){
        println params
        def result



        def company = params?.company
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("table",'sale').
                append("device.store.company",company as Integer).
                append("isDeleted", false);
        println query
        def filas = db.dataRow.find(query)
        if(filas.size() == 0) {
            response.setStatus(200)
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


    def zReport(){
        def result
        def company = params?.company
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }
        def dates = magicDates(params)
        println dates
        def startDate = dates.startDate
        def endDate = dates.endDate
        def doomdate = dates.doomdate
         /***********************************************
        *
        *
        *
        *
        *
        *
        ************************************* */
         def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("table",'sale').
                append("device.store.company",company as Integer).
                append("isDeleted", false)
                .append("timeCreated",new BasicDBObject('$gte', startDate).append('$lt', endDate));
        def filas
        try{
            filas = db.dataRow.find(query)
        }catch(Exception e){
            println ("Error cogiendo filas de mongo")
            e.printStackTrace()
        }
        if(filas == null){
            response.setStatus(200)
            result = [status: 416, code: 55531,message: 'Empty Result Set, null rows',payload: null]
            render result as JSON
            return
        }
        if(filas.size() == 0) {
            response.setStatus(200)
            result = [status: 416, code: 55531,message: 'Empty Result Set',payload: null]
            render result as JSON
            return
        }
        result = [status: 200, code: 555,message: 'Success. See payload.',payload: []]
        def totalVentas = 0
        def totalDescuentos = 0
        def totalImpuestos = 0
        def totalPropinas = 0
        def sumDomicilios = 0
        def sumLlevar = 0
        def sumTarjeta = 0
        def sumEfectivo = 0
        def contDomicilio = 0
        def contEfectivo = 0
        def contTarjeta = 0
        def contLlevar = 0
        def consecutivos = []
       // println filas
        while(filas.hasNext()) {

                def element
                if(filas){
                    element = filas.next() ?: null
                }


            //println  element
            if(element){
                def elementTotal = 0
                def elementImptotal = 0
                consecutivos.push(element?.fields?.sale_number)
                element?.fields?.ingredients?.each{
                    BasicDBObject q = new BasicDBObject("table",'ingredient').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def ingredientCursor
                    try{
                        ingredientCursor = db.dataRow.find(q)
                    }catch(Exception e){
                        println "error buscando ingrediente con syncID: "+it.sync_id
                        e.printStackTrace()
                    }
                    def ingredient = null
                    if(ingredientCursor){
                        ingredient = ingredientCursor.next()
                        ingredientCursor.close()
                    }

                    def tax = null
                    if(ingredient){
                        BasicDBObject q2 = new BasicDBObject("table",'tax').
                                append("syncId",ingredient.fields?.tax_id).
                                append("isDeleted", false);
                        def taxCursor
                        try{
                            taxCursor = db.dataRow.find(q2)
                        }catch(Exception e){
                            println "error buscando tax con syncID: "+ingredient.fields?.tax_id
                            e.printStackTrace()
                        }
                        if(taxCursor){
                            tax = taxCursor.next()
                            taxCursor.close()
                        }

                    }
                    if(ingredient && tax){
                        elementTotal += ingredient?.fields?.price_per_unit * (it.quantity)
                        totalImpuestos += (ingredient?.fields?.price_per_unit * (it.quantity)) * tax?.fields?.percentage
                    }


                }
                element?.fields?.products?.each{
                    BasicDBObject q = new BasicDBObject("table",'product').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def productCursor
                    try{
                        productCursor = db.dataRow.find(q)
                    }catch(Exception e){
                        println "error buscando producto con syncID: "+it.sync_id
                        e.printStackTrace()
                    }
                    def product = null
                    if(productCursor){
                        product = productCursor.next()
                        productCursor.close()
                    }
                    def tax = null
                    if(product){
                        BasicDBObject q2 = new BasicDBObject("table",'tax').
                                append("syncId",product.fields?.tax_id).
                                append("isDeleted", false);
                        def taxCursor
                        try{
                            taxCursor = db.dataRow.find(q2)
                        }catch(Exception e){
                            println "error buscando tax con syncID: "+product.fields?.tax_id
                            e.printStackTrace()
                        }

                        if(taxCursor){
                            tax = taxCursor.next()
                            taxCursor.close()
                        }

                    }
                    if(product && tax){
                    elementTotal += product?.fields?.price * it.quantity
                    totalImpuestos += (product?.fields?.price * it.quantity) * tax?.fields?.percentage
                    }
                }
                element?.fields?.combos?.each{
                    BasicDBObject q = new BasicDBObject("table",'combo').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def comboCursor
                    try{
                        comboCursor = db.dataRow.find(q)
                    }catch(Exception e){
                        println "error buscando combo con syncID: "+it.sync_id
                        e.printStackTrace()
                    }
                    def combo = null
                    if(comboCursor){
                        combo = comboCursor.next()
                        comboCursor.close()
                    }
                    def tax = null
                    if(combo){
                        BasicDBObject q2 = new BasicDBObject("table",'tax').
                                append("syncId",combo?.fields?.tax_id).
                                append("isDeleted", false);
                        def taxCursor
                        try{
                            taxCursor = db.dataRow.find(q2)
                        }catch(Exception e){
                            println "error buscando tax con syncID: "+combo.fields?.tax_id
                            e.printStackTrace()
                        }
                        if(taxCursor){
                            tax = taxCursor.next()
                            taxCursor.close()
                        }

                    }
                    if(combo && tax){
                        elementTotal += combo?.fields?.price * (it.quantity)
                        totalImpuestos += (combo?.fields?.price * (it.quantity)) * tax?.fields?.percentage
                    }
                }
                //once done, all combos, products and ngredients times quantties have been added, plus taxes, now get tip percentage and discounts
                totalVentas += elementTotal
                totalDescuentos += elementTotal * (element?.fields?.discount / 100) //fix this for any sale saved before 11/04/2014 (March the 11th 2014)
                if((element?.timeReceived <= doomdate) && element?.fields?.received <= 100){ //it means is a sale saved with the discount in the received field
                  totalDescuentos += elementTotal * (element?.fields?.received / 100)
                }
                totalImpuestos += elementImptotal
                if(element?.fields?.tip == 1)
                    totalPropinas += elementTotal * 0.1
                if(element?.fields?.togo == 1){
                    sumLlevar += elementTotal
                    contLlevar ++
                }
                if(element?.fields?.delivery == 1){
                    sumDomicilios += elementTotal
                    contDomicilio ++
                }
                if(element?.fields?.payment_method == "card"){
                    sumTarjeta += elementTotal
                    contTarjeta ++
                }else{
                    sumEfectivo += elementTotal
                    contEfectivo ++
                }


            }

        }
        if(filas)
            filas.close()
        result.pay = [
        ventas : totalVentas,
        descuentos : totalDescuentos,
        impuestos : totalImpuestos,
        propinas : totalPropinas,
        domicilios : sumDomicilios,
        llevar : sumLlevar,
        tarjeta : sumTarjeta,
        efectivo : sumEfectivo,
        contEfectivo : contEfectivo,
        contTarjeta : contTarjeta,
        contDomicilio :  contDomicilio,
        contLlevar : contLlevar,
        consecutivos: consecutivos.sort()
        ]
        render result as JSON
        return
    }

    def saleDetails(){
        def result
        def company = params?.company
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }
        def dates = magicDates(params)
        def startDate = dates.startDate
        def endDate = dates.endDate
        def doomdate = dates.doomdate
        def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("table",'sale').
                append("device.store.company",company as Integer).
                append("isDeleted", false)
                .append("timeCreated",new BasicDBObject('$gte', startDate).append('$lt', endDate));
        def filas
        try{
            filas = db.dataRow.find(query)
        }catch(Exception e){
            println ("Error cogiendo filas de mongo")
            e.printStackTrace()
        }
        if(filas == null){
            response.setStatus(200)
            result = [status: 416, code: 55531,message: 'Empty Result Set, null rows',payload: null]
            render result as JSON
            return
        }
        if(filas.size() == 0) {
            response.setStatus(200)
            result = [status: 416, code: 55531,message: 'Empty Result Set',payload: null]
            render result as JSON
            return
        }
        result = [status: 200, code: 555,message: 'Success. See payload.',payload : null]

        def facturas = [:]
        while(filas.hasNext()) {
            def element = filas.next() ?: null
            //println  element
            if(element){
                def items = []
                def elementTotal = 0
                def elementImptotal = 0
                def totalDescuentos = 0
                def propina = 0
                def consecutivo = element?.fields?.sale_number
                element?.fields?.ingredients?.each{
                    BasicDBObject q = new BasicDBObject("table",'ingredient').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def ingredientCursor
                    try{
                        ingredientCursor = db.dataRow.find(q)
                    }catch(Exception e){
                        println "error buscando ingrediente con syncID: "+it.sync_id
                        e.printStackTrace()
                    }
                    def ingredient = null
                    if(ingredientCursor){
                        ingredient = ingredientCursor.next()
                        ingredientCursor.close()
                    }

                    def tax = null
                    if(ingredient){
                        BasicDBObject q2 = new BasicDBObject("table",'tax').
                                append("syncId",ingredient.fields?.tax_id).
                                append("isDeleted", false);
                        def taxCursor
                        try{
                            taxCursor = db.dataRow.find(q2)
                        }catch(Exception e){
                            println "error buscando tax con syncID: "+ingredient.fields?.tax_id
                            e.printStackTrace()
                        }
                        if(taxCursor){
                            tax = taxCursor.next()
                            taxCursor.close()
                        }

                    }
                    if(ingredient && tax){
                        def item = [item : ingredient?.fields?.name,precio:ingredient?.fields?.price_per_unit, impuesto: tax?.fields?.percentage ,cantidad: it.quantity ]
                        items.push(item)
                        elementTotal += ingredient?.fields?.price_per_unit * (it.quantity)
                        elementImptotal += (ingredient?.fields?.price_per_unit * (it.quantity)) * tax?.fields?.percentage
                    }


                }
                element?.fields?.products?.each{
                    BasicDBObject q = new BasicDBObject("table",'product').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def productCursor
                    try{
                        productCursor = db.dataRow.find(q)
                    }catch(Exception e){
                        println "error buscando producto con syncID: "+it.sync_id
                        e.printStackTrace()
                    }
                    def product = null
                    if(productCursor){
                        product = productCursor.next()
                        productCursor.close()
                    }
                    def tax = null
                    if(product){
                        BasicDBObject q2 = new BasicDBObject("table",'tax').
                                append("syncId",product.fields?.tax_id).
                                append("isDeleted", false);
                        def taxCursor
                        try{
                            taxCursor = db.dataRow.find(q2)
                        }catch(Exception e){
                            println "error buscando tax con syncID: "+product.fields?.tax_id
                            e.printStackTrace()
                        }

                        if(taxCursor){
                            tax = taxCursor.next()
                            taxCursor.close()
                        }

                    }
                    if(product && tax){
                        def item = [item : product?.fields?.name,precio:product?.fields?.price, impuesto: tax?.fields?.percentage ,cantidad: it.quantity ]
                        items.push(item)
                        elementTotal += product?.fields?.price * (it.quantity)
                        elementImptotal += (product?.fields?.price * (it.quantity)) * tax?.fields?.percentage
                    }

                }
                element?.fields?.combos?.each{
                    BasicDBObject q = new BasicDBObject("table",'combo').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def comboCursor
                    try{
                        comboCursor = db.dataRow.find(q)
                    }catch(Exception e){
                        println "error buscando combo con syncID: "+it.sync_id
                        e.printStackTrace()
                    }
                    def combo = null
                    if(comboCursor){
                        combo = comboCursor.next()
                        comboCursor.close()
                    }
                    def tax = null
                    if(combo){
                        BasicDBObject q2 = new BasicDBObject("table",'tax').
                                append("syncId",combo?.fields?.tax_id).
                                append("isDeleted", false);
                        def taxCursor
                        try{
                            taxCursor = db.dataRow.find(q2)
                        }catch(Exception e){
                            println "error buscando tax con syncID: "+combo.fields?.tax_id
                            e.printStackTrace()
                        }
                        if(taxCursor){
                            tax = taxCursor.next()
                            taxCursor.close()
                        }

                    }
                    if(combo && tax){
                        def item = [item : combo?.fields?.name,precio:combo?.fields?.price, impuesto: tax?.fields?.percentage ,cantidad: it.quantity ]
                        items.push(item)
                        elementTotal += combo?.fields?.price * (it.quantity)
                        elementImptotal += (combo?.fields?.price * (it.quantity)) * tax?.fields?.percentage
                    }

                }
                totalDescuentos += elementTotal * (element?.fields?.discount / 100) //fix this for any sale saved before 11/04/2014 (March the 11th 2014)
                if((element?.timeCreated <= doomdate) && element?.fields?.received <= 100){ //it means is a sale saved with the discount in the received field
                    totalDescuentos += elementTotal * (element?.fields?.received / 100)
                }
                if(element?.fields?.tip == 1)
                    propina += elementTotal * 0.1
                facturas[consecutivo] = [items:items, venta : elementTotal, impuestos: elementImptotal,descuentos: totalDescuentos, propinas: propina]

            }

        }
        if(filas)
            filas.close()

        result.payload = facturas
        render result as JSON
        return
    }

    def fetchInvoice(){
        def result
        def company = params?.company
        def numb = params?.invoice
        if(!company){
            response.setStatus(400)
            result = [status: 400, code: 55522,message: 'Invalid company',payload: null]
            render result as JSON
            return
        }
        def dates = magicDates(params)
        def startDate = dates.startDate
        def endDate = dates.endDate
        def doomdate = dates.doomdate
         def db = mongo.getDB(grailsApplication.config.com.nest5.BusinessData.database)
        BasicDBObject query = new BasicDBObject("table",'sale').
                append("device.store.company",company as Integer).
                append("isDeleted", false)
                .append("fields.sale_number",numb as Integer);
        println query;
        def filas = db.dataRow.find(query)
        //println filas
        if(filas.size() == 0) {
            response.setStatus(200)
            result = [status: 416, code: 55531,message: 'Empty Result Set',payload: null]
            render result as JSON
            return
        }
        result = [status: 200, code: 555,message: 'Success. See payload.',payload : null]

        def facturas = [:]
        while(filas.hasNext()) {
            def element = filas.next() ?: null
            //println  element
            if(element){
                def items = []
                def elementTotal = 0
                def elementImptotal = 0
                def totalDescuentos = 0
                def propina = 0
                def consecutivo = element?.fields?.sale_number
                element?.fields?.ingredients?.each{
                    BasicDBObject q = new BasicDBObject("table",'ingredient').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def ingredient = db.dataRow.find(q).next()
                    BasicDBObject q2 = new BasicDBObject("table",'tax').
                            append("syncId",ingredient.fields?.tax_id).
                            append("isDeleted", false);
                    def tax = db.dataRow.find(q2).next()
                    def item = [item : ingredient?.fields?.name,precio:ingredient?.fields?.price_per_unit, impuesto: tax?.fields?.percentage ,cantidad: it.quantity ]
                    items.push(item)
                    elementTotal += ingredient?.fields?.price_per_unit * (it.quantity)
                    elementImptotal += (ingredient?.fields?.price_per_unit * (it.quantity)) * tax?.fields?.percentage

                }
                element?.fields?.products?.each{
                    BasicDBObject q = new BasicDBObject("table",'product').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def product = db.dataRow.find(q).next()
                    BasicDBObject q2 = new BasicDBObject("table",'tax').
                            append("syncId",product?.fields?.tax_id).
                            append("isDeleted", false);
                    def tax = db.dataRow.find(q2).next()
                    def item = [item : product?.fields?.name,precio:product?.fields?.price, impuesto: tax?.fields?.percentage ,cantidad: it.quantity ]
                    items.push(item)
                    elementTotal += product?.fields?.price * (it.quantity)
                    elementImptotal += (product?.fields?.price * (it.quantity)) * tax?.fields?.percentage
                }
                element?.fields?.combos?.each{
                    BasicDBObject q = new BasicDBObject("table",'combo').
                            append("syncId",it.sync_id).
                            append("isDeleted", false);
                    def combo = db.dataRow.find(q).next()
                    BasicDBObject q2 = new BasicDBObject("table",'tax').
                            append("syncId",combo.fields?.tax_id).
                            append("isDeleted", false);
                    def tax = db.dataRow.find(q2).next()
                    def item = [item : combo?.fields?.name,precio:combo?.fields?.price, impuesto: tax?.fields?.percentage ,cantidad: it.quantity ]
                    items.push(item)
                    elementTotal += combo?.fields?.price * (it.quantity)
                    elementImptotal += (combo?.fields?.price * (it.quantity)) * tax?.fields?.percentage
                }
                totalDescuentos += elementTotal * (element?.fields?.discount / 100) //fix this for any sale saved before 11/04/2014 (March the 11th 2014)
                if((element?.timeCreated <= doomdate) && element?.fields?.received <= 100){ //it means is a sale saved with the discount in the received field
                    totalDescuentos += elementTotal * (element?.fields?.received / 100)
                }
                if(element?.fields?.tip == 1)
                    propina += elementTotal * 0.1
                facturas[consecutivo] = [items:items, venta : elementTotal, impuestos: elementImptotal,descuentos: totalDescuentos, propinas: propina, date: formatDate([date:  element?.timeCreated,type: "datetime",timeStyle: "SHORT",dateStyle: "LONG", locale: "es_CO"]), method: paymentMethod(element?.fields?.payment_method), device: [name: element?.device?.name,resolution: element?.device?.resolution]]

            }

        }

        result.payload = facturas
        render result as JSON
        return
    }

    private magicDates(params){
        /***************
         *
         *
         *
         *      FECHAS - TOMADO DE QPON 0.2.2 BY QTAG TECHNOLOGIES
         *
         *
         *
         *
         *
         * ************************/


        def startDate
        def endDate
        def sentDate = params.reportDate ?: 'default'
        //def sentDate = "04/9/2014-04/13/2014"
        if(sentDate == 'Click para Seleccionar') //lo que diga el widget que se ponga
        {
            sentDate = 'default'
        }
        if(sentDate == 'default')
        {


            def start = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
            def end = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
            start.set(start.get(Calendar.YEAR),start.get(Calendar.MONTH),start.get(Calendar.DATE),0,0,0)
            end.set(end.get(Calendar.YEAR),end.get(Calendar.MONTH),end.get(Calendar.DATE) + 1,0,0,0)
            //println "inicial: "+start.getTime()
            //println "final"+end.getTime()

            startDate = start.getTime()
            endDate = end.getTime()


        }
        //
        else //recibió la fecha del calendario que seleccionó el usuario
        {

            def values = sentDate.split('-')
            if(values.size() == 1) //la fecha recibida es un día único
            {

                def fechas = values[0].split('/')

                def month
                switch (fechas[0].trim())
                {
                    case '01': month = Calendar.JANUARY
                        break
                    case '02': month = Calendar.FEBRUARY
                        break
                    case '03': month = Calendar.MARCH
                        break
                    case '04': month = Calendar.APRIL
                        break
                    case '05': month = Calendar.MAY
                        break
                    case '06': month = Calendar.JUNE
                        break
                    case '07': month = Calendar.JULY
                        break
                    case '08': month = Calendar.AUGUST
                        break
                    case '09': month = Calendar.SEPTEMBER
                        break
                    case '10': month = Calendar.OCTOBER
                        break
                    case '11': month = Calendar.NOVEMBER
                        break
                    case '12': month = Calendar.DECEMBER
                        break
                }

                def start = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
                start.set(year: fechas[2].toInteger(), month: month, date: fechas[1].toInteger(), hourOfDay: 0, minute: 0,second: 0,millisecond: 0)
                def end = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
                end.set(year: fechas[2].toInteger(), month: month, date: fechas[1].toInteger() + 1, hourOfDay: 0, minute: 0,second: 0,millisecond: 0)
                startDate = start.getTime()
                endDate = end.getTime()
                //println start
                //println end
                /*
                startDate = new Date().parse('MM/dd/yyyy', values[0])
                //println startDate
                endDate = startDate.next()
                def start = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
                //println start
                def end = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
                start.setTime(startDate)
                //println start
                end.setTime(endDate)
                start.clearTime()
                println start
                end.clearTime()
                startDate = start.getTime()
                //println startDate
                endDate = end.getTime()
                */
            }
            else //Es un rango de fechas
            {
                println "en rango de fechas"
                assert values.size() == 2
                def fecha1 = values[0].split('/')
                def fecha2 = values[1].split('/')

                def month1
                def month2
                switch (fecha1[0].trim())
                {
                    case '01': month1 = Calendar.JANUARY
                        break
                    case '02': month1 = Calendar.FEBRUARY
                        break
                    case '03': month1 = Calendar.MARCH
                        break
                    case '04': month1 = Calendar.APRIL
                        break
                    case '05': month1 = Calendar.MAY
                        break
                    case '06': month1 = Calendar.JUNE
                        break
                    case '07': month1 = Calendar.JULY
                        break
                    case '08': month1 = Calendar.AUGUST
                        break
                    case '09': month1 = Calendar.SEPTEMBER
                        break
                    case '10': month1 = Calendar.OCTOBER
                        break
                    case '11': month1 = Calendar.NOVEMBER
                        break
                    case '12': month1 = Calendar.DECEMBER
                        break
                }
                switch (fecha2[0].trim())
                {
                    case '01': month2 = Calendar.JANUARY
                        break
                    case '02': month2 = Calendar.FEBRUARY
                        break
                    case '03': month2 = Calendar.MARCH
                        break
                    case '04': month2 = Calendar.APRIL
                        break
                    case '05': month2 = Calendar.MAY
                        break
                    case '06': month2 = Calendar.JUNE
                        break
                    case '07': month2 = Calendar.JULY
                        break
                    case '08': month2 = Calendar.AUGUST
                        break
                    case '09': month2 = Calendar.SEPTEMBER
                        break
                    case '10': month2 = Calendar.OCTOBER
                        break
                    case '11': month2 = Calendar.NOVEMBER
                        break
                    case '12': month2 = Calendar.DECEMBER
                        break
                }
                def start = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
                start.set(year: fecha1[2].toInteger(), month: month1, date: fecha1[1].toInteger(), hourOfDay: 0, minute: 0,second: 0,millisecond: 0)
                def end = Calendar.getInstance(TimeZone.getTimeZone(grailsApplication.config.app.timezone))
                end.set(year: fecha2[2].toInteger(), month: month2, date: fecha2[1].toInteger() + 1, hourOfDay: 0, minute: 0,second: 0,millisecond: 0)
                println start
                println end
                startDate = start.getTime()
                endDate = end.getTime()
            }

        }
        SimpleDateFormat sdf = new SimpleDateFormat ("MM/dd/yyyy HH:mm:ss") //poner que esto siempre sea 00:00:00
        sdf.setTimeZone (TimeZone.getTimeZone ("UTC"))
        startDate = sdf.format(startDate)
        //println startDate
        endDate = sdf.format(endDate)
        startDate = new Date().parse('MM/dd/yyyy HH:mm:ss',startDate)
        endDate = new Date().parse('MM/dd/yyyy HH:mm:ss',endDate)
        DateTime sDt = new DateTime(startDate)
        DateTime eDt = new DateTime(endDate)
        def doomdate = Calendar.instance
        doomdate.set 2014, Calendar.APRIL, 13
        doomdate = sdf.format(doomdate.getTime())
        doomdate = new Date().parse('MM/dd/yyyy HH:mm:ss',doomdate) //doomdate in utc time
        def res = [startDate: startDate,endDate:endDate,doomdate:doomdate]
        return res
    }


    /*Metodo de Pago*/
    private paymentMethod(type){
        def result = "Efectivo"
        switch(type){
            case "cash": result = "Efectivo"
                break
            case "card": result = "Tarjeta Débito/Crédito"
                break
            default: result = "Efectivo"

        }
        return result
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
    def transformDataRows(company){
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
        BasicDBObject query2 = new BasicDBObject().append("device.company",company as Integer)
        db.dataRow.update(query2,set2,false,true)
    }
    /*
    *
    *
    *
    * */




 }
