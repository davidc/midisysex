package net.davidc.midisysex;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;


/**
 * Created by david on 05/11/2015.
 */
public class MidiSysEx implements ActionListener
{
  private static final String CMD_SEND_SYSEX = "SEND_SYSEX";
  private static final String CMD_SEND_DEVICE_CHANGED = "SEND_DEVICE_CHANGED";
  private static final String CMD_MONITOR_DEVICE_CHANGED = "MONITOR_DEVICE_CHANGED";
  private static final String CMD_SEND_DEVICE_INFO = "SEND_DEVICE_INFO";
  private static final String CMD_MONITOR_DEVICE_INFO = "MONITOR_DEVICE_INFO";
  private static final String CMD_MONITOR_BEGIN = "MONITOR_BEGIN";
  private static final String CMD_MONITOR_END = "MONITOR_END";
  private static final String CMD_MONITOR_CLEAR = "MONITOR_CLEAR";

  private JPanel mainPanel;

  private JComboBox<MidiDevice.Info> sendDeviceCombo;
  private JButton sendDeviceInfoButton;
  private JTextArea messageField;
  private JRadioButton hexRadioButton;
  private JRadioButton decRadioButton;
  private JButton sendButton;

  private JComboBox<MidiDevice.Info> monitorDeviceCombo;
  private JButton monitorDeviceInfoButton;
  private JScrollPane monitorOutputScroller;
  private JTextArea monitorOutput;
  private JButton beginMonitorButton;
  private JButton endMonitorButton;
  private JButton clearMonitorButton;

  private MidiDevice selectedSendMidiDevice;
  private MidiDevice selectedMonitorMidiDevice;
  private Transmitter selectedMidiTransmitter;

  public MidiSysEx()
  {
    MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();

    sendDeviceCombo.addItem(null);
    monitorDeviceCombo.addItem(null);

    for (MidiDevice.Info deviceInfo : devices) {

      try {
        MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);

        if (device.getMaxReceivers() != 0) {
          sendDeviceCombo.addItem(deviceInfo);
        }
        if (device.getMaxTransmitters() != 0) {
          monitorDeviceCombo.addItem(deviceInfo);
        }

      }
      catch (MidiUnavailableException e) {
        System.err.println("Warning: could not get info for device " + deviceInfo.getName() + " (" + deviceInfo.getDescription() + ")");
        e.printStackTrace();
      }
    }

    sendDeviceCombo.setActionCommand(CMD_SEND_DEVICE_CHANGED);
    sendDeviceCombo.addActionListener(this);
    sendDeviceInfoButton.setActionCommand(CMD_SEND_DEVICE_INFO);
    sendDeviceInfoButton.addActionListener(this);

    sendButton.setEnabled(false);
    sendButton.setActionCommand(CMD_SEND_SYSEX);
    sendButton.addActionListener(this);

    monitorDeviceCombo.setActionCommand(CMD_MONITOR_DEVICE_CHANGED);
    monitorDeviceCombo.addActionListener(this);
    monitorDeviceInfoButton.setActionCommand(CMD_MONITOR_DEVICE_INFO);
    monitorDeviceInfoButton.addActionListener(this);

    beginMonitorButton.setActionCommand(CMD_MONITOR_BEGIN);
    beginMonitorButton.addActionListener(this);
    endMonitorButton.setActionCommand(CMD_MONITOR_END);
    endMonitorButton.addActionListener(this);
    clearMonitorButton.setActionCommand(CMD_MONITOR_CLEAR);
    clearMonitorButton.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent evt)
  {
    if (evt.getActionCommand().equals(CMD_SEND_DEVICE_CHANGED)) {
      sendDeviceChanged();
    }
    else if (evt.getActionCommand().equals(CMD_SEND_DEVICE_INFO)) {
      deviceInfo(selectedSendMidiDevice);
    }
    else if (evt.getActionCommand().equals(CMD_MONITOR_DEVICE_CHANGED)) {
      monitorDeviceChanged();
    }
    else if (evt.getActionCommand().equals(CMD_MONITOR_DEVICE_INFO)) {
      deviceInfo(selectedMonitorMidiDevice);
    }
    else if (evt.getActionCommand().equals(CMD_SEND_SYSEX)) {
      sendSysEx();
    }
    else if (evt.getActionCommand().equals(CMD_MONITOR_BEGIN)) {
      beginMonitoring();
    }
    else if (evt.getActionCommand().equals(CMD_MONITOR_END)) {
      endMonitoring();
    }
    else if (evt.getActionCommand().equals(CMD_MONITOR_CLEAR)) {
      monitorOutput.setText("");
    }
  }

  private void deviceInfo(MidiDevice device)
  {
    MidiDevice.Info deviceInfo = device.getDeviceInfo();

    StringBuilder info = new StringBuilder();

    info.append("Name: ").append(deviceInfo.getName()).append("\n");
    info.append("Description: ").append(deviceInfo.getDescription()).append("\n");
    info.append("Vendor: ").append(deviceInfo.getVendor()).append("\n");
    info.append("Version: ").append(deviceInfo.getVersion()).append("\n");

    int maxTransmitters = device.getMaxTransmitters();
    int maxReceivers = device.getMaxReceivers();

    info.append("Max Transmitters: ").append(maxTransmitters == -1 ? "unlimited" : maxTransmitters == 0 ? "none" : maxTransmitters).append("\n");
    info.append("Max Receivers: ").append(maxReceivers == -1 ? "unlimited" : maxReceivers == 0 ? "none" : maxReceivers);

    JOptionPane.showMessageDialog(mainPanel, info.toString(), "Device Info", JOptionPane.INFORMATION_MESSAGE);
  }

  private void sendDeviceChanged()
  {
    Object item = sendDeviceCombo.getSelectedItem();
    if (item instanceof MidiDevice.Info) {
      MidiDevice.Info device = (MidiDevice.Info) item;

      try {
        selectedSendMidiDevice = MidiSystem.getMidiDevice(device);

        if (selectedSendMidiDevice.getMaxReceivers() == 0) {
          sendButton.setEnabled(false);
        }
        else {
          sendButton.setEnabled(true);
        }
        sendDeviceInfoButton.setEnabled(true);
      }
      catch (MidiUnavailableException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Exception: " + e.getMessage(), "Exception selecting MIDI Device", JOptionPane.ERROR_MESSAGE);
        sendDeviceCombo.setSelectedIndex(0);
        selectedSendMidiDevice = null;
      }
    }
    else {
      sendButton.setEnabled(false);
      sendDeviceInfoButton.setEnabled(false);
      selectedSendMidiDevice = null;
    }
  }

  private void monitorDeviceChanged()
  {
    if (selectedMonitorMidiDevice != null) {
      endMonitoring();
    }

    Object item = monitorDeviceCombo.getSelectedItem();
    System.out.println("item = " + item);
    if (item instanceof MidiDevice.Info) {
      MidiDevice.Info device = (MidiDevice.Info) item;

      try {
        selectedMonitorMidiDevice = MidiSystem.getMidiDevice(device);

        if (selectedMonitorMidiDevice.getMaxTransmitters() == 0) {
          monitorOutput.setText("Selected device cannot transmit");
          beginMonitorButton.setEnabled(false);
          endMonitorButton.setEnabled(false);
          clearMonitorButton.setEnabled(false);
        }
        else {
          monitorOutput.setText("");
          beginMonitorButton.setEnabled(true);
          endMonitorButton.setEnabled(false);
          clearMonitorButton.setEnabled(true);
        }
        monitorDeviceInfoButton.setEnabled(true);
      }
      catch (MidiUnavailableException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Exception: " + e.getMessage(), "Exception selecting MIDI Device", JOptionPane.ERROR_MESSAGE);
        sendDeviceCombo.setSelectedIndex(0);
        selectedMonitorMidiDevice = null;
      }
    }
    else {
      monitorDeviceInfoButton.setEnabled(false);
      monitorOutput.setText("");
      beginMonitorButton.setEnabled(false);
      endMonitorButton.setEnabled(false);
      clearMonitorButton.setEnabled(false);
    }
  }

  private void sendSysEx()
  {
    if (selectedSendMidiDevice == null) {
      return;
    }

    try {
      selectedSendMidiDevice.open();
      Receiver selectedMidiReceiver = selectedSendMidiDevice.getReceiver();

      SysexMessage sysexMessage = new SysexMessage();

      int radix = hexRadioButton.isSelected() ? 16 : 10;

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      String message = messageField.getText();

      Scanner scanner = new Scanner(message);
      scanner.useDelimiter("[ ,/\r\n|]");

      try {
        while (scanner.hasNext()) {
          int b = scanner.nextInt(radix);
          if (b < 0 || b > 255) {
            JOptionPane.showMessageDialog(null, "Can't understand input. Each byte should be 0-255", "Problem sending message", JOptionPane.ERROR_MESSAGE);
            return;
          }
          baos.write(b);
        }
      }
      catch (RuntimeException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Can't understand input. For hex, it should be e.g. F0 A7 71. For dec, it should be 251 31 12.", "Problem sending message", JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (baos.size() < 1) {
        JOptionPane.showMessageDialog(null, "No message entered!", "Problem sending message", JOptionPane.ERROR_MESSAGE);
        return;
      }

      byte[] data = baos.toByteArray();

      System.out.print("Sending ");
      for (byte b : data) {
        System.out.print(String.format("%02x", b).toUpperCase() + " ");
      }
      System.out.println();

      try {
        sysexMessage.setMessage(data, data.length);
      }
      catch (InvalidMidiDataException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Exception: " + e.getMessage(), "Exception with MIDI Data", JOptionPane.ERROR_MESSAGE);
        return;
      }

      selectedMidiReceiver.send(sysexMessage, -1);
    }
    catch (MidiUnavailableException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Exception: " + e.getMessage(), "Exception opening MIDI Device", JOptionPane.ERROR_MESSAGE);
    }
    finally {
      selectedSendMidiDevice.close();
    }
  }

  private void beginMonitoring()
  {
//    System.err.println("Begin monitoring " + selectedMonitorMidiDevice.getDeviceInfo().getName());
    try {
      selectedMonitorMidiDevice.open();
      selectedMidiTransmitter = selectedMonitorMidiDevice.getTransmitter();

      selectedMidiTransmitter.setReceiver(new MonitorReceiver());
    }
    catch (MidiUnavailableException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Exception: " + e.getMessage(), "Exception opening MIDI Device", JOptionPane.ERROR_MESSAGE);
      return;
    }

    beginMonitorButton.setEnabled(false);
    endMonitorButton.setEnabled(true);
  }

  private void addMonitorOutput(String output)
  {
    final JScrollBar verticalScrollBar = monitorOutputScroller.getVerticalScrollBar();

    // Are we at the bottom already?
    boolean isAtBottom = (verticalScrollBar.getValue() + verticalScrollBar.getVisibleAmount()) >= (verticalScrollBar.getMaximum() - 1); // fudge factor 1 for mac

    if (!monitorOutput.getText().isEmpty()) {
      monitorOutput.append("\n");
    }
    monitorOutput.append(output);

    if (isAtBottom) {
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          monitorOutputScroller.validate();
          verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        }
      });
    }
  }

  private void endMonitoring()
  {
    if (selectedMonitorMidiDevice != null) {
      if (selectedMidiTransmitter != null) {
        selectedMidiTransmitter.close();
        selectedMidiTransmitter = null;
      }
      // Don't close the device in case the user wants to open it again later ("Note that some devices, once closed, cannot be reopened. Attempts to reopen such a device will always result in a MidiUnavailableException.")
//      selectedMonitorMidiDevice.close();
    }

    beginMonitorButton.setEnabled(true);
    endMonitorButton.setEnabled(false);
  }

  private class MonitorReceiver implements Receiver
  {
    @Override
    public void send(MidiMessage message, long timeStamp)
    {
      if (message instanceof SysexMessage) {
        StringBuilder sb = new StringBuilder();

        sb.append("[").append(timeStamp).append("]");

        for (byte b : message.getMessage()) {
          sb.append(" ");
          sb.append(String.format("%02x", b).toUpperCase());
        }

        addMonitorOutput(sb.toString());
      }
    }

    @Override
    public void close()
    {
    }
  }

  public static void main(String[] args)
  {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

      MidiSysEx midiSysEx = new MidiSysEx();
      JFrame frame = new JFrame("Midi SysEx Tool");
      frame.setContentPane(midiSysEx.mainPanel);

      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    }
    catch (Throwable e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Exception: " + e.getMessage(), "Exception in MidiSysEx", JOptionPane.ERROR_MESSAGE);
    }
  }
}
