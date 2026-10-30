package com.trampolim.api.education;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.text.PDFTextStripper;

/** Runs in a separate JVM with a bounded heap and a parent-enforced deadline. No network or scripts are executed. */
public final class PdfDocumentWorker {
    private static final Set<String> FORBIDDEN=Set.of("JavaScript","JS","AA","OpenAction","Launch","EmbeddedFiles","EF","AcroForm","XFA","RichMediaContent","RichMediaSettings","Collection","AF");
    public static void main(String[] args) throws Exception {
        Path source=Path.of(args[1]),output=Path.of(args[2]);int maxPages=Integer.parseInt(args[3]);
        if(args[0].equals("generate")) generate(source,output);
        else inspect(source,output,maxPages);
    }
    private static void inspect(Path source,Path output,int maxPages) throws Exception {
        try(var doc=Loader.loadPDF(source.toFile())) {
            if(doc.isEncrypted()||doc.getNumberOfPages()<1||doc.getNumberOfPages()>maxPages)throw new IOException("Unsupported PDF");
            var visited=Collections.newSetFromMap(new IdentityHashMap<COSBase,Boolean>());
            var pending=new ArrayDeque<COSBase>();pending.add(doc.getDocument().getTrailer());int count=0;
            while(!pending.isEmpty()) {
                COSBase value=pending.removeLast();if(!visited.add(value))continue;if(++count>200000)throw new IOException("Complex PDF");
                if(value instanceof COSObject object){COSBase item=object.getObject();if(item!=null)pending.add(item);}
                else if(value instanceof COSDictionary dictionary){
                    for(var key:dictionary.keySet()) {
                        if(FORBIDDEN.contains(key.getName()))throw new IOException("Active PDF");
                        var item=dictionary.getItem(key);if(item!=null)pending.add(item);
                    }
                    // Actions of every kind (including external URLs/files) and multimedia annotations are refused.
                    if(dictionary.containsKey(COSName.A)||dictionary.containsKey(COSName.getPDFName("F"))&&dictionary instanceof COSStream)throw new IOException("External PDF resource");
                    String subtype=dictionary.getNameAsString(COSName.SUBTYPE,"");
                    if(Set.of("FileAttachment","Movie","Sound","Screen","RichMedia","3D").contains(subtype))throw new IOException("Media attachment");
                }else if(value instanceof COSArray array){for(COSBase item:array)if(item!=null)pending.add(item);}
            }
            var text=new StringWriter(){@Override public void write(String str){if(getBuffer().length()+str.length()>1000000)throw new IllegalArgumentException("Text limit");super.write(str);}@Override public void write(char[] chars,int off,int len){if(getBuffer().length()+len>1000000)throw new IllegalArgumentException("Text limit");super.write(chars,off,len);}};
            new PDFTextStripper().writeText(doc,text);
            Files.writeString(Path.of(output+".txt"),text.toString());
            var info=new Properties();info.setProperty("pages",String.valueOf(doc.getNumberOfPages()));info.setProperty("hasText",String.valueOf(!text.toString().isBlank()));
            info.setProperty("tagged",String.valueOf(doc.getDocumentCatalog().getStructureTreeRoot()!=null));
            try(var stream=Files.newOutputStream(Path.of(output+".properties"))){info.store(stream,"PDF inspection; tagging is not accessibility certification");}
            doc.save(output.toFile());
        }
    }
    private static void generate(Path source,Path output) throws Exception {
        String text=Files.readString(source);if(text.length()>50000)throw new IOException("Text limit");
        try(var doc=new PDDocument()) {
            var font=new PDType1Font(Standard14Fonts.FontName.HELVETICA);var lines=new ArrayList<String>();
            for(String paragraph:text.replace('\t',' ').replace("\r","").split("\n",-1)) {
                String line="";for(String word:paragraph.split(" ",-1)){
                    // Character validation prevents silent loss of the author's text.
                    font.encode(word);
                    if(font.getStringWidth(line+" "+word)/1000*12>480&&!line.isEmpty()){lines.add(line);line="";}
                    while(font.getStringWidth(word)/1000*12>480){int end=Math.min(60,word.length());lines.add(word.substring(0,end));word=word.substring(end);}
                    line=line.isEmpty()?word:line+" "+word;
                }lines.add(line);
            }
            for(int offset=0;offset<lines.size();offset+=44){var page=new PDPage();doc.addPage(page);try(var content=new PDPageContentStream(doc,page)){content.beginText();content.setFont(font,12);content.setLeading(16);content.newLineAtOffset(54,735);for(String line:lines.subList(offset,Math.min(offset+44,lines.size()))){content.showText(line);content.newLine();}content.endText();}}
            doc.getDocumentCatalog().setLanguage("pt-BR");doc.save(output.toFile());
        }
    }
}
