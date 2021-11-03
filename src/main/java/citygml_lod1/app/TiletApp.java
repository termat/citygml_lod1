package citygml_lod1.app;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import citygml_lod1.components.GeojsonProcesser;
import citygml_lod1.components.WebTile;

public class TiletApp {
	private JFrame frame;
	private JLabel label;
	private String dsm="https://gio.pref.hyogo.lg.jp/tile/dsm/{z}/{y}/{x}.png";
	private String dem="https://gio.pref.hyogo.lg.jp/tile/dem/{z}/{y}/{x}.png";
	
	public TiletApp() {
		frame=new JFrame();
		frame.setTitle("DMgetApp");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			SwingUtilities.updateComponentTreeUI(frame);
		}catch(Exception e){
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
				SwingUtilities.updateComponentTreeUI(frame);
			}catch(Exception ee){
				ee.printStackTrace();
			}
		}
		WindowAdapter wa=new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		};
		frame.addWindowListener(wa);
		frame.setSize(400,400);
		frame.setResizable(false);
		label=new JLabel("File Drop");
		label.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,24));
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(label,BorderLayout.CENTER);
		DropTargetListener dtl = new DropTargetAdapter() {
			  @Override public void dragOver(DropTargetDragEvent dtde) {
			    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			      dtde.acceptDrag(DnDConstants.ACTION_COPY);
			      return;
			    }
			    dtde.rejectDrag();
			  }
			  @SuppressWarnings("rawtypes")
			  @Override
			  public void drop(DropTargetDropEvent dtde) {
			    try {
			      if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			        dtde.acceptDrop(DnDConstants.ACTION_COPY);
			        Transferable transferable = dtde.getTransferable();
			        List list = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
			        for (Object o: list) {
			          if (o instanceof File) {
			            File f=(File) o;
			            if(f.getName().toLowerCase().endsWith(".geojson")) {
			            	proc(f);
			            }
			          }
			        }
			        dtde.dropComplete(true);
			        return;
			      }
			    } catch (UnsupportedFlavorException | IOException ex) {
			      ex.printStackTrace();
			    }
			    dtde.rejectDrop();
			  }
			};
			new DropTarget(label, DnDConstants.ACTION_COPY, dtl, true);
	}
	
	private void proc(File f) {
		Runnable r=new Runnable() {
			public void run() {
				try {
					setMessage("領域確認...");
					GeojsonProcesser gp = new GeojsonProcesser(f);
					Rectangle2D rect=null;
					for(Shape s : gp.getShapes()) {
						if(rect==null) {
							rect=s.getBounds2D();
						}else {
							rect.add(s.getBounds2D());
						}
					}
					setMessage("DSM取得...");
					WebTile tile=new WebTile(dsm,17,1);
					tile.create(5, rect);
					ImageIO.write(tile.getImage(), "png", new File(f.getAbsolutePath().replace(".geojson", "_dsm.png")));
					outTransform(tile.getTransform(),new File(f.getAbsolutePath().replace(".geojson", "_dsm.pgw")));
					setMessage("DEM取得...");
					tile=new WebTile(dem,17,1);
					tile.create(5, rect);
					ImageIO.write(tile.getImage(), "png", new File(f.getAbsolutePath().replace(".geojson", "_dem.png")));
					outTransform(tile.getTransform(),new File(f.getAbsolutePath().replace(".geojson", "_dem.pgw")));
					setMessage("終了...");
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		};
		new Thread(r).start();
	}
	
	private void outTransform(AffineTransform af,File path) throws IOException {
		BufferedWriter bw=new BufferedWriter(new FileWriter((path)));
		bw.write(af.getScaleX()+"\n");
		bw.write(af.getShearX()+"\n");
		bw.write(af.getShearY()+"\n");
		bw.write(af.getScaleY()+"\n");
		bw.write(af.getTranslateX()+"\n");
		bw.write(af.getTranslateY()+"\n");
		bw.flush();
		bw.close();
	}
	
	private void setMessage(String args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				label.setText(args);
			}
		});
	}
	
	private void close(){
		int id=JOptionPane.showConfirmDialog(frame, "Exit?", "Info", JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);
		if(id==JOptionPane.YES_OPTION){
			frame.setVisible(false);
			System.exit(0);
		}
	}
	
	public static void main(String[] args) {
		TiletApp app=new TiletApp();
		app.frame.setLocationRelativeTo(null);
		app.frame.setVisible(true);
	}
}
