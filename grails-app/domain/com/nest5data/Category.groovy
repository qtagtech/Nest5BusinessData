package com.nest5data

class Category {
    static hasMany = [families: Family]
    String name
    String description
    Icon icon


    static  mapping = {
        sort name: "asc"
        description type: 'text'
        name type: 'text'
        datasource "trans"

    }



    static constraints = {
    }

    String toString(){
        name
    }
}
