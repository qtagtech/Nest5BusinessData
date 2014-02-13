package com.nest5data

class Object {
    static hasMany = [categories:  Category]
    String name
    String description
    Icon icon
    static mapping = {
        datasource "trans"

    }

    static constraints = {
    }

    String toString(){
        name
    }
}
