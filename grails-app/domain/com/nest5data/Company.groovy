package com.nest5data

class Company extends SecUser {

    static belongsTo = [category: Category]
    String name
    String address
    String telephone
    String contactName
    String email
    String nit
    String logo = ""
    String url = "http://"
    Date registerDate
    Boolean active = true
    Long global_id

    static mapping = {
        datasource "trans"

    }




    static constraints = {

    }

    String toString(){
        name
    }
}
