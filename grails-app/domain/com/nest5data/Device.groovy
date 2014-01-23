package com.nest5data

import org.bson.types.ObjectId

class Device {
    ObjectId id
    String name
    String uid
    Integer company
    static mapping = {
        uid index:true
        name index: true
    }

    static constraints = {
    }
}
