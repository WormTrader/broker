package com.wormtrader.broker;
/********************************************************************
* @(#)OrderList.java 1.00 20120509
* Copyright © 2012-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* OrderList: A list of orders maintained by a Broker object that
* extends AbstractTableModel for display in a JTable.
*
* @version 1.00
* @author Rick Salamone
* 20120509 rts created by generalizing ATOrders
* 20130307 rts attempts to merge duplicate (soft) orders
*******************************************************/
import com.wormtrader.positions.PositionLeg;
import com.shanebow.ui.table.DollarCellRenderer;
import com.shanebow.ui.table.SideCellRenderer;
import com.shanebow.util.SBFormat;
import java.util.List;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;

public class OrderList<T extends OrderTracker>
	extends AbstractTableModel
	{
	public static final String MODULE="OrderList.";
	public static final String FILLED="Filled";
	public static final String CANCELED="Cancelled";
	public static final int COL_STATE = 0;
	public static final int COL_QTY = 1;
	public static final int COL_FILLED = 2;
	public static final int COL_TYPE = 3;	// 70
	public static final int COL_LMT = 4;	// 60
	public static final int COL_ISTOP = 5;	// 60
	public static final int COL_SYMBOL = 6;		// 240
	public static final int COL_STATUS = 7;	// 170
	public static final int COL_ID = 8;			// 20
	static final String[] columnNames =
		{ "State", "Qty", "Filled", "Type",
					"Limit", "IStop", "Security", "Status", "ID", };
	public static final int[]  colWidths =
		{      55,     55,      60,      60,
		            60,      60,    55,        200,      180,   20 };

	private static DollarCellRenderer
				 _dollarRenderer = DollarCellRenderer.getInstance();
	private static SideCellRenderer _sideRenderer = SideCellRenderer.getInstance();

	public void initColumns( javax.swing.JTable table )
		{
		for ( int c = getColumnCount(); c-- > 0; )
			{
			javax.swing.table.TableColumn column = table.getColumnModel().getColumn(c);
			switch (c)
				{
				case COL_FILLED:
				case COL_QTY:    column.setCellRenderer( _sideRenderer );
				                 break;

				case COL_ISTOP:
				case COL_LMT:    column.setCellRenderer( _dollarRenderer );
				                 break;
				}
			column.setPreferredWidth(colWidths[c]);
			}
		}

	private List<T> fOrders = new Vector<T>();
public List<T> getAll() { return fOrders; }

	public Object getValueAt(int r, int c)
		{
T order = null;
		try { order = fOrders.get(r); }
		catch (Exception e) { return e.toString(); }
if (order == null) return "";
		switch (c)
			{
			case COL_STATE: 
try { return OrderTracker.STATE_DESC[order.getState()]; }
catch(Exception e) { return "" + order.getState(); }

			case COL_ISTOP:   int istop = order.getInitialStop();
			                  return (istop==0)? "" : SBFormat.toDollarString(istop);
			case COL_QTY:     return "" + order.qty();
			case COL_FILLED:  return "" + order.getFilled();
			case COL_TYPE:    return order.type();
			case COL_LMT:     return SBFormat.toDollarString(order.getLmt());
			case COL_SYMBOL:  return order.leg().toString();
			case COL_ID:      return "" + order.id();
			case COL_STATUS:  return order.getReason() + " " + order.getStatus();
			default:	return null;
			}
		}

	/*********** In case we implement edit orders in table...
	public boolean DEBUG=true;
	public void setValueAt(Object value, int row, int col)
		{
		if (DEBUG)
			{
			System.out.println("Setting value at " + row + "," + col
                           + " to " + value
                           + " (an instance of " + value.getClass() + ")");
			}
		T ao = fOrders.get(row);
		ao.setState(Byte.parseByte((String)value));
		fireTableCellUpdated(row, col);
		}

	public final boolean isCellEditable(int r, int c)
		{
		if (c == COL_STATE)
			{
			byte state = getRow(r).getState();
			return state == OrderTracker.OPEN
			    || state == OrderTracker.PART_FILL;
			}
		else return false;
		}
	***********/

	public final int    getColumnCount() { return columnNames.length; }
	public final String getColumnName(int c) { return columnNames[c]; }
	public final int    getRowCount()   { return fOrders.size(); }
	public final T      getRow(int row) { return fOrders.get(row); }

	public final int size() { return fOrders.size(); }
	public final T remove(T aLegOrder)
		{
		for ( int r = fOrders.size(); r > 0; )
			if ( fOrders.get(--r).equals(aLegOrder))
				return removeRow(r);
		return null;
		}

	public final T removeRow(int r)
		{
		T it = fOrders.remove(r);
		fireTableRowsDeleted(r,r);
		return it;
		}

	/** clear() is required by the simulator for new runs, restart */
	public final void clear()
		{
		int lastRow = fOrders.size() - 1;
		if (lastRow < 0) return;
		fOrders.clear();
		fireTableRowsDeleted(0,lastRow);
		}

	public final boolean add(T aLegOrder)
		{
		if (fOrders.contains(aLegOrder))
			return false;
		insertInList(aLegOrder);
		return true;
		}

	/**
	* Inserts a OrderTracker object into the list sorted by symbol
	* Tries to merge orders that are same type and prices which may
	* result in an order being updated or even deleted.
	*/
	protected final void insertInList(T aLegOrder)
		{
		int row;
		int size = fOrders.size();		      // loop thru rows to insert new
		for ( row = 0; row < size; row++ ) // order sorted by symbol and ignore
			{												    // duplicate entries sent by broker
			OrderTracker test = fOrders.get(row);
			int comparison = test.leg().compareTo(aLegOrder.leg());
			if ( comparison < 0 )
				break;
			if ( comparison == 0 ) // same leg
				{
				int lmt = aLegOrder.getLmt();
				if ( aLegOrder.getLmt() > test.getLmt())
					break;
				if (test.merge(aLegOrder)) // duplicate price, type, etc
					{
					if (test.qty() == 0)
						removeRow(row);
					else
						fireTableCellUpdated(row, COL_QTY);
					return;
					}
				// continue cause price is less or equal and not duplicate
				}
			}
		fOrders.add(row,aLegOrder);
		fireTableRowsInserted(row, row);
		}
	}
