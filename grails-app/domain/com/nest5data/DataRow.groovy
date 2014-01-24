package com.nest5data

import org.bson.types.ObjectId

class DataRow {

    ObjectId id
    Long syncId  //generate it if it is a new insert, not used yet, find the way to save it in the device once it is generated here, show
    String table
    Integer rowId
    Date timeCreated
    Date timeReceived
    String hashKey
    Device device
    Boolean isDeleted
    static mapping = {
        compoundIndex table:1, rowId:1
    }
    static embedded = ['device']

    static constraints = {
    }
}
