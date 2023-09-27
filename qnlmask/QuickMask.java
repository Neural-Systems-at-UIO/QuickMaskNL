package qnlmask;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import data.Series;
import data.Slice;
import nonlin.Triangle;
import parsers.JSON;
import png.PngWriter;

public class QuickMask {
	public static boolean gathering = false;
	public static boolean scattering = false;

	public static void main(String[] args) /* throws Exception */ {
		if (args.length == 11) {
			try {
				generateMasks(args);
			} catch (Exception ex) {
				System.err.println(ex);
				System.exit(1);
			}
			return;
		}
		final JFrame frame = new JFrame("QuickMask Emergency UI");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		final JLabel file = new JLabel();
		c.gridwidth = 2;
		frame.add(file, c);
		JButton pickfile = new JButton("Pick JSON");
		frame.add(pickfile, c);
		pickfile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				jfc.setAcceptAllFileFilterUsed(false);
				jfc.setDialogTitle("Pick QuickNII/VisuAlign JSON");
				jfc.setFileFilter(new FileFilter() {
					@Override
					public String getDescription() {
						return "JSON";
					}

					@Override
					public boolean accept(File f) {
						String s = f.getName().toLowerCase();
						return f.isDirectory() || s.endsWith(".json");
					}
				});
				if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
					file.setText(jfc.getSelectedFile().getPath());
			}
		});
		c.weighty = 1;
		c.gridwidth = 1;
		JLabel tl = new JLabel("Top-left");
		c.gridx = 0;
		c.gridy = 2;
		frame.add(tl, c);
		JLabel tr = new JLabel("Top-right");
		c.gridy++;
		frame.add(tr, c);
		JLabel bl = new JLabel("Bottom-left");
		c.gridy++;
		frame.add(bl, c);
		JLabel x = new JLabel("x");
		c.gridx++;
		c.gridy = 1;
		c.fill = GridBagConstraints.CENTER;
		frame.add(x, c);
		JLabel y = new JLabel("y");
		c.gridx++;
		frame.add(y, c);
		JLabel z = new JLabel("z");
		c.gridx++;
		frame.add(z, c);
		c.fill = GridBagConstraints.BOTH;
		final JTextField combined = new JTextField();
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 4;
		frame.add(combined, c);
		c.gridwidth = 1;
		final JTextField corn[] = new JTextField[9];
		DocumentListener gather = new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				gather();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				gather();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				gather();
			}

			public void gather() {
				if (scattering)
					return;
				gathering = true;
				String s = corn[0].getText();
				for (int i = 1; i < corn.length; i++)
					s += " " + corn[i].getText();
				combined.setText(s);
				gathering = false;
			}
		};
		for (int j = 0; j < 3; j++)
			for (int i = 0; i < 3; i++) {
				JTextField jtf = corn[i + j * 3] = new JTextField();
				jtf.getDocument().addDocumentListener(gather);
				jtf.setHorizontalAlignment(JTextField.CENTER);
				c.gridx = i + 1;
				c.gridy = j + 2;
				frame.add(jtf, c);
			}
		combined.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				scatter();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				scatter();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				scatter();
			}

			public void scatter() {
				if (gathering)
					return;
				scattering = true;
				String items[] = combined.getText().trim().split("\\s+");
				for (int i = 0; i < corn.length; i++)
					corn[i].setText(i < items.length ? items[i] : "");
				scattering = false;
			}
		});
		final JLabel dir = new JLabel();
		c.weighty = 0;
		c.gridwidth = 2;
		c.gridy = 6;
		c.gridx = 0;
		frame.add(dir, c);
		JButton pickdir = new JButton("Destination");
		c.gridx = 2;
		frame.add(pickdir, c);
		pickdir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.setDialogTitle("Pick output folder");
				if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
					dir.setText(jfc.getSelectedFile().getPath());
			}
		});
		JButton go = new JButton("Go");
		c.gridwidth = 4;
		c.gridy = 7;
		c.gridx = 0;
		frame.add(go, c);
		frame.setSize(400, 300);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		go.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				try {
					generateMasks(new String[] { file.getText(), corn[0].getText(), corn[1].getText(),
							corn[2].getText(), corn[3].getText(), corn[4].getText(), corn[5].getText(),
							corn[6].getText(), corn[7].getText(), corn[8].getText(), dir.getText() });
					JOptionPane.showMessageDialog(null, "Everything went fine", "All good",
							JOptionPane.INFORMATION_MESSAGE);
					frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, ex.toString(), "Something went wrong",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	public static void generateMasks(String args[]) throws Exception {
		final double cx = Double.parseDouble(args[1]);
		final double cy = Double.parseDouble(args[2]);
		final double cz = Double.parseDouble(args[3]);
		final double nux = Double.parseDouble(args[4]) - cx;
		final double nuy = Double.parseDouble(args[5]) - cy;
		final double nuz = Double.parseDouble(args[6]) - cz;
		final double nvx = Double.parseDouble(args[7]) - cx;
		final double nvy = Double.parseDouble(args[8]) - cy;
		final double nvz = Double.parseDouble(args[9]) - cz;
		final double nx = nuy * nvz - nvy * nuz;
		final double ny = nuz * nvx - nvz * nux;
		final double nz = nux * nvy - nvx * nuy;

		File f = new File(args[0]);
		Series series = new Series();
		try (FileReader fr = new FileReader(f)) {
			Map<String, String> resolver = new HashMap<>();
			resolver.put("resolution", "target-resolution");
			JSON.mapObject(JSON.parse(fr), series, resolver);
		}
		if (series.slices.size() < 1) {
			throw new Exception(f.getName() + " is not compatible with VisuAlign.");
		}
		series.propagate();

		for (Slice slice : series.slices) {
			double ox = slice.anchoring.get(0);
			double oy = slice.anchoring.get(1);
			double oz = slice.anchoring.get(2);
			double ux = slice.anchoring.get(3);
			double uy = slice.anchoring.get(4);
			double uz = slice.anchoring.get(5);
			double vx = slice.anchoring.get(6);
			double vy = slice.anchoring.get(7);
			double vz = slice.anchoring.get(8);
			int width = (int) Math.sqrt(ux * ux + uy * uy + uz * uz) + 1;
			int height = (int) Math.sqrt(vx * vx + vy * vy + vz * vz) + 1;
			byte mask[][] = new byte[height * 3][width * 3];
			for (int v = -height; v < height * 2; v++)
				for (int u = -width; u < width * 2; u++)
					mask[v + height][u + width] = (ox + ux * u / width + vx * v / height - cx) * nx
							+ (oy + uy * u / width + vy * v / height - cy) * ny
							+ (oz + uz * u / width + vz * v / height - cz) * nz > 0 ? (byte) 255 : 0;
			FileOutputStream fos = new FileOutputStream(args[10] + File.separator
					+ slice.filename.substring(0, slice.filename.lastIndexOf('.')) + "_mask.png");
			PngWriter png = new PngWriter(fos, width, height, PngWriter.TYPE_GRAYSCALE, null);

			slice.triangulate();
			List<Triangle> triangles = slice.triangles;

			for (int v = 0; v < height; v++) {
				byte line[] = new byte[width];
				for (int u = 0; u < width; u++) {
					double fx = u * slice.width / width;
					double fy = v * slice.height / height;
					for (Triangle triangle : triangles) {
						double t[] = triangle.transform(fx, fy);
						if (t != null) {
							int xx = (int) (t[0] * width / slice.width);
							int yy = (int) (t[1] * height / slice.height);
							if (xx >= -width && yy >= -height && xx < width * 2 && yy < height * 2)
								line[u] = mask[yy + height][xx + width];
							break;
						}
					}
				}
				png.writeline(line);
			}
			fos.close();
		}
	}
}
