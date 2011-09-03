/*---

    iGeo - http://igeo.jp

    Copyright (c) 2002-2011 Satoru Sugihara

    This file is part of iGeo.

    iGeo is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation, version 3.

    iGeo is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with iGeo.  If not, see <http://www.gnu.org/licenses/>.

---*/
/*
  
    The Java code on this file is produced by tranlation and modification
    of open source codes of openNURBS toolkit written in C++. 
    For more detail of openNURBS toolkit, please see 
    http://www.opennurbs.org/
    
    openNURBS toolkit is copyrighted by Robert McNeel & Associates.
    openNURBS is a trademark of Robert McNeel & Associates.
    Rhinoceros is a registered trademark of Robert McNeel & Associates.
    
*/

package igeo.io;

import igeo.core.*;
import igeo.gui.*;
import igeo.geo.*;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.zip.*;

/**
   Rhinoceros 3dm exporter class.
   The current implementation is inefficient for large data.
   To be improved.
   
   @author Satoru Sugihara
   @version 0.7.0.0;
*/
public class IRhino3dmExporter extends IRhino3dm{
    
    // NOTE: 3dm file is little endian, Java is big endian
    
    public final static int version = 4; // Rhino version 4 format
    public final static int openNurbsVersion = 201004095;
    	
    public OutputStream ostream;
    
    public Rhino3dmFile file; // store data when reading or writing
    
    public IServerI server;
    
    public int currentPos;

    
    public IRhino3dmExporter(OutputStream ostr, IServerI svr){ ostream = ostr; server = svr; }
    
    
    
    /**
       Writing the content of server out to 3dm file.
       The main entry of the exporter class.
       @param file An exporting file object.
       @param server A server interface containing exporting data.
       @return Boolean true if writing is successful. Otherwise false.
    */
    public static boolean write(File file, IServerI server){
	try{
	    FileOutputStream fos = new FileOutputStream(file);
	    boolean retval = write(fos, server);
	    if(fos!=null) fos.close();
	    return retval;
	}catch(IOException e){ e.printStackTrace(); }
	return false;
    }
    
    public static boolean write(OutputStream os, IServerI server) throws IOException{
	IRhino3dmExporter exporter = new IRhino3dmExporter(os,server);
	exporter.write();
	return true;
    }
    
    
    public void write() throws IOException{
	
	file = new Rhino3dmFile(version,openNurbsVersion,server);
	
	currentPos = 0;
	
	// Start Section
	writeStartSection();
	
	// Properties
	writeProperties();
	
	// Setting
	
	writeSettings();
	
	// Bitmap
	writeBitmapTable();
	
	// Texture Mapping
	writeTextureMappingTable();
	
	// Material
	writeMaterialTable();
	
	// Linetype
	writeLinetypeTable();
	
	// Layer
	writeLayerTable();
	
	// Group
	writeGroupTable();
	
	// Font
	writeFontTable();
	
	// Dimstyle
	writeDimStyleTable();
	
	// Light
	writeLightTable();
	
	// Hatch Pattern
	writeHatchPatternTable();
	
	// Instance Definition
	writeInstanceDefinitionTable();
	
	// Object
	writeObjectTable();
	
	// History Record
	writeHistoryRecordTable();
	
	
	// User Data
	writeUserDataTable();
	
	writeEndMark();
	
    }
    
    
    public void writeStartSection() throws IOException{
	String versionNumText = String.valueOf(version);
	final int numTextWidth = 8;
	for(int i=0; i<numTextWidth && versionNumText.length() < numTextWidth;
	    i++, versionNumText = " "+versionNumText);
	
	final String versionString = "3D Geometry File Format " + versionNumText;
	write(versionString.getBytes(), 32, null);
	
	// info chunk
	final String infoString = " IGeo 3D geometry library : version "+IG.version();
	byte[] infoBytes = infoString.getBytes();
	byte[] infoBytes2 = new byte[infoBytes.length+2];
	System.arraycopy(infoBytes,0,infoBytes2,0,infoBytes.length);
	infoBytes2[infoBytes2.length-2] = 26;
	infoBytes2[infoBytes2.length-1] = 0;
	
	writeChunk(new Chunk(tcodeCommentBlock, infoBytes2));
    }
    
    public void writeProperties() throws IOException{
	Chunk[] chunks = new Chunk[]{
	    new Chunk(tcodePropertiesOpenNurbsVersion, openNurbsVersion)
	};
	writeChunkTable(tcodePropertiesTable, chunks);
    }
    
    public void writeSettings() throws IOException{
	writeChunkTable(tcodeSettingsTable);
    }
    
    public void writeBitmapTable() throws IOException{
	writeChunkTable(tcodeBitmapTable);
    }
    
    public void writeTextureMappingTable() throws IOException{
	writeChunkTable(tcodeTextureMappingTable);
    }
    
    public void writeMaterialTable() throws IOException{
	writeChunkTable(tcodeMaterialTable);
    }
    
    public void writeLinetypeTable() throws IOException{
	writeChunkTable(tcodeLinetypeTable);
    }
    
    public void writeLayerTable() throws IOException{
	//writeChunkTable(tcodeLayerTable);
	ChunkTable layerTable = new ChunkTable(tcodeLayerTable);
	for(int i=0; i<server.server().layerNum(); i++){
	    //IOut.p("layer["+i+"] = "+server.server().getLayer(i).name());
	    
	    Layer l = new Layer(server.server().getLayer(i), i);
	    layerTable.add(nestChunk(tcodeLayerRecord,getObjectChunk(l)));
	}
	writeChunkTable(layerTable);
    }
    
    
    public void writeGroupTable() throws IOException{
	writeChunkTable(tcodeGroupTable);
    }
    
    public void writeFontTable() throws IOException{
	writeChunkTable(tcodeFontTable);
    }
    
    public void writeDimStyleTable() throws IOException{
	writeChunkTable(tcodeDimStyleTable);
    }
    
    public void writeLightTable() throws IOException{
	writeChunkTable(tcodeLightTable);
    }
    
    public void writeHatchPatternTable() throws IOException{
	writeChunkTable(tcodeHatchPatternTable);
    }
    
    public void writeInstanceDefinitionTable() throws IOException{
	writeChunkTable(tcodeInstanceDefinitionTable);
    }
    
    public void writeObjectTable() throws IOException{
	//writeChunkTable(tcodeObjectTable);
	
	ChunkTable objectTable = new ChunkTable(tcodeObjectTable);
	for(int i=0; i<server.server().objectNum(); i++){
	    
	    if(i%100==0&&i>0){ IOut.debug(1, "object #"+i+"/"+server.server().objectNum()); } //
	    
	    Chunk objChunk = getObjectChunk(server.server().getObject(i));
	    if(objChunk!=null) objectTable.add(objChunk);
	}
	IOut.debug(1, objectTable.chunks.size() + " objects are wrote"); //
	objectTable.serialize();
	writeChunkTable(objectTable);
    }
    
    static public RhinoObject getRhinoObject(IObject e){
	RhinoObject obj=null;
	if(e instanceof IPoint){
	    obj = getRhinoPoint(((IPoint)e).get());
	}
	else if(e instanceof IPointR){
	    obj = getRhinoPoint(((IPointR)e).get());
	}
	else if(e instanceof ICurve){
	    obj =  getRhinoCurve(((ICurve)e).get());
	}
	else if(e instanceof ICurveR){
	    obj = getRhinoCurve(((ICurveR)e).get());
	}
	else if(e instanceof ISurface){
	    obj =  getRhinoSurface(((ISurface)e).get());
	}
	else if(e instanceof ISurfaceR){
	    obj =  getRhinoSurface(((ISurfaceR)e).get());
	}
	else if(e instanceof IMesh){
	    obj = getRhinoMesh( ((IMesh)e).mesh );
	}
	else if(e instanceof IMeshR){
	    obj = getRhinoMesh( ((IMeshR)e).mesh );
	}
	
	if(obj!=null) obj.setAttributes(new ObjectAttributes(e));
	
	return obj;
    }
    
    static public Point getRhinoPoint(IVec p){ return new Point(p); }
    
    static public NurbsCurve getRhinoCurve(ICurveGeo crv){ return new NurbsCurve(crv); }
    
    static public /*NurbsSurface*/ RhinoObject getRhinoSurface(ISurfaceGeo srf){
	if(!srf.hasTrim() || (srf.hasDefaultTrim() && !srf.hasInnerTrim()))
	    return new NurbsSurface(srf);
	// BRep
	return new Brep(srf);
	// tmp
	//return new NurbsSurface(srf);
    }
    
    static public Mesh getRhinoMesh(IMeshI mesh){ return new Mesh(mesh); }
        
    public Chunk getObjectChunk(IObject e) throws IOException{
	//IOut.p("e = "+e); //
	
	RhinoObject obj = getRhinoObject(e);
	
	if(obj==null) return null;
	
	ChunkTable ctable = new ChunkTable(tcodeObjectRecord, tcodeObjectRecordEnd);
	
	// object type
	ctable.add(new Chunk(tcodeObjectRecordType, obj.getType()));
	
	// object
	Chunk objChunk = getObjectChunk(obj);
	if(objChunk==null) return null; // no obj data
	
	//IOut.p("objChunk = "+objChunk); //
	
	ctable.add(objChunk);
	
	// attributes
	if(obj.attributes!=null){
	    ChunkOutputStream cosAttr = new ChunkOutputStream(tcodeObjectRecordAttributes);
	    obj.attributes.write(file, cosAttr, cosAttr.getCRC());
	    Chunk attrChunk = cosAttr.getChunk();
	    if(attrChunk!=null) ctable.add(attrChunk);
	}
	
	ctable.serialize();
	
	return ctable;
	
    }
    
    
    
    public void writeHistoryRecordTable() throws IOException{
	writeChunkTable(tcodeHistoryRecordTable);
    }
    
    public void writeUserDataTable() throws IOException{
	writeChunkTable(tcodeUserTable);
    }
    
    public void writeEndMark() throws IOException{
	//writeChunk(new Chunk(tcodeEndOfFile, itob(currentPos + 4*3 /*header+body+int content*/)));
	
	ChunkOutputStream cos = new ChunkOutputStream(tcodeEndOfFile);
	
	if(file.sizeOfChunkLength()==4){
	    int len = currentPos + 4*3; /*header+body+int content*/
	    writeInt32(cos, len, cos.getCRC());
	}
	else{
	    int len = currentPos + 8*3; /*header+body+int content*/
	    writeInt64(cos, (long)len, cos.getCRC());
	}
	
	writeChunk(cos.getChunk());
	
	//writeChunk(new Chunk(tcodeEndOfFile, itob(currentPois+4*3)));
    }
    
    
    public Chunk getObjectChunk(RhinoObject obj)throws IOException{
	
	UUID uuid = obj.getClassUUID();
	
	ChunkTable objChunkTable = new ChunkTable(tcodeOpenNurbsClass);
	
	ChunkOutputStream cosUUID = new ChunkOutputStream(tcodeOpenNurbsClassUUID);
	uuid.write(cosUUID,cosUUID.getCRC());
	objChunkTable.add(cosUUID.getChunk());
	
	ChunkOutputStream cosData = new ChunkOutputStream(tcodeOpenNurbsClassData);
	obj.write(file,cosData,cosData.getCRC());
	Chunk dataChunk = cosData.getChunk();
	if(dataChunk.content==null || dataChunk.content.length==0) return null; // no data 
	objChunkTable.add(dataChunk);
	
	// user data
	// skipped
	
	Chunk endChunk = new Chunk(tcodeOpenNurbsClassEnd,0);
	objChunkTable.add(endChunk);
	
	objChunkTable.serialize();
	return objChunkTable;
    }
    
    public static Chunk getObjectChunk(Rhino3dmFile context, RhinoObject obj)throws IOException{
	
	UUID uuid = obj.getClassUUID();
	
	ChunkTable objChunkTable = new ChunkTable(tcodeOpenNurbsClass);
	
	ChunkOutputStream cosUUID = new ChunkOutputStream(tcodeOpenNurbsClassUUID);
	uuid.write(cosUUID,cosUUID.getCRC());
	objChunkTable.add(cosUUID.getChunk());
	
	ChunkOutputStream cosData = new ChunkOutputStream(tcodeOpenNurbsClassData);
	obj.write(context,cosData,cosData.getCRC());
	Chunk dataChunk = cosData.getChunk();
	if(dataChunk.content==null || dataChunk.content.length==0) return null; // no data 
	objChunkTable.add(dataChunk);
	
	// user data
	// skipped
	
	Chunk endChunk = new Chunk(tcodeOpenNurbsClassEnd,0);
	objChunkTable.add(endChunk);
	
	objChunkTable.serialize();
	return objChunkTable;
    }
    
    
    public Chunk nestChunk(int tcode, Chunk c)throws IOException{
	ChunkOutputStream cos = new ChunkOutputStream(tcode);
	writeChunk(cos,c);
	return cos.getChunk();
    }
    
    
    public void write(byte[] data, CRC32 crc) throws IOException{
	ostream.write(data); currentPos += data.length; if(crc!=null) crc.update(data);
    }
    public void write(byte[] data, int len, CRC32 crc) throws IOException{
	ostream.write(data, 0, len); currentPos += len; if(crc!=null) crc.update(data,0,len);
    }
    public void writeBytes(byte[] data, CRC32 crc) throws IOException{ write(data,crc); }
    public void writeByte(byte b, CRC32 crc) throws IOException{ ostream.write(b); currentPos++; if(crc!=null) crc.update(b); }
    public void writeChar(char c, CRC32 crc) throws IOException{ ostream.write(c); currentPos++; if(crc!=null) crc.update(c); }
    public void writeInt32(int i, CRC32 crc) throws IOException{ write(itob(i),crc); }
    public void writeInt16(short s, CRC32 crc) throws IOException{ write(stob(s),crc); }
    public void writeInt64(long l, CRC32 crc) throws IOException{ write(ltob(l),crc); }
    public void writeShort(short s, CRC32 crc) throws IOException{ writeInt16(s,crc); }
    public void writeLong(long l, CRC32 crc) throws IOException{ writeInt64(l,crc); }
    
    
    public static void write(OutputStream os, byte[] data, CRC32 crc) throws IOException{ os.write(data); if(crc!=null) crc.update(data); }
    public static void write(OutputStream os, byte[] data, int len, CRC32 crc) throws IOException{
	os.write(data, 0, len); if(crc!=null) crc.update(data);
    }
    public static void writeBytes(OutputStream os, byte[] data, CRC32 crc) throws IOException{ write(os,data,crc); }
    public static void writeByte(OutputStream os, byte b, CRC32 crc) throws IOException{ os.write(b); if(crc!=null) crc.update(b); }
    public static void writeChar(OutputStream os, char c, CRC32 crc) throws IOException{ os.write(c); if(crc!=null) crc.update(c); }
    public static void writeInt32(OutputStream os, int i, CRC32 crc) throws IOException{ write(os,itob(i),crc); }
    public static void writeInt16(OutputStream os, short s, CRC32 crc) throws IOException{ write(os,stob(s),crc); }
    public static void writeInt64(OutputStream os, long l, CRC32 crc) throws IOException{ write(os,ltob(l),crc); }
    public static void writeShort(OutputStream os, short s, CRC32 crc) throws IOException{ writeInt16(os,s,crc); }
    public static void writeInt(OutputStream os, int i, CRC32 crc) throws IOException{ write(os,itob(i),crc); }
    public static void writeLong(OutputStream os, long l, CRC32 crc) throws IOException{ writeInt64(os,l,crc); }

    public static void writeBool(OutputStream os, boolean b, CRC32 crc) throws IOException{
	writeByte(os,(byte)(b?1:0),crc);
    }
    
    public static void writeDouble(OutputStream os, double d, CRC32 crc) throws IOException{
	writeInt64(os, Double.doubleToLongBits(d), crc);
    }
    public static void writeFloat(OutputStream os, float f, CRC32 crc) throws IOException{
	writeInt32(os, Float.floatToIntBits(f), crc);
    }
    
    public static void writePoint(OutputStream os, IVecI v, CRC32 crc) throws IOException{
	writeDouble(os,v.x(),crc);
	writeDouble(os,v.y(),crc);
	writeDouble(os,v.z(),crc);
    }
    public static void writePoint3f(OutputStream os, IVecI v, CRC32 crc) throws IOException{
	writeFloat(os,(float)v.x(),crc);
	writeFloat(os,(float)v.y(),crc);
	writeFloat(os,(float)v.z(),crc);
    }
    public static void writePoint2(OutputStream os, IVec2I v, CRC32 crc) throws IOException{
	writeDouble(os,v.x(),crc);
	writeDouble(os,v.y(),crc);
    }
    public static void writePoint2f(OutputStream os, IVec2I v, CRC32 crc) throws IOException{
	writeFloat(os,(float)v.x(),crc);
	writeFloat(os,(float)v.y(),crc);
    }
    
    public static void writeInterval(OutputStream os, Interval interval, CRC32 crc) throws IOException{
	writeDouble(os,interval.v1,crc);
	writeDouble(os,interval.v2,crc);
    }
    
    public static void writeString(OutputStream os, String s, CRC32 crc) throws IOException{
	/*
	if(s==null){
	    IOut.err("string is null"); //
	    return;
	}
	*/
	
	if(s==null){ s = ""; } // !! ??
	
	
	//IOut.p("input="+s);
	
	int len = s.length() + 1;
	
	byte[] b = s.getBytes("UTF-16LE");
	/*
	byte[] b = new byte[2*s.length()];
	for(int i=0; i<s.length(); i++){
	    b[i*2]=(byte)(s.charAt(i)&0xFF);
	    b[i*2+1] = 0;
	}
	//b[2*s.length()]=0;
	*/
	
	//IOut.p("byte[] = "+hex(b));
	
	//int len = b.length + 1 + 1;
	
	writeInt32(os,len,crc);
	writeBytes(os,b,crc);
	writeByte(os,(byte)0,crc); // 16 bit terminator
	writeByte(os,(byte)0,crc);
    }
    
    
    public static void writeColor(OutputStream os, Color color, CRC32 crc) throws IOException{
	int r = 0;
	int g = 0;
	int b = 0;
	int a = 0;
	if(color!=null){
	    r = color.getRed();
	    g = color.getGreen();
	    b = color.getBlue();
	    a = 255 - color.getAlpha();
	}
	writeByte(os, (byte)(r&0xFF), crc);
	writeByte(os, (byte)(g&0xFF), crc);
	writeByte(os, (byte)(b&0xFF), crc);
	writeByte(os, (byte)(a&0xFF), crc);
    }

    public static void writeBoundingBox(OutputStream os, BoundingBox bbox, CRC32 crc)throws IOException{
	writeDouble(os,bbox.min.x,crc);
	writeDouble(os,bbox.min.y,crc);
	writeDouble(os,bbox.min.z,crc);
	writeDouble(os,bbox.max.x,crc);
	writeDouble(os,bbox.max.y,crc);
	writeDouble(os,bbox.max.z,crc);
    }
    public static void writeSurfaceCurvature(OutputStream os, SurfaceCurvature sc, CRC32 crc)throws IOException{
	writeDouble(os,sc.k1,crc);
	writeDouble(os,sc.k2,crc);
    }
    
    public static void writeArray(OutputStream os, ArrayList<? extends RhinoObject> array, CRC32 crc) throws IOException{
	writeArray(null,os,array,crc);
    }
    public static void writeArray(Rhino3dmFile context, OutputStream os, ArrayList<? extends RhinoObject> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null&&i<count; i++) array.get(i).write(context, os,crc);
    }
    
    public static void writeArrayInt(OutputStream os, ArrayList<Integer> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null && i<count; i++) writeInt32(os,array.get(i),crc);
    }
    
    public static void writeArrayPoint(OutputStream os, ArrayList<IVec> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null && i<count; i++) writePoint(os,array.get(i),crc);
    }
    
    public static void writeArrayPoint3f(OutputStream os, ArrayList<IVec> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null && i<count; i++) writePoint3f(os,array.get(i),crc);
    }
    
    public static byte[] writeArrayPoint3f(ArrayList<IVec> array, CRC32 crc) throws IOException{
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i=0; array!=null&&i<array.size(); i++) writePoint3f(baos,array.get(i),crc);
	return baos.toByteArray();
    }
    
    public static void writeArrayPoint2(OutputStream os, ArrayList<IVec2> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null && i<count; i++) writePoint2(os,array.get(i),crc);
    }
    
    public static void writeArrayPoint2f(OutputStream os, ArrayList<IVec2> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null && i<count; i++) writePoint2f(os,array.get(i),crc);
    }
    
    public static byte[] writeArrayPoint2f(ArrayList<IVec2> array, CRC32 crc) throws IOException{
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i=0; array!=null&&i<array.size(); i++) writePoint2f(baos,array.get(i),crc);
	return baos.toByteArray();
    }
    
    public static void writeArrayColor(OutputStream os, ArrayList<Color> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null && i<count; i++) writeColor(os,array.get(i),crc);
    }
    
    public static byte[] writeArrayColor(ArrayList<Color> array, CRC32 crc) throws IOException{
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i=0; array!=null && i<array.size(); i++) writeColor(baos,array.get(i),crc);
	return baos.toByteArray();
    }
    
    public static void writeArraySurfaceCurvature(OutputStream os, ArrayList<SurfaceCurvature> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null && i<count; i++) writeSurfaceCurvature(os,array.get(i),crc);
    }
    public static byte[] writeArraySurfaceCurvature(ArrayList<SurfaceCurvature> array, CRC32 crc) throws IOException{
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i=0; array!=null && i<array.size(); i++) writeSurfaceCurvature(baos,array.get(i),crc);
	return baos.toByteArray();
    }
    
    
    public static void writeArrayDisplayMaterialRef(Rhino3dmFile context, OutputStream os, ArrayList<DisplayMaterialRef> array, CRC32 crc) throws IOException{
	int count = 0; // write zero size when array is null
	if(array!=null) count = array.size(); 
	writeInt32(os,count,crc);
	for(int i=0; array!=null&&i<count; i++) array.get(i).write(context, os,crc);
    }
    
    
    
    
    public static void writeUUID(OutputStream os, UUID uuid, CRC32 crc) throws IOException{
	if(uuid!=null) uuid.write(os,crc);
	else UUID.nilValue.write(os,crc);
    }
    
    public static void writeChunkVersion(OutputStream os, int majorVersion, int minorVersion,CRC32 crc) throws IOException{
	byte b = (byte)((majorVersion<<4)&0xF0 | minorVersion&0x0F);
	writeByte(os, b, crc);
    }
    
    
    public void writeChunk(Chunk chunk) throws IOException{
	if(chunk==null) throw new IOException("chunk is null");
	if(!chunk.isShort()){
	    if(chunk.getContent()==null){
		throw new IOException("chunk is big chunk and chunk content is null : chunk = "+chunk.toString());
	    }
	    if(chunk.contentLength()!= chunk.getBody()){
		throw new IOException("chunk content length doesn't match : chunk = "+chunk.toString());
	    }
	}
	writeInt32(chunk.header,null);
	if(!chunk.isShort()&&chunk.doCRC()) writeInt32(chunk.body+4,null);
	else writeInt32(chunk.body,null);
	if(!chunk.isShort()){
	    writeBytes(chunk.content,null);
	    if(chunk.doCRC()) writeInt32(chunk.getCRC(),null);
	}
    }
    
    public static void writeChunk(OutputStream os, Chunk chunk) throws IOException{
	if(chunk==null) throw new IOException("chunk is null");
	if(!chunk.isShort()){
	    if(chunk.getContent()==null){
		throw new IOException("chunk is big chunk and chunk content is null : chunk = "+chunk.toString());
	    }
	    if(chunk.contentLength()!= chunk.getBody()){
		throw new IOException("chunk content length doesn't match : chunk = "+chunk.toString());
	    }
	}
	writeInt32(os,chunk.header,null);
	if(!chunk.isShort()&&chunk.doCRC()) writeInt32(os,chunk.body+4,null);
	else writeInt32(os,chunk.body,null);
	if(!chunk.isShort()){
	    writeBytes(os,chunk.content,null);
	    if(chunk.doCRC()) writeInt32(os,chunk.getCRC(),null);
	}
    }
    
    public void writeChunkTable(int tableTypeCode) throws IOException{
	writeChunkTable(tableTypeCode,null);
    }
    
    public void writeChunkTable(int tableTypeCode, Chunk[] chunks) throws IOException{
	writeChunkTable(tableTypeCode, chunks, tcodeEndOfTable);
    }
    
    public void writeChunkTable(int tableTypeCode, Chunk[] chunks, int endTypeCode) throws IOException{
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	for(int i=0; chunks!=null && i<chunks.length; i++) writeChunk(bos, chunks[i]);
	writeChunk(bos, new Chunk(endTypeCode, 0));
	writeChunk(new Chunk(tableTypeCode, bos.toByteArray()));
    }
    
    
    public void writeChunkTable(ChunkTable chunkTable) throws IOException{
	chunkTable.serialize(); writeChunk(chunkTable);
    }
    public static void writeChunkTable(OutputStream os, ChunkTable chunkTable) throws IOException{
	chunkTable.serialize(); writeChunk(os,chunkTable);
    }
    
    /*
    public static void writeChunkVersion(OutputStream os, int majorVersion, int minorVersion)
	throws IOException{
	byte b = (byte)(((majorVersion&0x0F) <<4)&(minorVersion&0x0F));
	writeByte(os,b);
    }
    */
    
    
    public static void writeCompressedBuffer(OutputStream os, byte[] buf, int len, CRC32 crc) throws IOException {
	
	writeInt(os, len, crc);
	
	CRC32 bufCRC = new CRC32();
	bufCRC.update(buf);
	writeInt(os, (int)bufCRC.getValue(), crc); // crc?
	
	byte method = (len>128)? (byte)1:(byte)0;
	
	writeByte(os, method, crc);
	
	if(method==0){ // uncompressed
	    write(os, buf, len, crc);
	}
	else{ // compressed
	    writeDeflate(os,buf,len,crc);
	}
	
    }
    
    public static void writeDeflate(OutputStream os, byte[] buf, int len, CRC32 crc) throws IOException{
	
	Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
	//deflater.setInput(buf,0,len);
	//byte[] outbuf = new byte[len];
	//int dlen = deflater.deflate(buf, 0, len);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
	dos.write(buf,0,len);
	dos.close();
	baos.close();
	
	ChunkOutputStream cos = new ChunkOutputStream(tcodeAnonymousChunk);
	//write(cos, outbuf, dlen, cos.getCRC());
	byte[] outbuf = baos.toByteArray();
	write(cos, outbuf, outbuf.length, cos.getCRC());
	
	//IOut.err("input length="+len); //
	//IOut.err("deflated length="+outbuf.length); //
	
	writeChunk(os, cos.getChunk());
	
    }
    
    
    public static class ChunkOutputStream extends ByteArrayOutputStream{
	public int header;
	public CRC32 crc;
	
	public ChunkOutputStream(int header){ super(); this.header = header; crc = new CRC32(); }
	
	public ChunkOutputStream(int header, int majorVersion, int minorVersion) throws IOException{
	    super();
	    this.header = header;
	    crc = new CRC32();
	    writeInt32(this, majorVersion, crc);
	    writeInt32(this, minorVersion, crc);
	}
	
	// not embeding
	//public void write(byte[] b){ super.write(b); crc.update(b); }
	//public void write(byte[] b, int off, int len){ super.write(b,off,len); crc.update(b,off,len); }
	//public void write(byte b){ super.write(b); crc.updat(b); }
	
	public CRC32 getCRC(){ return crc; }
	
	public Chunk getChunk(){
	    byte[] content = super.toByteArray();
	    return new Chunk(header, content, crc);
	}
    }
    
        
    // for debug
    public static void main(String[] args){
	if(args.length>0){
	    IOut.p("saving "+args[0]);
	    IG.init();
	    
	    //IG.current().server().getLayer("Default").setColor(Color.red);
	    //IG.current().server().getLayer("Layer 01"); //.setColor(Color.orange);
	    //IG.current().server().getLayer("layer2").setColor(Color.blue);
	    //IG.current().server().getLayer("layer03").setColor(Color.yellow);
	    //IG.current().server().getLayer("layer 4").setColor(Color.pink);

	    /*
	    ILayer l0 = IG.layer("Default").setColor(Color.red);
	    ILayer l1= IG.layer("Layer 01"); //.setColor(Color.orange);
	    ILayer l2 = IG.layer("layer2").setColor(Color.blue);
	    ILayer l3 = IG.layer("layer03").setColor(Color.orange);
	    ILayer l4 = IG.layer("layer 4").setColor(Color.pink);
	    */
	    
	    /*
	    //new IPoint(0,0,0);
	    new IPoint(0,0,0).layer(l0);
	    new IPoint(10,0,0).layer(l1).setColor(Color.blue);
	    new IPoint(10,10,0).layer(l2).setColor(Color.cyan);
	    new IPoint(10,10,10).layer(l3);
	    new IPoint(50,10,10).layer(l4);
	    
	    new ICurve(0,0,0,100,10,10).layer(l4);
	    
	    //ICircle cir = new ICircle(new IVec(0,0,0),new IVec(0,0,1),10).layer(l3);
	    //ICircle cir = new ICircle(new IVec(0,0,0),new IVec(0,0,1),10,false);
	    ICircle cir = new ICircle(0,0,0,10);
	    */
	    
	    /*
	      new ICurve(new IVec4[]{
			   new IVec4(0,0,0,1),
			   new IVec4(10,0,0,.5),
			   new IVec4(10,10,0,.5),
			   new IVec4(0,10,0,1),
			   new IVec4(0,0,0)
		       }, 3);
	    */
	    //double[][] cpts = {{0,0,0,1},{10,0,0,.5},{10,10,10,.5},{0,10,0,1}};
	    //new ICurve(cpts,3,true);
	    /*
	    double[][] cpts = {{0,0,0,1},{10,0,0,.5},{10,10,10,.5},{0,10,0,1}};
	    new ICurve(new double[][]{{0,0,0,1},{10,0,0,.5},{10,10,10,.5},{0,10,0,1}},
		       3,true);
	    */
	    
	    /*
	    new ISurface(new double[][][]{
			     { {0,0}, {10,0}, {10,10}, {0,10} },
			     { {-5,-5}, {15,-5}, {15,15}, {-5,15} },
			     { {-5,-5,10,}, {15,-5,10}, {15,15,10}, {-5,15,10} },
			 }, 2, 3, true, true);
	    */
	    
	    //ISurface srf = new ISurface(0,0,0,10,0,0,10,10,10,0,10,0);
	    ISurface srf = new ISurface(0,0,0,10,0,0,10,10,0,0,10,0);
	    
	    //ITrimCurve trim1 = new ITrimCurve(new double[][]{ {0,0}, {1.0,0.2}, {0.8,0.8}, {0.2,1.0}}, true);
	    ITrimCurve trim1 = new ITrimCurve(new double[][]{ {0.001,0.001}, {0.9999,0.2}, {0.8,0.8}, {0.2,0.9999}}, true);
	    //ITrimCurve trim1 = new ITrimCurve(new double[][]{ {0.2,0.2}, {0.8,0.2}, {0.8,0.8}, {0.2,0.8}}, 3, true);
	    //ITrimCurve trim1 = new ITrimCurve(new double[][]{ {0.2,0.2}, {0.2,0.8}, {0.8,0.8}, {0.8,0.2}}, true);
	    //ITrimCurve trim1 = new ITrimCurve(new double[][]{ {0.2,0.2}, {0.2,0.8},{0.8,0.8},{0.8,0.2}, }, true);
	    //ITrimCurve trim0 = new ITrimCurve(new double[][]{ {0,0,0.0}, {0.0,1.0},{1.,1},{1,0}, }, true);
	    ITrimCurve trim0 = new ITrimCurve(new double[][]{ {0,0}, {1,0},{1,1},{0,1}, },1, true);
	    //ITrimCurve trim0 = new ITrimCurve(new double[][]{ {0,0}, {0,1},{1,1},{1,0}, },3, true);
	    //srf.addOuterTrimLoop(trim1);
	    
	    srf.addOuterTrimLoop(trim0);
	    srf.addInnerTrimLoop(trim1);
	    
	    //ITrimCurve trim1 = new ITrimCurve(srf,new double[][]{ {0.2,0.2}, {0.2,0.8} });
	    //ITrimCurve trim2 = new ITrimCurve(srf,new double[][]{ {0.2,0.8}, {0.8,0.8} });
	    //ITrimCurve trim3 = new ITrimCurve(srf,new double[][]{ {0.8,0.8}, {0.8,0.2} });
	    //ITrimCurve trim4 = new ITrimCurve(srf,new double[][]{ {0.2,0.8}, {0.2,0.2} });
	    //srf.addInnerTrimLoop(new ITrimCurve[]{ trim1, trim2, trim3, trim4 });
	    //ITrimCurve trim1 = new ITrimCurve(srf,new double[][]{ {0,0}, {1,0} });
	    //ITrimCurve trim2 = new ITrimCurve(srf,new double[][]{ {1,0}, {2./3,1./3}, {1./3,2./3},{0,1} },3);
	    //ITrimCurve trim3 = new ITrimCurve(srf,new double[][]{ {0,1}, {0,0} });
	    //srf.addInnerTrimLoop(new ITrimCurve[]{ trim1, trim2, trim3 });
	    //srf.addOuterTrimLoop(new ITrimCurve[]{ trim2, trim3, trim1 });
	    
	    
	    //for(double z=0; z<10; z+=0.1)new ICircle(new IVec(0,0,z),new IVec(0,0,1),Math.sin(z)*10).layer(l1);
	    
	    try{
		IRhino3dmExporter.write(new File(args[0]),IG.current());
		//IObjectFileExporter.write(new File(args[0]),IG.current());
	    }catch(Exception e){ e.printStackTrace(); }
	    
	    
	    //IOut.p("circle cp = ");
	    //for(int i=0; i<cir.num(); i++) IOut.p(cir.cp(i));
	    
	}
    }
    
    
}