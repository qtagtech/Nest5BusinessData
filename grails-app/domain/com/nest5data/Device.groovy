package com.nest5data

import org.bson.types.ObjectId

class Device {
    ObjectId id
    String name
    String uid
    Integer company
    static mapping = {
        compoundIndex uid:1, name:1
    }

    static constraints = {
    }
}
