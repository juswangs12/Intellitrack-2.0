package com.intellitrack.service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Adapts Google formatted values to the same validated importer as local XLSX. */
public final class SpreadsheetImportFile implements MultipartFile {
    private final byte[] bytes;
    public SpreadsheetImportFile(List<List<Object>> rows)throws IOException{
        try(var workbook=new XSSFWorkbook();var out=new ByteArrayOutputStream()){
            var sheet=workbook.createSheet("Classlist");
            for(int r=0;r<rows.size();r++){var row=sheet.createRow(r);var values=rows.get(r);for(int c=0;c<Math.min(7,values.size());c++)row.createCell(c).setCellValue(Objects.toString(values.get(c),""));}
            workbook.write(out);bytes=out.toByteArray();
        }
    }
    public String getName(){return "file";}
    public String getOriginalFilename(){return "google-classlist.xlsx";}
    public String getContentType(){return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";}
    public boolean isEmpty(){return bytes.length==0;}
    public long getSize(){return bytes.length;}
    public byte[] getBytes(){return bytes.clone();}
    public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
    public void transferTo(File dest)throws IOException{Files.write(dest.toPath(),bytes);}
}
