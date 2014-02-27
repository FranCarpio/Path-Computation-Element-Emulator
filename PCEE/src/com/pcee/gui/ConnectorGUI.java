/**
 *  This file is part of Path Computation Element Emulator (PCEE).
 *
 *  PCEE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PCEE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PCEE.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.pcee.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;

import com.pcee.architecture.ModuleEnum;
import com.pcee.architecture.ModuleManagement;
import com.pcee.logger.Logger;
import com.pcee.protocol.message.PCEPMessage;
import com.pcee.protocol.message.PCEPMessageFactory;
import com.pcee.protocol.message.objectframe.PCEPObjectFrameFactory;
import com.pcee.protocol.message.objectframe.impl.PCEPBandwidthObject;
import com.pcee.protocol.message.objectframe.impl.PCEPEndPointsObject;
import com.pcee.protocol.message.objectframe.impl.PCEPMetricObject;
import com.pcee.protocol.message.objectframe.impl.PCEPRequestParametersObject;
import com.pcee.protocol.message.objectframe.impl.erosubobjects.PCEPAddress;
import com.pcee.protocol.request.PCEPRequestFrame;
import com.pcee.protocol.request.PCEPRequestFrameFactory;

/**GUI for the PCE Client
 * 
 * @author Marek Drogon
 * @author Fran Carpio
 */
public class ConnectorGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	ModuleManagement lm;
	int port = 4189;
	static long timeStart;

	GridBagLayout gridbag;
	Dimension buttonDimension, labelDimension, textFieldDimension, panelDimension;

	JFrame windowFrame;
	JPanel windowPanel, introPanel, inputPanel, openMessagePanel, keepAliveMessagePanel, requestMessagePanel, textAreaPanel;

	JTextField serverAddressTextField, serverPortTextField, keepAliveTextField, deadTimerTextField, priTextField, sourceTextField, destinationTextField,bandwidthTextField,delayTextField;
	JButton connectButton, openMessageButton, keepAliveMessageButton, requestMessageButton;
	JCheckBox openMessagePFlagCheckBox, openMessageIFlagCheckBox, requestMessagePFlagCheckBox, requestMessageIFlagCheckBox, oFlagCheckBox, bFlagCheckBox, rFlagCheckBox, endPointsPFlag, endPointsIFlag;
	static JTextArea messageTextArea;
	JScrollPane scrollPane;

	PCEPAddress address, sourceAddress, destAddress;

	public ConnectorGUI(ModuleManagement layerManagement, String address, String sourceAddress, String destAddress) {
		lm = layerManagement;
		this.address = new PCEPAddress(address, port);
		this.sourceAddress = new PCEPAddress(sourceAddress, false);
		this.destAddress = new PCEPAddress(destAddress, false);

		gridbag = new GridBagLayout();

		setDimensions();

		buildIntroPanel();
		buildConnectionPanel();
		buildRequestMessagePanel();
		buildTextAreaPanel();
		buildWindowPanel();
		buildWindowFrame();

		setListener();

		setIDALogo();
	}

	private void setDimensions() {
		buttonDimension = new Dimension(100, 20);
		textFieldDimension = new Dimension(100, 20);
	}

	private void setListener() {
		connectButton.addActionListener(this);
		connectButton.setActionCommand("connect");
		requestMessageButton.addActionListener(this);
		requestMessageButton.setActionCommand("request");
	}

	private void buildIntroPanel() {

		introPanel = new JPanel(gridbag);

		JLabel infoLabel = new JLabel("Path Computation Element Emulator");

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(10, 20, 10, 20);
		c.anchor = GridBagConstraints.CENTER;

		gridbag.setConstraints(infoLabel, c);
		this.introPanel.add(infoLabel);

	}

	private void buildConnectionPanel() {

		inputPanel = new JPanel(gridbag);
		inputPanel.setBorder(new TitledBorder("Server Connection"));

		JLabel serverLabel = new JLabel("Server:", SwingConstants.LEFT);
		serverLabel.setVerticalAlignment(SwingConstants.CENTER);
		//serverLabel.setPreferredSize(labelDimension);

		JLabel serverPort = new JLabel("Port:", SwingConstants.LEFT);
		serverPort.setVerticalAlignment(SwingConstants.CENTER);
		//serverPort.setPreferredSize(labelDimension);

		serverAddressTextField = new JTextField(address.getIPv4Address(false));
		serverAddressTextField.setPreferredSize(textFieldDimension);

		if (address.getPort()!=0)
			serverPortTextField = new JTextField(Integer.toString(address.getPort()));
		else
			serverPortTextField = new JTextField("4189");
			
		serverPortTextField.setPreferredSize(textFieldDimension);
		
		
		connectButton = new JButton("Connect");
		connectButton.setPreferredSize(buttonDimension);

		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(5, 5, 3, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;

		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(serverLabel, c);
		this.inputPanel.add(serverLabel);

		c.gridx = 1;
		c.gridy = 0;
		gridbag.setConstraints(this.serverAddressTextField, c);
		this.inputPanel.add(this.serverAddressTextField);

		c.gridx = 2;
		c.gridy = 0;
		gridbag.setConstraints(serverPort, c);
		this.inputPanel.add(serverPort);

		
		c.gridx = 3;
		c.gridy = 0;
		gridbag.setConstraints(this.serverPortTextField, c);
		this.inputPanel.add(this.serverPortTextField);

		
		c.gridx = 4;
		c.gridy = 0;
		gridbag.setConstraints(this.connectButton, c);
		this.inputPanel.add(this.connectButton);
	}

	private void buildOpenMessagePanel() {

		openMessagePanel = new JPanel(gridbag);
		openMessagePanel.setBorder(new TitledBorder("Open Message"));

		keepAliveTextField = new JTextField("KeepAlive");
		keepAliveTextField.setPreferredSize(textFieldDimension);

		deadTimerTextField = new JTextField("DeadTimer");
		deadTimerTextField.setPreferredSize(textFieldDimension);

		openMessagePFlagCheckBox = new JCheckBox();
		openMessagePFlagCheckBox.setToolTipText("p");

		openMessageIFlagCheckBox = new JCheckBox();
		openMessageIFlagCheckBox.setToolTipText("i");

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 5, 3, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;

		openMessageButton = new JButton("Send");
//		openMessageButton.setPreferredSize(buttonDimension);

		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(keepAliveTextField, c);
		this.openMessagePanel.add(keepAliveTextField);

		c.gridx = 1;
		c.gridy = 0;
		gridbag.setConstraints(deadTimerTextField, c);
		this.openMessagePanel.add(deadTimerTextField);

		c.gridx = 2;
		c.gridy = 0;
		gridbag.setConstraints(openMessagePFlagCheckBox, c);
		this.openMessagePanel.add(openMessagePFlagCheckBox);

		c.gridx = 3;
		c.gridy = 0;
		gridbag.setConstraints(openMessageIFlagCheckBox, c);
		this.openMessagePanel.add(openMessageIFlagCheckBox);

		c.gridx = 4;
		c.gridy = 0;
		gridbag.setConstraints(openMessageButton, c);
		this.openMessagePanel.add(openMessageButton);

	}

	private void buildKeepAliveMessagePanel() {

		keepAliveMessagePanel = new JPanel(gridbag);
		keepAliveMessagePanel.setSize(1000, 300);
		keepAliveMessagePanel.setBorder(new TitledBorder("KeepAlive Message"));


		keepAliveMessageButton = new JButton("Send");
//		keepAliveMessageButton.setPreferredSize(buttonDimension);

		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(3, 5, 3, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;

		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(keepAliveMessageButton, c);
		this.keepAliveMessagePanel.add(keepAliveMessageButton);

	}

	private void buildRequestMessagePanel() {

		requestMessagePanel = new JPanel(gridbag);
		requestMessagePanel.setBorder(new TitledBorder("Request Message"));

		JPanel piPanel = new JPanel(gridbag);
		TitledBorder titledBorder = new TitledBorder("Common Object Header Flags");
		piPanel.setBorder(titledBorder);
		piPanel.setPreferredSize(new Dimension(215, 50));
		piPanel.setMaximumSize(piPanel.getPreferredSize()); 
		piPanel.setMinimumSize(piPanel.getPreferredSize());
		
		JPanel obrPriPanel = new JPanel(gridbag);
		obrPriPanel.setBorder(new TitledBorder("RP object body Flags"));
		obrPriPanel.setPreferredSize(new Dimension(260, 50));
		obrPriPanel.setMaximumSize(obrPriPanel.getPreferredSize()); 
		obrPriPanel.setMinimumSize(obrPriPanel.getPreferredSize());
		
//		JPanel endPointsPiPanel = new JPanel(gridbag);

		requestMessagePFlagCheckBox = new JCheckBox("P flag", true);
		requestMessagePFlagCheckBox.setToolTipText("p");

		requestMessageIFlagCheckBox = new JCheckBox("I flag");
		requestMessageIFlagCheckBox.setToolTipText("i");

		piPanel.add(requestMessagePFlagCheckBox);
		piPanel.add(requestMessageIFlagCheckBox);

		oFlagCheckBox = new JCheckBox("O flag", true);
		oFlagCheckBox.setToolTipText("o");

		bFlagCheckBox = new JCheckBox("B flag");
		bFlagCheckBox.setToolTipText("b");

		rFlagCheckBox = new JCheckBox("R flag");
		rFlagCheckBox.setToolTipText("r");

		JLabel priLabel = new JLabel("Priority:  ", SwingConstants.LEFT);
		priLabel.setVerticalAlignment(SwingConstants.CENTER);
		priTextField = new JTextField("1");
		priTextField.setPreferredSize(new Dimension(20, 20));

		obrPriPanel.add(oFlagCheckBox);
		obrPriPanel.add(bFlagCheckBox);
		obrPriPanel.add(rFlagCheckBox);
		obrPriPanel.add(priLabel);
		obrPriPanel.add(priTextField);

		JPanel qosPanel = new JPanel(gridbag);
		qosPanel.setBorder(new TitledBorder("QoS constraints"));
		qosPanel.setPreferredSize(new Dimension(280, 50));
		qosPanel.setMaximumSize(qosPanel.getPreferredSize()); 
		qosPanel.setMinimumSize(qosPanel.getPreferredSize());
		
		JLabel bandwidthLabel = new JLabel("Bandwidth (Gbps): ", SwingConstants.LEFT);
		bandwidthLabel.setVerticalAlignment(SwingConstants.CENTER);
		bandwidthTextField = new JTextField("1");
		bandwidthTextField.setPreferredSize(new Dimension(40, 20));
		
		JLabel delayLabel = new JLabel("   Delay (ms): ", SwingConstants.LEFT);
		delayLabel.setVerticalAlignment(SwingConstants.CENTER);
		delayTextField = new JTextField("20");
		delayTextField.setPreferredSize(new Dimension(30, 20));
		
		qosPanel.add(bandwidthLabel);
		qosPanel.add(bandwidthTextField);
		qosPanel.add(delayLabel);
		qosPanel.add(delayTextField);
		
		
		JPanel srcPanel = new JPanel(gridbag);
		srcPanel.setPreferredSize(new Dimension(215, 20));
		srcPanel.setMaximumSize(srcPanel.getPreferredSize()); 
		srcPanel.setMinimumSize(srcPanel.getPreferredSize());
		
		JLabel srcLabel = new JLabel("Source IP address:  ", SwingConstants.LEFT);
		srcLabel.setVerticalAlignment(SwingConstants.CENTER);
		sourceTextField = new JTextField(sourceAddress.getIPv4Address(false));
		sourceTextField.setPreferredSize(textFieldDimension);
		
		srcPanel.add(srcLabel);
		srcPanel.add(sourceTextField);

		JPanel dstPanel = new JPanel(gridbag);
		dstPanel.setPreferredSize(new Dimension(260, 20));
		dstPanel.setMaximumSize(dstPanel.getPreferredSize()); 
		dstPanel.setMinimumSize(dstPanel.getPreferredSize());
		JLabel dstLabel = new JLabel("Destination IP address:  ", SwingConstants.LEFT);
		dstLabel.setVerticalAlignment(SwingConstants.CENTER);
		destinationTextField = new JTextField(destAddress.getIPv4Address(false));
		destinationTextField.setPreferredSize(textFieldDimension);
		dstPanel.add(dstLabel);
		dstPanel.add(destinationTextField);

		requestMessageButton = new JButton("Send");
		requestMessageButton.setPreferredSize(buttonDimension);
		

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;

		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(piPanel, c);
		this.requestMessagePanel.add(piPanel);
		
		c.gridx = 1;
		c.gridy = 0;
		gridbag.setConstraints(obrPriPanel, c);
		this.requestMessagePanel.add(obrPriPanel);
		
		c.gridx = 2;
		c.gridy = 0;
		gridbag.setConstraints(qosPanel, c);
		this.requestMessagePanel.add(qosPanel);
		
		c.gridx = 0;
		c.gridy = 1;
		gridbag.setConstraints(srcPanel, c);
		this.requestMessagePanel.add(srcPanel);
		
		c.gridx = 1;
		c.gridy = 1;
		gridbag.setConstraints(dstPanel, c);
		this.requestMessagePanel.add(dstPanel);

		c.gridx = 2;
		c.gridy = 1;
		gridbag.setConstraints(requestMessageButton, c);
		this.requestMessagePanel.add(requestMessageButton);

	}
	
	

	private void buildTextAreaPanel() {

		messageTextArea = new JTextArea(30, 120); // 20,83
		messageTextArea.setEditable(false);
		messageTextArea.setFont(new Font("Monospaced", Font.TRUETYPE_FONT, 11));
		scrollPane = new JScrollPane(messageTextArea);
		
		DefaultCaret caret = (DefaultCaret)messageTextArea.getCaret();  
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);  
		
		textAreaPanel = new JPanel(gridbag);

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.FIRST_LINE_START;

		gridbag.setConstraints(this.scrollPane, c);
		this.textAreaPanel.add(this.scrollPane);

	}

	private void buildWindowPanel() {

		windowPanel = new JPanel(gridbag);
		//JPanel openKeepPanel = new JPanel(gridbag);

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 20, 3, 20);
		c.anchor = GridBagConstraints.CENTER;

		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(this.introPanel, c);
		this.windowPanel.add(this.introPanel);

		c.anchor = GridBagConstraints.FIRST_LINE_START;

		c.gridx = 0;
		c.gridy = 1;
		gridbag.setConstraints(this.inputPanel, c);
		this.windowPanel.add(this.inputPanel);

		c.gridx = 0;
		c.gridy = 2;
		gridbag.setConstraints(this.requestMessagePanel, c);
		this.windowPanel.add(this.requestMessagePanel);

		c.gridx = 0;
		c.gridy = 3;
		gridbag.setConstraints(this.textAreaPanel, c);
		this.windowPanel.add(this.textAreaPanel);

	}

	private void buildWindowFrame() {
		getContentPane().add(windowPanel);

		windowFrame = new JFrame();
		windowFrame.setTitle("Path Computation Element Emulator");
		windowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		windowFrame.add(getContentPane());
		windowFrame.pack();
		windowFrame.setLocationRelativeTo(null);
		windowFrame.setResizable(true);
		windowFrame.setVisible(true);
	}

	public void openConnection() throws Exception {
		PCEPAddress address = new PCEPAddress(serverAddressTextField.getText(), Integer.parseInt(serverPortTextField.getText()));
		killIDALogo();
		guiLogger("Trying to connect to " + address.getIPv4Address(true));
		lm.getClientModule().registerConnection(address, false, true, true);
	}
	
	public void requestMessage() throws IOException {
		localLogger("Sending Request Message");

		String pFlag = booleanToStringConverter(requestMessagePFlagCheckBox.isSelected());
		String iFlag = booleanToStringConverter(requestMessageIFlagCheckBox.isSelected());
		String oFlag = booleanToStringConverter(oFlagCheckBox.isSelected());
		String bFlag = booleanToStringConverter(bFlagCheckBox.isSelected());
		String rFlag = booleanToStringConverter(rFlagCheckBox.isSelected());
		String priFlag = priTextField.getText();
		Float bandwidth = Float.valueOf(bandwidthTextField.getText());
		Float delay = Float.valueOf(delayTextField.getText());

		// EndPoints Information
		String endPointsPFlag = booleanToStringConverter(false);
		String endPointsIFlag = booleanToStringConverter(false);

		PCEPAddress sourceAddress = new PCEPAddress(sourceTextField.getText().trim(), false);
		PCEPAddress destinationAddress = new PCEPAddress(destinationTextField.getText().trim(), false);

		PCEPRequestParametersObject RP = PCEPObjectFrameFactory.generatePCEPRequestParametersObject(pFlag, iFlag, oFlag, bFlag, rFlag, priFlag, "432");
		PCEPEndPointsObject endPoints = PCEPObjectFrameFactory.generatePCEPEndPointsObject(endPointsPFlag, endPointsIFlag, sourceAddress, destinationAddress);
		PCEPBandwidthObject bandwidthObject = PCEPObjectFrameFactory.generatePCEPBandwidthObject(pFlag, iFlag, bandwidth);
		PCEPMetricObject metricObject = PCEPObjectFrameFactory.generatePCEPMetricObject(pFlag, iFlag, "1", "1", 2, delay);
		
		// Address destAddress = new Address(serverAddressTextField.getText());
		PCEPAddress destAddress = new PCEPAddress(serverAddressTextField.getText(), Integer.parseInt(serverPortTextField.getText()));

		PCEPRequestFrame requestMessage = PCEPRequestFrameFactory.generatePathComputationRequestFrame(RP, endPoints, bandwidthObject, metricObject);
		PCEPMessage message = PCEPMessageFactory.generateMessage(requestMessage);

		message.setAddress(destAddress);

		guiLogger("Sending Path Computation Request Message.");
		guiLogger("Requesting a Way from " + sourceAddress.getIPv4Address(true) + " to " + destinationAddress.getIPv4Address(true));
		lm.getClientModule().sendMessage(message, ModuleEnum.SESSION_MODULE);
	}

	public void actionPerformed(ActionEvent event) {
		
		try {
			if (event.getActionCommand().equals("connect")) {
				localDebugger("Connection Event Triggered");
				openConnection();
			}
			if (event.getActionCommand().equals("request")) {
				localLogger("Request Event triggered");
				
				timeStart = System.currentTimeMillis();
				requestMessage();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	private String booleanToStringConverter(boolean value) {
		if (value == true) {
			return "1";
		}
		return "0";
	}

	private void setIDALogo() {
		updateTextArea("  -------------------------------------------------------------------------------  ");
		updateTextArea("  -----------********--@@@----**************@@@@@@@@----------------************-  ");
		updateTextArea("  ----------********--@@@----**************@@@@@@@@@@@-------------***********-@@  ");
		updateTextArea("  ---------********--@@@----**************@@@@@@@@@@@@@-----------***********-@@@  ");
		updateTextArea("  --------*******---@@@----**************@@@@@@@@@@@@@@---------***********-@@@@@  ");
		updateTextArea("  -------*******--@@@@----***********-----@@@@@@@@@@@@@--------**********--@@@@@@  ");
		updateTextArea("  ------******---@@@@----***********-------@@@@@@@@@@@@------***********-@@@@@@@@  ");
		updateTextArea("  -----******---@@@@----***********--------@@@@@@@@@@@@-----**********--@@@@@@@@@  ");
		updateTextArea("  ----*****---@@@@@----***********---------@@@@@@@@@@@----***********---@@@@@@@@@  ");
		updateTextArea("  ---*****--@@@@@@----***********---------@@@@@@@@@@@---***********-----@@@@@@@@@  ");
		updateTextArea("  --****---@@@@@@----***********---------@@@@@@@@@@@---***********------@@@@@@@@@  ");
		updateTextArea("  -****--@@@@@@@----***********---------@@@@@@@@@@---************-@@@@@@@@@@@@@@@  ");
		updateTextArea("  ***---@@@@@@@----***********@@@@@@@@@@@@@@@@@@----***********--@@@@@@@@@@@@@@@@  ");
		updateTextArea("  **--@@@@@@@@----***********@@@@@@@@@@@@@@@@@----***********---@@@@@@@@@@@@@@@@@  ");
		updateTextArea("  *--@@@@@@@@----***********@@@@@@@@@@@@@@@@-----***********------------@@@@@@@@@  ");
		updateTextArea("  -@@@@@@@@@----***********@@@@@@@@@@@@@-------***********--------------@@@@@@@@@  ");
		updateTextArea("  -------------------------------------------------------------------------------  ");
		updateTextArea("  -------------- Institut für Datentechnik und Kommunikationsnetze --------------  ");
		updateTextArea("  -------------------------------------------------------------------------------  ");
	}

	private void killIDALogo() {
		messageTextArea.setText("");
	}

	public static void updateTextArea(String text) {
		messageTextArea.append("\n" + text);
		
//		long time = (System.currentTimeMillis() - timeStart);
//		String ls = System.getProperty("line.separator");
//		System.out.println(ls + ls + "The task has taken " + time + " miliseconds");
	}

	private void guiLogger(String event) {
		Logger.logGUINotifications(event);
	}

	private void localLogger(String event) {
		Logger.logSystemEvents("[PCEClientGUI] " + event);
	}

	private void localDebugger(String event) {
		Logger.debugger("[PCEClientGUI] " + event);
	}

}
