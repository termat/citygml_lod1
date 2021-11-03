package citygml_lod1.components;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.awt.ShapeReader;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiLineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class GeojsonProcesser {
	private List<Feature> list;
	private GeoJsonReader geomReader;
	private GeoJsonWriter geomWriter;
	private ShapeReader shapeReader;
	private ShapeWriter shapeWriter;
	private List<Shape> shapes;
	private Rectangle2D bounds;
	
	public GeojsonProcesser(FeatureCollection fc) {
		list=fc.features();
		geomReader=new GeoJsonReader();
		geomWriter=new GeoJsonWriter();
		shapeReader=new ShapeReader(new GeometryFactory());
		shapeWriter=new ShapeWriter();
		createShape();
	}
	
	public GeojsonProcesser(File f) throws IOException {
		this(FeatureCollection.fromJson(getString(f)));
	}
	
	public static String getString(File f) throws IOException{
		StringBuffer sb=new StringBuffer();
		String line=null;
		BufferedReader br=new BufferedReader(new FileReader(f));
		while((line=br.readLine())!=null){
			sb.append(line);
		}
		br.close();
		return sb.toString();
	}
	
	public FeatureCollection getFeatureCollection() {
		return FeatureCollection.fromFeatures(list);
	}
	
	public List<Feature> getFeatures() {
		return list;
	}
	
	public Feature getFeature(int i) {
		return list.get(i);
	}
	
	public JsonObject getProperty(int i){
		return list.get(i).properties();
	}
	
	public Geometry getGeometry(int i) {
		return list.get(i).geometry();
	}
	
	public Feature getFeature(Point2D p) {
		if(!bounds.contains(p))return null;
		int n=shapes.size();
		for(int i=0;i<n;i++) {
			Shape s=shapes.get(i);
			if(s.contains(p))return list.get(i);
		}
		return null;
	}
	
	public Feature getFeature(double x,double y) {
		if(!bounds.contains(x,y))return null;
		int n=shapes.size();
		for(int i=0;i<n;i++) {
			Shape s=shapes.get(i);
			if(s.contains(x,y))return list.get(i);
		}
		return null;
	}

	public List<Shape> getShapes(){
		return shapes;
	}
	
	public Shape getShape(int i) {
		return shapes.get(i);
	}
	
	public void updateShape() {
		int n=shapes.size();
		AffineTransform af=AffineTransform.getScaleInstance(1.0, 1.0);
		for(int i=0;i<n;i++) {
			Shape s=shapes.get(i);
			org.locationtech.jts.geom.Geometry g=shapeReader.read(s.getPathIterator(af));
			Geometry geo=reverseGeom(g);
			Feature f=list.get(i);
			Feature f2=Feature.fromGeometry(geo, f.properties());
			list.remove(i);
			list.add(i, f2);
		}
	}
	
	private void createShape(){
		bounds=null;
		shapes=new ArrayList<>();
		for(Feature f : list) {
			org.locationtech.jts.geom.Geometry g=transGeom(f.geometry());
			Shape sp=shapeWriter.toShape(g);
			shapes.add(sp);
			if(bounds==null) {
				bounds=sp.getBounds2D();
			}else {
				bounds.add(sp.getBounds2D());
			}
		}
	}
	
	private org.locationtech.jts.geom.Geometry transGeom(com.mapbox.geojson.Geometry geo){
		try {
			org.locationtech.jts.geom.Geometry geom=geomReader.read(geo.toJson());
			return geom;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private com.mapbox.geojson.Geometry reverseGeom(org.locationtech.jts.geom.Geometry geo){
		String json=geomWriter.write(geo);
		String type=geo.getGeometryType();
		if(type.equals(org.locationtech.jts.geom.Geometry.TYPENAME_LINESTRING)) {
			return LineString.fromJson(json);
		}else if(type.equals(org.locationtech.jts.geom.Geometry.TYPENAME_POINT)) {
			return Point.fromJson(json);
		}else if(type.equals(org.locationtech.jts.geom.Geometry.TYPENAME_POLYGON)) {
			return Polygon.fromJson(json);
		}else if(type.equals(org.locationtech.jts.geom.Geometry.TYPENAME_MULTILINESTRING)) {
			return MultiLineString.fromJson(json);
		}else if(type.equals(org.locationtech.jts.geom.Geometry.TYPENAME_MULTIPOINT)) {
			return MultiPoint.fromJson(json);
		}else if(type.equals(org.locationtech.jts.geom.Geometry.TYPENAME_MULTIPOLYGON)) {
			return MultiPolygon.fromJson(json);
		}
		return null;
	}
}
