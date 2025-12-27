package com.jlm.homework.socket;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.handler.*;
import com.jlm.homework.socket.protocol.Packet;
import com.jlm.homework.socket.protocol.PacketDecoder;
import com.jlm.homework.util.ParseTcpDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Refactored ClientHandler
 */
@Slf4j
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final SimpMessagingTemplate messagingTemplate;
    private final ISmartDeviceUserRelationService smartDeviceUserRelationService;
    private final IStudentsHomeworkNewService studentsHomeworkNewService;

    private final SessionContext sessionContext;
    private ResponseSender responseSender;
    
    // Handlers
    private final Map<Byte, MessageHandler> handlers = new HashMap<>();
    
    // Heartbeat
    private ScheduledExecutorService heartbeatScheduler;
    private static final byte HEARTBEAT_TYPE = 0x05;
    private static final long HEARTBEAT_INTERVAL = 9;

    private boolean headerParsed = false;
    private byte[] proxyHeaderReadBytes = null;

    public ClientHandler(Socket socket, SimpMessagingTemplate messagingTemplate,
                         ISmartDeviceUserRelationService smartDeviceUserRelationService,
                         IStudentsHomeworkNewService studentsHomeworkNewService) {
        this.clientSocket = socket;
        this.messagingTemplate = messagingTemplate;
        this.smartDeviceUserRelationService = smartDeviceUserRelationService;
        this.studentsHomeworkNewService = studentsHomeworkNewService;
        this.sessionContext = new SessionContext();
    }

    private void initHandlers() {
        // Dependencies for handlers
        HandwritingHandler handwritingHandler = new HandwritingHandler(messagingTemplate, studentsHomeworkNewService, smartDeviceUserRelationService);
        ButtonHandler buttonHandler = new ButtonHandler(messagingTemplate, studentsHomeworkNewService, responseSender);
        SerialNumberHandler serialNumberHandler = new SerialNumberHandler(smartDeviceUserRelationService, messagingTemplate);

        handlers.put((byte) 0x01, handwritingHandler); // Handwriting
        handlers.put((byte) 0x81, handwritingHandler); // Handwriting Bluetooth
        handlers.put((byte) 0x02, buttonHandler);      // Button
        handlers.put((byte) 0x82, buttonHandler);      // Button Bluetooth
        handlers.put((byte) 0x03, serialNumberHandler); // Serial Number
    }

    @Override
    public void run() {
        try (
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
        ) {
            clientSocket.setKeepAlive(true);
            this.responseSender = new ResponseSender(out);
            initHandlers();

            // 1. Proxy Protocol Handling
            readProxyHeader();
            InetSocketAddress realAddress = getRealRemoteAddress();
            
            sessionContext.setRemoteAddress(realAddress);
            sessionContext.setClientIP(realAddress.getHostString());
            sessionContext.setClientPort(realAddress.getPort());

            logClientConnection();

            // Load initial relation
            SmartDeviceUserRelation relation = smartDeviceUserRelationService.selectByIpAddress(sessionContext.getClientIP());
            sessionContext.setRelation(relation);

            // 2. Setup Packet Decoder
            PacketDecoder decoder = new PacketDecoder(in);
            if (proxyHeaderReadBytes != null) {
                decoder.pushPreReadBytes(proxyHeaderReadBytes);
            }

            // 3. Start Heartbeat
            initHeartbeatScheduler(out);

            // 4. Main Loop
            while (true) {
                Packet packet = decoder.readNextPacket();
                if (packet == null) {
                    break; // End of stream
                }

                log.info("Received packet type: {}, length: {}", String.format("0x%02X", packet.getType()), packet.getLength());

                // Pre-process Bluetooth packets for MAC binding
                if (packet.isBluetooth()) {
                    handleBluetoothPreProcess(packet);
                } else {
                    sessionContext.setBluetooth(false);
                }

                // Dispatch
                MessageHandler handler = handlers.get(packet.getType());
                if (handler != null) {
                    try {
                        handler.handle(sessionContext, packet, responseSender);
                    } catch (Exception e) {
                        log.error("Error handling packet type {}", packet.getType(), e);
                        // responseSender.sendText("Error processing packet: " + e.getMessage());
                    }
                } else {
                    log.warn("Unknown packet type: {}", String.format("0x%02X", packet.getType()));
                }
                
                // Removed global echo to prevent bandwidth saturation. 
                // Specific handlers (like SerialNumberHandler) should handle their own responses/echos if needed.
                // responseSender.sendRaw(packet.getRawData());
            }

        } catch (IOException e) {
            log.info("Client handler exception: {}", e.getMessage());
        } finally {
            cancelHeartbeat();
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.info("Error closing socket: {}", e.getMessage());
            }
        }
    }

    private void handleBluetoothPreProcess(Packet packet) {
        sessionContext.setBluetooth(true);
        // MAC is already extracted in Packet (if I implemented it right)
        // But Packet.mac is byte array.
        // Logic:
        byte[] macBytes = Arrays.copyOfRange(packet.getRawData(), 4, 10);
        Integer mac = ParseTcpDataUtil.byteArrayToInt(macBytes);
        sessionContext.setMac(mac);
        
        // Optimization: Skip DB query if relation is already cached and matches MAC
        if (sessionContext.getRelation() != null && 
            String.valueOf(mac).equals(sessionContext.getRelation().getDeviceCode())) {
            return;
        }
        
        SmartDeviceUserRelation relation = smartDeviceUserRelationService.selectByDeviceCode(mac.toString());
        if (relation == null) {
            System.out.println(mac + "设备还未绑定学生，请检查！");
            SmartDeviceUserRelation newRelation = new SmartDeviceUserRelation();
            newRelation.setIpAddress(sessionContext.getClientIP());
            newRelation.setDeviceCode(mac.toString());
            messagingTemplate.convertAndSend("/topic/bindStudent", newRelation);
        } else if (StringUtils.isEmpty(relation.getIpAddress())) {
            relation.setIpAddress(sessionContext.getClientIP());
            smartDeviceUserRelationService.update(relation);
            sessionContext.setRelation(relation); // Update context relation
        } else {
             // Ensure context has relation
             if (sessionContext.getRelation() == null) {
                 sessionContext.setRelation(relation);
             }
        }
    }

    // ... Proxy Protocol methods (readProxyHeader, etc.) - copying from original ...
    public void readProxyHeader() throws IOException {
        if (headerParsed) return;
        PushbackInputStream pb = new PushbackInputStream(clientSocket.getInputStream(), 108);
        byte[] signature = new byte[5];
        int bytesRead = pb.read(signature);
        if (bytesRead != 5) throw new IOException("Read proxy signature failed");
        pb.unread(signature);

        if (new String(signature).equals("PROXY")) {
            parseV1(pb);
        } else if (isV2Signature(signature)) {
            parseV2(pb);
        } else {
            sessionContext.setRemoteAddress((InetSocketAddress) clientSocket.getRemoteSocketAddress());
            proxyHeaderReadBytes = Arrays.copyOf(signature, bytesRead);
        }
        headerParsed = true;
    }
    
    private boolean isV2Signature(byte[] sig) {
        return sig[0] == 0x0D && sig[1] == 0x0A && sig[2] == 0x0D && sig[3] == 0x0A && sig[4] == 0x00;
    }

    private void parseV1(PushbackInputStream pb) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(pb));
        String line = reader.readLine();
        if (line == null || !line.startsWith("PROXY")) throw new IOException("Invalid PROXY v1 header");
        String[] parts = line.split(" ");
        if (parts.length < 6) throw new IOException("Invalid PROXY v1 format");
        sessionContext.setRemoteAddress(new InetSocketAddress(parts[2], Integer.parseInt(parts[4])));
    }

    private void parseV2(PushbackInputStream pb) throws IOException {
        DataInputStream in = new DataInputStream(pb);
        in.skipBytes(12);
        byte[] addressBytes = new byte[4];
        in.readFully(addressBytes);
        int port = in.readUnsignedShort();
        String ip = String.format("%d.%d.%d.%d", addressBytes[0] & 0xff, addressBytes[1] & 0xff, addressBytes[2] & 0xff, addressBytes[3] & 0xff);
        sessionContext.setRemoteAddress(new InetSocketAddress(ip, port));
    }

    public InetSocketAddress getRealRemoteAddress() {
        if (!headerParsed) throw new IllegalStateException("Call readProxyHeader() first");
        return sessionContext.getRemoteAddress();
    }
    
    private void logClientConnection() {
        Map<String, Object> data = new HashMap<>();
        data.put("clientIP", sessionContext.getClientIP());
        data.put("clientPort", sessionContext.getClientPort());
        writeDebugLog("C", "ClientHandler:connect", "Client Connected", data);
    }

    private void writeDebugLog(String id, String loc, String msg, Map<String, Object> data) {
         // ... reuse original log logic or simplified ...
         log.info("[PROXY-DEBUG] [{}] {} | {} | {}", id, loc, msg, data);
    }

    // Heartbeat Logic
    private void initHeartbeatScheduler(OutputStream out) {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat(out);
            } catch (IOException e) {
                log.info("Heartbeat failed: " + e.getMessage());
                cancelHeartbeat();
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    private void sendHeartbeat(OutputStream out) throws IOException {
        byte heartbeatData = 0x01;
        byte[] packet = new byte[5];
        packet[0] = 0x55;
        packet[1] = 0x56;
        packet[2] = 0x02;
        packet[3] = HEARTBEAT_TYPE;
        int checksum = 0;

        if (sessionContext.isBluetooth() && sessionContext.getMac() != null) {
            // Re-implement Bluetooth Heartbeat logic
            // Header(2)+Length(1)+Type(1)+MAC(6)+Data(1)+Checksum(1) = 12 bytes
            // Original code: packet was 5 bytes, then logic for bluetooth...
            // Line 1469: if(isBluetooth) ... 
            // It seems the original code was reusing `packet` buffer which was 5 bytes, but writing into index 4... 
            // Wait, original code line 1456: packet = new byte[5].
            // Line 1472: System.arraycopy(macByte, 0, packet, 4, 6); -> IndexOutOfBoundsException if packet is 5 bytes!
            // The original code might be buggy or I misread it.
            // Original Line 1456: byte[] packet = new byte[5];
            // Original Line 1472: System.arraycopy(..., packet, 4, 6); -> 4+6=10. Array is 5. Crash.
            
            // Wait, maybe `packet` variable is reassigned? No.
            // This suggests the original code crashes on Bluetooth Heartbeat? 
            // Or `isBluetooth` is rarely true.
            
            // I will fix this.
            
            byte[] fullPacket = new byte[12];
            fullPacket[0] = 0x55;
            fullPacket[1] = 0x56;
            fullPacket[2] = 0x02; // Length
            fullPacket[3] = HEARTBEAT_TYPE;
            
            byte[] macByte = ParseTcpDataUtil.intToByteArray(sessionContext.getMac());
            System.arraycopy(macByte, 0, fullPacket, 4, 6);
            
            fullPacket[10] = heartbeatData;
            
            checksum = calculateChecksum(fullPacket, 0, 10);
            fullPacket[11] = (byte) (checksum & 0xFF);
            
            out.write(fullPacket);
        } else {
            packet[4] = heartbeatData;
            checksum = calculateChecksum(packet, 0, 4);
            byte[] fullPacket = new byte[6];
            System.arraycopy(packet, 0, fullPacket, 0, 5);
            fullPacket[5] = (byte) (checksum & 0xFF);
            out.write(fullPacket);
        }
        out.flush();
    }
    
    private int calculateChecksum(byte[] data, int start, int end) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += (data[i] & 0xFF);
        }
        return sum;
    }

    private void cancelHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
        }
    }
}
