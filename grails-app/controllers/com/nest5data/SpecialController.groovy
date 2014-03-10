package com.nest5data

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

@Secured(["permitAll"])
class SpecialController {

    def index() {
        render "Nest5 BigData Server v."+grailsApplication.metadata.'app.version'+'<br><span style="font-size:9; color: orange;">&copy;2014 - Nest5 By <a href="http://www.qtagtech.com">Qtag Technologies</a> Made Proudly in Colombia</span>'
        return
    }

    def channel(){
        def value = 60*60*24*365
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR) + 1;
        int month = now.get(Calendar.MONTH); // Note: zero based!
        int hour = now.get(Calendar.HOUR);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        int day = now.get(Calendar.DAY_OF_MONTH);
        int dWeek = now.get(Calendar.DAY_OF_WEEK)
        println dWeek
        def dayofWeek
        def mon
        switch (dWeek){
            case 1: dayofWeek = "Sun"
                break
            case 2: dayofWeek = "Mon"
                break
            case 3: dayofWeek = "Tue"
                break
            case 4: dayofWeek = "Wed"
                break
            case 5: dayofWeek = "Thu"
                break
            case 6: dayofWeek = "Fri"
                break
            case 7: dayofWeek = "Sat"
                break

        }
        switch (month){
            case 0: mon = "Jan"
                break
            case 1: mon = "Feb"
                break
            case 2: mon = "Mar"
                break
            case 3: mon = "Apr"
                break
            case 4: mon = "May"
                break
            case 5: mon = "Jun"
                break
            case 6: mon = "Jul"
                break
            case 7: mon = "Aug"
                break
            case 8: mon = "Sep"
                break
            case 9: mon = "Oct"
                break
            case 10: mon = "Nov"
                break
            case 11: mon = "Dec"
                break

        }
        //println month

        response.setHeader('Pragma','public')
        response.setHeader("Cache-Control",'max-age= '+value)
//        Fri, 16 Mar 2012 17:38:57 GMT
        response.setHeader('Expires',"${dayofWeek}, ${day} ${mon} ${year} ${hour}:${minute}:${second} GMT")


    }
}
