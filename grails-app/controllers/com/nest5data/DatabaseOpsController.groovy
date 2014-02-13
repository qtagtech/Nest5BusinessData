package com.nest5data

import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.io.FileUtils
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
        println params.payload
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





        def table_list = ["ingredient_category","product_category","tax","measurement_unit","ingredient","product","combo","productingredient","comboingredient","comboproduct","sale","sale_item","sync_row"]  //the order matters since for inserting an ingredient, tax, measuremet_unit and categories must be present in the database
        def db = mongo.getDB('pruebamongo')
        def str = new StringBuilder()
        str.append("\n")
        def sales = []
        def products = []
        def combos = []
        table_list.each {
            BasicDBObject query = new BasicDBObject("table",it).
                    append("device.company",company).
                    append("isDeleted", false);
            def filas = db.dataRow.find(query)
            def currentTable = it


            try {
                if((it == "ingredient_category") || (it == "product_category") || (it == "tax") || (it == "measurement_unit") || (it == "sync_row") || (it == "sale")) {
                    while(filas.hasNext()) {
                        //println it
                        def actual = filas.next()
                        def values = "null,"
                        def keys= "_id,"
                        def agregar = false

                        actual.fields.sort{it.getKey()}.each{
                            if((it.getKey() != "_id") && (it.getKey() != "ingredients")&& (it.getKey() != "products")&& (it.getKey() != "combos")){
                                if(it.getValue() != null & it.getValue() != "") {
                                    agregar = true
                                    keys += it.getKey()+","
                                    if(it.getKey() == "multipliers"){
                                        values +="'"+(it.getValue()).toString()+"',"
                                    }
                                    else{

                                        if( isNumeric(it.getValue() as String))
                                            values +=it.getValue()+","
                                        else
                                            values +="'"+it.getValue()+"',"

                                    }

                                    /*if(actual.fields.next?.getValue() != null)
                                        values+=","*/
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
                    /*   estructura de query para ingredientes que depende de tax, measurement_unit y ingredient_category. Se hace un insert selecconando los valores con los sync_id de esas tres tablas, luego
                 *los valores fijos se meten con un update diciendole que lo haga sobre el ultimo row actualizado en esa tabal
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
                        if(it != 'combo' && it != 'product'){
                            values += "INSERT into "+it+" (category_id, tax_id, unit_id)\n"
                            values += "SELECT ingredient_category._id as category_id, tax._id as tax_id, "+
                                        "measurement_unit._id as unit_id\n"+
                                        "FROM ingredient_category, tax, measurement_unit\n"+
                                        "WHERE\n"+
                                        "ingredient_category.sync_id = "+actual.fields.category_id+"\n"+
                                        "AND\n"+
                                        "tax.sync_id = "+actual.fields.tax_id+"\n"+
                                        "AND\n"+
                                        "measurement_unit.sync_id = "+actual.fields.unit_id+";\n"
                        }
                        else{
                            if( it != 'combo') {
                                values += "INSERT into "+it+" (category_id, tax_id)\n"
                                values += "SELECT product_category._id as category_id, tax._id as tax_id "+
                                        "FROM product_category, tax\n"+
                                        "WHERE\n"+
                                        "product_category.sync_id = "+actual.fields.category_id+"\n"+
                                        "AND\n"+
                                        "tax.sync_id = "+actual.fields.tax_id+";\n"
                            }else{
                                values += "INSERT into "+it+" (tax_id)"
                                values += "SELECT tax._id as tax_id \n"+
                                        "FROM tax\n"+
                                        "WHERE\n"+
                                        "tax.sync_id = "+actual.fields.tax_id+";\n"
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
                                    if( isNumeric(it.getValue() as String))
                                        val = it.getValue()
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
       /* println "sales: "
        println sales
        println "products: "
        println products
        println "combos: "
        println combos*/
        def TYPE_INGREDIENT = 0;
        def TYPE_PRODUCT = 1;
        def TYPE_COMBO = 3;
        def TYPE_CUSTOM = 2;
        sales.each {
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
        }

        combos.each {
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

        }
        products.each {
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

        }
        def directory = System.getProperty("java.io.tmpdir")
        def today = new Date()
        def fileStore = new File(directory+"dbExport_${company}_${today[Calendar.DATE]}_${today[Calendar.MONTH]+1}_${today[Calendar.YEAR]}_${today[Calendar.HOUR_OF_DAY]}_${today[Calendar.MINUTE]}_${today[Calendar.SECOND]}_${today[Calendar.MILLISECOND]}.sql");
        fileStore.createNewFile();
        FileUtils.writeStringToFile(fileStore, tables,'UTF-8');
        FileUtils.writeStringToFile(fileStore, str.toString(),'UTF-8',true);
        println fileStore.name
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
}
