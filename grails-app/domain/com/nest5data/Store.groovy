package com.nest5data

import org.bson.types.ObjectId

class Store {
    static hasMany = [devices: Device]
    ObjectId id
    String name
    Double latitude
    Double longitude
    List location
    Integer company


    static constraints = {
    }

    static mapWith = "mongo"
    static mapping = {
        location geoIndex: true
    }
    static embedded = 'devices'
}
