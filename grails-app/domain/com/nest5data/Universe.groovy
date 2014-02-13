package com.nest5data

class Universe {

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
