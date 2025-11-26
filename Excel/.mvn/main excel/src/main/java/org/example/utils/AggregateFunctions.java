package org.example.utils;

import org.example.controller.ExpressionParser;
import org.example.model.Spreadsheet;

import java.util.List;

public class AggregateFunctions {

    public static double sum(Spreadsheet spreadsheet, String range) {
        try {
            List<Double> values = ExpressionParser.getValuesFromRange(range, spreadsheet);
            double result = values.stream().mapToDouble(Double::doubleValue).sum();
            return result;
        } catch (Exception e) {
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
            return result;
        } catch (Exception e) {
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
            return result;
        } catch (Exception e) {
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
            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    public static double count(Spreadsheet spreadsheet, String range) {
        try {
            List<Double> values = ExpressionParser.getValuesFromRange(range, spreadsheet);
            double result = values.size();
            return result;
        } catch (Exception e) {
            throw e;
        }
    }
}