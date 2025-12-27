package com.phenikaa.scheduler.controller.util;

import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ExcelTemplateUtil {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private ExcelTemplateUtil() {
    }

    public static CellStyle createBoldHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public static Row createHeaderRow(Sheet sheet, String[] columns, CellStyle headerStyle, int columnWidthChars) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            if (headerStyle != null) {
                cell.setCellStyle(headerStyle);
            }
            if (columnWidthChars > 0) {
                sheet.setColumnWidth(i, columnWidthChars * 256);
            }
        }
        return header;
    }

    @SuppressWarnings("null")
    public static ResponseEntity<byte[]> toXlsxResponse(Workbook workbook, String fileName) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(XLSX_MEDIA_TYPE)
                    .body(out.toByteArray());
        }
    }
}
