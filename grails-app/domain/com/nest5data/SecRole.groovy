package com.nest5data

class SecRole {

    String authority

    static mapping = {
        datasource  "trans"

    }

    static constraints = {
        authority blank: false, unique: true
    }
}
