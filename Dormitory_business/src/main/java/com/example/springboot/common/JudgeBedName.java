package com.example.springboot.common;

/**
 * 把床位序号转成数据库字段名
 */

public class JudgeBedName {

    private static String bedName;

    public static String getBedName(int num) {
        switch (num) {
            case 1:
                return bedName = "first_bed";
            case 2:
                return bedName = "second_bed";
            case 3:
                return bedName = "third_bed";
            case 4:
                return bedName = "fourth_bed";
            default:
                return null;
        }
    }
}
