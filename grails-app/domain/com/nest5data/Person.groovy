package com.nest5data

import org.bson.types.ObjectId

class Person {
    static hasMany = [languages: Language]
    ObjectId id
    String name
    String email
    static mapping = {
        name index:true

    }
    static embedded = ['languages']
    static mapWith = "mongo"


    static constraints = {
    }

}
