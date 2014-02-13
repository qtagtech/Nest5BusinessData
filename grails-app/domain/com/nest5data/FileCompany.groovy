package com.nest5data

class FileCompany {

    static hasMany = [media: MediaCompany]
    String name
    String tipo
    String ruta
    String description




    static constraints = {
    }
    static mapping = {
        description type: 'text'
        datasource "trans"

    }
}
