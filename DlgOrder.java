package com.wormtrader.broker;
/********************************************************************
* @(#)DlgOrder.java 1.00 2009????
* Copyright © 2009-2012 by Richard T. Salamone, Jr. All rights reserved.
*
* DlgOrder: Allows the user to enter a discretionary order, along with
* initial risk settings.
*
* @author Rick Salamone
* @version 1.00
* 2009???? rts created
* 20120429 rts decoupled the broker functionality & generalized for CT
* 20120505 rts showIt takes price & type to support trading off chart
* 20120808 rts save/restore bounds
* 20120809 rts uses spinner for price
* 20121017 rts showIt takes a reason, and fixed bug in sending reason
*******************************************************/
import com.wormtrader.broker.Broker;
import com.wormtrader.dao.USDSpinner;
import com.wormtrader.positions.LegsList;
import com.wormtrader.positions.PositionLeg;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.SBRadioPanel;
import com.shanebow.util.SBFormat;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class DlgOrder
	extends JDialog
	implements ActionListener
	{
	public static final String BUY="BUY";
	public static final String SELL="SELL";
	private static final String HARD = "Send Now";
	private static final String SOFT = "Soft";
	private static final String[] HARDSOFT = { SOFT, HARD };
	private static final String[] ACTIONS = { BUY, SELL };
	private static final DlgOrder _instance = new DlgOrder();
	private static final String KEY_BOUNDS="usr.dlgorder.bounds";

	/**
	* display() static methods to display the singleton DlgOrder with
	* the specified initial inputs:
	* @param PositionLeg leg cannot be null
	* @param aAct must be eith BUY, SELL, or null
	* @param aType must be a valid Broker type (ie Broker.xxx_ORDER)
	* @param aPrice in cents, use zero for leg's last price
	* @param aWhy default reason to be displayed
	*/
	public static void display(PositionLeg leg, String aAct, String aType, int aPrice, String aWhy)
		{ _instance.showIt(leg, aAct, aType, aPrice, aWhy); }
	/**
	* display() is a convience method to display singleton DlgOrder with
	* a market order at the stock's last price.
	*/
	public static void display(PositionLeg leg)
		{ _instance.showIt(leg, null, Broker.MKT_ORDER, 0, ""); }

	private final JTextField   tfSymbol = new JTextField();
	private final JTextField   tfOptDesc = new JTextField();
	private final JTextField   tfQuantity = new JTextField( 5 );

	private final USDSpinner spinPrice = new USDSpinner();
	private final SBRadioPanel<String>  radioDest = new SBRadioPanel<String>(2, HARDSOFT);
	private final SBRadioPanel<String>  radioAct = new SBRadioPanel<String>(2, ACTIONS);
	private final SBRadioPanel<Integer> radioQty
		= new SBRadioPanel<Integer>(1, new Integer[]{100,200,300,400,500,600,700,800,900,1000});
	private final SBRadioPanel<String>  radioType
		= new SBRadioPanel<String>(3, Broker.ORDER_TYPES);

	private final JCheckBox chkUseStop = new JCheckBox("Stop", true);
	private final USDSpinner spinStop = new USDSpinner();
	private JTextField tfReason = new JTextField();

	private JButton    btnOK = new JButton( "Submit" );
	private JButton    btnCancel = new JButton( "Cancel" );

	private DlgOrder()
		{
		super( (java.awt.Frame)null, false);
		setTitle( "Place Order" );

		// create main Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.Y_AXIS ));
		mainPanel.setBorder(BorderFactory.createEmptyBorder( 0,10,0,10));
		mainPanel.add( LAF.titled(legPanel(), "Security" ));
		mainPanel.add( LAF.titled(orderPanel(), "Order" ));
		JPanel reasonPanel = new JPanel(new GridLayout(0,1));
		reasonPanel.add(tfReason);
		mainPanel.add( LAF.titled(reasonPanel, "Reason" ));
		mainPanel.add( LAF.titled(riskPanel(), "Risk" ));

		// create button panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.add( btnOK );
		buttonPanel.add( btnCancel );

		btnOK.addActionListener( this );
		btnCancel.addActionListener( this );

		radioQty.addActionListener(new ActionListener()
			{
			@Override public void actionPerformed(ActionEvent e)
				{
				tfQuantity.setText(e.getActionCommand());
				}
			});

		radioAct.addActionListener(new ActionListener()
			{
			@Override public void actionPerformed(ActionEvent e)
				{
				boolean buy = e.getActionCommand().equals(ACTIONS[0]);
				tfQuantity.setBackground( buy? Color.BLUE : Color.RED );
				int offset = SBProperties.getInstance().getInt("usr.stop.offset", 10);
				if (buy) offset = -offset;
				int stop = spinPrice.getCents() + offset;
				spinStop.setCents(stop);
				}
			});

		radioType.addActionListener(new ActionListener()
			{
			@Override public void actionPerformed(ActionEvent e)
				{
				radioDest.setEnabled(! e.getActionCommand().equals(Broker.MKT_ORDER));
				}
			});

		radioDest.select(SOFT);
		// create dlg box
		getContentPane().add( LAF.titled(radioQty,"Qty"), BorderLayout.EAST);
		getContentPane().add( mainPanel, BorderLayout.CENTER );
		getContentPane().add( buttonPanel, BorderLayout.SOUTH );
		LAF.addUISwitchListener(this);
		setBounds( SBProperties.getInstance()
		                       .getRectangle( KEY_BOUNDS, 50,50,260,328));
		addComponentListener( new ComponentAdapter()
			{
			public void componentMoved(ComponentEvent e) { saveBounds(); }
			public void componentResized(ComponentEvent e) { saveBounds(); }
			});
		}

	private void saveBounds()
		{
		SBProperties.getInstance().setProperty( KEY_BOUNDS, getBounds());
		}

	private JPanel legPanel()
		{
		JPanel p = new JPanel( new GridLayout(0,2));
		addLabeled( p, "Underlying", tfSymbol );
		addLabeled( p, "Option Desc", tfOptDesc );
		return p;
		}

	private JPanel orderPanel()
		{
		JPanel p = new JPanel( new GridLayout(0,1));
		p.add(radioDest);
		p.add(radioAct);
		p.add(radioType);
		JPanel q = new JPanel( new GridLayout(0,2));
		addLabeled( q, "Quantity", tfQuantity );
		p.add(q);
		q = new JPanel( new GridLayout(0,2));
		addLabeled( q, "Price",    spinPrice );
		p.add(q);
		tfQuantity.setForeground( Color.WHITE );
		return p;
		}

	private JPanel riskPanel()
		{
		JPanel p = new JPanel( new GridLayout(0,2));
		p.add(chkUseStop);
		p.add(spinStop);
		return p;
		}

	private void addLabeled( JPanel p, String label, JComponent jc )
		{
		p.add( new JLabel(label));
		p.add( jc );
		}

	public void actionPerformed( ActionEvent e )
		{
		Object src = e.getSource();

		if ( src.equals ( btnCancel )
		|| ( src.equals ( btnOK ) && onOk()))
			setVisible(false);
		}

	boolean onOk()
		{
		PositionLeg leg = LegsList.find( tfSymbol.getText(), tfOptDesc.getText());
		if ( leg == null )
			return SBDialog.inputError( "Leg not found!" );
		try
			{
			String type = radioType.getSelected();
			int qty = Integer.parseInt( tfQuantity.getText());
			if ( radioAct.getSelected().equals("SELL"))
				qty = -qty;
			int lmt = spinPrice.getCents();
			String dest = HARD;
			if (!type.equals(Broker.MKT_ORDER))
				{
				if ( lmt == 0 )
					return SBDialog.inputError( "Price required for " + type + " order");
				dest = radioDest.getSelected();
				}
			int aux = 0;
			int stop = 0;
			if ( chkUseStop.isSelected())
				stop = spinStop.getCents();

			String reason = tfReason.getText().trim();
			if (reason.isEmpty()) reason = null;
			OrderTracker ot = new OrderTracker(leg, type, qty, lmt, aux, stop, reason);
			if (dest.equals(HARD))
				leg.getTrader().getBroker().placeOrder(ot);
			else
				leg.add(ot);
			}
		catch( Exception e) { return SBDialog.inputError( "Error - " + e ); }
		return true;
		}

	public void showIt(PositionLeg leg, String aAct, String aType, int aPrice, String aWhy )
		{
		if ( leg == null )
			{
			SBDialog.inputError( "No leg selected" );
			return;
			}
		tfReason.setText(aWhy);
		tfSymbol.setText( leg.getUnderlying());
		tfOptDesc.setText( leg.getOptDesc());
		int qty = leg.getQty();
		int last = leg.getLast().cents();
		if (aPrice == 0) aPrice = last;
		spinPrice.setCents(aPrice);
		if ( aAct != null )
			radioAct.select(aAct);
		else if ( aType.equals(Broker.MKT_ORDER))
			radioAct.select(( qty < 0 )? BUY : SELL);
		else if ( aType.equals(Broker.STP_ORDER))
			radioAct.select((aPrice > last )? BUY : SELL);
		else if ( aType.equals(Broker.LMT_ORDER))
			radioAct.select((aPrice < last )? BUY : SELL);
		radioType.select(aType);
		tfQuantity.setText((qty == 0)?
					"" + leg.getStrategy().getTrader().sizePosition(leg)
					: "" + Math.abs(qty));
		radioQty.select(Integer.valueOf(Math.abs(qty)));
		pack();
		setVisible( true );
		}
	} // 278