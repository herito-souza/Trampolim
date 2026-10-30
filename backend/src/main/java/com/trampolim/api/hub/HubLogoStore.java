package com.trampolim.api.hub;
import java.nio.file.*;import java.io.*;import java.awt.image.BufferedImage;import java.util.*;
import javax.imageio.*;import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
@Component
public class HubLogoStore {
 private final Path root;
 public HubLogoStore(@Value("${hub.logo-directory:storage/hub-logos}")String directory){root=Path.of(directory).toAbsolutePath().normalize();}
 public String save(long org,MultipartFile file){if(org<1||file==null||file.isEmpty()||file.getSize()>1048576)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Envie PNG ou JPEG de até 1 MB.");try{
  byte[] input=file.getBytes();boolean png=input.length>8&&input[0]==(byte)137&&input[1]==80&&input[2]==78&&input[3]==71;boolean jpg=input.length>3&&input[0]==(byte)255&&input[1]==(byte)216&&input[2]==(byte)255;if(!png&&!jpg)throw invalid();
  try(ImageInputStream stream=ImageIO.createImageInputStream(new ByteArrayInputStream(input))){var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw invalid();var reader=readers.next();try{reader.setInput(stream,true,true);int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||width>2048||height>2048||(long)width*height>4194304)throw invalid();BufferedImage decoded=reader.read(0);BufferedImage clean=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var graphics=clean.createGraphics();try{graphics.drawImage(decoded,0,0,null);}finally{graphics.dispose();}var output=new ByteArrayOutputStream();ImageIO.write(clean,"png",output);if(output.size()>4194304)throw invalid();String key=UUID.randomUUID()+".png";Path directory=root.resolve(Long.toString(org)).normalize();Files.createDirectories(directory);Files.write(directory.resolve(key),output.toByteArray(),StandardOpenOption.CREATE_NEW);return key;}finally{reader.dispose();}}
 }catch(ResponseStatusException error){throw error;}catch(Exception error){throw invalid();}}
 public byte[] read(long org,String key){if(org<1||key==null||!key.matches("[a-f0-9-]{36}\\.png"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);Path path=root.resolve(Long.toString(org)).resolve(key).normalize();if(!path.startsWith(root))throw invalid();try{return Files.readAllBytes(path);}catch(IOException error){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Logotipo indisponível.");}}
 public void remove(long org,String key){if(key==null||!key.matches("[a-f0-9-]{36}\\.png"))return;try{Files.deleteIfExists(root.resolve(Long.toString(org)).resolve(key));}catch(IOException ignored){}}
 private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Imagem inválida. Use PNG/JPEG estático, até 2048 pixels por dimensão e 1 MB. A imagem será reprocessada antes de ser armazenada.");}
}
