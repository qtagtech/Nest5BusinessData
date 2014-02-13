package com.nest5data

class MediaCompany {
    static belongsTo = [company: Company, file: FileCompany]
    Boolean isMain
    static mapping = {
        datasource "trans"

    }
    static constraints = {
    }
}
