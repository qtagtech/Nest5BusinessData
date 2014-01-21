package com.nest5data

import grails.converters.JSON

class PersonController {

    def index() { }

    def generateMany(){
       def n=  new Language(name: 'java').save()
        def o=  new Language(name: 'c').save()
        def q=   new Language(name: 'c++').save()
        def r=  new Language(name: 'groovy').save()
        def t=  new Language(name: 'python').save()
        def u=  new Language(name: 'php').save()

        def i = 0
        while (i < 5000){
          new Person(name: "persona_"+i, email: 'emailpersona_'+i+'@gmail.com',languages: [n,o,q,r,u,t]).save()
            i++
        }
        def person = new Person()
        //person['nuevo_campo'] = "este es un campo que solo tiene este"
        person.save()

    }

    def showAll(){
        def people = Person.findAll()
        people.each {
            render text: it as JSON
        }
        return
    }
}
