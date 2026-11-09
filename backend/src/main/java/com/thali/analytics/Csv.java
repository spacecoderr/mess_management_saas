package com.thali.analytics;

import java.nio.charset.StandardCharsets;

/** Minimal RFC 4180 writer. Cells that start with = + - @ are prefixed with ' so spreadsheets never run them as formulas. */
final class Csv {

    private final StringBuilder sb = new StringBuilder("﻿");   // BOM so Excel reads UTF-8

    Csv row(Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
        return this;
    }

    byte[] bytes() {
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(Object cell) {
        if (cell == null) return "";
        String s = cell.toString();
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+(\\.\\d+)?")) s = "'" + s;
        return s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
