package com.nest5data

import org.bson.types.ObjectId

class Language {
    ObjectId id
    String name
    static mapping = {
        name index: true
    }

    static constraints = {
    }
    static mapWith = "mongo"
}
