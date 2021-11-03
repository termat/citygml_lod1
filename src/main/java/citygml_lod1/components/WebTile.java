package citygml_lod1.components;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;


public class WebTile {
	private static final double L=85.05112877980659;
	private String url;
	private Map<Point2D,BufferedImage> tiles;
	private BufferedImage img;
	private AffineTransform af;
	private int zoom;
	private double resolution;
	
	public WebTile(String url,int zoom,double res){
		this.url=url;
		this.zoom=zoom;
		this.resolution=res;
		tiles=new HashMap<>();
	}
	
	public void create(int coordSys,Rectangle2D xy)throws IOException{
		int w=(int)Math.ceil(xy.getWidth()/resolution);
		int h=(int)Math.ceil(xy.getHeight()/resolution);
		if(w%2==1)w++;
		if(h%w==1)h++;
		double[] param=new double[]{
				resolution,0,0,-resolution,xy.getX(),xy.getY()+xy.getHeight()};
		af=new AffineTransform(param);
		img=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<w;i++){
			for(int j=0;j<h;j++){
				Point2D pxy=af.transform(new Point2D.Double(i,j),new Point2D.Double());
				Point2D lonlat=LonLatXY.xyToLonlat(coordSys, pxy.getX(), pxy.getY());
				Point2D pixel=lonlatToPixel(zoom,lonlat);
				Point2D tile=new Point2D.Double(Math.floor(pixel.getX()/256),Math.floor(pixel.getY()/256));
				if(tiles.containsKey(tile)){
					BufferedImage tmp=tiles.get(tile);
					if(tmp!=null){
						int xx=(int)pixel.getX()%256;
						int yy=(int)pixel.getY()%256;
						img.setRGB(i, j, tmp.getRGB(xx, yy));
					}
				}else{
					BufferedImage tmp=getTile(zoom,(long)tile.getX(),(long)tile.getY());
					if(tmp!=null){
						int xx=(int)pixel.getX()%256;
						int yy=(int)pixel.getY()%256;
						img.setRGB(i, j, tmp.getRGB(xx, yy));
					}
					tiles.put(tile, tmp);
				}
			}
		}
	}

	protected BufferedImage getTile(int zoom,long x,long y) {
		String uu=new String(url).replace("{z}", Integer.toString(zoom));
		uu=uu.replace("{x}", Long.toString(x));
		uu=uu.replace("{y}", Long.toString(y));
		try {
			HttpsURLConnection con=(HttpsURLConnection)new URL(uu).openConnection();
	        SSLContext sslContext = SSLContext.getInstance("SSL");
	        sslContext.init(null,
	                        new X509TrustManager[] { new LooseTrustManager() },
	                        new SecureRandom());

	        con.setSSLSocketFactory(sslContext.getSocketFactory());
	        con.setHostnameVerifier(new LooseHostnameVerifier());
	        BufferedImage tmp=ImageIO.read(con.getInputStream());
			if(tmp!=null)return tmp;
		}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public BufferedImage getImage() {
		return img;
	}

	public AffineTransform getTransform() {
		return af;
	}

	private static Point2D lonlatToPixel(int zoom,Point2D p){
		long x=(long)(Math.pow(2, zoom+7)*(p.getX()/180.0+1.0));
		long y=(long)((Math.pow(2, zoom+7)/Math.PI)*(-atanh(Math.sin(Math.toRadians(p.getY())))+atanh(Math.sin(Math.toRadians(L)))));
		return new Point2D.Double(x,y);
	}

	private static double atanh(double v){
		return 0.5*Math.log((1.0+v)/(1.0-v));
	}

	public static void main(String[] args){
		Rectangle2D rect=new Rectangle2D.Double(-55121.73598,-85147.39639,707.516947,512.3398582);
		try{
			String url="https://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png";
			WebTile app=new WebTile(url,18,0.5);
			app.create(8, rect);
			ImageIO.write(app.getImage(), "png", new File("test.png"));
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private static class LooseTrustManager implements X509TrustManager {
	    @Override
	    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
	 
	    @Override
	    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
	 
	    @Override
	    public X509Certificate[] getAcceptedIssuers() {
	        return null;
	    }
	}
	
	private static class LooseHostnameVerifier implements HostnameVerifier {
	    public boolean verify(String hostname, SSLSession session) {
	        return true;
	    }
	}
}
