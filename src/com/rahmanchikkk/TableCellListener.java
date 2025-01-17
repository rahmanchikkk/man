package com.rahmanchikkk;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class TableCellListener implements PropertyChangeListener, Runnable {
	
	private JTable table;
	private Action action;
	private int row, column;
	private Object oldValue, newValue;
	
	public TableCellListener(JTable table, Action action) {
		this.table = table;
		this.action = action;
		this.table.addPropertyChangeListener(this);
	}
	
	private TableCellListener(JTable table, int row, int column, Object oldValue, Object newValue) {
		this.table = table;
		this.row = row;
		this.column = column;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public int getColumn() { return column; }
	
	public int getRow() { return row; }
	
	public Object getOldValue() { return oldValue; }
	
	public Object getNewValue() { return newValue; }
	
	public JTable getTable() { return table; }
	
	public void propertyChange(PropertyChangeEvent e) {
		if ("tableCellEditor".equals(e.getPropertyName())) {
			if (table.isEditing()) processEditingStarted();
			else { processEditingStopped(); }
		}
	}
	
	public void processEditingStarted() {
		SwingUtilities.invokeLater(this);
	}
	
	public void run() {
		row = table.getEditingRow();
		column = table.convertColumnIndexToModel(column);
		oldValue = table.getModel().getValueAt(row, column);
		newValue = null;
	}
	
	public void processEditingStopped() {
		newValue = table.getModel().getValueAt(row, column);
		if (!newValue.equals(oldValue)) {
			TableCellListener tcl = new TableCellListener(getTable(), getRow(), getColumn(), 
				getOldValue(), getNewValue());
			ActionEvent event = new ActionEvent(tcl, ActionEvent.ACTION_PERFORMED, "");
			action.actionPerformed(event);
		}
	}
}
