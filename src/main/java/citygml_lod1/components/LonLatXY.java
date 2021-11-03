package citygml_lod1.components;

import java.awt.geom.Point2D;

public class LonLatXY {
	private static final double a=6378137;
	private static final double rf=298.257222101;
	private static final double m0=0.9999;
	private static final double s2r=Math.PI/648000;
	private static final double n=0.5/(rf-0.5);
	private static final double n15=1.5*n;
	private static final double anh=0.5*a/(1+n);
	private static final double nsq=n*n;
	private static final double e2n=2*Math.sqrt(n)/(1+n);
	private static final double ra=2*anh*m0*(1+nsq/4+nsq*nsq/64);
	private static int jt=5;
	private static int jt2=2*jt;
	private static double ep=1.0;
	private static double[] e=getE();

	// 平面直角座標の座標系原点の緯度を度単位で、経度を分単位で格納
	private static final double[] phi0=new double[]{0,33,33,36,33,36,36,36,36,36,40,44,44,44,26,26,26,26,20,26};
	private static final double[] lmbd0=new double[]{0,7770,7860,7930,8010,8060,8160,8230,8310,8390,8450,8415,8535,8655,8520,7650,7440,7860,8160,9240};

	private static double[] alp=getAlp();
	private static double[] beta=getBeta();
	private static double[] dlt=getDlt();


	private static double[] getAlp(){
		double[] alp=new double[6];
		alp[1]=(1.0/2.0+(-2.0/3.0+(5.0/16.0+(41.0/180.0-127.0/288.0*n)*n)*n)*n)*n;
		alp[2]=(13.0/48.0+(-3.0/5.0+(557.0/1440.0+281.0/630.0*n)*n)*n)*nsq;
		alp[3]=(61.0/240.0+(-103.0/140.0+15061.0/26880.0*n)*n)*n*nsq;
		alp[4]=(49561.0/161280.0-179.0/168.0*n)*nsq*nsq;
		alp[5]=34729.0/80640.0*n*nsq*nsq;
		return alp;
	}

	private static double[] getBeta(){
		double[] beta=new double[6];
		beta[1]=(1.0/2.0+(-2.0/3.0+(37.0/96.0+(-1.0/360.0-81.0/512.0*n)*n)*n)*n)*n;
		beta[2]=(1.0/48.0+(1.0/15.0+(-437.0/1440.0+46.0/105.0*n)*n)*n)*nsq;
		beta[3]=(17.0/480.0+(-37.0/840.0-209.0/4480.0*n)*n)*n*nsq;
		beta[4]=(4397.0/161280.0-11.0/504.0*n)*nsq*nsq;
		beta[5]=4583.0/161280.0*n*nsq*nsq;
		return beta;
	}

	private static double[] getDlt(){
		double[] dlt=new double[7];
		dlt[1]=(2.0+(-2.0/3.0+(-2.0+(116.0/45.0+(26.0/45.0-2854.0/675.0*n)*n)*n)*n)*n)*n;
		dlt[2]=(7.0/3.0+(-8.0/5.0+(-227.0/45.0+(2704.0/315.0+2323.0/945.0*n)*n)*n)*n)*nsq;
		dlt[3]=(56.0/15.0+(-136.0/35.0+(-1262.0/105.0+73814.0/2835.0*n)*n)*n)*n*nsq;
		dlt[4]=(4279.0/630.0+(-332.0/35.0-399572.0/14175.0*n)*n)*nsq*nsq;
		dlt[5]=(4174.0/315.0-144838.0/6237.0*n)*n*nsq*nsq;
		dlt[6]=601676.0/22275.0*nsq*nsq*nsq;
		return dlt;
	}

	private static double[] getE(){
		double[] e=new double[jt2+2];
		for(int k=1;k<=jt;k++){
			ep*=e[k]=n15/k-n;
			e[k+jt]=n15/(k+jt)-n;
		}
		return e;
	}

	private LonLatXY() {}
	
	@SuppressWarnings("unused")
	public static Point2D xyToLonlat(int num,double xx,double yy){
		double x=yy;
		double y=xx;
		double xi=(x+m0*Merid(2*phi0[num]*3600*s2r))/ra;
		double xip=xi;
		double eta=y/ra;
		double etap=eta;
		double sgmp=1;
		double taup=0;
		for(int j=beta.length-1;j>0;j--){
			double besin=beta[j]*Math.sin(2*j*xi);
			double becos=beta[j]*Math.cos(2*j*xi);
			xip -=besin*Math.cosh(2*j*eta);
			etap -=becos*Math.sinh(2*j*eta);
			sgmp -=2*j*becos*Math.cosh(2*j*eta);
			taup +=2*j*besin*Math.sinh(2*j*eta);
		}
		double sxip=Math.sin(xip);
		double cxip=Math.cos(xip);
		double shetap=Math.sinh(etap);
		double chetap=Math.cosh(etap);
		double chi=Math.asin(sxip/chetap);
		double phi=chi;
		for(int j=dlt.length-1;j>=0;j--){
			phi +=dlt[j]*Math.sin(2*j*chi);
		}
		double nphi=(1-n)/(1+n)*Math.tan(phi);

		double lmbd=lmbd0[num]*60+Math.atan2(shetap, cxip)/s2r;
		double gmm=Math.atan2(taup*cxip*chetap+sgmp*sxip*shetap,sgmp*cxip*chetap-taup*sxip*shetap);//真北方向角
		double m=ra/a*Math.sqrt((cxip*cxip+shetap*shetap)/(sgmp*sgmp+taup*taup)*(1+nphi*nphi));//縮尺係数
		double lat=phi/s2r/3600;
		double lon=lmbd/3600;
		return new Point2D.Double(lon,lat);
	}

	@SuppressWarnings("unused")
	public static Point2D lonlatToXY(int num,double lon,double lat){
		double phirad=Math.toRadians(lat);
		double lmbddeg=Math.floor(lon);
		double lmbdmin=Math.floor(60.0*(lon-lmbddeg));
		double lmbdsec=lmbddeg*3600.0+lmbdmin*60.0+(lon-lmbddeg-lmbdmin/60)*3600.0;

		double sphi=Math.sin(phirad);
		double nphi=(1-n)/(1+n)*Math.tan(phirad);
		double dlmbd=(lmbdsec-lmbd0[num]*60.0)*s2r;
		double sdlmbd=Math.sin(dlmbd);
		double cdlmbd=Math.cos(dlmbd);
		double tchi=Math.sinh(atanh(sphi)-e2n*atanh(e2n*sphi));
		double cchi=Math.sqrt(1+tchi*tchi);
		double xip=Math.atan2(tchi, cdlmbd);
		double xi=xip;
		double etap=atanh(sdlmbd/cchi);
		double eta=etap;
		double sgm=1;
		double tau=0;
		for(int j=alp.length-1;j>0;j--){
			double alsin=alp[j]*Math.sin(2*j*xip);
			double alcos=alp[j]*Math.cos(2*j*xip);
			xi +=alsin*Math.cosh(2*j*etap);
			eta +=alcos*Math.sinh(2*j*etap);
			sgm +=2*j*alcos*Math.cosh(2*j*etap);
			tau +=2*j*alsin*Math.sinh(2*j*etap);
		}
		double y=ra*xi-m0*Merid(2*phi0[num]*3600*s2r);
		double x=ra*eta;
		double gmm=Math.atan2(tau*cchi*cdlmbd+sgm*tchi*sdlmbd, sgm*cchi*cdlmbd-tau*tchi*sdlmbd);//真北方向角
		double m=ra/a*Math.sqrt((sgm*sgm+tau*tau)/(tchi*tchi+cdlmbd*cdlmbd)*(1+nphi*nphi));//縮尺係数
		return new Point2D.Double(x,y);
	}

	// 該当緯度の2 倍角の入力により赤道からの子午線弧長を求める関数
	private static double Merid(double phi2) {
			double dc=2.0*Math.cos(phi2);
			double[] s=new double[jt2+2];
			double[] t=new double[jt2+2];
			s[1]=Math.sin(phi2);
			for(int i=1;i<=jt2;i++){
				s[i+1]=dc*s[i]-s[i-1];
				t[i]=(1.0/i-4.0*i)*s[i];
			}
			double sum=0.0;
			double c1=ep;
			int j=jt;
			while(j>0){
				double c2=phi2;
				double c3=2.0;
				int l=j;
				int m=0;
				while(l>0){
					c2 +=(c3/=e[l--])*t[++m]+(c3*=e[2*j-l])*t[++m];
				}
				sum +=c1*c1*c2 ; c1/=e[j--];
			}
			return anh*(sum+phi2);
	}

	private static double atanh(double v){
		return 0.5*Math.log((1.0+v)/(1.0-v));
	}
}

