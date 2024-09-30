package com.zhaocai.business.filez.util;

import java.text.SimpleDateFormat;

public class DateUtil {

    public static String getCurrentTimestamp() {
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateformat.format(System.currentTimeMillis());
    }

}
