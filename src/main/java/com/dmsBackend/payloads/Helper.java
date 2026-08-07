package com.dmsBackend.payloads;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public  class  Helper {
    public static Timestamp getCurrentTimeStamp() {
        return  new Timestamp(new Date().getTime());
    }

}
