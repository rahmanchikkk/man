package com.rahmanchikkk;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.*;
import javax.swing.table.*;

import jxl.*;
import jxl.write.*;

public class ExcelExporter {
	public void fillData(JTable table, File file) {
		try {
			WritableWorkbook workbook = Workbook.createWorkbook(file);
			WritableSheet sheet1 = workbook.createSheet("results", 0);
			TableModel model = table.getModel();
			for (int i = 0; i < model.getColumnCount(); i++) {
				Label column = new Label(0, i, model.getColumnName(i));
				sheet1.addCell(column);
			}
			for (int i = 0; i < model.getRowCount(); i++) {
				for (int j = 0; j < model.getColumnCount(); j++) {
					Label row = new Label(j, i + 1, model.getValueAt(i, j).toString());
					sheet1.addCell(row);
				}
			}
			workbook.write();
			workbook.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}