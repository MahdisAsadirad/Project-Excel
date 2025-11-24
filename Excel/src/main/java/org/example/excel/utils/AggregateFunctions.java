// utils/AggregateFunctions.java
package org.example.excel.utils;

import org.example.excel.controller.ExpressionParser;
import org.example.excel.model.Cell;
import org.example.excel.model.Spreadsheet;

import java.util.List;

public class AggregateFunctions {

    public static double sum(Spreadsheet spreadsheet, String range) {
        try {
            List<Double> values = ExpressionParser.getValuesFromRange(range, spreadsheet);
            double result = values.stream().mapToDouble(Double::doubleValue).sum();
            System.out.println("DEBUG: SUM(" + range + ") = " + result);
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: SUM error: " + e.getMessage());
            throw e;
        }
    }

    public static double average(Spreadsheet spreadsheet, String range) {
        try {
            List<Double> values = ExpressionParser.getValuesFromRange(range, spreadsheet);
            if (values.isEmpty()) {
                return 0;
            }
            double result = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            System.out.println("DEBUG: AVG(" + range + ") = " + result);
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: AVG error: " + e.getMessage());
            throw e;
        }
    }

    public static double max(Spreadsheet spreadsheet, String range) {
        try {
            List<Double> values = ExpressionParser.getValuesFromRange(range, spreadsheet);
            if (values.isEmpty()) {
                return 0;
            }
            double result = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            System.out.println("DEBUG: MAX(" + range + ") = " + result);
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: MAX error: " + e.getMessage());
            throw e;
        }
    }

    public static double min(Spreadsheet spreadsheet, String range) {
        try {
            List<Double> values = ExpressionParser.getValuesFromRange(range, spreadsheet);
            if (values.isEmpty()) {
                return 0;
            }
            double result = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            System.out.println("DEBUG: MIN(" + range + ") = " + result);
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: MIN error: " + e.getMessage());
            throw e;
        }
    }

    public static double count(Spreadsheet spreadsheet, String range) {
        try {
            List<Double> values = ExpressionParser.getValuesFromRange(range, spreadsheet);
            double result = values.size();
            System.out.println("DEBUG: COUNT(" + range + ") = " + result);
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: COUNT error: " + e.getMessage());
            throw e;
        }
    }
}