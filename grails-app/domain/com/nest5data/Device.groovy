package com.nest5data

import org.bson.types.ObjectId

class Device {
    ObjectId id
    String name
    String uid
   // Integer company
    Store store
    Date registeredOn
    Date lastUpdated
    Integer minSale
    Integer maxSale
    Integer currentSale
    String prefix
    String resolution
    static mapping = {
        compoundIndex uid:1, name:1
        resolution type: 'text'
    }

    static constraints = {
    }
    static mapWith = "mongo"
    static embedded = ['store']

}
