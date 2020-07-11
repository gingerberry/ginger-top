package com.gingerberry;

import com.amazonaws.regions.Regions;

public class Config {
    public static final String DB_USR = "root";
    public static final String DB_PWD = "";
    public static final String DB_NAME = "gingerberry";
    public static final String DB_HOST = "localhost";

    /**
     * LOCAL_STORAGE is a directory with read and write permissions for all processes.
     * MUST end in /.
     */
    public static final String LOCAL_STORAGE = "/Applications/XAMPP/xamppfiles/htdocs/ginger_storage/";

    /**
     * S3_BUCKET_NAME is the name of the S3 bucket in which the presentation files are going to be stored.
     * When not empty it has advantage over the LOCAL_STORAGE config thus turning it off.
     */
    public static final String S3_BUCKET_NAME = "";
    public static final Regions S3_REGION = Regions.US_EAST_1;

    private Config() {
    }
}