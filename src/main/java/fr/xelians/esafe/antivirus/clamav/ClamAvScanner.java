/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.antivirus.clamav;

import fr.xelians.esafe.antivirus.AntiVirus;
import fr.xelians.esafe.antivirus.AntiVirusScanner;
import fr.xelians.esafe.antivirus.ScanResult;
import fr.xelians.esafe.antivirus.ScanStatus;
import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/*
 * The daemon works by listening for commands on the sockets specified in clamd.conf. Listening is
 * supported over both unix local sockets and TCP sockets.
 *
 * IMPORTANT: clamd does not currently protect or authenticate traffic coming over the TCP socket,
 * meaning it will accept any and all of the following commands listed from any source. Thus, we
 * strongly recommend following best networking practices when setting up your clamd instance.
 *
 * I.e. don't expose your TCP socket to the Internet.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
public class ClamAvScanner implements AntiVirusScanner {

  private static final int DEFAULT_PORT = 3310;
  private static final int CHUNK_SIZE = 2048;

  private static final byte[] INSTREAM = toBytes("zINSTREAM\0");
  private static final byte[] ZERO = new byte[] {0, 0, 0, 0};
  private static final String FOUND = " FOUND";
  private static final String OK = " OK";

  private final AntiVirusServer[] antiVirusServers;
  private final int timeout;
  private final long scanLength;
  private int count = -1;

  public ClamAvScanner(String[] hosts, int timeout, long scanLength) {
    this.antiVirusServers = createAntiVirusServers(hosts);
    this.timeout = timeout;
    this.scanLength = scanLength;
  }

  @Override
  public AntiVirus getName() {
    return AntiVirus.ClamAV;
  }

  @Override
  public ScanResult scan(Path path) throws IOException {
    AntiVirusServer antiVirusServer = nextAntiVirusServer();
    return antiVirusServer.isLocal
        ? localScan(antiVirusServer.address, path)
        : remoteScan(antiVirusServer.address, path);
  }

  // Choose the antivirus server using a round-robin strategy
  private synchronized AntiVirusServer nextAntiVirusServer() {
    if (++count >= antiVirusServers.length) count = 0;
    return antiVirusServers[count];
  }

  private ScanResult localScan(InetSocketAddress address, Path path) throws IOException {
    try (Socket socket = new Socket()) {
      socket.connect(address, timeout);
      socket.setSoTimeout(timeout);
      Path realPath = path.toRealPath();
      try (OutputStream socketOutputStream = socket.getOutputStream()) {
        socketOutputStream.write(toBytes("zSCAN " + realPath + "\0"));
        socketOutputStream.flush();
        return createResult(socket, realPath + ":");
      }
    }
  }

  private ScanResult remoteScan(InetSocketAddress address, Path path) throws IOException {
    try (Socket socket = new Socket()) {
      socket.connect(address, timeout);
      socket.setSoTimeout(timeout);

      try (OutputStream socketOutputStream = socket.getOutputStream()) {
        socketOutputStream.write(INSTREAM);
        socketOutputStream.flush();

        byte[] buffer = new byte[CHUNK_SIZE];
        try (InputStream fileInputStream = Files.newInputStream(path)) {
          int maxSize = 0;
          int c;
          while ((c = fileInputStream.read(buffer)) != -1) {
            maxSize += c;
            int d = maxSize > scanLength ? (int) (maxSize - scanLength) : 0;
            c = c - d;
            socketOutputStream.write(toBytes(c));
            socketOutputStream.write(buffer, 0, c);
            if (d > 0) break;
          }
          socketOutputStream.write(ZERO);
          socketOutputStream.flush();
        }
        return createResult(socket, "stream:");
      }
    }
  }

  private static ScanResult createResult(Socket socket, String str) throws IOException {
    try (InputStream socketInputStream = socket.getInputStream()) {
      String response = toString(socketInputStream).trim();
      if (response.endsWith(OK)) {
        return ScanResult.OK;
      }
      if (response.endsWith(FOUND)) {
        return new ScanResult(ScanStatus.KO, response.substring(str.length()).trim());
      }
      return new ScanResult(ScanStatus.ERROR, response);
    }
  }

  private static String toString(InputStream is) throws IOException {
    byte[] buffer = is.readNBytes(4096);
    return buffer.length > 0 ? new String(buffer, StandardCharsets.UTF_8) : "";
  }

  private static byte[] toBytes(String str) {
    return str.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] toBytes(int n) {
    return ByteBuffer.allocate(Integer.BYTES).putInt(n).array();
  }

  private static boolean isLocalAddress(String hostname) {
    try {
      InetAddress address = InetAddress.getByName(hostname);
      return address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || isOnNetworkInterface(address);
    } catch (UnknownHostException e) {
      throw new InternalException(
          "Failed to init clamav",
          String.format("Failed to convert host '%s' to address", hostname),
          e);
    }
  }

  // Check if the address exists on any local-interface.
  private static boolean isOnNetworkInterface(InetAddress address) {
    try {
      return NetworkInterface.getByInetAddress(address) != null;
    } catch (SocketException e) {
      return false;
    }
  }

  private static AntiVirusServer[] createAntiVirusServers(String[] hosts) {
    AntiVirusServer[] antiVirusServers = new AntiVirusServer[hosts.length];
    for (int i = 0; i < hosts.length; i++) {
      String[] tokens = StringUtils.split(hosts[i], ':');
      String hostname = tokens[0];
      int port = tokens.length > 1 ? Integer.parseInt(tokens[1]) : DEFAULT_PORT;
      var address = new InetSocketAddress(hostname, port);
      antiVirusServers[i] = new AntiVirusServer(address, isLocalAddress(hostname));
    }
    return antiVirusServers;
  }

  private record AntiVirusServer(InetSocketAddress address, boolean isLocal) {}
}
