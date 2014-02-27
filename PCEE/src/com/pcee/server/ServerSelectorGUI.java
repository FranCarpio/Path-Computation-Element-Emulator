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

package com.pcee.server;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import com.pcee.architecture.ModuleManagement;

/**
 * GUI for the PCE Server
 * 
 * @author Fran Carpio
 */
public class ServerSelectorGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	boolean selection;
	int numberOfRequests = 0;

	GridBagLayout gridbag;
	JFrame windowFrame;
	JPanel windowPanel, introPanel, inputPanel;
	JButton selectButton;
	Dimension buttonDimension;
	JRadioButton simpleRequestRadioButton, multipleRequestRadioButton;
	JTextField numberRequestsTextField;
	ButtonGroup buttonGroup = new ButtonGroup();

	public ServerSelectorGUI() {

		gridbag = new GridBagLayout();
		buttonDimension = new Dimension(100, 20);
		buildIntroPanel();
		buildSelectorPanel();
		buildWindowPanel();
		buildWindowFrame();
		setListener();
	}

	private void setListener() {
		selectButton.addActionListener(this);
		selectButton.setActionCommand("select");
		simpleRequestRadioButton.addActionListener(this);
		multipleRequestRadioButton.addActionListener(this);
	}

	private void buildIntroPanel() {

		introPanel = new JPanel(gridbag);

		JLabel infoLabel = new JLabel("PCEE Server");

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(10, 20, 10, 20);
		c.anchor = GridBagConstraints.CENTER;

		gridbag.setConstraints(infoLabel, c);
		this.introPanel.add(infoLabel);

	}

	private void buildSelectorPanel() {

		inputPanel = new JPanel(gridbag);
		inputPanel.setBorder(new TitledBorder("Computation module selector"));
		inputPanel.setPreferredSize(new Dimension(700, 60));
		inputPanel.setMaximumSize(inputPanel.getPreferredSize());
		inputPanel.setMinimumSize(inputPanel.getPreferredSize());

		selectButton = new JButton("Select");
		selectButton.setPreferredSize(buttonDimension);

		simpleRequestRadioButton = new JRadioButton("Computation module 1.0",
				true);

		multipleRequestRadioButton = new JRadioButton(
				"Computation module 2.0 (ILP solver)");

		buttonGroup.add(simpleRequestRadioButton);
		buttonGroup.add(multipleRequestRadioButton);

		inputPanel.add(simpleRequestRadioButton);
		inputPanel.add(multipleRequestRadioButton);

		JLabel numberRequestsLabel = new JLabel("Number of requests:  ",
				SwingConstants.LEFT);
		numberRequestsLabel.setVerticalAlignment(SwingConstants.CENTER);
		numberRequestsTextField = new JTextField("1");
		numberRequestsTextField.setPreferredSize(new Dimension(20, 20));
		numberRequestsTextField.setEnabled(false);

		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(5, 5, 3, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;

		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(simpleRequestRadioButton, c);
		this.inputPanel.add(simpleRequestRadioButton);

		c.gridx = 1;
		c.gridy = 0;
		gridbag.setConstraints(multipleRequestRadioButton, c);
		this.inputPanel.add(multipleRequestRadioButton);

		c.gridx = 2;
		c.gridy = 0;
		gridbag.setConstraints(numberRequestsLabel, c);
		this.inputPanel.add(numberRequestsLabel);

		c.gridx = 3;
		c.gridy = 0;
		gridbag.setConstraints(numberRequestsTextField, c);
		this.inputPanel.add(numberRequestsTextField);

		c.gridx = 4;
		c.gridy = 0;
		gridbag.setConstraints(this.selectButton, c);
		this.inputPanel.add(this.selectButton);
	}

	private void buildWindowPanel() {

		windowPanel = new JPanel(gridbag);

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

	public void selectServer() throws IOException {

		boolean multipleRequest = multipleRequestRadioButton.isSelected();

		if (multipleRequest) {
			selection = true;
			numberOfRequests = Integer.parseInt(numberRequestsTextField
					.getText());
		} else
			selection = false;

		if (!multipleRequest)
			new ModuleManagement(true, false, 0);
		else
			new ModuleManagement(true, multipleRequest, numberOfRequests);

		windowFrame.setVisible(false);
	}

	public void actionPerformed(ActionEvent event) {
		try {
			if (event.getActionCommand().equals("select")) {
				selectServer();
			}
			numberRequestsTextField.setEnabled(multipleRequestRadioButton
					.isSelected());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean getSelection() {
		return selection;
	}
}
