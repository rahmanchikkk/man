package com.rahmanchikkk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.ImageIcon;

public class driver extends JFrame {
	
	private JPanel panel;
	private JTable table;
	private JButton calculate;
	private JButton back;
	private JButton delete;
	private JButton export;
	private JTextField minInterval;
	private JTextField maxInterval;
	private JLabel minLabel;
	private JScrollPane scrollPane;
	private JLabel maxLabel;
	private String[] columns = {"Очікувана ціна", "NPV", "Середньоквадратичне відхилення", "Коефійієнт варіації"};
	private TableModelListener tableModelListener;
	private String currentDirectory;
	
	public JButton getButtonImage(String path, Dimension size) {
		ImageIcon icon = new ImageIcon(currentDirectory + "\\" + path);
		Image img = icon.getImage().getScaledInstance(size.width, size.height, DO_NOTHING_ON_CLOSE);
		icon = new ImageIcon(img);
		JButton btn = new JButton(icon);
		btn.setBorder(null);
		btn.setBackground(Color.white);
		return btn;
	}
	
	public driver() {
		DefaultTableModel model = new DefaultTableModel(columns, 0);
		setResizable(false);
		table = new JTable(model);
		panel = new JPanel();
		panel.setBackground(Color.white);
		currentDirectory = System.getProperty("user.dir");
		
		try {
			createSQL();
			populateSQL(table, -1, 10000);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		scrollPane = new JScrollPane(table);
		scrollPane.setBounds(15, 75, 750, 400);
		scrollPane.setBorder(null);
		
		Action action = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent arg0) {}
		};
		
		TableCellListener tcl = new TableCellListener(table, action);
		table.setCellSelectionEnabled(true);
		table.setPreferredScrollableViewportSize(new Dimension(400, 400));
		table.setFillsViewportHeight(true);
		
		minInterval = new JTextField();
		maxInterval = new JTextField();
		minLabel = new JLabel("Enter minimum cost:");
		maxLabel = new JLabel("Enter maximum cost:");
		
		delete = new JButton("delete");
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (table.getSelectedRow() > -1) {
					try {
						deleteSQL(String.valueOf(table.getSelectedRow() + 1));
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					} catch (SQLException e2) {
						e2.printStackTrace();
					}
				}
			}
		});
		
		export = getButtonImage("excel.png", new Dimension(50, 50));
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ExcelExporter exporter = new ExcelExporter();
					exporter.fillData(table, new File(currentDirectory + "\\result.xls"));
					JOptionPane.showMessageDialog(null, "Data saved at " + "'result.xls' successfully", "Message", 
						JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		calculate = new JButton("calculate");
		calculate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int min = Integer.parseInt(minInterval.getText());
					int max = Integer.parseInt(maxInterval.getText());
					populateSQL(table, min, max);
				} catch (NumberFormatException e1) {
					JOptionPane.showMessageDialog(null, "Invalid interval!", "Message", JOptionPane.INFORMATION_MESSAGE);
					return;
				} catch (SQLException | ClassNotFoundException e2) {
					e2.printStackTrace();
					return;
				}
				calculate.setVisible(false);
				minLabel.setVisible(false);
				maxLabel.setVisible(false);
				minInterval.setVisible(false);
				maxInterval.setVisible(false);
				scrollPane.setVisible(true);
				delete.setBounds(350, 500, 100, 30);
				export.setBounds(700, 10, 50, 50);
				delete.setVisible(true);
				export.setVisible(true);
				back.setVisible(true);
			}
		});
		
		calculate.setBounds(300, 300, 100, 30);
		calculate.setVisible(true);
		
		back = getButtonImage("back.png", new Dimension(100, 50));
		back.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent e) {
				delete.setVisible(false);
				export.setVisible(false);
				calculate.setVisible(true);
				minInterval.setVisible(true);
				maxInterval.setVisible(true);
				minLabel.setVisible(true);
				maxLabel.setVisible(true);
				scrollPane.setVisible(false);
				back.setVisible(false);
			}
		});
		back.setBounds(10, 10, 100, 50);
		
		tableModelListener = new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					if (String.valueOf(tcl.getOldValue()).equals(
							String.valueOf(table.getModel().getValueAt(e.getFirstRow(), e.getColumn())))) {
						System.out.println("SAME");
					} else {
						String cost = String.valueOf(table.getModel().getValueAt(e.getFirstRow(), 0));
						String npv = String.valueOf(table.getModel().getValueAt(e.getFirstRow(), 1));
						String quadraticDeviation = String.valueOf(table.getModel().getValueAt(e.getFirstRow(), 2));
						String coef = String.valueOf(table.getModel().getValueAt(e.getFirstRow(), 3));
						try {
							updateSQL(cost, npv, quadraticDeviation, coef);
						} catch (ClassNotFoundException | SQLException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		};
		table.getModel().addTableModelListener(tableModelListener);
	}
	
	public void populateSQL(JTable table, int min, int max) throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:manbase.db");
		Statement stat = conn.createStatement();
		ResultSet rs = stat.executeQuery("SELECT rowid, * FROM investments WHERE CAST(cost as INT) >= "+min+" AND CAST(cost as INT) <= "+max+";");
		DefaultTableModel m = (DefaultTableModel)table.getModel();
		m.setRowCount(0);
		while (rs.next()) {
			Object[] row = new Object[columns.length];
			for (int i = 1; i <= columns.length; i++) {
				row[i-1] = rs.getObject(i);
			}
			((DefaultTableModel)table.getModel()).insertRow(rs.getRow() - 1, row);
		}
		rs.close();
		conn.close();
	}
	
	public void deleteSQL(String cost) throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:manbase.db");
		Statement stat = conn.createStatement();
		stat.executeUpdate("DELETE FROM investments WHERE cost = "+cost+";");
		((DefaultTableModel)table.getModel()).removeRow(Integer.parseInt(cost) - 1);
	}
	
	public void updateSQL(String cost, String npv, String deviation, String coef) throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:manbase.db");
		Statement stat = conn.createStatement();
		stat.executeUpdate("UPDATE investments SET npv = '"+npv+"', deviation = '"+deviation+"', coef = '"+coef+"' WHERE cost = "+cost+";");
	}
	public void createSQL() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:manbase.db");
		Statement stat = conn.createStatement();
		stat.executeUpdate("CREATE TABLE IF NOT EXISTS investments (cost, npv, deviation, coef)");
		Statement delStatement = conn.createStatement();
		delStatement.executeUpdate("DELETE FROM investments");
		PreparedStatement prep = conn.prepareStatement("INSERT INTO investments VALUES (?, ?, ?, ?);");
		for (int i = 1; i < 100; i++) {
			prep.setString(1, String.valueOf(i));
			prep.setString(2, String.valueOf(2 * i));
			prep.setString(3, String.valueOf(3 * i));
			prep.setString(4, String.valueOf(4 * i));
			prep.addBatch();
		}
		conn.setAutoCommit(false);
		prep.executeBatch();
		conn.setAutoCommit(true);
		conn.close();
	}
	
	public static void main(String[] args) {
		driver frame = new driver();
        frame.getContentPane();
        Dimension size = frame.calculate.getPreferredSize();
        frame.calculate.setBounds(300, 450, size.width, size.height);
        frame.minLabel.setBounds(250, 250, 150, 20);
        frame.maxLabel.setBounds(250, 350, 150, 20);
        frame.minInterval.setBounds(400, 255, 100, 20);
        frame.maxInterval.setBounds(400, 355, 100, 20);
        frame.panel.setLayout(null);
        frame.panel.add(frame.calculate);
        frame.panel.add(frame.minLabel);
        frame.panel.add(frame.maxLabel);
        frame.panel.add(frame.back);
        frame.back.setVisible(false);
        frame.panel.add(frame.minInterval);
        frame.panel.add(frame.maxInterval);
        frame.panel.add(frame.delete);
        frame.panel.add(frame.scrollPane);
        frame.scrollPane.setVisible(false);
        frame.panel.add(frame.export);
        frame.panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(frame.panel);
        frame.setSize(800, 800);
        frame.setVisible(true);
	}
}
