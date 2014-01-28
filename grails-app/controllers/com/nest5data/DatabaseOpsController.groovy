package com.nest5data

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.commons.io.FileUtils

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
                'name            text not null\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE product_category (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text not null\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE ingredient (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text not null,\n' +
                'category_id     integer,\n' +
                'tax_id          real,\n' +
                'unit_id\t\t    integer,\n' +
                'cost_per_unit   real,\n' +
                'price_per_unit    real,\n' +
                'price_measure    real,\n' +
                'quantity         real,\n' +
                'date             real,\n' +
                'FOREIGN KEY (category_id) REFERENCES ingredient_category (_id),\n' +
                'FOREIGN KEY (tax_id) REFERENCES tax (_id),\n' +
                'FOREIGN KEY (unit_id) REFERENCES measurement_unit (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE product (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text not null,\n' +
                'category_id     integer,\n' +
                'automatic_cost  integer,\n' +
                'cost \t\t\treal,\n' +
                'price\t\t\treal,\n' +
                'tax_id\t\t\t\treal,\n' +
                'FOREIGN KEY (category_id) REFERENCES product_category (_id),\n' +
                'FOREIGN KEY (tax_id) REFERENCES tax (_id)\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE combo (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text not null,\n' +
                'automatic_cost  integer,\n' +
                'cost \t\t\treal,\n' +
                'price\t\t\treal,\n' +
                'tax_id\t\t\t\treal,\n' +
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
                'name            text not null,\n' +
                'percentage     real\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE measurement_unit (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'name            text not null,\n' +
                'initials        text not null,\n' +
                'multipliers     text\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE sale (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'date            integer not null,\n' +
                'received        real,\n' +
                'payment_method  text\n' +
                ');\n' +
                '\n' +
                'CREATE TABLE sale_item (\n' +
                '_id\t\t\t    integer PRIMARY KEY autoincrement NOT NULL,\n' +
                'sale_id \t\tinteger,\n' +
                'sale_item_id\tinteger,\n' +
                'sale_item_qty   real,\n' +
                'sale_item_type  integer,\n' +
                'FOREIGN KEY (sale_id) REFERENCES sale (_id)\n' +
                ');\n';

        def table_list = ["ingredient_category","product_category","ingredient","product","combo","productingredient","comboingredient","comboproduct","tax","measurement_unit","sale","sale_item"]
        def db = mongo.getDB('nest5BigData')
        def str = "\n"
        table_list.each {
            BasicDBObject query = new BasicDBObject("table",it).
                    append("device.company",1).
                    append("isDeleted", true);
            def filas = db.dataRow.find(query)
            def currentTable = it

            try {
                def a = 1
                while(filas.hasNext()) {
                    a++
                    def actual = filas.next()
                    def values = "null,"
                    actual.fields.each{
                        if(it.getKey() != "_id"){
                            values +="'"+it.getValue()+"'"
                            if(actual.fields.next?.getValue() != null)
                                values+=","
                        }


                        }
                    str += "INSERT INTO "+currentTable+" VALUES (\n" +
                            "${values}\n" +
                            ");"

                }
            } finally {
                filas.close();
            }

        }
        def directory = System.getProperty("java.io.tmpdir")
        def today = new Date()
        def fileStore = new File(directory+"dbExport_${company}_${today[Calendar.DATE]}_${today[Calendar.MONTH]+1}_${today[Calendar.YEAR]}_${today[Calendar.HOUR_OF_DAY]}_${today[Calendar.MINUTE]}_${today[Calendar.SECOND]}_${today[Calendar.MILLISECOND]}.sql");
        fileStore.createNewFile();
        FileUtils.writeStringToFile(fileStore, tables,'UTF-8');
        FileUtils.writeStringToFile(fileStore, str,'UTF-8',true);
        println fileStore.name
        response.setHeader "Content-disposition", "attachment; filename="+fileStore.name.lastIndexOf('.').with {it != -1 ? fileStore.name[0..<it] : fileStore.name}+".sql"
        response.contentType = 'application/octet-stream'
        response.outputStream << fileStore.text
        response.outputStream.flush()


    }
}
