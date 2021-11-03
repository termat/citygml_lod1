package citygml_lod1.app;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import citygml_lod1.components.GeojsonProcesser;

public class GeoJsonSetDCHM {

	public static void main(String[] args) {
		try {
			GeojsonProcesser geo=new GeojsonProcesser(new File("data/sannomiya.geojson"));
			BufferedImage dsm=ImageIO.read(new File("data/sannomiya_dsm.png"));
			BufferedImage dem=ImageIO.read(new File("data/sannomiya_dem.png"));
			AffineTransform af=loadTransform(new File("data/sannomiya_dsm.pgw"));
			try {
				af=af.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			int n=geo.getShapes().size();
			for(int i=0;i<n;i++) {
				if(i%10000==0)System.out.println(i);
				Shape s=geo.getShape(i);
				Point2D p=getPoint(s);
				double dsmv=getZ(dsm,af,p);
				double demv=getZ(dem,af,p);
				geo.getProperty(i).addProperty("DEM", demv);
				geo.getProperty(i).addProperty("DCHM", dsmv-demv);
			}
			BufferedWriter bw=new BufferedWriter(new FileWriter(new File("data/sannomiya_gml.geojson")));
			bw.write(geo.getFeatureCollection().toJson());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private static double getZ(BufferedImage img,AffineTransform af,Point2D p) {
		Point2D px=af.transform(p, new Point2D.Double());
		int xx=(int)Math.round(px.getX());
		int yy=(int)Math.round(px.getY());
		if(xx>=0&&xx<img.getWidth()&&yy>=0&&yy<img.getHeight()) {
			return getVal(img.getRGB(xx, yy));
		}else {
			return 0.0;
		}
	}
	
	public static double getVal(int color){
		color=(color << 8) >> 8;
		if(color==8388608||color==-8388608){
			return Double.NaN;
		}else if(color<8388608){
			return color * 0.01;
		}else{
			return (color-16777216)*0.01;
		}
	}
	
	private static Point2D getPoint(Shape s) {
		Rectangle2D r=s.getBounds2D();
		double xx=0.0,yy=0.0;
		boolean flg=true;
		while(flg) {
			xx=Math.random()*r.getWidth()+r.getX();
			yy=Math.random()*r.getHeight()+r.getY();
			if(s.contains(xx, yy))flg=false;
		}
		return new Point2D.Double(xx,yy);
	}
	
	public static AffineTransform loadTransform(File path)throws IOException{
		BufferedReader br=new BufferedReader(new FileReader(path));
		List<Double> dd=new ArrayList<Double>();
		String line=null;
		while((line=br.readLine())!=null){
			double d=Double.parseDouble(line);
			dd.add(d);
		}
		br.close();
		double[] p=new double[dd.size()];
		for(int i=0;i<p.length;i++){
			p[i]=dd.get(i);
		}
		return new AffineTransform(p);
	}
}
