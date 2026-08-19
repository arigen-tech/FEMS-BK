package com.dmsBackend.entity;



/**
 * Implemented by masters that have a short 'code' column in addition
 * to name (currently: State, District).
 */
public interface CodedMaster {
    String getCode();
    void setCode(String code);
}
