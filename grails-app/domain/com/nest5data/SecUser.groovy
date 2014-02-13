package com.nest5data

class SecUser {

    transient springSecurityService

    String username
    String password
    boolean enabled = true
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired

    static transients = ['springSecurityService']

    static constraints = {
        username blank: false, unique: true
        password blank: false
    }

    static mapping = {
        password column: '`password`'
        datasource  "trans"

    }

    Set<SecRole> getAuthorities() {
        SecUserSecRole.findAllBySecUser(this).collect { it.secRole } as Set
    }
    //Here we will never get a new company registering, only registered companies from nest5 server, so the password comes encoded and should never be re-econded again.
    def beforeInsert() {
        //encodePassword()
    }

    def beforeUpdate() {
        /*if (isDirty('password')) {
            encodePassword()
        }*/
    }

    protected void encodePassword() {
        password = springSecurityService.encodePassword(password)
    }
}
