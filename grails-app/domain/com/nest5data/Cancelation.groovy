package com.nest5data

import org.bson.types.ObjectId

class Cancelation {

    ObjectId id
    Date date
    Long sale
    Boolean reviewed
    Long seller
    Device device




    static constraints = {
    }
    static mapWith = "mongo"
    static embedded = ['device']
}
